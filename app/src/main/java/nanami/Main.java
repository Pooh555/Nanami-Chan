package nanami;

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

        VoskSTT decoder = new VoskSTT();
        decoder.test();
    }
}
