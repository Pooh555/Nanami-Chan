package com.nanami.tts;

import android.content.Context;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.BufferedInputStream;
import java.io.InputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javazoom.jl.player.Player;
import keys.API_keys;

public class ElevenlabsTTS {

    private static final String API_KEY = API_keys.ELEVENLABS_API_KEY;
    private static final String TTS_API_URL_BASE = "https://api.elevenlabs.io/v1/text-to-speech/";
    private static final String DEFAULT_VOICE_ID = "PoHUWWWMHFrA8z7Q88pu";
    private final Context context;

    public ElevenlabsTTS(Context context) {
        this.context = context;
    }

    private String cleanTextForTTS(String text) {
        Pattern pattern = Pattern.compile("\\*[^*]+\\*");
        Matcher matcher = pattern.matcher(text);
        return matcher.replaceAll("").trim();
    }

    public void speak(String textToSpeak) throws JSONException {
        String cleanedText = cleanTextForTTS(textToSpeak);
        JSONObject requestBody = new JSONObject();

        requestBody.put("text", cleanedText);
        requestBody.put("model_id", "eleven_multilingual_v1");

        JSONObject voiceSettings = new JSONObject();
        voiceSettings.put("stability", 0.3);
        voiceSettings.put("similarity_boost", 0.9);
        requestBody.put("voice_settings", voiceSettings);

        String url = TTS_API_URL_BASE + DEFAULT_VOICE_ID;

        ByteRequest request = new ByteRequest(Request.Method.POST, url,
                response -> {
                    try {
                        playAudioStream(new ByteArrayInputStream(response));
                    } catch (Exception e) {
                        Log.e("ElevenlabsTTS", "Playback error: " + e.getMessage(), e);
                    }
                },
                error -> {
                    Log.e("ElevenlabsTTS", "Volley Error: " + error.getMessage(), error);
                    if (error.networkResponse != null) {
                        Log.e("ElevenlabsTTS", "Response code: " + error.networkResponse.statusCode);
                    }
                }) {
            @Override
            public byte[] getBody() {
                return requestBody.toString().getBytes();
            }

            @Override
            public String getBodyContentType() {
                return "application/json";
            }

            @Override
            public java.util.Map<String, String> getHeaders() {
                java.util.Map<String, String> headers = new java.util.HashMap<>();
                headers.put("xi-api-key", API_KEY);
                return headers;
            }
        };

        Volley.newRequestQueue(context).add(request);
    }

    private void playAudioStream(InputStream audioStream) {
        try (BufferedInputStream bis = new BufferedInputStream(audioStream)) {
            Player player = new Player(bis);
            player.play();
        } catch (Exception e) {
            Log.e("ElevenlabsTTS", "Error playing MP3 audio", e);
        }
    }
}
