package tts;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javazoom.jl.player.Player;

import keys.API_keys;

public class ElevenlabsTTS {

    private static final String API_KEY = API_keys.ELEVENLABS_API_KEY; // Add your API key in API_keys.java
    private static final String TTS_API_URL_BASE = "https://api.elevenlabs.io/v1/text-to-speech/"; // Default URL for
                                                                                                   // TTS
    private static final String DEFAULT_VOICE_ID = "PoHUWWWMHFrA8z7Q88pu"; // Get voice ID from Voice Library on
                                                                           // Elevenlabs
    private final HttpClient httpClient;

    public ElevenlabsTTS() {
        this.httpClient = HttpClient.newHttpClient();
    }

    private String cleanTextForTTS(String text) {
        // Regex to find patterns like *word word*
        Pattern pattern = Pattern.compile("\\*[^*]+\\*");
        Matcher matcher = pattern.matcher(text);

        return matcher.replaceAll("").trim();
    }

    public void speak(String textToSpeak) {
        String cleanedText = cleanTextForTTS(textToSpeak);
        JSONObject requestBody = new JSONObject();

        requestBody.put("text", cleanedText);
        requestBody.put("model_id", "eleven_multilingual_v1");

        JSONObject voiceSettings = new JSONObject();

        voiceSettings.put("stability", 0.1); // Adjust for more or less variability
        voiceSettings.put("similarity_boost", 0.9); // Adjust for clarity and expressiveness
        requestBody.put("voice_settings", voiceSettings);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(TTS_API_URL_BASE + DEFAULT_VOICE_ID))
                    .header("Content-Type", "application/json")
                    .header("xi-api-key", API_KEY)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                    .build();

            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() == 200) {
                playAudioStream(response.body());
            } else {
                String errorBody = new String(response.body().readAllBytes());
                System.err.println("Error generating audio. Status Code: " + response.statusCode());
                System.err.println("Response Body: " + errorBody);
            }

        } catch (Exception e) {
            System.err.println("An error occurred during audio generation or playback: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void playAudioStream(InputStream audioStream) {
        try (BufferedInputStream bis = new BufferedInputStream(audioStream)) {
            Player player = new Player(bis);
            
            player.play();
        } catch (Exception e) {
            System.err.println("Error playing MP3 audio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    // // Test method
    // public void test() {
    // ElevenlabsTTS tts = new ElevenlabsTTS();
    // String message = "Um, s-sorry to hear that... *gets nervous* I-I think it's
    // really important to take care of yourself when you're feeling tired and
    // stressed out... *pauses slightly* Do you like playing games or doing things
    // that help you relax? Maybe we could talk about something else for a bit?";
    // tts.playAudio(message);
    // }
}
