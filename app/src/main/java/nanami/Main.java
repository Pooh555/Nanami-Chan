package nanami;

import llm_wrapper.Ollama;
import tts.ElevenlabsTTS;

public class Main {
    public static String personality = "Nanami Osaka";
    public static String modelName = "llama3:latest";
    public static void main(String[] args) throws Exception {
        /*
         * Available model
         *  - Ollama: llama3:lastest
         * 
         * Available personalities
         *  - Kita Ikuyo
         *  - Default
         */
        Ollama model = new Ollama(Main.modelName, Main.personality);
        // model.launch();
        ElevenlabsTTS test = new ElevenlabsTTS();
        test.test();
    }
}
