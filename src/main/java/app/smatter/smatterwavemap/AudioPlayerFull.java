package app.smatter.smatterwavemap;


import be.tarsos.dsp.AudioDispatcher;
import be.tarsos.dsp.io.jvm.AudioDispatcherFactory;
import be.tarsos.dsp.io.jvm.AudioPlayer;
import be.tarsos.dsp.AudioEvent;
import be.tarsos.dsp.AudioProcessor;
import be.tarsos.dsp.util.fft.FFT;
import be.tarsos.dsp.util.fft.HannWindow;
import jspectrumanalyzer.core.DatasetSpectrum;
import javax.swing.*;
import java.io.File;
import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

public class AudioPlayerFull {

    private static final int BUFFER_SIZE = 2048;
    private static final int OVERLAP = 1024;
    public static DatasetSpectrum ds;
    private AudioDispatcher dispatcher;
    private final SpectrumPanel spectrumPanel;
    public static double[] mags;
    public AudioPlayerFull(SpectrumPanel panel) {
        this.spectrumPanel = panel;
    }

    public void play(String audioFilePath) throws LineUnavailableException {
        try {

            dispatcher = AudioDispatcherFactory.fromFile(
                    new File(audioFilePath), BUFFER_SIZE, OVERLAP);

            // 🔥 Built-in playback component keeps dispatcher running full length
            AudioPlayer audioOutput = new AudioPlayer(dispatcher.getFormat());
            dispatcher.addAudioProcessor(audioOutput);

            final FFT fft = new FFT(BUFFER_SIZE);
            final HannWindow window = new HannWindow();
            final float[] fftBuffer = new float[BUFFER_SIZE * 2];
            final int half = BUFFER_SIZE / 2;

            dispatcher.addAudioProcessor(new AudioProcessor() {

                @Override
                public boolean process(AudioEvent audioEvent) {
                    float[] buffer = audioEvent.getFloatBuffer();

                    window.apply(buffer);

                    for (int i = 0; i < buffer.length; i++) {
                        fftBuffer[i] = buffer[i];
                        fftBuffer[i + BUFFER_SIZE] = 0f;
                    }

                    fft.forwardTransform(fftBuffer);

                    mags = new double[half];
                    for (int i = 0; i < half; i++) {
                        float real = fftBuffer[2 * i];
                        float imag = fftBuffer[2 * i + 1];
                        mags[i] = Math.sqrt(real * real + imag * imag);
                    }

                    
                    spectrumPanel.updateSpectrum(mags);
                    return true;
                }

                @Override
                public void processingFinished() {
                    System.out.println("FULL SONG FINISHED");
                }

            });

            // 🔥 Just start dispatcher — DO NOT run in a loop!
            Thread audioThread = new Thread(dispatcher, "Audio Dispatcher");
            audioThread.start();

        } catch (UnsupportedAudioFileException | IOException e) {
            JOptionPane.showMessageDialog(null,
                    "Error loading audio file: " + e.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }
    

    

    public void stop() {
        if (dispatcher != null) {
            dispatcher.stop();
        }
    }
}
