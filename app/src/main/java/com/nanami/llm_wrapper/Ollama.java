package com.nanami.llm_wrapper;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.nanami.keys.API_keys;

public class Ollama extends Model {
     private static final String TAG = "Ollama";

     public interface OllamaCallback {
          void onSuccess(String response);
          void onError(Exception e);
     }

     public Ollama(Context context, String modelName, String personalityIdentifier) throws Exception {
          super(context, modelName, personalityIdentifier);
     }

     public void getResponseText(String inputPrompt, OllamaCallback callback) {
          new Thread(() -> {
               HttpURLConnection connection = null;

               try {
                    URI uri = new URI(API_keys.OLLAMA_API_URL);
                    URL url = uri.toURL();
                    Log.d(TAG, "Attempting to connect to " + url);

                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(5000);
                    connection.setReadTimeout(60000);
                    connection.setRequestMethod("POST");
                    connection.setRequestProperty("Content-Type", "application/json; utf-8");
                    connection.setRequestProperty("Accept", "application/json");
                    connection.setDoOutput(true);

                    JSONObject userMessage = new JSONObject();
                    userMessage.put("role", "user");
                    userMessage.put("content", super.escapeJson(inputPrompt));
                    conversationHistory.add(userMessage);

                    JSONObject requestBody = new JSONObject();
                    requestBody.put("model", super.modelName);
                    requestBody.put("messages", new JSONArray(super.conversationHistory));
                    requestBody.put("stream", false);

                    String jsonInputString = requestBody.toString();
                    JSONObject jsonResponse = getJsonObject(connection, jsonInputString);
                    String responseText = jsonResponse.getJSONObject("message").getString("content");

                    JSONObject assistantMessage = new JSONObject();
                    assistantMessage.put("role", "assistant");
                    assistantMessage.put("content", responseText);
                    conversationHistory.add(assistantMessage);

                    Log.d(TAG, "Ollama's response: " + responseText);
                    callback.onSuccess(responseText);

               } catch (Exception e) {
                    Log.e(TAG, "Ollama connection failed: " + e.getMessage(), e);
                    callback.onError(e);
               } finally {
                    if (connection != null) connection.disconnect();
               }
          }).start();
     }


     @NonNull
     private static JSONObject getJsonObject(HttpURLConnection connection, String jsonInputString) throws IOException, JSONException {
          try (OutputStream os = connection.getOutputStream()) {
               byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
               os.write(input, 0, input.length);
          } catch (IOException e) {
               Log.d(TAG, "Failed to write request body to Ollama. Is the server running and accessible? Attempted to access " + API_keys.OLLAMA_API_URL);
               throw new IOException("Failed to write request body to Ollama. Is the server running and accessibleAttempted to access " + API_keys.OLLAMA_API_URL, e);
          }

          // Retrieving a response from the model
          int responseCode = connection.getResponseCode();

          String rawResponse = getString(connection, responseCode);
          return new JSONObject(rawResponse);
     }

     @NonNull
     private static String getString(HttpURLConnection connection, int responseCode) throws IOException {
          if (responseCode != HttpURLConnection.HTTP_OK) {
               Log.d(TAG, "Ollama API call failed with code " + responseCode);
               throw new IOException("Ollama API call failed with code " + responseCode);
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