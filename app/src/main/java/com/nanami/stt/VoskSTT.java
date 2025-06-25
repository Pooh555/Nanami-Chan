package com.nanami.stt;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;

import java.io.IOException;

public class VoskSTT {

    private static final String TAG = "VoskSTT";

    private static final int SAMPLE_RATE = 16000;
    private static final int CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO;
    private static final int AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT;

    private static final long SILENCE_TIMEOUT_MS = 2000;
    private static final String STT_MODEL_PATH = "model"; // Path inside assets (see note below)

    private AudioRecord recorder;
    private boolean isRecording = false;

    public interface OnResultListener {
        void onResult(String text);
    }

    public void startListening(android.content.Context context, OnResultListener listener) {
        new Thread(() -> {
            try (Model model = new Model();
                 Recognizer recognizer = new Recognizer(model, SAMPLE_RATE)) {

                int bufferSize = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT);
                if (ActivityCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                    // Permission not granted; handle appropriately
                    Log.e(TAG, "Microphone permission not granted.");
                    return;
                }

                recorder = new AudioRecord(
                        MediaRecorder.AudioSource.MIC,
                        SAMPLE_RATE,
                        CHANNEL_CONFIG,
                        AUDIO_FORMAT,
                        bufferSize);

                byte[] buffer = new byte[bufferSize];
                recorder.startRecording();
                isRecording = true;

                StringBuilder transcript = new StringBuilder();
                long lastSpeechTime = System.currentTimeMillis();
                boolean hasSpoken = false;

                while (isRecording) {
                    int read = recorder.read(buffer, 0, buffer.length);
                    if (read <= 0) continue;

                    if (recognizer.acceptWaveForm(buffer, read)) {
                        String resultJson = recognizer.getResult();
                        String text = new JSONObject(resultJson).getString("text");

                        if (!text.isEmpty()) {
                            transcript.append(text).append(" ");
                            lastSpeechTime = System.currentTimeMillis();
                            hasSpoken = true;
                        }
                    } else {
                        String partialJson = recognizer.getPartialResult();
                        String partial = new JSONObject(partialJson).getString("partial");

                        if (!partial.isEmpty()) {
                            lastSpeechTime = System.currentTimeMillis();
                            hasSpoken = true;
                        }
                    }

                    if (hasSpoken && (System.currentTimeMillis() - lastSpeechTime) > SILENCE_TIMEOUT_MS) {
                        break;
                    }
                }

                String finalResult = recognizer.getFinalResult();
                String finalText = new JSONObject(finalResult).getString("text");
                transcript.append(finalText);

                if (listener != null) {
                    listener.onResult(transcript.toString().trim());
                }

            } catch (IOException | RuntimeException | JSONException e) {
                Log.e(TAG, "Error in speech recognition", e);
            } finally {
                stopListening();
            }
        }).start();
    }

    public void stopListening() {
        isRecording = false;
        if (recorder != null) {
            try {
                recorder.stop();
                recorder.release();
            } catch (Exception e) {
                Log.w(TAG, "Error releasing recorder", e);
            }
            recorder = null;
        }
    }
}
