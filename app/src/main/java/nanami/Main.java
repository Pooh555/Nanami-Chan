package nanami;

import java.io.IOException;

import LLM_wrapper.Ollama;

public class Main {
    public static void main(String[] args) throws IOException {
        Ollama model = new Ollama();
        System.out.println(model.getResponse());
    }
}
