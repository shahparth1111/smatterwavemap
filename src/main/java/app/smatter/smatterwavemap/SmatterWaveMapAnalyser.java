package app.smatter.smatterwavemap;

/*
 * SmatterWaveMapAnalyzer.java
 *
 * Safe, defensive tool:
 * - parse HackRF sweep CSV (timestamp,freq_hz,power_db)
 * - compute audio spectrogram (STFT) for an mp3/wav file
 * - resample/align both RF and audio into time-frequency matrices
 * - compute normalized overlap % between RF spectrogram and audio spectrogram
 *
 * OUTPUT:
 * - summary overlap percent printed to stdout
 * - per-time-slice overlap CSV: <outPrefix>_overlap_timeseries.csv
 * - matched RF & audio spectrogram snapshots saved as CSV for forensic review
 *
 * NOTE: This tool only computes statistical correlations. It DOES NOT attempt to
 * convert RF power into physiological effects or provide any parameters to create them.
 *
 * Usage example:
 *   mvn compile
 *   mvn exec:java -Dexec.mainClass=SmatterWaveMapAnalyzer -Dexec.args="--rf rf_sweeps.csv --audio clip.mp3 --out result"
 *
 */

import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.TarsosDSPAudioFloatConverter;
 
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;

import org.jtransforms.fft.DoubleFFT_1D;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.text.*;
import java.util.*;
import java.util.stream.Collectors;

public class SmatterWaveMapAnalyser {

    // ---- CONFIG ----------------------------------------------------------------
    // STFT window and hop for audio spectrogram
    private static final int AUDIO_SAMPLE_RATE = 44100;  // we'll request this when decoding
    private static final int AUDIO_FFT_SIZE = 2048;      // power-of-two
    private static final int AUDIO_HOP = AUDIO_FFT_SIZE / 4; // 75% overlap

    // RF: we will aggregate sweeps into uniform time bins (seconds or fraction)
    private static final double RF_TIMEBIN_SECONDS = 0.25; // adjust as needed; smaller = higher time resolution

    // Output formatting
    private static final DateFormat ISO = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");

    static { ISO.setTimeZone(TimeZone.getTimeZone("UTC")); }

    // ---- DATA CONTAINERS -------------------------------------------------------
    // RF entry: one row of sweep (timestamp, freqHz, powerDb)
    static class RfRow { long tsMillis; double freqHz; double powerDb; }
    // Spectrogram container
    static class Spectrogram {
        double[][] mag;    // time x freqbin magnitudes (non-negative)
        double[] freqs;    // center freq for each column
        double[] times;    // center time (seconds since epoch or relative)
    }

    // -------------------------- main -------------------------------------------
    public static void main(String[] args) throws Exception {
        Map<String,String> arg = parseArgs(args);
        if (!arg.containsKey("--rf") || !arg.containsKey("--audio") || !arg.containsKey("--out")) {
            System.err.println("Usage: --rf <rf_csv> --audio <audiofile> --out <outPrefix>");
            System.exit(2);
        }

        Path rfCsv = Paths.get(arg.get("--rf"));
        Path audioFile = Paths.get(arg.get("--audio"));
        String outPrefix = arg.get("--out");

        System.out.println("Reading RF CSV: " + rfCsv);
        List<RfRow> rfRows = parseRfCsv(rfCsv);
        System.out.println("RF rows: " + rfRows.size());

        System.out.println("Computing RF spectrogram (time x freq) ...");
        Spectrogram rfSpec = buildRfSpectrogram(rfRows, RF_TIMEBIN_SECONDS);

        System.out.println("Computing audio spectrogram (STFT) ...");
        Spectrogram audioSpec = computeAudioSpectrogram(audioFile.toFile(), AUDIO_FFT_SIZE, AUDIO_HOP);

        System.out.println("Resampling / aligning spectrograms ...");
        // Align time ranges and resample frequencies: we will map columns by interpolation
        ResampledPair rp = resampleAndAlign(rfSpec, audioSpec);

        System.out.println("Computing overlap percent ...");
        double overlapPercent = computeOverlapPercent(rp.rfAligned, rp.audioAligned);
        System.out.printf("SUMMARY OVERLAP PERCENT = %.3f %%\n", overlapPercent * 100.0);

        // Write per-time overlap CSV and save spectrograms for review
        Path outTimes = Paths.get(outPrefix + "_overlap_timeseries.csv");
        saveOverlapTimeseries(rp.rfAligned, rp.audioAligned, outTimes);

        Path rfOut = Paths.get(outPrefix + "_rf_spectrogram.csv");
        Path audioOut = Paths.get(outPrefix + "_audio_spectrogram.csv");
        saveSpectrogramCsv(rp.rfAligned, rfOut);
        saveSpectrogramCsv(rp.audioAligned, audioOut);

        System.out.println("Wrote outputs: " + outTimes + ", " + rfOut + ", " + audioOut);
        System.out.println("Done.");
    }

    // -------------------------- helpers ----------------------------------------
    private static Map<String,String> parseArgs(String[] args) {
        Map<String,String> m = new HashMap<>();
        for (int i=0;i<args.length-1;i+=2) {
            m.put(args[i], args[i+1]);
        }
        return m;
    }

    // ---------------- RF parsing ------------------------------------------------
    private static List<RfRow> parseRfCsv(Path csv) throws IOException, ParseException {
        List<RfRow> rows = new ArrayList<>();
        Reader r = Files.newBufferedReader(csv, StandardCharsets.UTF_8);
        CSVParser parser = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(r);
        for (CSVRecord rec : parser) {
            // Expect columns: timestamp_iso, freq_hz, power_db  (header names tolerant)
            String tsS = findField(rec, Arrays.asList("timestamp","time","ts","timestamp_iso"));
            String fS = findField(rec, Arrays.asList("freq","freq_hz","frequency","frequency_hz"));
            String pS = findField(rec, Arrays.asList("power","power_db","db","dB","amplitude"));

            if (tsS == null || fS == null || pS == null) {
                // try positional fallback: first three columns
                if (rec.size() < 3) continue;
                tsS = rec.get(0);
                fS = rec.get(1);
                pS = rec.get(2);
            }

            // parse timestamp tolerant: if numeric assume epoch millis or seconds
            long tsMillis;
            try {
                tsMillis = parseTimestampToMillis(tsS);
            } catch (Exception ex) {
                // skip row if no parse
                continue;
            }

            double freq = Double.parseDouble(fS);
            double p = Double.parseDouble(pS);
            RfRow rr = new RfRow();
            rr.tsMillis = tsMillis;
            rr.freqHz = freq;
            rr.powerDb = p;
            rows.add(rr);
        }
        parser.close();
        // sort by time
        rows.sort(Comparator.comparingLong(rw -> rw.tsMillis));
        return rows;
    }

    private static String findField(CSVRecord rec, List<String> names) {
        for (String n : names) {
            for (String h : rec.getParser().getHeaderNames()) {
                if (h == null) continue;
                if (h.toLowerCase().contains(n.toLowerCase())) {
                    return rec.get(h);
                }
            }
        }
        return null;
    }

    private static long parseTimestampToMillis(String s) throws ParseException {
        s = s.trim();
        // try numeric epoch (seconds or millis)
        if (s.matches("^\\d+$")) {
            try {
                long v = Long.parseLong(s);
                if (v > 1000000000000L) return v; // millis
                return v * 1000L; // seconds -> millis
            } catch (NumberFormatException ignored) {}
        }
        // try ISO parse via SimpleDateFormat
        try {
            Date d = ISO.parse(s);
            return d.getTime();
        } catch (Exception e) {
            // try fallback ISO without millis
            SimpleDateFormat f2 = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX");
            f2.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date d2 = f2.parse(s);
            return d2.getTime();
        }
    }

    // ---------------- RF spectrogram builder -----------------------------------
    private static Spectrogram buildRfSpectrogram(List<RfRow> rows, double timeBinSec) {
        if (rows.isEmpty()) throw new IllegalArgumentException("No RF rows");

        long start = rows.get(0).tsMillis;
        long end = rows.get(rows.size()-1).tsMillis;
        int bins = (int) Math.ceil((end - start) / 1000.0 / timeBinSec) + 1;

        // collect all unique frequency values sorted
        double[] uniqueFreqs = rows.stream().mapToDouble(r->r.freqHz).distinct().sorted().toArray();

        double[] times = new double[bins];
        for (int i=0;i<bins;i++) times[i] = (start/1000.0) + i * timeBinSec;

        double[][] mag = new double[bins][uniqueFreqs.length];
        // Fill with -inf default in dB -> convert to linear
        for (double[] row : mag) Arrays.fill(row, 0.0);

        // Aggregate: for each rf row, find time bin and freq column
        Map<Double,Integer> freqIndex = new HashMap<>();
        for (int i=0;i<uniqueFreqs.length;i++) freqIndex.put(uniqueFreqs[i], i);

        for (RfRow r : rows) {
            int tbin = (int) Math.floor((r.tsMillis - start) / 1000.0 / timeBinSec);
            if (tbin < 0) tbin = 0;
            if (tbin >= bins) tbin = bins - 1;
            Integer fi = freqIndex.get(r.freqHz);
            if (fi == null) {
                // find nearest freq index
                double freq = r.freqHz;
                int nearest = 0; double mind = Math.abs(freq - uniqueFreqs[0]);
                for (int i=1;i<uniqueFreqs.length;i++){
                    double d = Math.abs(freq - uniqueFreqs[i]);
                    if (d < mind) { mind=d; nearest=i; }
                }
                fi = nearest;
            }
            // convert dB to linear power
            double powerLin = Math.pow(10.0, r.powerDb / 10.0);
            mag[tbin][fi] += powerLin;
        }

        Spectrogram s = new Spectrogram();
        s.freqs = uniqueFreqs;
        s.times = times;
        s.mag = mag;
        return s;
    }

    // ---------------- Audio spectrogram via TarsosDSP --------------------------
    private static Spectrogram computeAudioSpectrogram(File audioFile, int fftSize, int hop) throws Exception {
        // Use Tarsos to read and resample audio to AUDIO_SAMPLE_RATE if necessary
        // We'll produce STFT frames using Tarsos's dispatcher with float[] windows
        final List<double[]> frames = new ArrayList<>();
        final int sampleRate = AUDIO_SAMPLE_RATE; // target
        final FFT tarsosFft = new FFT(fftSize);
        final double[] window = hanningWindow(fftSize);

        // Create dispatcher
        AudioDispatcher dispatcher = AudioDispatcherFactory.fromFile(audioFile, fftSize, hop);
        final int actualSampleRate = Integer.parseInt((dispatcher.getFormat().getSampleRate() <= 0 ? sampleRate : dispatcher.getFormat().getSampleRate())+"");

        // if sample rates differ, dispatcher will handle resampling internally (Tarsos does limited resampling)
        final double secPerFrame = (double) hop / (double) actualSampleRate;
        final List<Double> times = new ArrayList<>();

        dispatcher.addAudioProcessor(new AudioProcessor() {
            long frameIndex = 0;
            @Override
            public boolean process(AudioEvent audioEvent) {
                float[] buffer = audioEvent.getFloatBuffer();
                double[] real = new double[fftSize];
                double[] imag = new double[fftSize];
                // copy with zero-pad/truncate to fftSize
                int copy = Math.min(buffer.length, fftSize);
                for (int i=0;i<copy;i++) real[i] = buffer[i] * window[i];
                for (int i=copy;i<fftSize;i++) real[i] = 0.0;
                // compute FFT using Tarsos's FFT wrapper
                float[] floatReal = new float[fftSize*2];
                for (int i=0;i<fftSize;i++) { floatReal[2*i] = (float) real[i]; floatReal[2*i+1] = 0f; }
                tarsosFft.forwardTransform(floatReal);
                tarsosFft.modulus(floatReal, floatReal);
                double[] mags = new double[fftSize/2];
                for (int i=0;i<mags.length;i++) mags[i] = Math.max(0.0, floatReal[i]); // positive magnitude
                frames.add(mags);
                times.add(frameIndex * secPerFrame + (audioEvent.getTimeStamp())); // timestamp relative to file start
                frameIndex++;
                return true;
            }
            @Override public void processingFinished() { }
        });

        dispatcher.run(); // blocking

        if (frames.isEmpty()) throw new IOException("No audio frames processed.");

        int tcount = frames.size();
        int fcount = frames.get(0).length;
        double[][] mag = new double[tcount][fcount];
        for (int t=0;t<tcount;t++){
            double[] row = frames.get(t);
            System.arraycopy(row,0,mag[t],0,fcount);
        }

        // compute frequency centers for audio bins
        double[] freqs = new double[fcount];
        for (int i=0;i<fcount;i++) freqs[i] = (double)i * ((double)actualSampleRate / (double)fftSize);

        double[] timesArr = new double[tcount];
        for (int i=0;i<tcount;i++) timesArr[i] = times.get(i);

        Spectrogram s = new Spectrogram();
        s.freqs = freqs;
        s.times = timesArr;
        s.mag = mag;
        return s;
    }

    // ---------------- Resample/align ------------------------------------------
    // We will map both spectrograms onto a common time grid (union of time ranges with step = min bin)
    private static class ResampledPair { Spectrogram rfAligned, audioAligned; }

    private static ResampledPair resampleAndAlign(Spectrogram rf, Spectrogram audio) {
        // common time range: intersection of both
        double startT = Math.max(rf.times[0], audio.times[0]);
        double endT = Math.min(rf.times[rf.times.length-1], audio.times[audio.times.length-1]);
        if (endT <= startT) {
            // if no intersection, fallback to union and allow zeros
            startT = Math.min(rf.times[0], audio.times[0]);
            endT = Math.max(rf.times[rf.times.length-1], audio.times[audio.times.length-1]);
        }

        // choose time step = min of audio frame step and rf time bin step
        double audioStep = audio.times.length > 1 ? audio.times[1]-audio.times[0] : 0.05;
        double rfStep = rf.times.length > 1 ? rf.times[1]-rf.times[0] : RF_TIMEBIN_SECONDS;
        double step = Math.min(audioStep, rfStep);

        int tcount = (int) Math.ceil((endT - startT) / step) + 1;
        double[] times = new double[tcount];
        for (int i=0;i<tcount;i++) times[i] = startT + i * step;

        // We'll resample both spectrograms (time interpolation) into this grid.
        Spectrogram rfA = new Spectrogram();
        Spectrogram audioA = new Spectrogram();

        // Frequencies: we'll choose a common freq axis by union of both ranges with N columns = max columns
        // Simpler approach: resample both to same number of freq columns = max(columns) and map by log-frequency scaling
        int fcols = Math.max(rf.freqs.length, audio.freqs.length);
        double fmin = Math.max(20.0, Math.min(rf.freqs[0], audio.freqs[0])); // avoid DC
        double fmax = Math.max(rf.freqs[rf.freqs.length-1], audio.freqs[audio.freqs.length-1]);
        double[] freqs = new double[fcols];
        // use linear spacing (you may prefer log spacing)
        for (int i=0;i<fcols;i++) freqs[i] = fmin + (fmax - fmin) * ((double)i / (double)(fcols - 1));

        rfA.freqs = freqs;
        audioA.freqs = freqs;
        rfA.times = times;
        audioA.times = times;
        rfA.mag = new double[tcount][fcols];
        audioA.mag = new double[tcount][fcols];

        // Fill by interpolation: for each target time and freq, sample from original spectrogram using nearest-neighbor or linear interpolation
        for (int ti=0; ti<tcount; ti++) {
            double tt = times[ti];
            // RF: time interpolation
            int rfTidx = Arrays.binarySearch(rf.times, tt);
            if (rfTidx < 0) rfTidx = -rfTidx - 2;
            double rfTfrac = 0;
            if (rfTidx < 0) { rfTidx = 0; rfTfrac = 0; }
            else if (rfTidx >= rf.times.length-1) { rfTidx = rf.times.length -1; rfTfrac = 0;}
            else {
                double t0 = rf.times[rfTidx], t1 = rf.times[rfTidx+1];
                rfTfrac = (tt - t0) / (t1 - t0);
            }

            int audTidx = Arrays.binarySearch(audio.times, tt);
            if (audTidx < 0) audTidx = -audTidx - 2;
            double audTfrac = 0;
            if (audTidx < 0) { audTidx = 0; audTfrac = 0; }
            else if (audTidx >= audio.times.length-1) { audTidx = audio.times.length -1; audTfrac = 0;}
            else {
                double t0 = audio.times[audTidx], t1 = audio.times[audTidx+1];
                audTfrac = (tt - t0) / (t1 - t0);
            }

            // For each freq column, interpolate in original freq axes
            for (int fi=0; fi<fcols; fi++) {
                double targetF = freqs[fi];
                // RF freq interpolation
                double rfVal = sampleSpectrogramValue(rf, rfTidx, rfTfrac, targetF);
                double audVal = sampleSpectrogramValue(audio, audTidx, audTfrac, targetF);
                rfA.mag[ti][fi] = rfVal;
                audioA.mag[ti][fi] = audVal;
            }
        }

        ResampledPair rp = new ResampledPair();
        rp.rfAligned = rfA;
        rp.audioAligned = audioA;
        return rp;
    }

    // sampleSpectrogramValue: linear time interpolation between row idx and idx+1, and linear freq interpolation between nearest freq bins
    private static double sampleSpectrogramValue(Spectrogram s, int tidx, double tfrac, double targetF) {
        if (s == null || s.times == null || s.freqs == null) return 0.0;
        // time row values
        int t0 = Math.max(0, Math.min(tidx, s.mag.length-1));
        int t1 = Math.min(s.mag.length-1, t0 + 1);
        double[] row0 = s.mag[t0];
        double[] row1 = s.mag[t1];

        // freq interpolation on each row
        double v0 = interpFreq(row0, s.freqs, targetF);
        double v1 = interpFreq(row1, s.freqs, targetF);
        return v0 * (1.0 - tfrac) + v1 * tfrac;
    }

    private static double interpFreq(double[] row, double[] freqs, double f) {
        if (freqs == null || freqs.length == 0) return 0.0;
        if (f <= freqs[0]) return row[0];
        if (f >= freqs[freqs.length-1]) return row[freqs.length-1];
        int idx = Arrays.binarySearch(freqs, f);
        if (idx >= 0) return row[idx];
        idx = -idx - 2;
        int idx1 = idx + 1;
        double f0 = freqs[idx], f1 = freqs[idx1];
        double v0 = row[idx], v1 = row[idx1];
        double frac = (f - f0) / (f1 - f0);
        return v0 * (1.0 - frac) + v1 * frac;
    }

    // ---------------- Overlap computation -------------------------------------
    // We'll compute per-time cosine-similarity between RF and audio magnitude vectors,
    // then aggregate into a single overlap percent (0..1).
    private static double computeOverlapPercent(Spectrogram rf, Spectrogram audio) {
        if (rf.times.length != audio.times.length || rf.freqs.length != audio.freqs.length) {
            throw new IllegalStateException("Aligned spectrograms must match sizes");
        }
        int T = rf.times.length;
        double sumSim = 0.0;
        int count = 0;
        for (int t=0;t<T;t++){
            double sim = cosineSimilarity(rf.mag[t], audio.mag[t]);
            if (!Double.isNaN(sim)) {
                sumSim += sim;
                count++;
            }
        }
        if (count == 0) return 0.0;
        double avgSim = sumSim / count; // -1..1 theoretically, but mags >= 0 => [0..1]
        // map to percent: average similarity * 100
        return Math.max(0.0, Math.min(1.0, avgSim));
    }

    private static double cosineSimilarity(double[] a, double[] b) {
        if (a.length != b.length) return Double.NaN;
        double dot = 0.0, na = 0.0, nb = 0.0;
        for (int i=0;i<a.length;i++) { dot += a[i] * b[i]; na += a[i]*a[i]; nb += b[i]*b[i]; }
        if (na <= 0 || nb <= 0) return 0.0;
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }

    // ---------------- save outputs -------------------------------------------
    private static void saveOverlapTimeseries(Spectrogram rf, Spectrogram audio, Path outCsv) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(outCsv, StandardCharsets.UTF_8)) {
            bw.write("time_iso,similarity\n");
            for (int t=0;t<rf.times.length;t++) {
                double sim = cosineSimilarity(rf.mag[t], audio.mag[t]);
                String ts = ISO.format(new Date((long)(rf.times[t]*1000.0)));
                bw.write(String.format(Locale.ROOT, "%s,%.6f\n", ts, sim));
            }
        }
    }

    private static void saveSpectrogramCsv(Spectrogram s, Path out) throws IOException {
        try (BufferedWriter bw = Files.newBufferedWriter(out, StandardCharsets.UTF_8)) {
            // header: time, freq1, freq2, ...
            StringBuilder hdr = new StringBuilder("time_iso");
            for (double f : s.freqs) hdr.append(",").append(String.format(Locale.ROOT,"%.3f", f));
            bw.write(hdr.toString() + "\n");
            for (int t=0;t<s.times.length;t++) {
                StringBuilder line = new StringBuilder();
                line.append(ISO.format(new Date((long)(s.times[t]*1000.0))));
                for (int fi=0; fi<s.freqs.length; fi++) {
                    line.append(",").append(String.format(Locale.ROOT, "%.9f", s.mag[t][fi]));
                }
                bw.write(line.toString() + "\n");
            }
        }
    }

    // ---------------- utility functions --------------------------------------
    private static double[] hanningWindow(int n) {
        double[] w = new double[n];
        for (int i=0;i<n;i++) w[i] = 0.5 * (1.0 - Math.cos(2.0 * Math.PI * i / (n - 1)));
        return w;
    }
}