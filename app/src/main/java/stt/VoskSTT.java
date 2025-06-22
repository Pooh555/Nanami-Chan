package stt;

import java.io.IOException;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;

import org.vosk.LogLevel;
import org.vosk.Recognizer;
import org.vosk.LibVosk;
import org.vosk.Model;

public class VoskSTT {

    private final float SAMPLE_RATE = 16000;
    private final int SAMPLE_SIZE_IN_BITS = 16;
    private final int CHANNELS = 1;
    private final boolean SIGNED = true;
    private final boolean BIG_ENDIAN = false;
    private final long SILENCE_TIMEOUT_MS = 2000;
    private final String STT_MODEL_PATH = "./stt/vosk-model-small-en-us-0.15";

    public String listenAndTranscribe() throws LineUnavailableException, IOException, UnsupportedAudioFileException {
        LibVosk.setLogLevel(LogLevel.DEBUG);

        AudioFormat format = new AudioFormat(
                this.SAMPLE_RATE,
                this.SAMPLE_SIZE_IN_BITS,
                this.CHANNELS,
                this.SIGNED,
                this.BIG_ENDIAN);

        DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);

        if (!AudioSystem.isLineSupported(info)) {
            System.err.println("Microphone line not supported with the specified format: " + format);
        }

        StringBuilder fullTranscript = new StringBuilder();

        try (Model model = new Model(this.STT_MODEL_PATH);
                TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
                Recognizer recognizer = new Recognizer(model, this.SAMPLE_RATE)) {

            microphone.open(format);
            microphone.start();

            int nbytes;
            byte[] buffer = new byte[4096];
            long lastSpeechTime = System.currentTimeMillis();
            boolean speechDetected = false;
            boolean hasUserStartedSpeaking = false;
            

            while (true) {
                nbytes = microphone.read(buffer, 0, buffer.length);

                if (nbytes == -1) {
                    break;
                }

                if (recognizer.acceptWaveForm(buffer, nbytes)) {
                    String result = recognizer.getResult();
                    if (result != null && !result.trim().equals("{\"text\":\"\"}")) {
                        String text = new org.json.JSONObject(result).getString("text");
                        lastSpeechTime = System.currentTimeMillis();
                        speechDetected = true;
                        hasUserStartedSpeaking = true;

                        fullTranscript.append(text).append(" ");
                    }
                } else {
                    String partialResult = recognizer.getPartialResult();

                    if (partialResult != null && !partialResult.trim().equals("{\"partial\":\"\"}")) {
                        String partialText = new org.json.JSONObject(partialResult).getString("partial");

                        if (!partialText.trim().isEmpty()) {
                            lastSpeechTime = System.currentTimeMillis();
                            hasUserStartedSpeaking = true;
                        }
                    }
                }

                if (hasUserStartedSpeaking && !speechDetected
                        && (System.currentTimeMillis() - lastSpeechTime > SILENCE_TIMEOUT_MS)) {
                    // System.out.println("Detected silence for " + (SILENCE_TIMEOUT_MS / 1000) + "
                    // seconds. Stopping.");
                    break;
                }

                speechDetected = false;
            }

            String finalResult = recognizer.getFinalResult();
            String finalText = new org.json.JSONObject(finalResult).getString("text");

            fullTranscript.append(finalText);
        } finally {
            return fullTranscript.toString().trim();
        }
    }
}