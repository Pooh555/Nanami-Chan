package com.nanami.tts;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;

import com.nanami.keys.API_keys;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElevenlabsTTS {

    private static final String TAG = "Elevenlabs";
    private MediaPlayer mediaPlayer;
    private static ElevenlabsTTS elevenlabsInstance;
    private AudioLevelListener audioLevelListener;
    private static final float LIP_SYNC_SCALER = 2.0f;
    private static final float LIP_SYNC_THRESHOLD = 0.002f;
    private static final float LIP_SYNC_BOOST = 1.0f;


    // Define an interface for callbacks when speech completes or errors
    public interface SpeechCompletionListener {
        void onSpeechFinished();
        void onSpeechError(Exception e);
    }

    // Send audio volume message to Live2D model (used for lip-sync)
    public interface AudioLevelListener {
        void onAudioLevelUpdate(float level);
    }

    // Get instance
    public static ElevenlabsTTS getInstance() {
        if (elevenlabsInstance == null) {
            elevenlabsInstance = new ElevenlabsTTS();
        }
        return elevenlabsInstance;
    }

    // Elevenlabs constructor
    @OptIn(markerClass = UnstableApi.class)
    public ElevenlabsTTS() {
        // Using Android's Log for consistency
        Log.d(TAG, "Elevenlabs TTS model is loaded successfully.");
    }

    // Launch Elevenlabs service
    public void onStart() {
        Log.d(TAG, "Elevenlabs service is initiated.");
    }

    // Clean text for TTS
    private String cleanTextForTTS(String text) {
        // Clean the text for the model to speak e.g. removing *emotion emotion*
        Pattern pattern = Pattern.compile("\\*[^*]+\\*");
        Matcher matcher = pattern.matcher(text);

        Log.d(TAG, "Cleaned the input text");

        return matcher.replaceAll("").trim();
    }

    // Sets the listener for audio level updates.
    public void setAudioLevelListener(AudioLevelListener listener) {
        this.audioLevelListener = listener;
        Log.d(TAG, "AudioLevelListener set for ElevenlabsTTS.");
    }

    public void speak(Context context, String textToSpeak, SpeechCompletionListener listener) {
        // Launch a parallel thread to handle TTS network request and audio playback
        new Thread(() -> {
            String cleanedText = cleanTextForTTS(textToSpeak);
            JSONObject requestBody = new JSONObject();
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;

            try {
                // Initialize a request to the Elevenlabs service
                requestBody.put("text", cleanedText);
                requestBody.put("model_id", "eleven_multilingual_v1");

                // Initialize settings for Elevenlabs TTS voice
                JSONObject voiceSettings = new JSONObject();
                voiceSettings.put("stability", 0.3);
                voiceSettings.put("similarity_boost", 0.9);
                requestBody.put("voice_settings", voiceSettings);

                Log.d(TAG, "Attempting to speak: " + textToSpeak);

                // Send the request body to the Elevenlabs server
                urlConnection = getHttpURLConnection();

                try (OutputStream os = urlConnection.getOutputStream()) {
                    byte[] input = requestBody.toString().getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                // Retrieve the response from the Elevenlabs server
                int responseCode = urlConnection.getResponseCode();

                // Play the audio
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    inputStream = urlConnection.getInputStream();
                    // Pass the listener to playAudioStream
                    playAudioStream(inputStream, context, listener);
                } else {
                    Log.e(TAG, "Elevenlabs API request failed with code: " + responseCode);

                    String errorResponse;

                    try (BufferedInputStream bis = new BufferedInputStream(urlConnection.getErrorStream())) {
                        java.util.Scanner s = new java.util.Scanner(bis).useDelimiter("\\A");
                        errorResponse = s.hasNext() ? s.next() : "";
                        Log.e(TAG, "Elevenlabs API error response: " + errorResponse);
                    }
                    if (listener != null) {
                        listener.onSpeechError(new IOException("Elevenlabs API request failed: " + responseCode + " - " + errorResponse));
                    }
                }
            } catch (JSONException | IOException e) {
                Log.e(TAG, "Error in Elevenlabs TTS speak method", e);
                if (listener != null) {
                    listener.onSpeechError(e);
                }
            } finally {
                // Ensure streams and connections are closed
                try {
                    if (inputStream != null) {
                        inputStream.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error closing input stream", e);
                }
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }).start();
    }

    // Prepare url connection
    private static HttpURLConnection getHttpURLConnection() throws IOException {
        String urlString = API_keys.ELEVENLABS_API_URL_BASE + API_keys.VOICE_ID; // Elevenlabs voice URL
        URL url = new URL(urlString);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();

        // POST the request to the server
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Accept", "audio/wav");
        urlConnection.setRequestProperty("xi-api-key", API_keys.ELEVENLABS_API_KEY);
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setDoOutput(true);

        return urlConnection;
    }

    // Plays an audio stream using MediaPlayer.
    public void playAudioStream(InputStream audioStream, Context context, SpeechCompletionListener listener) {
        File tempWavFile;

        try {
            tempWavFile = File.createTempFile("temp_elevenlabs_audio", ".wav", context.getCacheDir()); // Save as WAV
            tempWavFile.deleteOnExit();

            List<Float> amplitudeData = new ArrayList<>();

            try (FileOutputStream fos = new FileOutputStream(tempWavFile)) {
                // Skip WAV header (typical size is 44 bytes)
                byte[] headerBuffer = new byte[44];
                int bytesReadFromHeader = audioStream.read(headerBuffer);

                if (bytesReadFromHeader < 44) {
                    Log.e(TAG, "Failed to read WAV header or WAV is too short.");
                    if (listener != null) {
                        listener.onSpeechError(new IOException("Invalid WAV file: header missing or incomplete."));
                    }
                    return;
                }

                fos.write(headerBuffer); // Write header to temp file for playback

                byte[] buffer = new byte[1024]; // Keep buffer size for reading
                int len;

                while ((len = audioStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);

                    // Process 16-bit PCM samples for amplitude
                    long sumAbsoluteSamples = 0;
                    int numSamples = 0;

                    for (int i = 0; i < len - 1; i += 2) {
                        short sample = (short) ((buffer[i + 1] << 8) | (buffer[i] & 0xFF));
                        sumAbsoluteSamples += Math.abs(sample);
                        numSamples++;
                    }

                    float currentRawAmplitude = 0.0f;

                    if (numSamples > 0) {
                        // Normalize by the maximum possible 16-bit signed value (32768)
                        currentRawAmplitude = (float) sumAbsoluteSamples / (numSamples * 32768.0f);
                    }

                    float processedLevel = currentRawAmplitude;

                    if (processedLevel < LIP_SYNC_THRESHOLD) {
                        processedLevel = 0.0f;
                    } else {
                        processedLevel = (processedLevel - LIP_SYNC_THRESHOLD) * LIP_SYNC_SCALER + LIP_SYNC_BOOST;
                        processedLevel = Math.min(1.0f, Math.max(0.0f, processedLevel));
                    }

                    amplitudeData.add(processedLevel);
                }
            }

            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            } else {
                mediaPlayer.reset();
            }

            final List<Float> finalAmplitudeData = amplitudeData;

            mediaPlayer.setOnCompletionListener(mp -> {
                Log.d(TAG, "Audio playback completed and temporary file deleted.");

                if (listener != null) {
                    listener.onSpeechFinished();
                }

                if (audioLevelListener != null) {
                    audioLevelListener.onAudioLevelUpdate(0.0f);
                }
            });

            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                if (listener != null) {
                    listener.onSpeechError(new Exception("MediaPlayer error: what=" + what + ", extra=" + extra));
                }

                if (audioLevelListener != null) {
                    audioLevelListener.onAudioLevelUpdate(0.0f);
                }
                return false;
            });

            mediaPlayer.setDataSource(tempWavFile.getAbsolutePath());
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());

            mediaPlayer.prepare();
            mediaPlayer.start();

            Log.d(TAG, "Audio playback started.");

            new Thread(() -> {
                for (int i = 0; i < finalAmplitudeData.size(); i++) {
                    if (!mediaPlayer.isPlaying()) {
                        break;
                    }

                    float level = finalAmplitudeData.get(i);

                    if (audioLevelListener != null) {
                        audioLevelListener.onAudioLevelUpdate(level);
                    }

                    try {
                        Thread.sleep(30);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        Log.e(TAG, "Amplitude thread interrupted", e);
                    }
                }
                if (audioLevelListener != null && mediaPlayer != null && !mediaPlayer.isPlaying()) {
                    audioLevelListener.onAudioLevelUpdate(0.0f);
                }
            }).start();

        } catch (IOException e) {
            Log.e(TAG, "Error playing WAV audio: IOException", e);
            if (listener != null) {
                listener.onSpeechError(e);
            }
        } catch (Exception e) {
            Log.e(TAG, "General error playing WAV audio", e);
            if (listener != null) {
                listener.onSpeechError(e);
            }
        } finally {
            try {
                if (audioStream != null) {
                    audioStream.close();
                }
            } catch (IOException e) {
                Log.e(TAG, "Error closing audio stream in finally block", e);
            }
        }
    }

    // Terminate Elevenlabs service
    public void onDestroy() {
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release(); // Release MediaPlayer resources
            mediaPlayer = null;

            Log.d(TAG, "MediaPlayer resources released.");
        }
    }
}