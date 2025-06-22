package llm_wrapper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

import org.json.JSONArray;
import org.json.JSONObject;

import keys.API_keys;
import tts.ElevenlabsTTS;

public class Ollama extends Model {
     public Ollama(String modelName, String personalityIdentifier) throws IOException {
          super(modelName, personalityIdentifier);
     }

     public String getResponseText(String inputPrompt) throws IOException {
          HttpURLConnection connection = null;

          try {
               // Open a connection to Ollama
               URI uri = new URI(API_keys.OLLAMA_API_URL);
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

               if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw new IOException("Ollama API call failed with code " + responseCode);
               }

               StringBuilder responseBuilder = new StringBuilder();
               String line;

               try (BufferedReader in = new BufferedReader(
                         new InputStreamReader(
                                   (responseCode >= 200 && responseCode < 300) ? connection.getInputStream()
                                             : connection.getErrorStream(),
                                   StandardCharsets.UTF_8))) {

                    while ((line = in.readLine()) != null) {
                         responseBuilder.append(line);
                    }
               }

               String rawResponse = responseBuilder.toString();
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

                    ElevenlabsTTS voice = new ElevenlabsTTS();
                    String userPrompt = scanner.nextLine();

                    // Prompt to exit LLM
                    if (userPrompt.equalsIgnoreCase("exit") || userPrompt.equalsIgnoreCase("quit") || userPrompt.equalsIgnoreCase("bye")) {
                         recievedMessage = this.getResponseText("I have got to go now. Goodbye, see you next time.") + " さようなら!";
                         
                         System.out.println("\n" + recievedMessage);
                         voice.speak(recievedMessage);
                         break;
                    }

                    recievedMessage = this.getResponseText(userPrompt);
                    
                    System.out.println("\n" + recievedMessage);
                    voice.speak(recievedMessage);
               }
          } finally {
               super.saveConversationHistory();
               scanner.close();
          }
     }

}