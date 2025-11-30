package app.smatter.smatterwavemap;

import java.awt.EventQueue;
import java.awt.Cursor;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.UIManager;
import javax.swing.filechooser.FileSystemView;

import javax.swing.JComboBox;
import javax.swing.JFrame;

import java.awt.*;
import javax.swing.*;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.StandardTickUnitSource;
import org.jfree.chart.event.ChartChangeEvent;
import org.jfree.chart.event.ChartChangeListener;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.event.OverlayChangeListener;
import org.jfree.chart.event.PlotChangeEvent;
import org.jfree.chart.event.PlotChangeListener;
import org.jfree.chart.panel.Overlay;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.chart.ui.Align;
import org.jfree.chart.ui.HorizontalAlignment;
import org.jfree.chart.ui.RectangleAnchor;
import org.jfree.chart.ui.RectangleEdge;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.chart.ui.TextAnchor;

import jspectrumanalyzer.capture.ScreenCapture;
import jspectrumanalyzer.core.DatasetSpectrumPeak;
import jspectrumanalyzer.core.FFTBins;
import jspectrumanalyzer.core.FrequencyAllocationTable;
import jspectrumanalyzer.core.FrequencyAllocations;
import jspectrumanalyzer.core.FrequencyBand;
import jspectrumanalyzer.core.FrequencyRange;
import jspectrumanalyzer.core.HackRFSettings;
import jspectrumanalyzer.core.PersistentDisplay;
import jspectrumanalyzer.core.SpurFilter;
import jspectrumanalyzer.core.jfc.XYSeriesCollectionImmutable;
import jspectrumanalyzer.nativebridge.HackRFSweepDataCallback;
import jspectrumanalyzer.nativebridge.HackRFSweepNativeBridge;
import jspectrumanalyzer.ui.HackRFSweepSettingsUI;
import jspectrumanalyzer.ui.WaterfallPlot;
import shared.mvc.MVCController;
import shared.mvc.ModelValue;
import shared.mvc.ModelValue.ModelValueBoolean;
import shared.mvc.ModelValue.ModelValueInt;

public class SmatterWaveMap implements HackRFSettings, HackRFSweepDataCallback {

	    // Audio settings
	    private static final float SAMPLE_RATE = 96000f; // try 48000 or 96000 for more ultrasound
	    private static final int SAMPLE_SIZE_BITS = 16;
	    private static final int CHANNELS = 1; // mono
	    private static final boolean SIGNED = true;
	    private static final boolean BIG_ENDIAN = false;

	    // Masker settings
	    private static final int NUM_BANDS = 6; // number of independent bandpass bands
	    private static final double[] DEFAULT_BAND_CENTERS = {200, 600, 1500, 5000, 12000, 22000};
	    private static final double[] DEFAULT_BAND_Q = {1.0, 1.0, 1.2, 0.9, 0.8, 0.7};

	    // Envelope update interval (ms)
	    private static final int MIN_ENV_MS = 80;
	    private static final int MAX_ENV_MS = 600;

	    // Brown noise mixing level (0..1)
	    private static final double BROWN_LEVEL = 0.45;

	    // Master output gain (0..1) - keep modest to avoid loud output
	    private static final double MASTER_GAIN = 0.35;

	    // Secure RNG used as entropy source (represents STE's high-entropy stream)
	    private static final SecureRandom GLOBAL_RNG = new SecureRandom();
		private JFrame frame;
		Icon imageIcon11;
		static String languageCode="gu";
		static int durationSeconds = 0;
		static boolean saveWav = false;
		static Thread playThread;
 
	
 
		
		String getfileChosenLocation(String option)
		{
			String toReturn="nullLocation";
			JFileChooser j = new JFileChooser(FileSystemView.getFileSystemView().getHomeDirectory()); 
			if(option.equals("Save"))
			{
				int r = j.showSaveDialog(null); 
		        if (r == JFileChooser.APPROVE_OPTION)
		        	toReturn = j.getSelectedFile().getAbsolutePath(); 
			}
			else
			{
	            int r = j.showOpenDialog(null); 
	            if (r == JFileChooser.APPROVE_OPTION) 
	            	toReturn = j.getSelectedFile().getAbsolutePath(); 
			}
	        return toReturn;
		}
		
		void hideButtonsetCursor(final JButton button)
		{
			button.setOpaque(false);
			button.setFocusable(false);
			button.setBorderPainted(false);
			button.setContentAreaFilled(false);
			button.addMouseListener(new MouseAdapter() {
				@Override
				public void mouseEntered(MouseEvent arg0) {
					button.setCursor(new Cursor(Cursor.HAND_CURSOR));
				}
				@Override
				public void mouseExited(MouseEvent e) {
					button.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
				}
			});
		}
		
		

	    // --- Generator class ---
	    static class STEGenerator {
	        private final AudioFormat format;
	        private final SecureRandom rng = GLOBAL_RNG; // represent STE entropy
	        private final Biquad[] bands;
	        private final Envelope[] envs;
	        private final double sampleRate;

	        private double brownState = 0.0;

	        public STEGenerator(AudioFormat format) {
	            this.format = format;
	            this.sampleRate = format.getSampleRate();

	            int n = NUM_BANDS;
	            bands = new Biquad[n];
	            envs = new Envelope[n];
	            for (int i = 0; i < n; i++) {
	                double fc = DEFAULT_BAND_CENTERS[Math.min(i, DEFAULT_BAND_CENTERS.length-1)];
	                double q = DEFAULT_BAND_Q[Math.min(i, DEFAULT_BAND_Q.length-1)];
	                bands[i] = new Biquad();
	                bands[i].setBandpass(fc, q, sampleRate);
	                envs[i] = new Envelope(rng, sampleRate);
	            }
	        }

	        public void play(AtomicBoolean running, int durationSeconds, boolean saveWav) throws Exception {
	            DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
	            if (!AudioSystem.isLineSupported(info)) {
	                System.err.println("Audio line not supported with format: " + format);
	                return;
	            }

	            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
	            int bufferSamples = 4096;
	            int bufferBytes = bufferSamples * (SAMPLE_SIZE_BITS / 8) * CHANNELS;
	            line.open(format, bufferBytes);
	            line.start();

	            ByteArrayOutputStream wavOut = saveWav ? new ByteArrayOutputStream() : null;

	            long totalSamplesToPlay = (durationSeconds > 0) ? (long)(durationSeconds * sampleRate) : Long.MAX_VALUE;
	            long playedSamples = 0;

	            byte[] outBuf = new byte[bufferBytes];

	            while (running.get() && playedSamples < totalSamplesToPlay) {
	                // generate buffer
	                ByteBuffer bb = ByteBuffer.wrap(outBuf);
	                bb.order(ByteOrder.LITTLE_ENDIAN);
	                for (int s = 0; s < bufferSamples && playedSamples < totalSamplesToPlay; s++) {
	                    // generate high-entropy white sample in range -1..1 using SecureRandom
	                    double white = (rng.nextDouble() * 2.0 - 1.0);

	                    // apply band filters - feed same white into each band (independent states)
	                    double bandSum = 0.0;
	                    for (int b = 0; b < bands.length; b++) {
	                        double w = white * 0.6; // reduce per-band drive
	                        double y = bands[b].process(w);
	                        double g = envs[b].nextGain();
	                        bandSum += y * g;
	                    }

	                    // brown noise component (integrated white)
	                    double brown = brownState + (0.02 * white);
	                    // simple damping for brown
	                    brown *= 0.995;
	                    brownState = brown;

	                    // small randomized phase modulation using STE entropy
	                    double micro = Math.sin((playedSamples % 1000) / 1000.0 * 2*Math.PI * (rng.nextDouble()*8000 + 2000)) * (rng.nextDouble()*0.002);

	                    // final mix
	                    double mix = (bandSum * (1.0 - BROWN_LEVEL)) + (brown * BROWN_LEVEL) + micro;
	                    mix *= MASTER_GAIN;

	                    // clamp
	                    if (mix > 1.0) mix = 1.0;
	                    if (mix < -1.0) mix = -1.0;

	                    short pcm = (short) Math.round(mix * Short.MAX_VALUE);
	                    bb.putShort(pcm);

	                    playedSamples++;
	                }

	                // write to line
	                line.write(outBuf, 0, outBuf.length);

	                // optionally save
	                if (saveWav && wavOut != null) {
	                    wavOut.write(outBuf);
	                }

	                // periodically update band center frequencies and Q slightly (entropy cycling)
	                if ((playedSamples % (int)sampleRate) < bufferSamples) {
	                    // once per second-ish
	                    for (int b=0; b<bands.length; b++) {
	                        // small random walk around default centers using STE entropy
	                        double base = DEFAULT_BAND_CENTERS[Math.min(b, DEFAULT_BAND_CENTERS.length-1)];
	                        double jitter = 1 + (rng.nextGaussian() * 0.02); // +-2%
	                        double newFc = Math.max(20.0, base * jitter);
	                        double newQ = Math.max(0.4, DEFAULT_BAND_Q[Math.min(b, DEFAULT_BAND_Q.length-1)] * (1.0 + (rng.nextGaussian()*0.08)));
	                        bands[b].setBandpass(newFc, newQ, sampleRate);
	                        // refresh envelope timing randomness
	                        envs[b].randomizeTiming();
	                    }
	                }

	            }

	            // stop
	            line.drain();
	            line.stop();
	            line.close();

	            if (saveWav && wavOut != null) {
	                // write WAV file header + data
	                byte[] pcmData = wavOut.toByteArray();
	                try (FileOutputStream fos = new FileOutputStream("C:/Users/ParthShahSmatterLLP/Desktop/ste_masker_output.wav")) {
	                    writeWavHeader(fos, pcmData.length, (int)sampleRate, CHANNELS, SAMPLE_SIZE_BITS);
	                    fos.write(pcmData);
	                    System.out.println("Saved WAV to ste_masker_output.wav");
	                }
	            }

	            System.out.println("Playback finished.");
	        }

	        private static void writeWavHeader(OutputStream os, int pcmDataLength, int sampleRate, int channels, int bitsPerSample) throws IOException {
	            ByteBuffer buffer = ByteBuffer.allocate(44);
	            buffer.order(ByteOrder.LITTLE_ENDIAN);
	            buffer.put("RIFF".getBytes());
	            buffer.putInt(36 + pcmDataLength);
	            buffer.put("WAVE".getBytes());
	            buffer.put("fmt ".getBytes());
	            buffer.putInt(16);
	            buffer.putShort((short)1);
	            buffer.putShort((short)channels);
	            buffer.putInt(sampleRate);
	            int byteRate = sampleRate * channels * (bitsPerSample/8);
	            buffer.putInt(byteRate);
	            short blockAlign = (short)(channels * (bitsPerSample/8));
	            buffer.putShort(blockAlign);
	            buffer.putShort((short)bitsPerSample);
	            buffer.put("data".getBytes());
	            buffer.putInt(pcmDataLength);
	            os.write(buffer.array());
	        }
	    }

	    // --- Envelope generator ---
	    static class Envelope {
	        private final SecureRandom rng;
	        private final double sampleRate;
	        private double currentGain = 0.5;
	        private double targetGain = 0.5;
	        private int samplesToTarget = 1;
	        private int samplesElapsed = 0;

	        public Envelope(SecureRandom rng, double sampleRate) {
	            this.rng = rng;
	            this.sampleRate = sampleRate;
	            randomizeTiming();
	        }

	        public void randomizeTiming() {
	            // pick new target
	            targetGain = Math.max(0.0, Math.min(1.0, rng.nextDouble()));
	            int ms = MIN_ENV_MS + rng.nextInt(Math.max(1, MAX_ENV_MS - MIN_ENV_MS));
	            samplesToTarget = Math.max(1, (int)( (ms/1000.0) * sampleRate ));
	            samplesElapsed = 0;
	        }

	        public double nextGain() {
	            // linear interpolate
	            if (samplesElapsed >= samplesToTarget) {
	                // pick a new target occasionally
	                if (rng.nextDouble() < 0.02) randomizeTiming();
	                currentGain = targetGain;
	                return currentGain;
	            }
	            double t = (double)samplesElapsed / (double)samplesToTarget;
	            double g = currentGain + (targetGain - currentGain) * t;
	            samplesElapsed++;
	            // small micro-random jitter
	            g += (rng.nextDouble() - 0.5) * 0.02;
	            if (g < 0) g = 0;
	            if (g > 1) g = 1;
	            return g;
	        }
	    }

	    // --- Simple biquad filter implementation (bandpass) ---
	    static class Biquad {
	        private double a0, a1, a2, b1, b2;
	        private double z1 = 0.0, z2 = 0.0;

	        public void setBandpass(double freq, double q, double fs) {
	            double omega = 2.0 * Math.PI * freq / fs;
	            double alpha = Math.sin(omega) / (2.0 * q);
	            double cosw = Math.cos(omega);
	            double b0 = alpha;
	            double b1c = 0.0;
	            double b2 = -alpha;
	            double a0c = 1.0 + alpha;
	            double a1c = -2.0 * cosw;
	            double a2c = 1.0 - alpha;

	            // normalize
	            a0 = b0 / a0c;
	            a1 = b1c / a0c;
	            a2 = b2 / a0c;
	            b1 = a1c / a0c;
	            b2 = a2c / a0c;
	        }

	        public double process(double in) {
	            double out = in * a0 + z1;
	            z1 = in * a1 + z2 - b1 * out;
	            z2 = in * a2 - b2 * out;
	            return out;
	        }
	    }

	    // --- Utility: reverse ArrayList ---
	    static ArrayList<String> reverseArrayList(ArrayList<String> inputArray){
	        ArrayList<String> toRet = new ArrayList<String>();
	        for(int i=inputArray.size()-1; i>=0; i--) {
	            toRet.add(inputArray.get(i));
	        }
	        return toRet;
	    }

	    // ------------------------------------------------------------------
	    // Below: User-supplied Smatter Time Encryption (STE) methods embedded verbatim.
	    // These are proprietary to the user (PCT IN/2023/050389, WO2024218781). Treat as secret.
	    // ------------------------------------------------------------------

	    public String encryptM(String sText){
	        String keyID = System.currentTimeMillis()+"";
	        int aV = rAValue(keyID);
	        String cipherText = breakKeyAddToPlainTextAndEncryptRecursively(keyID, aV, sText, keyID.length()-1-aV, 0);
	        String cTM = maskText(cipherText);
	        String kM = maskText(keyID);
	        return cTM + "7" +  kM;
	    }

	    public String maskText(String cipherText){
	        String fC = "";
	        ArrayList<Integer> cN = new ArrayList<>(Arrays.asList(51, 48, 12, 88, 24, 98, 65, 46, 69, 41));
	        for(int i=0; i<cipherText.length(); i++){
	            fC+=cN.get(Integer.parseInt(cipherText.charAt(i)+""));
	        }
	        return fC;
	    }

	    public String breakKeyAddToPlainTextAndEncryptRecursively(String keyID, int aV, String plainText, int loopV, int abc){
	        String gCT="";
	        String kNST = keyID.substring(loopV, (loopV+aV)).toString();
	        int kN = rAValue(keyID.substring(loopV, (loopV+aV)).toString());
	        System.out.println(kNST+"  "+loopV+"  "+(loopV+aV));
	        System.out.println(kNST.length()+"**");
	        if(kNST.length()!=0) {
	            if(kN%2==0){
	                plainText = kN+""+plainText;
	                gCT=processEncoding(keyID, kN, plainText);
	            }
	            else{
	                plainText = plainText+kN+"";
	                gCT=processEncoding(keyID, kN, plainText);
	            }
	            if((loopV-aV)<0 && abc==0) {
	                return breakKeyAddToPlainTextAndEncryptRecursively(keyID, loopV, gCT, 0, 1);
	            }
	            else if((loopV-aV)>=0 && abc==0){
	                return breakKeyAddToPlainTextAndEncryptRecursively(keyID, aV, gCT, loopV-aV, 0);
	            }
	            else{
	                return gCT;
	            }
	        }
	        else{
	            return plainText;
	        }
	    }

	    public String processEncoding(String keyID, int kN, String plainText){
	        String cT="";
	        for(int i=0; i<plainText.length(); i++){
	            String eASCII="";
	            eASCII = (int)plainText.charAt(i)+"";

	            if(eASCII.length()>=3){
	                eASCII="51"+eASCII;
	            }
	            else if(eASCII.length()==1){
	                eASCII="0"+eASCII;
	            }
	            cT+=eASCII;
	        }
	        return cT;
	    }

	    int rAValue(String oKey){
	        int aT=0;
	        for(int i=0; i<oKey.length(); i++){
	            int eC=Integer.parseInt(oKey.charAt(i)+"");
	            aT+=eC;
	        }
	        if(aT>9){
	            return rAValue(aT+"");
	        }
	        else{
	            return aT;
	        }
	    }

	    public String decryptM(String encodedText){
	        String keyID = unMaskText(encodedText.substring(encodedText.indexOf('7')+1));
	        String sText = unMaskText(encodedText.substring(0, encodedText.indexOf('7')));
	        int aV = rAValue(keyID);
	        return unBreakRemoveFromCipherAndDecryptRecursively(keyID, aV, sText);
	    }

	    public String unMaskText(String plainText){
	        String fC = "";
	        for(int i=0; i<plainText.length(); i=i+2){
	            String pT = plainText.substring(i, i+2);
	            fC+=getRPosition(pT)+"";
	        }
	        return fC;
	    }

	    int getRPosition(String comC){
	        if(comC.equals("51")==true){
	            return 0;
	        }
	        else if(comC.equals("48")==true){
	            return 1;
	        }
	        else if(comC.equals("12")==true){
	            return 2;
	        }
	        else if(comC.equals("88")==true){
	            return 3;
	        }
	        else if(comC.equals("24")==true){
	            return 4;
	        }
	        else if(comC.equals("98")==true){
	            return 5;
	        }
	        else if(comC.equals("65")==true){
	            return 6;
	        }
	        else if(comC.equals("46")==true){
	            return 7;
	        }
	        else if(comC.equals("69")==true){
	            return 8;
	        }
	        else if(comC.equals("41")==true){
	            return 9;
	        }
	        else{
	            return -10000000;
	        }
	    }

	    String unBreakRemoveFromCipherAndDecryptRecursively(String keyID, int aV, String sText){
	        ArrayList<String> keysA = new ArrayList<>();
	        String lastSlot = "";
	        for(int i=keyID.length()-1; i>=0; i=i-aV){
	            keysA.add(keyID.substring(i-aV, i));
	            i=i-aV;
	            if(i-aV<0) {
	                i=i+aV;
	                lastSlot = keyID.substring(0, Math.abs(i-aV));
	                try {
	                    lastSlot = lastSlot.trim();
	                    if(lastSlot.length()>0) {
	                        keysA.add(lastSlot);
	                    }
	                }catch(Exception e) {}

	                break;
	            }
	            i=i+aV;

	        }
	        keysA = reverseArrayList(keysA);
	        String unMaskedT = sText;
	        for(int element=0; element<keysA.size(); element++) {
	            if(element==keysA.size()-1) {
	                int kN = rAValue(keysA.get(element)+"");
	                String newST ="";
	                if(kN%2==0){
	                    for(int i=2; i<unMaskedT.length(); i=i+2){
	                        String eachAC = unMaskedT.substring(i, i+2).toString();
	                        int cI = Integer.parseInt(eachAC);
	                        eachAC = (char)cI +"";
	                        if(!eachAC.equals("3")){
	                            newST+=eachAC;
	                        }
	                        else{
	                            i=i+2;
	                            String eachACNS = unMaskedT.substring(i, i+3).toString();
	                            int cIA = Integer.parseInt(eachACNS);
	                            eachACNS = (char)cIA +"";
	                            newST+=eachACNS;
	                            i++;
	                        }
	                    }
	                }
	                else{
	                    for(int i=0; i<unMaskedT.length()-2; i=i+2){
	                        String eachAC = unMaskedT.substring(i, i+2).toString();
	                        int cI = Integer.parseInt(eachAC);
	                        eachAC = (char)cI +"";
	                        if(eachAC.equals("3")!=true){
	                            newST+=eachAC;
	                        }
	                        else{
	                            i=i+2;
	                            String eachACAS = unMaskedT.substring(i, i+3).toString();
	                            int cIN = Integer.parseInt(eachACAS);
	                            eachACAS = (char)cIN +"";
	                            newST+=eachACAS;
	                            i++;
	                        }
	                    }
	                }
	                unMaskedT = newST;
	            }
	            else {
	                int kN = rAValue(keysA.get(element)+"");
	                String newST ="";
	                if(kN%2==0){
	                    for(int i=2; i<unMaskedT.length()-1; i=i+2){
	                        System.out.println(i + "  ** " + unMaskedT);
	                        String eachAC = unMaskedT.substring(i, i+2).toString();
	                        int cI = Integer.parseInt(eachAC);
	                        eachAC = (char)cI +"";

	                        newST+=eachAC;

	                    }
	                }
	                else{
	                    for(int i=0; i<unMaskedT.length()-2; i=i+2){
	                        String eachAC = unMaskedT.substring(i, i+2).toString();
	                        int cI = Integer.parseInt(eachAC);
	                        eachAC = (char)cI +"";
	                        newST+=eachAC;
	                    }
	                }
	                unMaskedT = newST;
	            }
	        }
	        return unMaskedT;
	    }

	    int getAsciiJava(String charStr) {
	        int toRet=0;
	        for (int i = 0; i < charStr.length(); i++) {
	            char character = charStr.charAt(i);
	            toRet = character;
	        }
	        return toRet;
	    }
	    
	    
	    
	    
	    
	    
	    
	    
	    private static class PerformanceEntry{
			final String name;
			long nanosSum;
			int count;
			public PerformanceEntry(String name) {
				this.name 	= name;
			}
			public void addDrawingTime(long nanos) {
				nanosSum	+= nanos;
				count++;
			}
			public void reset() {
				count	= 0;
				nanosSum	= 0;
			}
			@Override
			public String toString() {
				return name;
			}
		}
		
		private static class RuntimePerformanceWatch {
			/**
			 * incoming full spectrum updates from the hardware
			 */
			int				hwFullSpectrumRefreshes	= 0;
			volatile long	lastStatisticsRefreshed	= System.currentTimeMillis();
			PerformanceEntry persisentDisplay	= new PerformanceEntry("Pers.disp");
			PerformanceEntry waterfallUpdate	= new PerformanceEntry("Wtrfall.upd");
			PerformanceEntry waterfallDraw	= new PerformanceEntry("Wtrfll.drw");
			PerformanceEntry chartDrawing	= new PerformanceEntry("Spectr.chart");
			PerformanceEntry spurFilter = new PerformanceEntry("Spur.fil");
			
			private ArrayList<PerformanceEntry> entries	= new ArrayList<>();
			public RuntimePerformanceWatch() {
				entries.add(persisentDisplay);
				entries.add(waterfallUpdate);
				entries.add(waterfallDraw);
				entries.add(chartDrawing);
				entries.add(spurFilter);
			}
			
			public synchronized String generateStatistics() {
				long timeElapsed = System.currentTimeMillis() - lastStatisticsRefreshed;
				if (timeElapsed <= 0)
					timeElapsed = 1;
				StringBuilder b	= new StringBuilder();
				long sumNanos	= 0;
				for (PerformanceEntry entry : entries) {
					sumNanos	+= entry.nanosSum;
					float callsPerSec	= entry.count/(timeElapsed/1000f);
					b.append(entry.name).append(String.format(" %3dms (%5.1f calls/s) \n", entry.nanosSum/1000000, callsPerSec));
				}
				b.append(String.format("Total: %4dms draw time/s: ", sumNanos/1000000));
				return b.toString();
//				double timeSpentDrawingChartPerSec = chartDrawingSum / (timeElapsed / 1000d) / 1000d;
//				return String.format("Spectrum refreshes: %d / Chart redraws: %d / Drawing time in 1 sec %.2fs",
//						hwFullSpectrumRefreshes, chartRedrawed, timeSpentDrawingChartPerSec);

			}

			public synchronized void reset() {
				hwFullSpectrumRefreshes = 0;
				for (PerformanceEntry dataDrawingEntry : entries) {
					dataDrawingEntry.reset();
				}
				lastStatisticsRefreshed = System.currentTimeMillis();
			}
		}

		/**
		 * Color palette for UI
		 */
		protected static class ColorScheme {
			Color	palette0	= Color.white;
			Color	palette1	= new Color(0xe5e5e5);
			Color	palette2	= new Color(0xFCA311);
			Color	palette3	= new Color(0x14213D);
			Color	palette4	= Color.BLACK;
		}

		public static final int	SPECTRUM_PALETTE_SIZE_MIN	= 5;
		private static boolean	captureGIF					= false;

		private static long		initTime					= System.currentTimeMillis();

		public static void main(String[] args) throws IOException {
			
			
 
				//		System.out.println(new File("").getAbsolutePath());
				if (args.length > 0) {
					if (args[0].equals("capturegif")) {
						captureGIF = true;
					}
				}
				//		try { Thread.sleep(20000); System.out.println("Started..."); } catch (InterruptedException e) {}
				
				EventQueue.invokeLater(new Runnable() {
					public void run() {
						try {
							UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
							SmatterWaveMap window = new SmatterWaveMap();
							window.frame.setVisible(true);
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				});
			

 
		}

		public boolean									flagIsHWSendingData						= false;
		private float									alphaFreqAllocationTableBandsImage	= 0.5f;
		private float									alphaPersistentDisplayImage			= 1.0f;
		private JFreeChart								chart;

		private ModelValue<Rectangle2D>					chartDataArea						= new ModelValue<Rectangle2D>(
				"Chart data area", new Rectangle2D.Double(0, 0, 1, 1));
		private XYSeriesCollectionImmutable				chartDataset								= new XYSeriesCollectionImmutable();
		private XYLineAndShapeRenderer					chartLineRenderer;
		private ChartPanel								chartPanel;
		private ColorScheme								colors								= new ColorScheme();
		private DatasetSpectrumPeak						datasetSpectrum;
		private int										dropped								= 0;
		private volatile boolean						flagManualGain						= false;
		private volatile boolean						forceStopSweep						= false;
		/**
		 * Capture a GIF of the program for the GITHUB page
		 */
		private ScreenCapture							gifCap								= null;
		private ArrayList<HackRFEventListener>			hRFlisteners							= new ArrayList<>();
		private ArrayBlockingQueue<FFTBins>				hwProcessingQueue						= new ArrayBlockingQueue<>(
				1000);
		private BufferedImage							imageFrequencyAllocationTableBands	= null;
		private boolean											isChartDrawing						= false;
		private ReentrantLock							lock								= new ReentrantLock();

		private ModelValueBoolean						parameterAntennaLNA   				= new ModelValueBoolean("Antenna LNA +14dB", false);
		private ModelValueBoolean						parameterAntPower					= new ModelValueBoolean("Ant power", false);
		private ModelValueInt							parameterFFTBinHz					= new ModelValueInt("FFT Bin [Hz]", 100000);
		private ModelValueBoolean						parameterFilterSpectrum				= new ModelValueBoolean("Filter", false);
		private ModelValue<FrequencyRange>				parameterFrequency					= new ModelValue<>("Frequency range", new FrequencyRange(1, 2500));
		private ModelValue<FrequencyAllocationTable>	parameterFrequencyAllocationTable	= new ModelValue<FrequencyAllocationTable>("Frequency allocation table", null);

		private ModelValueInt							parameterGainLNA					= new ModelValueInt("LNA Gain",0, 8, 0, 40);
		private ModelValueInt							parameterGainTotal					= new ModelValueInt("Gain [dB]", 40);
		private ModelValueInt							parameterGainVGA					= new ModelValueInt("VGA Gain", 0, 2, 0, 60);
		private ModelValueBoolean						parameterIsCapturingPaused			= new ModelValueBoolean("Capturing paused", false);

		private ModelValueInt							parameterPersistentDisplayPersTime  = new ModelValueInt("Persistence time", 30, 1, 1, 60);
		private ModelValueInt							parameterPeakFallRateSecs			= new ModelValueInt("Peak fall rate", 30);
		private ModelValueBoolean						parameterPersistentDisplay			= new ModelValueBoolean("Persistent display", false);

		private ModelValueInt							parameterSamples					= new ModelValueInt("Samples", 8192);

		private ModelValueBoolean						parameterShowPeaks					= new ModelValueBoolean("Show peaks", false);

		private ModelValueBoolean 						parameterDebugDisplay				= new ModelValueBoolean("Debug", false);
		
		private ModelValue<BigDecimal>					parameterSpectrumLineThickness		= new ModelValue<>("Spectrum line thickness", new BigDecimal("1"));
		private ModelValueInt							parameterSpectrumPaletteSize		= new ModelValueInt("Spectrum palette size", 0);
		private ModelValueInt							parameterSpectrumPaletteStart		= new ModelValueInt("Spectrum palette start", 0);
		private ModelValueBoolean						parameterSpurRemoval				= new ModelValueBoolean("Spur removal", false);
		private ModelValueBoolean						parameterWaterfallVisible			= new ModelValueBoolean("Waterfall visible", true);
		
		private PersistentDisplay						persistentDisplay					= new PersistentDisplay();
		private float									spectrumInitValue					= -150;
		private SpurFilter								spurFilter;
		private Thread									threadHackrfSweep;
		private ArrayBlockingQueue<Integer>				threadLaunchCommands				= new ArrayBlockingQueue<>(1);
		private Thread									threadLauncher;
		private Thread									threadProcessing;
		private TextTitle								titleFreqBand						= new TextTitle("",
				new Font("Dialog", Font.PLAIN, 11));
		private RuntimePerformanceWatch					perfWatch							= new RuntimePerformanceWatch();
	 
		private ValueMarker								waterfallPaletteEndMarker;
		private ValueMarker								waterfallPaletteStartMarker;
		private WaterfallPlot							waterfallPlot;
		private JLabel labelMessages;

		public SmatterWaveMap() {
			printInit(0);

			if (captureGIF) {
//				parameterFrequency.setValue(new FrequencyRange(700, 2700));
				parameterFrequency.setValue(new FrequencyRange(2400, 2700));
				parameterGainTotal.setValue(60);
				parameterSpurRemoval.setValue(true);
				parameterPersistentDisplay.setValue(true);
				parameterFFTBinHz.setValue(500000);
				parameterFrequencyAllocationTable.setValue(new FrequencyAllocations().getTable().values().stream().findFirst().get());
			}

			recalculateGains(parameterGainTotal.getValue());

			try {
				UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
			} catch (Exception e1) {
				e1.printStackTrace();
			}
			//		UIManager.getLookAndFeelDefaults().put("TabbedPane.borderHightlightColor", Color.black);
			//		UIManager.getLookAndFeelDefaults().put("TabbedPane.background", Color.black);
			//		UIManager.getLookAndFeelDefaults().put("TabbedPane.contentAreaColor", Color.black);
			//		UIManager.getLookAndFeelDefaults().put("TabbedPane.darkShadow", Color.black);
			//		UIManager.getLookAndFeelDefaults().put("TabbedPane.focus", Color.black);
			//		UIManager.getLookAndFeelDefaults().put("TabbedPane.highlight", Color.black);
			//		UIManager.getLookAndFeelDefaults().put("TabbedPane.light", Color.black);
			//		UIManager.getLookAndFeelDefaults().put("TabbedPane.selected", Color.black);
			//		UIManager.getLookAndFeelDefaults().put("TabbedPane.selectedForeground", Color.black);
			//		UIManager.getLookAndFeelDefaults().put("TabbedPane.selectHighlight", Color.black);
			//		UIManager.getLookAndFeelDefaults().put("TabbedPane.shadow", Color.black);
			//		UIManager.getLookAndFeelDefaults().put("TabbedPane.tabAreaBackground", Color.black);

			Insets insets = new Insets(1, 1, 1, 1);
			UIManager.getLookAndFeelDefaults().put("TabbedPane.contentBorderInsets", insets);
			UIManager.getLookAndFeelDefaults().put("TabbedPane.selectedTabPadInsets", insets);
			UIManager.getLookAndFeelDefaults().put("TabbedPane.tabAreaInsets", insets);
			//		UIManager.getLookAndFeelDefaults().put("", insets);
			//		UIManager.getLookAndFeelDefaults().put("", insets);

			//		UIManager.getLookAndFeelDefaults().values().forEach((p) -> {
			//			System.out.println(p.toString());
			//		});

			setupChart();

 
			/*waterfallPlot = new WaterfallPlot(chartPanel, 300);
			waterfallPaletteStartMarker = new ValueMarker(waterfallPlot.getSpectrumPaletteStart(), colors.palette2,
					new BasicStroke(1f));
			waterfallPaletteEndMarker = new ValueMarker(
					waterfallPlot.getSpectrumPaletteStart() + waterfallPlot.getSpectrumPaletteSize(), colors.palette2,
					new BasicStroke(1f));
			//		chart.getXYPlot().addRangeMarker(waterfallPaletteStartMarker);
			//		chart.getXYPlot().addRangeMarker(waterfallPaletteEndMarker);*/

			printInit(2);

			printInit(3);
			
			
			JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, chartPanel, waterfallPlot);
			splitPane.setResizeWeight(0.8);
			splitPane.setBorder(null);

			labelMessages = new JLabel("");
			labelMessages.setForeground(Color.white);
			labelMessages.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
			parameterDebugDisplay.addListener((debug) -> {
				labelMessages.setVisible(debug);
			});
			parameterDebugDisplay.callObservers();
			
			JPanel splitPanePanel	= new JPanel(new BorderLayout());
			//splitPanePanel.setBackground(Color.black);
			splitPanePanel.add(splitPane, BorderLayout.CENTER);
			//splitPanePanel.add(labelMessages, BorderLayout.SOUTH);

			frame = new JFrame();
			frame.setUndecorated(captureGIF);
			frame.setExtendedState(frame.getExtendedState() | Frame.MAXIMIZED_BOTH);
			frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
			frame.getContentPane().setLayout(new BorderLayout());
			//frame.setTitle("Spectrum Analyzer - hackrf_sweep");
			frame.getContentPane().add(splitPanePanel, BorderLayout.CENTER);
			frame.setMinimumSize(new Dimension(600, 600));
			try {
				frame.setIconImage(new ImageIcon("program.png").getImage());
			} catch (Exception e) {
				//			e.printStackTrace();
			}
			
			printInit(4);
			setupFrequencyAllocationTable();
			printInit(5);
			
			frame.pack();
			frame.setVisible(true);

			printInit(6);

			startLauncherThread();
			restartHackrfSweep();

			/**
			 * register parameter observers
			 */
			setupParameterObservers();

			//shutdown on exit
			Runtime.getRuntime().addShutdownHook(new Thread(() -> stopHackrfSweep()));

			if (captureGIF) {
				try {
					gifCap = new ScreenCapture(frame, 35 * 1, 10, 5, 760, 660, new File("screenshot.gif"));
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}

		@Override
		public ModelValueBoolean getAntennaPowerEnable() {
			return parameterAntPower;
		}

		@Override
		public ModelValueInt getFFTBinHz() {
			return parameterFFTBinHz;
		}

		@Override
		public ModelValue<FrequencyRange> getFrequency() {
			return parameterFrequency;
		}

		@Override
		public ModelValue<FrequencyAllocationTable> getFrequencyAllocationTable() {
			return parameterFrequencyAllocationTable;
		}

		@Override
		public ModelValueInt getGain() {
			return parameterGainTotal;
		}

		@Override
		public ModelValueInt getGainLNA() {
			return parameterGainLNA;
		}

		@Override
		public ModelValueInt getGainVGA() {
			return parameterGainVGA;
		}

		@Override
		public ModelValueBoolean getAntennaLNA() {
			return parameterAntennaLNA;
		}
		
		@Override
		public ModelValueInt getPeakFallRate() {
			return parameterPeakFallRateSecs;
		}

		@Override
		public ModelValueInt getSamples() {
			return parameterSamples;
		}

		@Override
		public ModelValue<BigDecimal> getSpectrumLineThickness() {
			return parameterSpectrumLineThickness;
		}
		
		@Override
		public ModelValueInt getPersistentDisplayDecayRate() {
			return parameterPersistentDisplayPersTime;
		}

		@Override
		public ModelValueInt getSpectrumPaletteSize() {
			return parameterSpectrumPaletteSize;
		}

		@Override
		public ModelValueInt getSpectrumPaletteStart() {
			return parameterSpectrumPaletteStart;
		}

		@Override
		public ModelValueBoolean isCapturingPaused() {
			return parameterIsCapturingPaused;
		}

		@Override
		public ModelValueBoolean isChartsPeaksVisible() {
			return parameterShowPeaks;
		}
		
		@Override
		public ModelValueBoolean isDebugDisplay() {
			return parameterDebugDisplay;
		}

		@Override
		public ModelValueBoolean isFilterSpectrum() {
			return parameterFilterSpectrum;
		}

		@Override
		public ModelValueBoolean isPersistentDisplayVisible() {
			return parameterPersistentDisplay;
		}

		@Override
		public ModelValueBoolean isSpurRemoval() {
			return this.parameterSpurRemoval;
		}

		@Override
		public ModelValueBoolean isWaterfallVisible() {
			return parameterWaterfallVisible;
		}

		@Override
		public void newSpectrumData(boolean fullSweepDone, double[] frequencyStart, float fftBinWidthHz,
				float[] signalPowerdBm) {
			//		System.out.println(frequencyStart+" "+fftBinWidthHz+" "+signalPowerdBm);
			fireHardwareStateChanged(true);
			if (!hwProcessingQueue.offer(new FFTBins(fullSweepDone, frequencyStart, fftBinWidthHz, signalPowerdBm))) {
				System.out.println("queue full");
				dropped++;
			}
		}

		@Override
		public void registerListener(HackRFEventListener listener) {
			hRFlisteners.add(listener);
		}

		@Override
		public void removeListener(HackRFEventListener listener) {
			hRFlisteners.remove(listener);
		}

		private void fireCapturingStateChanged() {
			SwingUtilities.invokeLater(() -> {
				synchronized (hRFlisteners) {
					for (HackRFEventListener hackRFEventListener : hRFlisteners) {
						try {
							hackRFEventListener.captureStateChanged(!parameterIsCapturingPaused.getValue());
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
				}
			});
		}

		private void fireHardwareStateChanged(boolean sendingData) {
			if (this.flagIsHWSendingData != sendingData) {
				this.flagIsHWSendingData = sendingData;
				SwingUtilities.invokeLater(() -> {
					synchronized (hRFlisteners) {
						for (HackRFEventListener hackRFEventListener : hRFlisteners) {
							try {
								hackRFEventListener.hardwareStatusChanged(sendingData);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
				});
			}
		}

		private FrequencyRange getFreq() {
			return parameterFrequency.getValue();
		}

		private void printInit(int initNumber) {
			//		System.out.println("Startup "+(initNumber++)+" in " + (System.currentTimeMillis() - initTime) + "ms");
		}

		private void processingThread() {
			long counter = 0;
			long frameCounterChart = 0;

			//mainWhile:
			//while(true)
			{
				FFTBins bin1 = null;
				try {
					bin1 = hwProcessingQueue.take();
				} catch (InterruptedException e1) {
					return;
				}
				float binHz = bin1.fftBinWidthHz;

				/**
				 * prevents from spectrum chart from using too much CPU
				 */
				int limitChartRefreshFPS		= 30;
				int limitPersistentRefreshEveryChartFrame	= 2;
				
				//			PowerCalibration calibration	 = new PowerCalibration(-45, -12.5, 40); 

				datasetSpectrum = new DatasetSpectrumPeak(binHz, getFreq().getStartMHz(), getFreq().getEndMHz(),
						spectrumInitValue, 15, parameterPeakFallRateSecs.getValue() * 1000);
				chart.getXYPlot().getDomainAxis().setRange(getFreq().getStartMHz(), getFreq().getEndMHz());

				XYSeries spectrumPeaksEmpty	= new XYSeries("peaks");
				
				float maxPeakJitterdB = 6;
				float peakThresholdAboveNoise = 4;
				int maxPeakBins = 4;
				int validIterations = 25;
				spurFilter = new SpurFilter(maxPeakJitterdB, peakThresholdAboveNoise, maxPeakBins, validIterations,
						datasetSpectrum);

				long lastChartUpdated = System.currentTimeMillis();
				long lastScanStartTime = System.currentTimeMillis();
				double lastFreq = 0;

				while (true) {
					try {
						counter++;
						FFTBins bins = hwProcessingQueue.take();
						if (parameterIsCapturingPaused.getValue())
							continue;
						boolean triggerChartRefresh = bins.fullSweepDone;
						//continue;
					
						if (bins.freqStart != null && bins.sigPowdBm != null) {
							//						PowerCalibration.correctPower(calibration, parameterGaindB, bins);
							datasetSpectrum.addNewData(bins);
						}

						if ((triggerChartRefresh/* || timeDiff > 1000 */)) {
							//						System.out.println("ctr "+counter+" dropped "+dropped);
							/**
							 * filter first
							 */
							if (parameterSpurRemoval.getValue()) {
								long start	= System.nanoTime();
								spurFilter.filterDataset();
								synchronized (perfWatch) {
									perfWatch.spurFilter.addDrawingTime(System.nanoTime()-start);
								}
							}
							/**
							 * after filtering, calculate peak spectrum
							 */
							if (parameterShowPeaks.getValue()) {
								datasetSpectrum.refreshPeakSpectrum();
								//**waterfallPlot.setStatusMessage(String.format("Total Spectrum Peak Power %.1fdBm",datasetSpectrum.calculateSpectrumPeakPower()), 0);
							}

							/**
							 * Update performance counters
							 */
							if (System.currentTimeMillis() - perfWatch.lastStatisticsRefreshed > 1000) {
								synchronized (perfWatch) {
//									waterfallPlot.setStatusMessage(perfWatch.generateStatistics(), 1);
									//**perfWatch.waterfallDraw.nanosSum	= waterfallPlot.getDrawTimeSumAndReset();
									//**perfWatch.waterfallDraw.count	= waterfallPlot.getDrawingCounterAndReset();
									String stats	= perfWatch.generateStatistics();
									SwingUtilities.invokeLater(() -> {
										labelMessages.setText(stats);
									});
									perfWatch.reset();
								}
							}

							boolean flagChartRedraw	= false;
							/**
							 * Update chart in the swing thread
							 */
							if (System.currentTimeMillis() - lastChartUpdated > 1000/limitChartRefreshFPS) {
								flagChartRedraw	= true;
								frameCounterChart++;
								lastChartUpdated = System.currentTimeMillis();
							}

							
							XYSeries spectrumSeries;
							XYSeries spectrumPeaks;

							if (true) {
								spectrumSeries = datasetSpectrum.createSpectrumDataset("spectrum");

								if (parameterShowPeaks.getValue()) {
									spectrumPeaks = datasetSpectrum.createPeaksDataset("peaks");
								} else {
									spectrumPeaks = spectrumPeaksEmpty;
								}
							} else {
								spectrumSeries = new XYSeries("spectrum", false, true);
								spectrumSeries.setNotify(false);
								datasetSpectrum.fillToXYSeries(spectrumSeries);
								spectrumSeries.setNotify(true);

								spectrumPeaks =
										//									new XYSeries("peaks");
										new XYSeries("peaks", false, true);
								if (parameterShowPeaks.getValue()) {
									spectrumPeaks.setNotify(false);
									datasetSpectrum.fillPeaksToXYSeries(spectrumPeaks);
									spectrumPeaks.setNotify(false);
								}
							}

							if (parameterPersistentDisplay.getValue()) {
								long start	= System.nanoTime();
								boolean redraw	= false;
								if (flagChartRedraw && frameCounterChart % limitPersistentRefreshEveryChartFrame == 0)
									redraw	= true;
								
								//persistentDisplay.drawSpectrumFloat
								persistentDisplay.drawSpectrum2
								(datasetSpectrum,
										(float) chart.getXYPlot().getRangeAxis().getRange().getLowerBound(),
										(float) chart.getXYPlot().getRangeAxis().getRange().getUpperBound(), redraw);
								synchronized (perfWatch) {
									perfWatch.persisentDisplay.addDrawingTime(System.nanoTime()-start);	
								}
							}

							/**
							 * do not render it in swing thread because it might
							 * miss data
							 */
							if (parameterWaterfallVisible.getValue()) {
								long start	= System.nanoTime();
								//**waterfallPlot.addNewData(datasetSpectrum);
								synchronized (perfWatch) {
									perfWatch.waterfallUpdate.addDrawingTime(System.nanoTime()-start);	
								}
							}
							
							if (flagChartRedraw) {
								if (parameterWaterfallVisible.getValue()) {
									//**waterfallPlot.repaint();
								}
								SwingUtilities.invokeLater(() -> {

									chart.setNotify(false);

									chartDataset.removeAllSeries();
									chartDataset.addSeries(spectrumPeaks);
									chartDataset.addSeries(spectrumSeries);
									chart.setNotify(true);

									if (gifCap != null) {
										gifCap.captureFrame();
									}
								});
							}

							synchronized (perfWatch) {
								perfWatch.hwFullSpectrumRefreshes++;
							}

							counter = 0;
						}

					} catch (InterruptedException e) {
						return;
					}
				}

			}

		}

		private void recalculateGains(int totalGain) {
			/**
			 * use only lna gain when <=40 when >40, add only vga gain
			 */
			int lnaGain = totalGain / 8 * 8; //lna gain has step 8, range <0, 40>
			if (lnaGain > 40)
				lnaGain = 40;
			int vgaGain = lnaGain != 40 ? 0 : ((totalGain - lnaGain) & ~1); //vga gain has step 2, range <0,60>
			this.parameterGainLNA.setValue(lnaGain);
			this.parameterGainVGA.setValue(vgaGain);
			this.parameterGainTotal.setValue(lnaGain + vgaGain);
		}

		/**
		 * uses fifo queue to process launch commands, only the last launch command
		 * is important, delete others
		 */
		private synchronized void restartHackrfSweep() {
			if (threadLaunchCommands.offer(0) == false) {
				threadLaunchCommands.clear();
				threadLaunchCommands.offer(0);
			}
		}

		/**
		 * no need to synchronize, executes only in the launcher thread
		 */
		private void restartHackrfSweepExecute() {
			stopHackrfSweep();
			threadHackrfSweep = new Thread(() -> {
				Thread.currentThread().setName("hackrf_sweep");
				try {
					forceStopSweep = false;
					sweep();
				} catch (IOException e) {
					e.printStackTrace();
				}
			});
			threadHackrfSweep.start();
		}

		private void setupChart() {
			int axisWidthLeft = 100;
			int axisWidthRight = 20;

			chart = ChartFactory.createXYLineChart("Spectrum analyzer", "Frequency [MHz]", "Power [dB]", chartDataset,
					PlotOrientation.VERTICAL, false, false, false);
			chart.getRenderingHints().put(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

			XYPlot plot = chart.getXYPlot();
			NumberAxis domainAxis = ((NumberAxis) plot.getDomainAxis());
			NumberAxis rangeAxis = ((NumberAxis) plot.getRangeAxis());
			chartLineRenderer = new XYLineAndShapeRenderer();
			chartLineRenderer.setDefaultShapesVisible(false);
			chartLineRenderer.setDefaultStroke(new BasicStroke(parameterSpectrumLineThickness.getValue().floatValue()));

			rangeAxis.setAutoRange(false);
			rangeAxis.setRange(-110, 20);
			rangeAxis.setTickUnit(new NumberTickUnit(10, new DecimalFormat("###")));

			domainAxis.setNumberFormatOverride(new DecimalFormat(" #.### "));

			chartLineRenderer.setAutoPopulateSeriesStroke(false);
			chartLineRenderer.setAutoPopulateSeriesPaint(false);
			chartLineRenderer.setSeriesPaint(0, colors.palette2);

			if (false)
				chart.addProgressListener(new ChartProgressListener() {
					StandardTickUnitSource tus = new StandardTickUnitSource();

					@Override
					public void chartProgress(ChartProgressEvent event) {
						if (event.getType() == ChartProgressEvent.DRAWING_STARTED) {
							Range r = domainAxis.getRange();
							domainAxis.setTickUnit((NumberTickUnit) tus.getCeilingTickUnit(r.getLength() / 20));
							domainAxis.setMinorTickCount(2);
							domainAxis.setMinorTickMarksVisible(true);

						}
					}
				});

			plot.setDomainGridlinesVisible(false);
			plot.setRenderer(chartLineRenderer);

			/**
			 * sets empty space around the plot
			 */
			AxisSpace axisSpace = new AxisSpace();
			axisSpace.setLeft(axisWidthLeft);
			axisSpace.setRight(axisWidthRight);
			axisSpace.setTop(0);
			axisSpace.setBottom(50);
			plot.setFixedDomainAxisSpace(axisSpace);//sets width of the domain axis left/right
			plot.setFixedRangeAxisSpace(axisSpace);//sets heigth of range axis top/bottom

			rangeAxis.setAxisLineVisible(false);
			rangeAxis.setTickMarksVisible(false);

			plot.setAxisOffset(RectangleInsets.ZERO_INSETS); //no space between range axis and plot

			Font labelFont = new Font(Font.MONOSPACED, Font.BOLD, 16);
			rangeAxis.setLabelFont(labelFont);
			rangeAxis.setTickLabelFont(labelFont);
			rangeAxis.setLabelPaint(colors.palette1);
			rangeAxis.setTickLabelPaint(colors.palette1);
			domainAxis.setLabelFont(labelFont);
			domainAxis.setTickLabelFont(labelFont);
			domainAxis.setLabelPaint(colors.palette1);
			domainAxis.setTickLabelPaint(colors.palette1);
			chartLineRenderer.setDefaultPaint(Color.white);
			plot.setBackgroundPaint(colors.palette4);
			chart.setBackgroundPaint(colors.palette4);
			chartLineRenderer.setSeriesPaint(1, colors.palette1);

			chartPanel = new ChartPanel(chart);
			chartPanel.setMaximumDrawWidth(4096);
			chartPanel.setMaximumDrawHeight(2160);
			chartPanel.setMouseWheelEnabled(false);
			chartPanel.setDomainZoomable(false);
			chartPanel.setRangeZoomable(false);
			chartPanel.setPopupMenu(null);
			chartPanel.setMinimumSize(new Dimension(200, 200));

			printInit(1);

		 
		}

		/**
		 * Displays a cross marker with current frequency and signal strength when
		 * mouse hovers over the frequency chart
		 */
	 

		private void setupFrequencyAllocationTable() {
			SwingUtilities.invokeLater(() -> {
				chartPanel.addComponentListener(new ComponentAdapter() {
					public void componentResized(ComponentEvent e) {
						redrawFrequencySpectrumTable();
					}
				});
				chart.getXYPlot().getDomainAxis().addChangeListener((e) -> {
					redrawFrequencySpectrumTable();
				});
				chart.getXYPlot().getRangeAxis().addChangeListener(event -> {
					redrawFrequencySpectrumTable();
					System.out.println(event);
				});

			});
			parameterFrequencyAllocationTable.addListener(this::redrawFrequencySpectrumTable);
		}

		private void setupParameterObservers() {
			Runnable restartHackrf = this::restartHackrfSweep;
			parameterFrequency.addListener(restartHackrf);
			parameterAntPower.addListener(restartHackrf);
			parameterAntennaLNA.addListener(restartHackrf);
			parameterFFTBinHz.addListener(restartHackrf);
			parameterSamples.addListener(restartHackrf);
			parameterIsCapturingPaused.addListener(this::fireCapturingStateChanged);

			parameterGainTotal.addListener((gainTotal) -> {
				if (flagManualGain) //flag is being adjusted manually by LNA or VGA, do not recalculate the gains
					return;
				recalculateGains(gainTotal);
				restartHackrfSweep();
			});
			Runnable gainRecalc = () -> {
				int totalGain = parameterGainLNA.getValue() + parameterGainVGA.getValue();
				flagManualGain = true;
				try {
					parameterGainTotal.setValue(totalGain);
				} catch (Exception e) {
					e.printStackTrace();
				} finally {
					flagManualGain = false;
				}
				restartHackrfSweep();
			};
			parameterGainLNA.addListener(gainRecalc);
			parameterGainVGA.addListener(gainRecalc);

			parameterSpurRemoval.addListener(() -> {
				SpurFilter filter = spurFilter;
				if (filter != null) {
					filter.recalibrate();
				}
			});
			parameterShowPeaks.addListener(() -> {
				DatasetSpectrumPeak p = datasetSpectrum;
				if (p != null) {
					p.resetPeaks();
				}
			});
			/***parameterSpectrumPaletteStart.setValue((int) waterfallPlot.getSpectrumPaletteStart());
			parameterSpectrumPaletteSize.setValue((int) waterfallPlot.getSpectrumPaletteSize());
			parameterSpectrumPaletteStart.addListener((dB) -> {
				waterfallPlot.setSpectrumPaletteStart(dB);
				SwingUtilities.invokeLater(() -> {
					waterfallPaletteStartMarker.setValue(waterfallPlot.getSpectrumPaletteStart());
					waterfallPaletteEndMarker
							.setValue(waterfallPlot.getSpectrumPaletteStart() + waterfallPlot.getSpectrumPaletteSize());
				});
			});
			parameterSpectrumPaletteSize.addListener((dB) -> {
				if (dB < SPECTRUM_PALETTE_SIZE_MIN)
					return;
				waterfallPlot.setSpectrumPaletteSize(dB);
				SwingUtilities.invokeLater(() -> {
					waterfallPaletteStartMarker.setValue(waterfallPlot.getSpectrumPaletteStart());
					waterfallPaletteEndMarker
							.setValue(waterfallPlot.getSpectrumPaletteStart() + waterfallPlot.getSpectrumPaletteSize());
				});

			});*/
			parameterPeakFallRateSecs.addListener((fallRate) -> {
				datasetSpectrum.setPeakFalloutMillis(fallRate * 1000l);
			});

			parameterSpectrumLineThickness.addListener((thickness) -> {
				SwingUtilities.invokeLater(() -> chartLineRenderer.setDefaultStroke(new BasicStroke(thickness.floatValue())));
			});
			
			parameterPersistentDisplayPersTime.addListener((time) -> {
				persistentDisplay.setPersistenceTime(time);
			});

			int persistentDisplayDownscaleFactor = 4;

			Runnable resetPersistentImage = () -> {
				boolean display = parameterPersistentDisplay.getValue();
				persistentDisplay.reset();
				chart.getXYPlot().setBackgroundImage(display ? persistentDisplay.getDisplayImage().getValue() : null);
				chart.getXYPlot().setBackgroundImageAlpha(alphaPersistentDisplayImage);
			};
			persistentDisplay.getDisplayImage().addListener((image) -> {
				if (parameterPersistentDisplay.getValue())
					chart.getXYPlot().setBackgroundImage(image);
			});

			registerListener(new HackRFEventAdapter() {
				@Override
				public void hardwareStatusChanged(boolean hardwareSendingData) {
					SwingUtilities.invokeLater(() -> {
						if (hardwareSendingData && parameterPersistentDisplay.getValue()) {
							resetPersistentImage.run();
						}
					});
				}
			});

			parameterPersistentDisplay.addListener((display) -> {
				SwingUtilities.invokeLater(resetPersistentImage::run);
			});

			chartDataArea.addListener((area) -> {
				SwingUtilities.invokeLater(() -> {
					/*
					 * Align the waterfall plot and the spectrum chart
					 */
					/**if (waterfallPlot != null)
						waterfallPlot.setDrawingOffsets((int) area.getX(), (int) area.getWidth());*/

					/**
					 * persistent display config
					 */
					persistentDisplay.setImageSize((int) area.getWidth() / persistentDisplayDownscaleFactor,
							(int) area.getWidth() / persistentDisplayDownscaleFactor);
					if (parameterPersistentDisplay.getValue()) {
						chart.getXYPlot().setBackgroundImage(persistentDisplay.getDisplayImage().getValue());
						chart.getXYPlot().setBackgroundImageAlpha(alphaPersistentDisplayImage);
					}
				});
			});
		}

		private void startLauncherThread() {
			threadLauncher = new Thread(() -> {
				Thread.currentThread().setName("Launcher-thread");
				while (true) {
					try {
						threadLaunchCommands.take();
						restartHackrfSweepExecute();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			});
			threadLauncher.start();
		}

		/**
		 * no need to synchronize, executes only in launcher thread
		 */
		private void stopHackrfSweep() {
			forceStopSweep = true;
			if (threadHackrfSweep != null) {
				while (threadHackrfSweep.isAlive()) {
					forceStopSweep = true;
					//				System.out.println("Calling HackRFSweepNativeBridge.stop()");
					HackRFSweepNativeBridge.stop();
					try {
						Thread.sleep(20);
					} catch (InterruptedException e) {
					}
				}
				try {
					threadHackrfSweep.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				threadHackrfSweep = null;
			}
			System.out.println("HackRFSweep thread stopped.");
			if (threadProcessing != null) {
				threadProcessing.interrupt();
				try {
					threadProcessing.join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				threadProcessing = null;
				System.out.println("Processing thread stopped.");
			}
		}

		private void sweep() throws IOException {
			lock.lock();
			try {
				threadProcessing = new Thread(() -> {
					Thread.currentThread().setName("hackrf_sweep data processing thread");
					processingThread();
				});
				threadProcessing.start();

				/**
				 * Ensures auto-restart if HW disconnects
				 */
				while (forceStopSweep == false) {
					System.out.println(
							"Starting hackrf_sweep... " + getFreq().getStartMHz() + "-" + getFreq().getEndMHz() + "MHz ");
					System.out.println("hackrf_sweep params:  freq " + getFreq().getStartMHz() + "-" + getFreq().getEndMHz()
							+ "MHz  FFTBin " + parameterFFTBinHz.getValue() + "Hz  samples " + parameterSamples.getValue()
							+ "  lna: " + parameterGainLNA.getValue() + " vga: " + parameterGainVGA.getValue() + " antenna_lna: "+parameterAntennaLNA.getValue());
					fireHardwareStateChanged(false);
					HackRFSweepNativeBridge.start(this, getFreq().getStartMHz(), getFreq().getEndMHz(),
							parameterFFTBinHz.getValue(), parameterSamples.getValue(), parameterGainLNA.getValue(),
							parameterGainVGA.getValue(), parameterAntPower.getValue(), parameterAntennaLNA.getValue());
					fireHardwareStateChanged(false);
					if (forceStopSweep == false) {
						Thread.sleep(1000);
					}
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				lock.unlock();
				fireHardwareStateChanged(false);
			}
		}

		protected void redrawFrequencySpectrumTable() {
			Rectangle2D area = chartPanel.getChartRenderingInfo().getPlotInfo().getDataArea();
			FrequencyAllocationTable activeTable = parameterFrequencyAllocationTable.getValue();
			if (activeTable == null) {
				imageFrequencyAllocationTableBands = null;
			} else if (area.getWidth() > 0 && area.getHeight() > 0) {
				imageFrequencyAllocationTableBands = activeTable.drawAllocationTable((int) area.getWidth(),
						(int) area.getHeight(), alphaFreqAllocationTableBandsImage, getFreq().getStartMHz() * 1000000l,
						getFreq().getEndMHz() * 1000000l,
						//colors.palette4, 
						Color.white,
						//colors.palette1
						Color.DARK_GRAY);
			}
		}

	}

 