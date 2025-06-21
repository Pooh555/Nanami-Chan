package LLM_wrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONObject;

public class Ollama {
     public String getResponse() throws IOException {
       // Variables for the model and prompt
        String modelName = "llama3:latest";
        String promptText = "Hello, who are you?";

        // Set up the URL and connection
        URI uri = null;
        
        try {
            uri = new URI("http://localhost:11434/api/generate");
        } catch (java.net.URISyntaxException e) {
            // Handle the case where the URI string is malformed
            throw new IOException("Invalid URI syntax: " + e.getMessage(), e);
        }

        URL url = uri.toURL();
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json; utf-8");
        connection.setRequestProperty("Accept", "application/json");
        connection.setDoOutput(true);

        // JSON body using variables
        String jsonInputString = String.format("{\"model\": \"%s\", \"prompt\":\"%s\", \"stream\":false}", modelName,
                promptText);

        // Write the JSON body to the request
        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
            os.write(input, 0, input.length);
        } catch (Exception e) {
            throw new IOException("Unable to access the output stream.");
        }

        // Get the response
        int code = connection.getResponseCode();
        
        System.out.println("Response code: " + code);

        // Read the response body
        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;

        while ((line = in.readLine()) != null) {
            response.append(line);
        }

        in.close();

        System.out.println(response.toString());

        // Parse the JSON response and print the "response" field
        JSONObject jsonResponse = new JSONObject(response.toString());
        String responseText = jsonResponse.getString("response");
        System.out.println("Response: " + responseText);

        return responseText;
    }
}
