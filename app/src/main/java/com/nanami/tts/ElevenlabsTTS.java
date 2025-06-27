package com.nanami.tts;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.MediaPlayer;
import android.util.Log; // Using Android's Log for consistency

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi; // Keep this annotation if it's relevant to your project

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ElevenlabsTTS {

    private static final String TAG = "Elevenlabs";
    private MediaPlayer mediaPlayer;
    private static ElevenlabsTTS elevenlabsInstance;

    // Define an interface for callbacks when speech completes or errors
    public interface SpeechCompletionListener {
        void onSpeechFinished();
        void onSpeechError(Exception e);
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
    @OptIn(markerClass = UnstableApi.class)
    private String cleanTextForTTS(String text) {
        // Clean the text for the model to speak e.g. removing *emotion emotion*
        Pattern pattern = Pattern.compile("\\*[^*]+\\*");
        Matcher matcher = pattern.matcher(text);

        Log.d(TAG, "Cleaned the input text");

        return matcher.replaceAll("").trim();
    }

    @OptIn(markerClass = UnstableApi.class)
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
        urlConnection.setRequestProperty("Accept", "audio/mpeg");
        urlConnection.setRequestProperty("xi-api-key", API_keys.ELEVENLABS_API_KEY);
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setDoOutput(true);

        return urlConnection;
    }

    /// Plays an audio stream using MediaPlayer.
    public void playAudioStream(InputStream audioStream, Context context, SpeechCompletionListener listener) {
        File tempMp3 = null;
        try {
            // Create a temporary file to store the audio stream
            tempMp3 = File.createTempFile("temp_elevenlabs_audio", ".mp3", context.getCacheDir());
            tempMp3.deleteOnExit(); // Delete when the program terminates

            // Read the audio stream into the temporary file
            try (FileOutputStream fos = new FileOutputStream(tempMp3)) {
                byte[] buffer = new byte[1024];
                int len;
                while ((len = audioStream.read(buffer)) != -1) {
                    fos.write(buffer, 0, len);
                }
            }

            // Initialize MediaPlayer if it's null or reset it if it's already in use
            if (mediaPlayer == null) {
                mediaPlayer = new MediaPlayer();
            } else {
                mediaPlayer.reset(); // Reset if already in use
            }

            // Set up listener for when playback is complete
            final File finalTempMp3 = tempMp3;

            mediaPlayer.setOnCompletionListener(mp -> {
                mp.reset(); // Reset for next playback

                // Delete temporary file after playback
                if (!finalTempMp3.delete()) {
                    Log.e(TAG, "Failed to delete the temporary audio file.");
                }

                Log.d(TAG, "Audio playback completed and temporary file deleted.");

                if (listener != null) {
                    listener.onSpeechFinished(); // Notify listener
                }
            });

            // Set up listener for playback errors
            mediaPlayer.setOnErrorListener((mp, what, extra) -> {
                Log.e(TAG, "MediaPlayer error: what=" + what + ", extra=" + extra);
                mp.reset();

                // Delete temporary file after playback
                if (!finalTempMp3.delete()) {
                    Log.e(TAG, "Failed to delete the temporary audio file.");
                }
                if (listener != null) {
                    listener.onSpeechError(new Exception("MediaPlayer error: what=" + what + ", extra=" + extra)); // Notify listener
                }

                return false; // Return false to indicate that the error was not handled
            });

            // Set the data source from the temporary file
            mediaPlayer.setDataSource(tempMp3.getAbsolutePath());

            // Set audio attributes
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build());

            mediaPlayer.prepare(); // Prepares the media player for playback
            mediaPlayer.start();   // Starts playback

            Log.d(TAG, "Audio playback started.");

        } catch (IOException e) {
            Log.e(TAG, "Error playing MP3 audio: IOException", e);

            if (listener != null) {
                listener.onSpeechError(e);
            }
        } catch (Exception e) {
            Log.e(TAG, "General error playing MP3 audio", e);

            if (listener != null) {
                listener.onSpeechError(e);
            }
        } finally {
            // Ensure the audio stream is closed regardless of success or failure
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