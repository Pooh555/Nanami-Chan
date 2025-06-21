package nanami;

import LLM_wrapper.Ollama;

public class Main {
    public static void main(String[] args) throws Exception {
        /*
         * Available model
         *  - Ollama: llama3:lastest
         * 
         * Available personalities
         *  - Kita Ikuyo
         *  - Default
         */
        Ollama model = new Ollama("llama3:latest", "Kita Ikuyo");
        model.launch();
    }
}
