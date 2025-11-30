package app.smatter.smatterwavemap;

import javax.sound.sampled.*;
import java.util.Random;

/**
 * Safe Local-Only Audio Masker
 * --------------------------------
 * - Plays an input audio file
 * - Injects broadband + shaped noise
 * - Amplifies final mixed output
 * - Does not transmit or receive RF
 * - Does not Jam/Intercept any signal
 */

public class AudioMaskerPlayer {

    // ======== CONFIG =========
    private static final float USER_GAIN = 2.5f;     // Final amplification of output audio
    private static final float NOISE_LEVEL = 0.35f;  // Noise injection amount (0 to 1)
    private static final float MASK_COMPLEXITY = 0.45f; // Controls the "chaotic" masking
    // =========================

    public static void main(String[] args) throws Exception {
        if(args.length == 0){
            System.out.println("Usage: java AudioMaskerPlayer <audioFile.wav>");
            return;
        }

        playWithMasking(args[0]);
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
