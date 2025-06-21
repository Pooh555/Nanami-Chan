package tts;

import org.json.JSONObject;

import javax.sound.sampled.*;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javazoom.jl.player.Player;

import tts.API_keys;

/**
 * A Java class to interact with the ElevenLabs Text-to-Speech API
 * and play the generated audio.
 */
public class ElevenlabsTTS {

    // Your ElevenLabs API Key. Replace with your actual key.
    private static final String API_KEY = API_keys.ELEVENLABS_API_KEY;
    // Base URL for the ElevenLabs Text-to-Speech API
    private static final String TTS_API_URL_BASE = "https://api.elevenlabs.io/v1/text-to-speech/";
    // Default Voice ID. You might want to change this to a specific "child" voice
    // ID from ElevenLabs.
    // Example: "21m00Tcm4azwk8nPzBqV" is Rachel. Look for specific child voices on
    // ElevenLabs.com
    private static final String DEFAULT_VOICE_ID = "EXAVITQu4vr4xnSDxMaL"; // Rachel's voice ID

    private final HttpClient httpClient;

    /**
     * Constructor for the ElevenlabsTTS class.
     */
    public ElevenlabsTTS() {
        this.httpClient = HttpClient.newHttpClient();
    }

    /**
     * Cleans the input text by removing specific emotional cues like *gets nervous*
     * or *pauses slightly*.
     * These cues are for human reading and are not processed by the ElevenLabs API
     * as direct SSML.
     * For advanced emotional control, you would use ElevenLabs' specific SSML
     * features.
     *
     * @param text The original text with potential emotional cues.
     * @return The cleaned text suitable for TTS conversion.
     */
    private String cleanTextForTTS(String text) {
        // Regex to find patterns like *word word*
        Pattern pattern = Pattern.compile("\\*[^*]+\\*");
        Matcher matcher = pattern.matcher(text);
        return matcher.replaceAll("").trim();
    }

    /**
     * Converts the given text to speech using ElevenLabs API and plays the audio.
     *
     * @param textToSpeak The text message to be converted and spoken.
     */
    public void playAudio(String textToSpeak) {
        if (API_KEY.equals("YOUR_ELEVENLABS_API_KEY_HERE") || API_KEY.isEmpty()) {
            System.err.println(
                    "Error: ElevenLabs API Key is not set. Please replace 'YOUR_ELEVENLABS_API_KEY_HERE' with your actual key.");
            return;
        }

        String cleanedText = cleanTextForTTS(textToSpeak);

        System.out.println("Generating audio for: \"" + cleanedText + "\"");

        // Construct the JSON request body
        JSONObject requestBody = new JSONObject();
        requestBody.put("text", cleanedText);
        requestBody.put("model_id", "eleven_monolingual_v1"); // Or "eleven_multilingual_v2" if needed

        JSONObject voiceSettings = new JSONObject();
        voiceSettings.put("stability", 0.5); // Adjust for more or less variability
        voiceSettings.put("similarity_boost", 0.75); // Adjust for clarity and expressiveness
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
                System.out.println("Audio stream received. Playing...");
                playAudioStream(response.body());
                System.out.println("Audio playback finished.");
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

    /**
     * Plays audio from a given InputStream. Assumes the audio is in a supported
     * format (e.g., MP3 or WAV).
     * ElevenLabs often returns MP3.
     *
     * @param audioStream The InputStream containing the audio data.
     */
    // private void playAudioStream(InputStream audioStream) {
    // try (BufferedInputStream bis = new BufferedInputStream(audioStream)) {
    // AudioInputStream ais = AudioSystem.getAudioInputStream(bis);
    // AudioFormat format = ais.getFormat();
    // DataLine.Info info = new DataLine.Info(SourceDataLine.class, format);
    // SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
    // line.open(format);
    // line.start();

    // int bytesRead;
    // byte[] buffer = new byte[4096]; // Buffer for reading audio data
    // while ((bytesRead = ais.read(buffer, 0, buffer.length)) != -1) {
    // line.write(buffer, 0, bytesRead);
    // }

    // line.drain();
    // line.close();
    // ais.close();
    // } catch (UnsupportedAudioFileException e) {
    // System.err.println("Audio format not supported: " + e.getMessage());
    // } catch (LineUnavailableException e) {
    // System.err.println("Audio line not available: " + e.getMessage());
    // } catch (Exception e) {
    // System.err.println("Error playing audio: " + e.getMessage());
    // e.printStackTrace();
    // }
    // }

    private void playAudioStream(InputStream audioStream) {
        try (BufferedInputStream bis = new BufferedInputStream(audioStream)) {
            Player player = new Player(bis);
            player.play(); // blocks until audio is done
        } catch (Exception e) {
            System.err.println("Error playing MP3 audio: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Test method as requested by the user.
     * Demonstrates how to use the ElevenlabsTTS class.
     */
    public void test() {
        ElevenlabsTTS tts = new ElevenlabsTTS();
        String message = "Um, s-sorry to hear that... *gets nervous* I-I think it's really important to take care of yourself when you're feeling tired and stressed out... *pauses slightly* Do you like playing games or doing things that help you relax? Maybe we could talk about something else for a bit?";
        tts.playAudio(message);
    }

    // You would typically have a main method in a separate class to run this.
    // For example:
    /*
     * public static void main(String[] args) {
     * new ElevenlabsTTS().test();
     * }
     */
}
