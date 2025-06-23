package web_server;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

import org.json.JSONObject;

import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import llm_wrapper.Ollama;
import tts.ElevenlabsTTS;

public class WebServer {
    private final String FRONTEND_URL = "http://localhost:5000";
    private HttpServer server;
    private final Ollama ollamaModel;
    private final ElevenlabsTTS elevenlabsTTS;

    public WebServer(Ollama ollamaModel, ElevenlabsTTS elevenlabsTTS) {
        this.ollamaModel = ollamaModel;
        this.elevenlabsTTS = elevenlabsTTS;
    }

    public void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress(8080), 0); // Listen on port 8080

        // Context for processing LLM and TTS
        server.createContext("/api/chat", (HttpExchange exchange) -> {
            if (null == exchange.getRequestMethod()) {
                exchange.sendResponseHeaders(405, -1);
            } else
                switch (exchange.getRequestMethod()) {
                    case "POST" -> {
                        String requestBody = new String(exchange.getRequestBody().readAllBytes());
                        String userPrompt = requestBody;
                        String llmResponse;
                        String audioBase64 = null;
                        
                        try {
                            // Get LLM response
                            llmResponse = ollamaModel.getResponseText(userPrompt);
                            
                            System.out.println("\nNanami Chan: " + llmResponse);
                            
                            // Generate audio and get base64
                            audioBase64 = elevenlabsTTS.generateAudioBase64(llmResponse);
                            
                        } catch (IOException | InterruptedException e) {
                            llmResponse = "Error: " + e.getMessage();
                        }
                        
                        // Construct JSON response for frontend
                        JSONObject jsonResponse = new JSONObject();
                        
                        jsonResponse.put("text", llmResponse);
                        
                        if (audioBase64 != null) {
                            jsonResponse.put("audioBase64", audioBase64);
                        }
                        
                        Headers headers = exchange.getResponseHeaders();
                        
                        headers.add("Content-Type", "application/json");
                        
                        // Enable CORS for your frontend domain
                        headers.add("Access-Control-Allow-Origin", FRONTEND_URL);
                        headers.add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
                        headers.add("Access-Control-Allow-Headers", "Content-Type");
                        byte[] responseBytes = jsonResponse.toString()
                                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
                        exchange.sendResponseHeaders(200, responseBytes.length);
                        try (OutputStream os = exchange.getResponseBody()) {
                            os.write(responseBytes);
                        }
                    }
                    case "OPTIONS" -> {
                        // Handle preflight requests for CORS
                        Headers headers = exchange.getResponseHeaders();
                        headers.add("Access-Control-Allow-Origin", FRONTEND_URL);
                        headers.add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
                        headers.add("Access-Control-Allow-Headers", "Content-Type");
                        exchange.sendResponseHeaders(204, -1); // No content
                    }
                    default -> exchange.sendResponseHeaders(405, -1); // Method Not Allowed
                }
        });

        server.setExecutor(null);
        server.start();

        System.out.println("Java backend server started on port 8080");
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            System.out.println("Java backend server stopped.");
        }
    }
}