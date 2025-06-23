package main;

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
         * - Ollama: llama3:lastest (default)
         * 
         * Available personalities
         * - Kita Ikuyo (From ぼっちざろっく!)
         * - Nanami Osaka (From 現実もたまには嘘をつく) (default)
         */
        Ollama model = new Ollama(Main.modelName, Main.personality);
        /*
         * Available speech-to-text options
         *  - Vosk (vosk-model-small-en-us-0.15) (default)
         */
        VoskSTT stt = new VoskSTT();
        /*
         * Available text-to-speech options
         * - Elevenlabs (default)
         */
        ElevenlabsTTS voice = new ElevenlabsTTS();
        String userPrompt = "";
        String recievedMessage;
        
        try {
            System.out.println("\nNanami has woken up. You can talk to her now.");

            while (true) {
                // Get user prompt using sound-to-text (STT)
                try {
                    userPrompt = stt.listenAndTranscribe();
                } catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
                    throw new Exception("Invalid URI syntax for Ollama API: " + e.getMessage(), e);
                }

                System.out.println("\nUser: " + userPrompt);

                // Prompt to exit LLM
                if (userPrompt.equalsIgnoreCase("good bye") || userPrompt.equalsIgnoreCase("goodbye")) {
                    recievedMessage = model.getResponseText("I have got to go now. Goodbye, see you next time.")
                            + " さようなら!";

                    System.out.println("\nNanami Chan: " + recievedMessage);
                    voice.speak(recievedMessage);
                    break;
                }
                recievedMessage = model.getResponseText(userPrompt);

                System.out.println("\nNanami Chan: " + recievedMessage);
                voice.speak(recievedMessage);
            }
        } finally {
            model.saveConversationHistory();
        }
    }
}