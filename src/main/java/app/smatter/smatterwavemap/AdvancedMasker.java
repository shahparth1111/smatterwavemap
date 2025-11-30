package app.smatter.smatterwavemap;

/* AdvancedMasker.java
Pure-Java advanced audio masker for perceptual masking (playback-only).
- Multi-band bank (default 24 bands across audible range)
- Per-band stochastic envelopes
- Brown/white/pink noise blend
- Binaural mode (stereo) with independent envelopes
- Spectral walkers (bands drift slowly)
- Aggressive masking mode: emphasizes 1-6 kHz speech band + transients
- Live GUI controls + save-to-WAV option
- No RF transmission. Audio-only. Use volume safety.
*/

import javax.sound.sampled.*;
import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.ArrayList;

public class AdvancedMasker {

 // Config defaults
 private static float SAMPLE_RATE = 48000f; // try 96000f if supported
 private static final int SAMPLE_BITS = 16;
 private static final int CHANNELS_MONO = 1;
 private static final int CHANNELS_STEREO = 2;
 private static final boolean SIGNED = true;
 private static final boolean BIG_ENDIAN = false;

 // Band settings
 private static int NUM_BANDS = 24; // default; GUI allows changing (between 6 and 48)
 private static final double MIN_FREQ = 50.0;
 private static final double MAX_FREQ = 22000.0;

 // Envelope durations (ms)
 private static final int MIN_ENV_MS = 50;
 private static final int MAX_ENV_MS = 900;

 // Brown/white/pink mix
 private static double BROWN_LEVEL = 0.45;
 private static double PINK_LEVEL = 0.20;
 private static double WHITE_LEVEL = 0.35;

 // Master gain
 private static double MASTER_GAIN = 0.3;

 // Aggressive masking toggles
 private static volatile boolean AGGRESSIVE = false;
 private static volatile boolean BINAURAL = false;
 private static volatile boolean RECORD_TO_WAV = false;

 private static SourceDataLine line;
 private static AudioFormat format;
 private static AtomicBoolean running = new AtomicBoolean(true);
 private static final float USER_GAIN = 2.5f;     // Final amplification of output audio
 private static final float NOISE_LEVEL = 0.35f;  // Noise injection amount (0 to 1)
 private static final float MASK_COMPLEXITY = 0.45f; // Controls the "chaotic" masking

 public static void main(String[] args) throws Exception {
     // Try a high sample rate; fallback if unsupported
     float[] tryRates = new float[]{96000f, 48000f, 44100f};
     boolean openOk = false;
     for (float r : tryRates) {
         if (attemptOpenLine(r)) { SAMPLE_RATE = r; openOk = true; break; }
     }
     if (!openOk) {
         System.err.println("No supported audio format found. Exiting.");
         return;
     }

     // Build engine
     MaskEngine engine = new MaskEngine((int) SAMPLE_RATE);
     // GUI in EDT
     SwingUtilities.invokeLater(() -> createAndShowGUI(engine));

     // Playback thread
     int bufferSamples = 4096;
     int channels = BINAURAL ? CHANNELS_STEREO : CHANNELS_MONO;
     byte[] outBuf = new byte[bufferSamples * (SAMPLE_BITS/8) * channels];

     // WAV recording stream if enabled
     ByteArrayOutputStream wavStore = new ByteArrayOutputStream();

     System.out.println("AdvancedMasker running at " + SAMPLE_RATE + " Hz. Binaural: " + BINAURAL);
     System.out.println("Press Stop in UI or Ctrl+C to stop. Keep MASTER_GAIN modest.");

     while (running.get()) {
         float[][] block = engine.renderBlock(bufferSamples, BINAURAL);
         // convert to bytes (stereo interleaved if binaural)
         int idx = 0;
         for (int i = 0; i < bufferSamples; i++) {
             if (BINAURAL) {
                 // left
                 short lv = floatToPCM(block[0][i]);
                 outBuf[idx++] = (byte) (lv & 0xFF);
                 outBuf[idx++] = (byte) ((lv >> 8) & 0xFF);
                 // right
                 short rv = floatToPCM(block[1][i]);
                 outBuf[idx++] = (byte) (rv & 0xFF);
                 outBuf[idx++] = (byte) ((rv >> 8) & 0xFF);
             } else {
                 short v = floatToPCM(block[0][i]);
                 outBuf[idx++] = (byte) (v & 0xFF);
                 outBuf[idx++] = (byte) ((v >> 8) & 0xFF);
             }
         }
         line.write(outBuf, 0, idx);
         if (RECORD_TO_WAV) {
             wavStore.write(outBuf, 0, idx);
         }
     }

     // cleanup
     line.drain();
     line.stop();
     line.close();

     if (RECORD_TO_WAV && wavStore.size() > 0) {
         // write WAV file
         saveWavFile(wavStore.toByteArray(), BINAURAL ? CHANNELS_STEREO : CHANNELS_MONO, (int) SAMPLE_RATE, "mask_output.wav");
         System.out.println("Saved mask_output.wav (" + wavStore.size() + " bytes).");
     }

     System.out.println("AdvancedMasker stopped.");
     System.exit(0);
 }

 // Attempt to open line at given rate; clones to set global line
 private static boolean attemptOpenLine(float rate) {
     try {
         format = new AudioFormat(rate, SAMPLE_BITS, CHANNELS_STEREO, SIGNED, BIG_ENDIAN);
         DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
         if (!AudioSystem.isLineSupported(info)) {
             // try mono
             format = new AudioFormat(rate, SAMPLE_BITS, CHANNELS_MONO, SIGNED, BIG_ENDIAN);
             info = new DataLine.Info(SourceDataLine.class, format);
             if (!AudioSystem.isLineSupported(info)) return false;
         }
         line = (SourceDataLine) AudioSystem.getLine(info);
         line.open(format, 8192 * 4);
         line.start();
         return true;
     } catch (Exception e) {
         return false;
     }
 }

 // Convert float [-1..1] to 16-bit PCM short with soft limiter and master gain
 private static short floatToPCM(float f) {
     double s = f * MASTER_GAIN;
     // soft clip (tanh-style gentle limiter)
     s = Math.tanh(1.1 * s);
     return (short) Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, (int) (s * Short.MAX_VALUE)));
 }

 // Save bytes as WAV file (PCM 16-bit little endian)
 private static void saveWavFile(byte[] pcmData, int channels, int sampleRate, String filename) {
     try {
         ByteArrayInputStream bais = new ByteArrayInputStream(pcmData);
         AudioFormat af = new AudioFormat(sampleRate, SAMPLE_BITS, channels, SIGNED, BIG_ENDIAN);
         AudioInputStream ais = new AudioInputStream(bais, af, pcmData.length / (SAMPLE_BITS/8) / channels);
         // Java AudioSystem may need Big-Endian flag consistent; if issues, flip endianness
         File outFile = new File(filename);
         AudioSystem.write(ais, AudioFileFormat.Type.WAVE, outFile);
         ais.close();
         bais.close();
     } catch (Exception ex) {
         ex.printStackTrace();
     }
 }

 // Build a simple GUI to control parameters
 private static void createAndShowGUI(MaskEngine engine) {
     JFrame frame = new JFrame("AdvancedMasker — perceptual masking tool");
     frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
     frame.setSize(720, 520);
     frame.setLayout(new BorderLayout());

     // Top: sliders and toggles
     JPanel top = new JPanel(new GridLayout(3, 3, 8, 8));

     JSlider gainSlider = new JSlider(0, 100, (int) (MASTER_GAIN * 100));
     gainSlider.setMajorTickSpacing(25);
     gainSlider.setPaintTicks(true);
     gainSlider.setPaintLabels(true);
     gainSlider.addChangeListener(e -> MASTER_GAIN = gainSlider.getValue() / 100.0);

     JSlider brownSlider = new JSlider(0, 100, (int) (BROWN_LEVEL * 100));
     brownSlider.addChangeListener(e -> BROWN_LEVEL = brownSlider.getValue() / 100.0);

     JSlider pinkSlider = new JSlider(0, 100, (int) (PINK_LEVEL * 100));
     pinkSlider.addChangeListener(e -> PINK_LEVEL = pinkSlider.getValue() / 100.0);

     JSlider bandsSlider = new JSlider(6, 48, NUM_BANDS);
     bandsSlider.setMajorTickSpacing(6);
     bandsSlider.setPaintLabels(true);
     bandsSlider.setPaintTicks(true);
     bandsSlider.addChangeListener(e -> {
         int nb = bandsSlider.getValue();
         if (nb != NUM_BANDS) {
             NUM_BANDS = nb;
             engine.rebuildBands(NUM_BANDS);
         }
     });

     JCheckBox aggressiveBox = new JCheckBox("Aggressive masking (speech band + transients)");
     aggressiveBox.addActionListener(e -> AGGRESSIVE = aggressiveBox.isSelected());

     JCheckBox binauralBox = new JCheckBox("Binaural / Stereo mode");
     binauralBox.addActionListener(e -> {
         BINAURAL = binauralBox.isSelected();
         // NOTE: to actually change SourceDataLine channels we'd need reopen line; simpler is to ask restart
         JOptionPane.showMessageDialog(frame, "Binaural toggle will take effect after restart of the program. (Change takes effect on next run.)");
     });

     JCheckBox recordBox = new JCheckBox("Record to WAV (mask_output.wav)");
     recordBox.addActionListener(e -> {
         RECORD_TO_WAV = recordBox.isSelected();
         if (RECORD_TO_WAV) JOptionPane.showMessageDialog(frame, "Recording enabled: mask_output.wav will be written on stop.");
     });

     top.add(makeLabeledPanel("Master Gain", gainSlider));
     top.add(makeLabeledPanel("Brown Noise Level", brownSlider));
     top.add(makeLabeledPanel("Pink Noise Level", pinkSlider));
     top.add(makeLabeledPanel("White Level (auto)", new JLabel(String.format("%.2f", WHITE_LEVEL))));
     top.add(makeLabeledPanel("Number of Bands", bandsSlider));
     top.add(aggressiveBox);
     top.add(binauralBox);
     top.add(recordBox);

     // Middle: instructions & safety
     JTextArea info = new JTextArea();
     info.setEditable(false);
     info.setLineWrap(true);
     info.setWrapStyleWord(true);
     info.setText(
         "Notes:\n" +
         "- This program provides PERCEPTUAL masking only. It cannot physically block RF/microwave signals.\n" +
         "- Use low-to-moderate MASTER GAIN. Continuous loud noise can damage hearing.\n" +
         "- Ultrasound content may affect pets. Keep levels modest.\n" +
         "- Aggressive mode increases speech-band energy (1-6 kHz) and adds short bursts to mask transient percepts.\n" +
         "- Use binaural mode for stronger cortical occupation (may require restart).\n" +
         "- Click STOP to exit and optionally save WAV if recording was enabled."
     );

     // Bottom: control buttons
     JPanel bottom = new JPanel(new FlowLayout());
     JButton stopBtn = new JButton("STOP");
     stopBtn.addActionListener(e -> {
         running.set(false);
         frame.dispose();
     });
     bottom.add(stopBtn);

     frame.add(top, BorderLayout.NORTH);
     frame.add(new JScrollPane(info), BorderLayout.CENTER);
     frame.add(bottom, BorderLayout.SOUTH);

     frame.setVisible(true);
 }

 private static JPanel makeLabeledPanel(String label, JComponent comp) {
     JPanel p = new JPanel(new BorderLayout());
     p.add(new JLabel(label), BorderLayout.NORTH);
     p.add(comp, BorderLayout.CENTER);
     return p;
 }

 // ===== MaskEngine: generates samples =====
 private static class MaskEngine {
     private final int sampleRate;
     private Random rnd = new Random();

     // Noise state
     private double brownPrevL = 0, brownPrevR = 0;
     private PinkNoiseFilter pinkFilterL, pinkFilterR;

     // Bands
     private ArrayList<Biquad> bandsL;
     private ArrayList<Biquad> bandsR;
     private ArrayList<Envelope> envelopesL;
     private ArrayList<Envelope> envelopesR;
     private double[] bandCenters;

     public MaskEngine(int sampleRate) {
         this.sampleRate = sampleRate;
         pinkFilterL = new PinkNoiseFilter();
         pinkFilterR = new PinkNoiseFilter();
         rebuildBands(NUM_BANDS);
     }

     // rebuild bands when number changes
     public synchronized void rebuildBands(int numBands) {
         bandsL = new ArrayList<>();
         bandsR = new ArrayList<>();
         envelopesL = new ArrayList<>();
         envelopesR = new ArrayList<>();
         bandCenters = new double[numBands];
         // log-spaced centers emphasizing speech region
         for (int i = 0; i < numBands; i++) {
             double t = i / (double) (numBands - 1);
             // bias towards speech 300-4000 Hz using power curve
             double spread = Math.pow(t, 0.7);
             double freq = MIN_FREQ * Math.pow(MAX_FREQ / MIN_FREQ, spread);
             bandCenters[i] = freq;
             double q = 0.7 + 1.5 * (1.0 - Math.abs(Math.log(freq / 1000.0) / Math.log(1000.0))); // rough Q variation
             bandsL.add(Biquad.makeBandPass(freq, Math.max(0.5, q), sampleRate));
             bandsR.add(Biquad.makeBandPass(freq, Math.max(0.5, q), sampleRate));
             envelopesL.add(new Envelope(rnd, MIN_ENV_MS, MAX_ENV_MS, sampleRate));
             envelopesR.add(new Envelope(rnd, MIN_ENV_MS, MAX_ENV_MS, sampleRate));
         }
     }

     // render block: returns array [channel][samples]
     public synchronized float[][] renderBlock(int blockSize, boolean stereo) {
         float[][] out;
         out = new float[stereo ? 2 : 1][blockSize];

         for (int n = 0; n < blockSize; n++) {
             // generate white sample
             double whiteL = rnd.nextGaussian() * 0.4;
             double whiteR = rnd.nextGaussian() * 0.4;

             // brown (integrated white) per channel
             brownPrevL = (brownPrevL + rnd.nextGaussian() * 0.02);
             brownPrevR = (brownPrevR + rnd.nextGaussian() * 0.02);
             clamp(ref(brownPrevL), -1.0, 1.0);
             clamp(ref(brownPrevR), -1.0, 1.0);
             double brownL = brownPrevL;
             double brownR = brownPrevR;

             // pink
             double pinkL = pinkFilterL.next(rnd);
             double pinkR = pinkFilterR.next(rnd);

             // base mix
             double baseL = BROWN_LEVEL * brownL + PINK_LEVEL * pinkL + (1.0 - BROWN_LEVEL - PINK_LEVEL) * whiteL;
             double baseR = BROWN_LEVEL * brownR + PINK_LEVEL * pinkR + (1.0 - BROWN_LEVEL - PINK_LEVEL) * whiteR;

             double mixL = 0.0;
             double mixR = 0.0;

             // apply spectral walkers: slowly shift band center (occasionally)
             if (rnd.nextDouble() < 0.0005) {
                 // random small drift -- rebuild filters for slight shift
                 for (int b = 0; b < bandsL.size(); b++) {
                     double shiftHz = (rnd.nextDouble() - 0.5) * 20.0; // +-20Hz jitter
                     bandCenters[b] = Math.max(MIN_FREQ, Math.min(MAX_FREQ, bandCenters[b] + shiftHz));
                     bandsL.set(b, Biquad.makeBandPass(bandCenters[b], 0.9 + rnd.nextDouble(), sampleRate));
                     bandsR.set(b, Biquad.makeBandPass(bandCenters[b] * (1.0 + (BINAURAL ? 0.001 : 0.0)), 0.9 + rnd.nextDouble(), sampleRate));
                 }
             }

             // per-band processing
             for (int b = 0; b < bandsL.size(); b++) {
                 Envelope envL = envelopesL.get(b);
                 Envelope envR = envelopesR.get(b);

                 double gainL = envL.nextSample();
                 double gainR = envR.nextSample();

                 double bandOutL = bandsL.get(b).process(baseL);
                 double bandOutR = bandsR.get(b).process(baseR);

                 mixL += gainL * bandOutL;
                 mixR += gainR * bandOutR;
             }

             // background low-level unfiltered base
             double backgroundL = 0.06 * baseL;
             double backgroundR = 0.06 * baseR;

             // occasional transients (to cover micropulse-like perception)
             if (AGGRESSIVE && rnd.nextDouble() < 0.003) {
                 double burst = (rnd.nextDouble() * 2.0 - 1.0) * 0.6;
                 mixL += burst * (0.5 + rnd.nextDouble());
             }
             if (AGGRESSIVE && rnd.nextDouble() < 0.003) {
                 double burst = (rnd.nextDouble() * 2.0 - 1.0) * 0.6;
                 mixR += burst * (0.5 + rnd.nextDouble());
             }

             // speech-band emphasis if aggressive
             if (AGGRESSIVE) {
                 // boost energy roughly 1k-4k
                 mixL += 0.35 * bandBoost(baseL, 1000, 4000, sampleRate);
                 mixR += 0.35 * bandBoost(baseR, 1000, 4000, sampleRate);
             }

             // simple stereo decorrelation: small delay or phase diff (simulated by slightly different envelopes)
             double sampleL = Math.tanh(1.2 * (mixL + backgroundL));
             double sampleR = Math.tanh(1.2 * (mixR + backgroundR));

             out[0][n] = (float) sampleL;
             if (stereo) out[1][n] = (float) sampleR;
         }

         return out;
     }

     // tiny helper: simple band boost via dynamic bandpass processing (cheap)
     private double bandBoost(double base, double f1, double f2, int sr) {
         // simple envelope-like boost: if base contains high energy, return scaled base; else small
         double center = Math.sqrt(f1 * f2);
         // approximate by scaling base (there's no direct spect analyzer here)
         return base * 0.6;
     }

     private void clamp(double[] v, double lo, double hi) { if (v[0] < lo) v[0] = lo; if (v[0] > hi) v[0] = hi; }
     // trick to pass by reference for primitive
     private double[] ref(double x) { return new double[]{x}; }

 }

 // === Envelope ===
 private static class Envelope {
     private Random rnd;
     private int minMs, maxMs, sampleRate;
     private double current = 0.0, target = 0.0;
     private int remaining = 0;
     private double step = 0.0;

     public Envelope(Random rnd, int minMs, int maxMs, int sampleRate) {
         this.rnd = rnd;
         this.minMs = minMs;
         this.maxMs = maxMs;
         this.sampleRate = sampleRate;
         this.current = 0.05 + rnd.nextDouble() * 0.3;
         scheduleNew();
     }

     private void scheduleNew() {
         target = 0.1 + rnd.nextDouble() * 0.9; // avoid absolute silence
         int durMs = minMs + rnd.nextInt(Math.max(1, maxMs - minMs + 1));
         remaining = Math.max(1, (int) (durMs * sampleRate / 1000.0));
         step = (target - current) / (double) remaining;
     }

     public double nextSample() {
         if (remaining <= 0) scheduleNew();
         current += step;
         remaining--;
         double jitter = (rnd.nextDouble() - 0.5) * 0.03;
         double out = current + jitter;
         if (out < 0) out = 0;
         if (out > 1) out = 1;
         // slight curve
         return Math.pow(out, 1.05);
     }
 }

 // === Basic RBJ Biquad bandpass ===
 private static class Biquad {
     private double a1, a2, b0, b1, b2;
     private double z1 = 0, z2 = 0;

     public double process(double in) {
         double out = b0 * in + b1 * z1 + b2 * z2 - a1 * z1 - a2 * z2;
         z2 = z1;
         z1 = out;
         return out;
     }

     public static Biquad makeBandPass(double freqHz, double q, double sampleRate) {
         Biquad f = new Biquad();
         double w0 = 2.0 * Math.PI * freqHz / sampleRate;
         double cosw0 = Math.cos(w0);
         double sinw0 = Math.sin(w0);
         double alpha = sinw0 / (2.0 * q);

         double b0 = alpha;
         double b1 = 0.0;
         double b2 = -alpha;
         double a0 = 1.0 + alpha;
         double a1 = -2.0 * cosw0;
         double a2 = 1.0 - alpha;

         f.b0 = b0 / a0;
         f.b1 = b1 / a0;
         f.b2 = b2 / a0;
         f.a1 = a1 / a0;
         f.a2 = a2 / a0;

         return f;
     }
 }

 // === Pink noise filter (Voss-McCartney style approximation) ===
 private static class PinkNoiseFilter {
     private final double[] rows = new double[16];
     private int p = 0;
     private Random rnd = new Random();

     public double next(Random sharedRnd) {
         p++;
         if (p >= rows.length) p = 0;
         rows[p] = sharedRnd.nextGaussian() * 0.1;
         double sum = 0;
         for (double r : rows) sum += r;
         return sum / rows.length;
     }
 }
 
 public static void playWithMasking(String filename) throws Exception {

     AudioInputStream inputStream = AudioSystem.getAudioInputStream(new java.io.File(filename));
     AudioFormat baseFormat = inputStream.getFormat();

     AudioFormat format = new AudioFormat(
             AudioFormat.Encoding.PCM_SIGNED,
             baseFormat.getSampleRate(),
             16,
             baseFormat.getChannels(),
             baseFormat.getChannels() * 2,
             baseFormat.getSampleRate(),
             false
     );

     AudioInputStream din = AudioSystem.getAudioInputStream(format, inputStream);

     SourceDataLine line = AudioSystem.getSourceDataLine(format);
     line.open(format);
     line.start();

     byte[] buffer = new byte[4096];
     Random rand = new Random();

     while (true) {
         int bytesRead = din.read(buffer, 0, buffer.length);
         if (bytesRead < 0)
             break;

         // =====================================================================
         // 🔥🔥🔥 MASK BLOCK — MODIFY THIS SECTION TO INCREASE MASKING 🔥🔥🔥
         // =====================================================================
         for (int i = 0; i < bytesRead; i += 2) {

             // Extract sample (little-endian)
             int sample = ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));

             // 1️⃣ Broadband white noise
             int n1 = (int)((rand.nextGaussian() * 5000) * NOISE_LEVEL);

             // 2️⃣ Shaped chaotic noise (pseudo fractal)
             float chaos = (float)(Math.sin(i * MASK_COMPLEXITY * 0.001));
             int n2 = (int)(chaos * 6000 * NOISE_LEVEL);

             // Mix original + noise
             sample += n1 + n2;

             // Apply USER GAIN (final perceived loudness)
             sample *= USER_GAIN;

             // Clip to 16-bit range
             if (sample > 32767) sample = 32767;
             if (sample < -32768) sample = -32768;

             // Save back to buffer (little-endian)
             buffer[i] = (byte)(sample & 0xFF);
             buffer[i + 1] = (byte)((sample >> 8) & 0xFF);
         }
         // =====================================================================

         line.write(buffer, 0, bytesRead);
     }

     line.drain();
     line.stop();
     line.close();
     din.close();
 }
}
