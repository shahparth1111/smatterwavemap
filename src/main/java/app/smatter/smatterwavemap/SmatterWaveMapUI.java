package app.smatter.smatterwavemap;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.Image;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisSpace;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.NumberTickUnit;
import org.jfree.chart.axis.StandardTickUnitSource;
import org.jfree.chart.event.ChartProgressEvent;
import org.jfree.chart.event.ChartProgressListener;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.chart.title.TextTitle;
import org.jfree.chart.ui.RectangleInsets;
import org.jfree.data.Range;
import org.jfree.data.xy.XYSeries;
import app.smatter.smatterwavemap.FFTFactory.JavaFFT;
import app.smatter.smatterwavemap.SmatterWaveMap.ColorScheme;
import jspectrumanalyzer.capture.ScreenCapture;
import jspectrumanalyzer.core.DatasetSpectrumPeak;
import jspectrumanalyzer.core.FFTBins;
import jspectrumanalyzer.core.FrequencyAllocationTable;
import jspectrumanalyzer.core.FrequencyAllocations;
import jspectrumanalyzer.core.FrequencyRange;
import jspectrumanalyzer.core.PersistentDisplay;
import jspectrumanalyzer.core.SpurFilter;
import jspectrumanalyzer.core.jfc.XYSeriesCollectionImmutable;
import jspectrumanalyzer.nativebridge.HackRFSweepNativeBridge;
import jspectrumanalyzer.ui.WaterfallPlot;
import shared.mvc.ModelValue;
import shared.mvc.ModelValue.ModelValueBoolean;
import shared.mvc.ModelValue.ModelValueInt;
import javax.swing.*;
import javax.swing.plaf.FontUIResource;
import javax.sound.sampled.*;
import java.security.SecureRandom;
import jspectrumanalyzer.core.HackRFSettings;
import jspectrumanalyzer.nativebridge.HackRFSweepDataCallback;
import java.util.Timer;
import java.util.TimerTask;

public class SmatterWaveMapUI implements HackRFSettings, HackRFSweepDataCallback {
    private static final float SAMPLE_RATE = 96000f; // try 48000 or 96000 for more ultrasound
    private static final int SAMPLE_SIZE_BITS = 16;
    private static final int CHANNELS = 1; // mono
    private static final boolean SIGNED = true;
    private static final boolean BIG_ENDIAN = false;
    private static final int NUM_BANDS = 6; // number of independent bandpass bands
    private static final double[] DEFAULT_BAND_CENTERS = {200, 600, 1500, 5000, 12000, 22000};
    private static final double[] DEFAULT_BAND_Q = {1.0, 1.0, 1.2, 0.9, 0.8, 0.7};
    private static final int MIN_ENV_MS = 80;
    private static final int MAX_ENV_MS = 600;
    private static final double BROWN_LEVEL = 0.45;
    private static final double MASTER_GAIN = 0.35;
    private static final SecureRandom GLOBAL_RNG = new SecureRandom();
	Icon imageIcon11;
	static String languageCode="gu";
	static int durationSeconds = 0;
	static boolean saveWav = false;
	static Thread playThread;
	public static final int	SPECTRUM_PALETTE_SIZE_MIN	= 5;
	private static boolean captureGIF= false;
	private static long initTime = System.currentTimeMillis();
	private JFrame frame;
	public boolean									flagIsHWSendingData						= false;
	private float									alphaFreqAllocationTableBandsImage	= 0.5f;
	private float									alphaPersistentDisplayImage			= 1.0f;
	private JFreeChart								chart;
	private ModelValue<Rectangle2D>					chartDataArea						= new ModelValue<Rectangle2D>("Chart data area", new Rectangle2D.Double(0, 0, 1, 1));
	private XYSeriesCollectionImmutable				chartDataset								= new XYSeriesCollectionImmutable();
	private XYLineAndShapeRenderer					chartLineRenderer;
	private ChartPanel								chartPanel;
	private ColorScheme								colors								= new ColorScheme();
	private DatasetSpectrumPeak						datasetSpectrum;
	private int										dropped								= 0;
	private volatile boolean						flagManualGain						= false;
	private volatile boolean						forceStopSweep						= false;
	private ScreenCapture							gifCap								= null;
	private ArrayList<HackRFEventListener>			hRFlisteners							= new ArrayList<>();
	private ArrayBlockingQueue<FFTBins>				hwProcessingQueue						= new ArrayBlockingQueue<>(1000);
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
	private TextTitle								titleFreqBand						= new TextTitle("",new Font("Dialog", Font.PLAIN, 11));
	private ValueMarker								waterfallPaletteEndMarker;
	private ValueMarker								waterfallPaletteStartMarker;
	private WaterfallPlot							waterfallPlot;
	private JLabel labelMessages;
	private RuntimePerformanceWatch					perfWatch							= new RuntimePerformanceWatch();
	private static final float NORMALIZATION_FACTOR_2_BYTES = Short.MAX_VALUE + 1.0f;
	static String percentMatchCount="0";
	static JLabel percentMatch; 
	public static double updatedPCount=0;
	/**
	 * @wbp.nonvisual location=-20,-21
	 */
	private final JPanel panel = new JPanel();
	/**
	 * @wbp.nonvisual location=470,149
	 */
	private final JSplitPane splitPane_1 = new JSplitPane();
	
	public static void main(String[] args) {
		SmatterWaveMapUI window = new SmatterWaveMapUI();
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					window.frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
            	window.updateTime();
            }
        }, 0, 1000);  
		window.SWU();
	}

    public void updateTime() {

    	SwingUtilities.invokeLater(new Runnable() {
    	    public void run() {
    	    	percentMatchCount = updatedPCount+ "%";
    	    	percentMatch.setForeground(Color.GREEN);
    	    	percentMatch.setText(percentMatchCount);
    	    	percentMatch.setVisible(true);
    	    }
    	});
    	
        
    }
 

	void loadAudioFrequency() {
        final AudioFormat format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, 44100, 16, 1, 2, 44100, false);
        final DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
        TargetDataLine targetLine;
        byte[] buf = new byte[256]; // <--- increase this for higher frequency resolution
        int numberOfSamples = buf.length / format.getFrameSize();
        JavaFFT fft = new JavaFFT(numberOfSamples);
        try {
	        targetLine = (TargetDataLine) AudioSystem.getLine(info);
	        targetLine.open();
	        targetLine.start();
	        final AudioInputStream audioStream = new AudioInputStream(targetLine);
            while (true) {
                // in real impl, don't just ignore how many bytes you read
                audioStream.read(buf);
                // the stream represents each sample as two bytes -> decode
                final float[] samples = decode(buf, format);
                final float[][] transformed = fft.transform(samples);
                final float[] realPart = transformed[0];
                final float[] imaginaryPart = transformed[1];
                final double[] magnitudes = toMagnitudes(realPart, imaginaryPart);
            }	
        }catch(Exception ignored) {}
    }

    private static float[] decode(final byte[] buf, final AudioFormat format) {
        final float[] fbuf = new float[buf.length / format.getFrameSize()];
        for (int pos = 0; pos < buf.length; pos += format.getFrameSize()) {
            final int sample = format.isBigEndian()
                    ? byteToIntBigEndian(buf, pos, format.getFrameSize())
                    : byteToIntLittleEndian(buf, pos, format.getFrameSize());
            // normalize to [0,1] (not strictly necessary, but makes things easier)
            fbuf[pos / format.getFrameSize()] = sample / NORMALIZATION_FACTOR_2_BYTES;
        }
        return fbuf;
    }

    private static double[] toMagnitudes(final float[] realPart, final float[] imaginaryPart) {
        final double[] powers = new double[realPart.length / 2];
        for (int i = 0; i < powers.length; i++) {
            powers[i] = Math.sqrt(realPart[i] * realPart[i] + imaginaryPart[i] * imaginaryPart[i]);
        }
        return powers;
    }

    private static int byteToIntLittleEndian(final byte[] buf, final int offset, final int bytesPerSample) {
        int sample = 0;
        for (int byteIndex = 0; byteIndex < bytesPerSample; byteIndex++) {
            final int aByte = buf[offset + byteIndex] & 0xff;
            sample += aByte << 8 * (byteIndex);
        }
        return sample;
    }

    private static int byteToIntBigEndian(final byte[] buf, final int offset, final int bytesPerSample) {
        int sample = 0;
        for (int byteIndex = 0; byteIndex < bytesPerSample; byteIndex++) {
            final int aByte = buf[offset + byteIndex] & 0xff;
            sample += aByte << (8 * (bytesPerSample - byteIndex - 1));
        }
        return sample;
    }

	public SmatterWaveMapUI() {
		initialize();
	}

	private void initialize() {
	
	}
	
	private void SWU() {
		printInit(0);
		frame = new JFrame("SmatterWaveMap Real-Time RF ⇆ Audio Correlation Engine");
		frame.setUndecorated(captureGIF);
		frame.getContentPane().setLayout(null);
		
		percentMatch = new JLabel(percentMatchCount+"%");
		percentMatch.setHorizontalAlignment(SwingConstants.CENTER); 
	    Dimension size= Toolkit.getDefaultToolkit().getScreenSize();
	    int width = (int)size.getWidth();
	    int height = (int)size.getHeight();
	    // Source - https://stackoverflow.com/a
	    // Posted by moolsbytheway
	    // Retrieved 2025-12-03, License - CC BY-SA 4.0
	    percentMatch.setFont(new FontUIResource(new Font("Cabin", Font.PLAIN, 60)));
	    percentMatch.setForeground(Color.white);
		percentMatch.setBounds(width/2-150, height-(height/8)-50, 300, 100);
		JButton btnNewButton = new JButton("Get Audio File Overlap");
		btnNewButton.setEnabled(true);
		btnNewButton.addActionListener(e -> selectAndProcessAudio());
		btnNewButton.setBounds(100, height/2+10, 200, 30);
		frame.getContentPane().add(btnNewButton);
		frame.getContentPane().add(percentMatch);
		
		/*percentMatchCount = "90" + "%";
		percentMatch.setText(percentMatchCount);
 
		 Timer timer = new Timer(3000, e -> {
        	 percentMatchCount = "45";
        	 SwingUtilities.invokeLater(new Runnable() {
        		    public void run() {
        		    	percentMatch.setForeground(Color.GREEN);
        		    	percentMatch.setText(percentMatchCount+"%");
        		    	percentMatch.setVisible(true);
        		    }
        		});

		     ((Timer)e.getSource()).stop();
		 });

		 timer.start();
		*/
		if (captureGIF) {
			parameterFrequency.setValue(new FrequencyRange(76, 2700));
			//parameterFrequency.setValue(new FrequencyRange(2400, 2700));
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
		//splitPane.setResizeWeight(1);
		splitPane.setBorder(null);
		labelMessages = new JLabel("");
		labelMessages.setForeground(Color.white);
		labelMessages.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
		parameterDebugDisplay.addListener((debug) -> {
			labelMessages.setVisible(debug);
		});
		parameterDebugDisplay.callObservers();
		JPanel splitPanePanel	= new JPanel(new BorderLayout());
		splitPanePanel.add(splitPane, BorderLayout.CENTER);
 	 
		printInit(4);
		setupFrequencyAllocationTable();
		printInit(5);
		//frame.pack();
		//frame.setVisible(true);
		printInit(6);
		startLauncherThread();
		restartHackrfSweep();
		setupParameterObservers();
		Runtime.getRuntime().addShutdownHook(new Thread(() -> stopHackrfSweep()));
		if (captureGIF) {
			try {
				gifCap = new ScreenCapture(frame, 35 * 1, 10, 5, 760, 660, new File("screenshot.gif"));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		JLabel lblNewLabel = new JLabel("");
		lblNewLabel.setBounds(0, 0, 1366, 768);
		//splitPanePanel.setBackground(Color.black);
		splitPanePanel.add(splitPane, BorderLayout.CENTER);
		//splitPanePanel.add(labelMessages, BorderLayout.SOUTH);
		/*
		Dimension screenRes = Toolkit.getDefaultToolkit().getScreenSize();
		int width = Integer.parseInt(screenRes.getWidth()+"");
		double height = screenRes.getHeight();
		frame = new JFrame("SmatterWaveMap Real-Time RF ⇆ Audio Correlation Engine");
		frame.setUndecorated(captureGIF);
		//frame.setExtendedState(frame.getExtendedState() | Frame.MAXIMIZED_BOTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.getContentPane().setLayout(new BorderLayout());*/
		Image img1=new ImageIcon(this.getClass().getResource("smattericon.png")).getImage();
		frame.setIconImage(img1);
		frame.setUndecorated(captureGIF);
		splitPanePanel.setPreferredSize(new Dimension(300, 300));
		//frame.setExtendedState(frame.getExtendedState() | Frame.MAXIMIZED_BOTH);
		frame.getContentPane().setLayout(null);
		//frame.getContentPane().add(splitPanePanel);
		frame.getContentPane().add(lblNewLabel);
		// Ensure JSplitPane preferred size matters
		splitPanePanel.setBounds((width/21)+10, (height/(5))+20, width-((width/8)), (height/4)+10);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		//frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(splitPanePanel);
		// Final UI adjustments
		//frame.pack();       // Use preferred sizes
		//frame.setLocationRelativeTo(null);  // Center window
		frame.setVisible(true);
		SpectrumPanel spectrumPanel = new SpectrumPanel();
        JLabel infoLabel = new JLabel("Audio Frequency Display", SwingConstants.CENTER);
        JSplitPane splitPaneEAudio = new JSplitPane(JSplitPane.VERTICAL_SPLIT, spectrumPanel, infoLabel);
		JPanel splitPanePanelEA	= new JPanel(new BorderLayout());
		splitPanePanelEA.add(splitPaneEAudio, BorderLayout.CENTER);
        splitPaneEAudio.setDividerLocation(400);
        //AudioPlayer player = new AudioPlayer(spectrumPanel);
        AudioPlayerFull player = new AudioPlayerFull(spectrumPanel);
        try {
			player.play("C:/Users/ParthShahSmatterLLP/Desktop/goodlife.wav");
		} catch (LineUnavailableException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        splitPanePanelEA.setBounds(width/20, height/2+(height/16), width-(width/8), (height/2 - height/4));
		JLabel frequencySpectrumDisplayBackgroud = new JLabel("");
		frequencySpectrumDisplayBackgroud.setBounds(0, 0, width, height);
		Image backgroundImg=new ImageIcon(this.getClass().getResource("pn.png")).getImage();
		frequencySpectrumDisplayBackgroud.setIcon(new ImageIcon(backgroundImg));
		frame.getContentPane().add(frequencySpectrumDisplayBackgroud);
        frame.getContentPane().add(splitPanePanelEA);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setBounds(0, 0, width, height);
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
	
	public static double computeSpectralPatternMatch(double[] magsA, double[] magsB) {

	    int n = magsA.length;
	    int m = magsB.length;
        
	    // DTW matrix
	    double[][] dtw = new double[n + 1][m + 1];

	    for (int i = 0; i <= n; i++)
	        Arrays.fill(dtw[i], Double.POSITIVE_INFINITY);

	    dtw[0][0] = 0.0;

	    for (int i = 1; i <= n; i++) {
	        for (int j = 1; j <= m; j++) {

	            double cost = Math.abs(magsA[i - 1] - magsB[j - 1]);

	            // dynamic time warping recurrence
	            dtw[i][j] = cost + Math.min(
	                dtw[i - 1][j],         // insertion
	                Math.min(
	                    dtw[i][j - 1],     // deletion
	                    dtw[i - 1][j - 1]  // match
	                )
	            );
	        }
	    }

	    // final dtw accumulated deviation
	    double distance = dtw[n][m];

	    // convert DTW distance -> 0–100 overlay %
	    double normalization = Math.max(sum(magsA), sum(magsB));

	    double score = 100 * (1 - (distance / normalization));
	    System.out.println(n + " ** *************************"+score+"******************************** " +m);
	    return Math.max(0, Math.min(100, score));
	}

 


	private static double sum(double[] arr) {
	    double s = 0;
	    for (double v : arr) s += Math.abs(v);
	    return s;
	}


	/**
	 * Computes spectral overlay percentage between two FFT power/magnitude arrays.
	 * Uses cosine similarity — the most correct measure for spectrum overlap.
	 *
	 * @param fftA first spectrum magnitudes (e.g., live FFT)
	 * @param fftB second spectrum magnitudes (e.g., stored SDR spectrum)
	 * @return overlay match percentage (0–100)
	 */
	public static double computeSpectrumOverlayPercent(double[] fftA, double[] fftB) {

	    int len = Math.min(fftA.length, fftB.length);

	    double dot = 0;
	    double magA = 0;
	    double magB = 0;

	    for (int i = 0; i < len; i++) {

	        double a = fftA[i];
	        double b = fftB[i];

	        dot += a * b;
	        magA += a * a;
	        magB += b * b;
	    }

	    if (magA == 0 || magB == 0)
	        return 0.0;

	    double cosine = dot / (Math.sqrt(magA) * Math.sqrt(magB));

	    // Clamp numerical drift
	    cosine = Math.max(-1, Math.min(1, cosine));

	    return (cosine * 100.0);
	}
	
	/**
	 * Computes overlay match between audio FFT magnitudes and DatasetSpectrum power bins.
	 * Uses cosine similarity (best spectral similarity metric).
	 *
	 * @param mags      FFT magnitudes from audio analysis
	 * @param spectrum  power spectrum array from DatasetSpectrum
	 * @return percent match (0 - 100)
	 */
	public static double computeOverlayMatch(double[] mags, float[] spectrum) {
	    int len = Math.min(mags.length, spectrum.length);

	    double dot = 0;
	    double magA = 0;
	    double magB = 0;

	    for (int i = 0; i < len; i++) {

	        double a = mags[i];

	        // Convert SDR dBm-like values to linear-like weights
	        // 10^(x/20) is proper linear approximation for magnitude
	        double b = Math.pow(10, spectrum[i] / 20.0);

	        dot += a * b;
	        magA += a * a;
	        magB += b * b;
	    }

	    if (magA == 0 || magB == 0)
	        return 0.0;

	    double cosine = dot / (Math.sqrt(magA) * Math.sqrt(magB));
	    cosine = Math.max(-1.0, Math.min(1.0, cosine));

	    return cosine * 100.0;
	}

	
	private void selectAndProcessAudio() {
        JFileChooser fileChooser = new JFileChooser();
        int result = fileChooser.showOpenDialog(frame);
        if (result == JFileChooser.APPROVE_OPTION) {
            File audioFile = fileChooser.getSelectedFile();
            double[] samples = readAudioPCM(audioFile);

 
        }
    }

    /** Reads audio file and extracts raw samples **/
    private double[] readAudioPCM(File file) {
        try {
            AudioInputStream ais = AudioSystem.getAudioInputStream(file);
            AudioFormat format = ais.getFormat();

            if (format.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                JOptionPane.showMessageDialog(panel, this, "Use PCM WAV format audio.", dropped);
                return null;
            }

            byte[] rawData = ais.readAllBytes();
            int bytesPerSample = format.getSampleSizeInBits() / 8;
            int sampleCount = rawData.length / bytesPerSample;

            double[] samples = new double[sampleCount];

            for (int i = 0, sampleIndex = 0; i < rawData.length; i += bytesPerSample, sampleIndex++) {
                int value = 0;
                for (int b = 0; b < bytesPerSample; b++)
                    value |= (rawData[i + b] & 0xFF) << (8 * b);

                samples[sampleIndex] = value / 32768.0; // normalize
            }
            return samples;

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(panel, this, "Error reading audio: " + ex.getMessage(), dropped, imageIcon11);
            return null;
        }
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
			//PowerCalibration calibration	 = new PowerCalibration(-45, -12.5, 40); 
			datasetSpectrum = new DatasetSpectrumPeak(binHz, getFreq().getStartMHz(), getFreq().getEndMHz(),
					spectrumInitValue, 15, parameterPeakFallRateSecs.getValue() * 1000);
			chart.getXYPlot().getDomainAxis().setRange(getFreq().getStartMHz(), getFreq().getEndMHz());
			XYSeries spectrumPeaksEmpty	= new XYSeries("peaks");
			float maxPeakJitterdB = 6;
			float peakThresholdAboveNoise = 4;
			int maxPeakBins = 4;
			int validIterations = 25;
			spurFilter = new SpurFilter(maxPeakJitterdB, peakThresholdAboveNoise, maxPeakBins, validIterations, datasetSpectrum);
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
//								waterfallPlot.setStatusMessage(perfWatch.generateStatistics(), 1);
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

        chart = ChartFactory.createXYLineChart("Spectrum analyzer Real-Time RF", "Frequency [MHz]", "Power [dB]", chartDataset,
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
		//rangeAxis.setLabelFont(labelFont);
		//rangeAxis.setTickLabelFont(labelFont);
		//rangeAxis.setLabelPaint(colors.palette1);
		//rangeAxis.setTickLabelPaint(colors.palette1);
		//domainAxis.setLabelFont(labelFont);
		//domainAxis.setTickLabelFont(labelFont);
		//domainAxis.setLabelPaint(colors.palette1);
		//domainAxis.setTickLabelPaint(colors.palette1);
		//chartLineRenderer.setDefaultPaint(Color.white);
		//plot.setBackgroundPaint(colors.palette4);
		//chart.setBackgroundPaint(colors.palette4);
		chartLineRenderer.setSeriesPaint(1, colors.palette1);

		chartPanel = new ChartPanel(chart);
		chartPanel.setMaximumDrawWidth(4096);
		//chartPanel.setMaximumDrawHeight(2160);
		chartPanel.setMouseWheelEnabled(false);
		chartPanel.setDomainZoomable(false);
		chartPanel.setRangeZoomable(false);
		chartPanel.setPopupMenu(null);
		chartPanel.setMinimumSize(new Dimension(200, 4096));

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
//			double timeSpentDrawingChartPerSec = chartDrawingSum / (timeElapsed / 1000d) / 1000d;
//			return String.format("Spectrum refreshes: %d / Chart redraws: %d / Drawing time in 1 sec %.2fs",
//					hwFullSpectrumRefreshes, chartRedrawed, timeSpentDrawingChartPerSec);

		}

		public synchronized void reset() {
			hwFullSpectrumRefreshes = 0;
			for (PerformanceEntry dataDrawingEntry : entries) {
				dataDrawingEntry.reset();
			}
			lastStatisticsRefreshed = System.currentTimeMillis();
		}
	}

}

