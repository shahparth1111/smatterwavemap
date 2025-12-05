package app.smatter.smatterwavemap;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

// Hypothetical Java Class to interface with the HackRF C library
// This class represents the required JNI (Java Native Interface) wrapper
class HackRFDevice {
    // Native method to initialize the HackRF device
    public native int init();
    // Native method to set the center frequency in Hz
    public native int setFrequency(long freqHz);
    // Native method to set the sample rate in Hz
    public native int setSampleRate(long rateHz);
    // Native method to enable the TX antenna amplifier
    public native int setTxAmp(int enable);
    // Native method to start transmitting the provided IQ data
    public native int startTx(ByteBuffer buffer, int bufferSize);
    // Native method to stop transmission
    public native int stopTx();
    // Native method to close the device
    public native int close();

    // Load the native C library (e.g., libhackrf_java_bindings.so)
    static {
        // System.loadLibrary("hackrf_java_bindings");
    }
}


class LegalSdrToneTransmitter {

    private static final long FREQUENCY_HZ = 433000000L; // Legal ISM Band frequency (e.g., 433 MHz)
    private static final long SAMPLE_RATE_HZ = 2000000L;  // 2 MSPS
    private static final int TONE_FREQUENCY_HZ = 10000;    // 10 kHz tone
    private static final int DURATION_SECONDS = 5;


    public static void main(String[] args) {
        HackRFDevice hackrf = new HackRFDevice();
        System.out.println("Starting HackRF Tone Transmitter (Legal Use)");

        if (hackrf.init() != 0) {
            System.err.println("Error: Could not initialize HackRF device.");
            return;
        }

        try {
            // 1. Configure the HackRF
            hackrf.setFrequency(FREQUENCY_HZ);
            hackrf.setSampleRate(SAMPLE_RATE_HZ);
            hackrf.setTxAmp(1); // Enable internal amplifier

            // 2. Generate a simple Complex Sinusoidal Signal (IQ data)
            int numSamples = (int) (SAMPLE_RATE_HZ * DURATION_SECONDS);
            // IQ data is 4 bytes per sample (float I, float Q)
            int bufferSize = numSamples * 8; 
            
            // Allocate a direct buffer for native C code interaction
            ByteBuffer txBuffer = ByteBuffer.allocateDirect(bufferSize);
            txBuffer.order(ByteOrder.nativeOrder());
            
            // Generate the sine wave
            for (int i = 0; i < numSamples; i++) {
                double time = (double) i / SAMPLE_RATE_HZ;
                
                // In-phase component (I): Cosine wave
                float i_sample = (float) Math.cos(2 * Math.PI * TONE_FREQUENCY_HZ * time);
                // Quadrature component (Q): Sine wave (90 degree phase shift)
                float q_sample = (float) Math.sin(2 * Math.PI * TONE_FREQUENCY_HZ * time);

                // Write I then Q as float (4 bytes each)
                txBuffer.putFloat(i_sample);
                txBuffer.putFloat(q_sample);
            }
            txBuffer.flip(); // Prepare buffer for reading

            // 3. Transmit the Signal
            System.out.println("Transmitting 10kHz tone on " + (FREQUENCY_HZ / 1e6) + " MHz for " + DURATION_SECONDS + "s...");
            hackrf.startTx(txBuffer, numSamples);
            
            // Hold transmission for the duration
            Thread.sleep(DURATION_SECONDS * 1000); 

        } catch (Exception e) {
            System.err.println("Transmission error: " + e.getMessage());
        } finally {
            // 4. Clean up
            hackrf.stopTx();
            hackrf.close();
            System.out.println("Transmission stopped and device closed.");
        }
    }
}