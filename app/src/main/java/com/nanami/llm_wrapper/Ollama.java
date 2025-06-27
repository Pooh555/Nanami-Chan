package com.nanami.llm_wrapper;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.nanami.keys.API_keys;

public class Ollama extends Model {
     private final String TAG = super.TAG + ":Ollama";
     private static Ollama ollamaInstance;

     // Callback interface for a robust workflow
     public interface OllamaCallback {
          void onSuccess(String response);
          void onError(Exception e);
     }

     // Get instance
     public static Ollama getInstance(Context context, String modelName, String personalityIdentifier) {
          if (ollamaInstance == null) {
               try {
                    ollamaInstance = new Ollama(context, modelName, personalityIdentifier);
               } catch (Exception e) {
                    Log.d("LLM_wrapper:Ollama", "Failed to initialize Ollama.");
               }
          }

          return ollamaInstance;
     }

     // Ollama constructor
     public Ollama(Context context, String modelName, String personalityIdentifier) throws Exception {
          super(context, modelName, personalityIdentifier);
     }

     // Launch Ollama
     public void onStart(Context context) {
          // ------------------------------------ //
          // Temporary code snippet for debugging //
          // TODO: Implement real service
          // ------------------------------------ //
          getResponseText("Hello! Who are you?", new Ollama.OllamaCallback() {
               @Override
               public void onSuccess(String response) {
                    Log.d(TAG, "Ollama said: " + response);

                    // Display a pop-up message
                    ((Activity) context).runOnUiThread(() ->
                              Toast.makeText(context, "Ollama said: " + response, Toast.LENGTH_LONG).show()
                    );
               }

               @Override
               public void onError(Exception e) {
                    Log.e(TAG, "Ollama error", e);

                    // Display a pop-up message
                    ((Activity) context).runOnUiThread(() ->
                             Toast.makeText(context, "Failed to get response from Ollama: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );
               }
          });
     }

     // Retrieve a response JSON String from Ollama API
     private void getResponseText(String inputPrompt, OllamaCallback callback) {
          // Create a parallel thread for Ollama
          new Thread(() -> {
               HttpURLConnection connection = null;

               try {
                    URI uri = new URI(API_keys.OLLAMA_API_URL);
                    URL url = uri.toURL();

                    Log.d(TAG, "Attempting to connect to " + url);

                    // Define connection's attributes
                    connection = (HttpURLConnection) url.openConnection();
                    
                    connection.setConnectTimeout(5000);     // Connection's timeout: 5000 ms
                    connection.setReadTimeout(60000);  // Response wait time timeout: 60000 ms
                    connection.setRequestMethod("POST");    // Send a POST request to the server
                    connection.setRequestProperty("Content-Type", "application/json; utf-8");  // Use utf-8
                    connection.setRequestProperty("Accept", "application/json");     // Use JSON
                    connection.setDoOutput(true);

                    // Create a JSONObject for the user's input prompt
                    JSONObject userMessage = new JSONObject();

                    userMessage.put("role", "user");   // Append the role "user" to user's message
                    userMessage.put("content", super.escapeJson(inputPrompt));  // Append the user's prompt as content
                    conversationHistory.add(userMessage);

                    // Create a JSONObject for the request body (conversation history + latest user's prompt)
                    JSONObject requestBody = new JSONObject();

                    requestBody.put("model", super.modelName);   // Specify the model name
                    requestBody.put("messages", new JSONArray(super.conversationHistory));     //  Append the entire conversation history to the request body
                    requestBody.put("stream", false);  // Disable data stream

                    // Send a request, wait, and retrieve the response.
                    String jsonInputString = requestBody.toString();
                    JSONObject jsonResponse = getJsonObject(connection, jsonInputString);
                    String responseText = jsonResponse.getJSONObject("message").getString("content");

                    JSONObject assistantMessage = new JSONObject();

                    assistantMessage.put("role", "assistant");   // Append the role "assistant" to the server's response
                    assistantMessage.put("content", responseText);    // Append the server's response as content
                    conversationHistory.add(assistantMessage);   // Add the server's response to the conversation history

                    Log.d(TAG, "Ollama's response: " + responseText);

                    callback.onSuccess(responseText);

               } catch (Exception e) {
                    Log.e(TAG, "Ollama connection failed: " + e.getMessage(), e);

                    callback.onError(e);
               } finally {
                    if (connection != null) connection.disconnect();  // Disconnect if connection failed
               }
          }).start();
     }

     // Retrieve JSONObject from Ollama's API
     private JSONObject getJsonObject(HttpURLConnection connection, String jsonInputString) throws IOException, JSONException {
          try (OutputStream os = connection.getOutputStream()) {
               byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);

               os.write(input, 0, input.length);
          } catch (IOException e) {
               Log.e(TAG, "Failed to write request body to Ollama. Is the server running and accessible? Attempted to access " + API_keys.OLLAMA_API_URL);
          }

          // Retrieving a response from the model, and convert to string
          int responseCode = connection.getResponseCode();
          String rawResponse = getString(connection, responseCode);

          return new JSONObject(rawResponse);
     }

     // Build a string from Ollama's API raw response
     private String getString(HttpURLConnection connection, int responseCode) throws IOException {
          if (responseCode != HttpURLConnection.HTTP_OK) {
               Log.e(TAG, "Ollama API call failed with code " + responseCode);
          }

          StringBuilder responseBuilder = new StringBuilder();
          String line;

          try (BufferedReader in = new BufferedReader(
                    new InputStreamReader(
                            connection.getInputStream(),
                              StandardCharsets.UTF_8))) {

               while ((line = in.readLine()) != null) {
                    responseBuilder.append(line);
               }
          }

         return responseBuilder.toString();
     }
}
