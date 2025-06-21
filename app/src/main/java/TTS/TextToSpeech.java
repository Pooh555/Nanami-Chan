package nanami;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

public class TextToSpeech {
    private static final String VOICE_NAME = "kevin16";

    public static void main(String[] args) {
        String text = "Hello! I am speaking this text out loud using FreeTTS.";

        // Get voice
        Voice voice = VoiceManager.getInstance().getVoice(VOICE_NAME);

        if (voice == null) {
            System.err.println("Voice not found: " + VOICE_NAME);
            return;
        }

        // Allocate the resources for the voice
        voice.allocate();

        // Speak the text
        voice.speak(text);

        // Clean up
        voice.deallocate();
    }
}
