package llm_wrapper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;

import org.json.JSONArray;
import org.json.JSONObject;

import tts.ElevenlabsTTS;

public class Ollama extends Model {
     private static final String OLLAMA_API_URL = "http://localhost:11434/api/chat";

     public Ollama(String modelName, String personalityIdentifier) throws IOException {
          super.modelName = modelName;
          super.conversationHistory = new ArrayList<>();
          super.modelPersonality = super.loadPersonalityFromFile(personalityIdentifier);

          if (!super.modelPersonality.isEmpty()) {
               JSONObject systemMessage = new JSONObject();
               systemMessage.put("role", "system");
               systemMessage.put("content", super.modelPersonality);
               conversationHistory.add(systemMessage);
          }
     }

     public String getResponseText(String inputPrompt) throws IOException {
          HttpURLConnection connection = null;

          try {
               // Open a connection to Ollama
               URI uri = new URI(Ollama.OLLAMA_API_URL);
               URL url = uri.toURL();
               connection = (HttpURLConnection) url.openConnection();

               connection.setConnectTimeout(5000);
               connection.setReadTimeout(60000);

               connection.setRequestMethod("POST");
               connection.setRequestProperty("Content-Type", "application/json; utf-8");
               connection.setRequestProperty("Accept", "application/json");
               connection.setDoOutput(true);

               // Add the user's latest prompt
               JSONObject userMessage = new JSONObject();
               userMessage.put("role", "user");
               userMessage.put("content", super.escapeJson(inputPrompt));
               conversationHistory.add(userMessage);

               // Input the entire conversation history to the model
               JSONObject requestBody = new JSONObject();
               requestBody.put("model", super.modelName);
               requestBody.put("messages", new JSONArray(super.conversationHistory));
               requestBody.put("stream", false);

               String jsonInputString = requestBody.toString();

               try (OutputStream os = connection.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
               } catch (IOException e) {
                    throw new IOException(
                              "Failed to write request body to Ollama. Is the server running and accessible?", e);
               }

               // Retrieving a response from the model
               int responseCode = connection.getResponseCode();
               // System.out.println("Ollama API Response Code: " + responseCode);
               StringBuilder responseBuilder = new StringBuilder();

               try (BufferedReader in = new BufferedReader(
                         new InputStreamReader(
                                   (responseCode >= 200 && responseCode < 300) ? connection.getInputStream()
                                             : connection.getErrorStream(),
                                   StandardCharsets.UTF_8))) {

                    String line;

                    while ((line = in.readLine()) != null) {
                         responseBuilder.append(line);
                    }
               }

               String rawResponse = responseBuilder.toString();
               // System.out.println("Ollama Raw Response: " + rawResponse);

               if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Ollama API call failed with code " + responseCode + ": " + rawResponse);
               }

               JSONObject jsonResponse = new JSONObject(rawResponse);
               String responseText = jsonResponse.getJSONObject("message").getString("content");

               JSONObject assistantMessage = new JSONObject();
               assistantMessage.put("role", "assistant");
               assistantMessage.put("content", responseText);
               super.conversationHistory.add(assistantMessage);

               return responseText;

          } catch (java.net.URISyntaxException e) {
               throw new IOException("Invalid URI syntax for Ollama API: " + e.getMessage(), e);
          } finally {
               if (connection != null) {
                    connection.disconnect();
               }
          }
     }

     @Override
     public void launch() throws Exception {
          // Get messages from the terminal (temporary)
          Scanner scanner = new Scanner(System.in);
          String recievedMessage;

          try {
               while (true) {
                    System.out.print(">> ");

                    String userPrompt = scanner.nextLine();

                    // Prompt to exit LLM
                    if (userPrompt.equalsIgnoreCase("exit") || userPrompt.equalsIgnoreCase("quit")) {
                         System.out.println("おやすみなさい!");
                         break;
                    }

                    recievedMessage = this.getResponseText(userPrompt);
                    ElevenlabsTTS voice = new ElevenlabsTTS();

                    System.out.println("\n" + recievedMessage);
                    voice.speak(recievedMessage);
               }
          } finally {
               super.saveConversationHistory();
               scanner.close();
          }
     }

     
}