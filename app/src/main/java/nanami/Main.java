package nanami;

import java.io.IOException;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import llm_wrapper.Ollama;
import stt.VoskSTT;

public class Main {
    public static String personality = "Nanami Osaka";
    public static String modelName = "llama3:latest";
    public static void main(String[] args) throws Exception {
        /*
         * Available model
         *  - Ollama: llama3:lastest
         * 
         * Available personalities
         *  - Kita Ikuyo (From  ぼっちざろっく!)
         *  - Nanami Osaka (From 現実もたまには嘘をつく)
         */
        // Ollama model = new Ollama(Main.modelName, Main.personality);
        
        // model.launch();

        VoskSTT stt = new VoskSTT();
        try {
            stt.listenAndTranscribe();
        } catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
            e.printStackTrace();
        }
    }
}
