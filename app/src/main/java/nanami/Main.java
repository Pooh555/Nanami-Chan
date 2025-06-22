package nanami;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import llm_wrapper.Ollama;
import stt.VoskSTT;
import tts.ElevenlabsTTS;

public class Main {
    public static String personality = "Nanami Osaka";
    public static String modelName = "llama3:latest";

    public static void main(String[] args) throws Exception {
        /*
         * Available model
         * - Ollama: llama3:lastest
         * 
         * Available personalities
         * - Kita Ikuyo (From ぼっちざろっく!)
         * - Nanami Osaka (From 現実もたまには嘘をつく)
         */
        Ollama model = new Ollama(Main.modelName, Main.personality);

        // model.launchTerminal();

        // VoskSTT stt = new VoskSTT();

        VoskSTT stt = new VoskSTT();
        ElevenlabsTTS voice = new ElevenlabsTTS();
        String userPrompt = "";
        String recievedMessage;
        
        try {
            while (true) {
                // Get user prompt using sound-to-text (STT)
                try {
                    userPrompt = stt.listenAndTranscribe();
                } catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
                    e.printStackTrace();
                }

                // Prompt to exit LLM
                if (userPrompt.equalsIgnoreCase("exit") || userPrompt.equalsIgnoreCase("quit")
                        || userPrompt.equalsIgnoreCase("goodbye")) {
                    recievedMessage = model.getResponseText("I have got to go now. Goodbye, see you next time.")
                            + " さようなら!";

                    System.out.println("\n" + recievedMessage);
                    voice.speak(recievedMessage);
                    break;
                }

                recievedMessage = model.getResponseText(userPrompt);

                System.out.println("\n" + recievedMessage);
                voice.speak(recievedMessage);
            }
        } finally {
            model.saveConversationHistory();
        }
    }
}
