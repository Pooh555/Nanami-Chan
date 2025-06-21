package tts;

import com.sun.speech.freetts.Voice;
import com.sun.speech.freetts.VoiceManager;

public class FreeTTS {
    private static final String VOICE_NAME = "kevin16";

    public static void test() {
        String text = "Hello! I am speaking this text out loud using FreeTTS.";

        System.setProperty("freetts.voices",
            "com.sun.speech.freetts.en.us.cmu_us_kal.KevinVoiceDirectory");

        // Get voice
        Voice voice = VoiceManager.getInstance().getVoice(VOICE_NAME);

        if (voice == null) {
            System.err.println("Voice not found: " + VOICE_NAME);
            return;
        }

        voice.allocate();
        voice.speak(text);
        voice.deallocate();
    }
}
