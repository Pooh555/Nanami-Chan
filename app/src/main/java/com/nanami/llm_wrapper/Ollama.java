package com.nanami.llm_wrapper;

import android.content.Context;

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
     public Ollama(Context context, String modelName, String personalityIdentifier) throws Exception {
          super(context, modelName, personalityIdentifier);
     }

     public String getResponseText(String inputPrompt) throws IOException {
          HttpURLConnection connection = null;

          try {
               // Open a connection to Ollama
               URI uri = new URI(API_keys.OLLAMA_API_URL);
               URL url = uri.toURL();

               System.out.println(url);

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

               JSONObject jsonResponse = getJsonObject(connection, jsonInputString);
               String responseText = jsonResponse.getJSONObject("message").getString("content");
               JSONObject assistantMessage = new JSONObject();

               assistantMessage.put("role", "assistant");
               assistantMessage.put("content", responseText);
               super.conversationHistory.add(assistantMessage);

               return responseText;

          } catch (URISyntaxException | JSONException e) {
               throw new IOException("Invalid URI syntax for Ollama API: " + e.getMessage(), e);
          } finally {
               if (connection != null) {
                    connection.disconnect();
               }
          }
     }

     @NonNull
     private static JSONObject getJsonObject(HttpURLConnection connection, String jsonInputString) throws IOException, JSONException {
          try (OutputStream os = connection.getOutputStream()) {
               byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
               os.write(input, 0, input.length);
          } catch (IOException e) {
               throw new IOException(
                         "Failed to write request body to Ollama. Is the server running and accessible?", e);
          }

          // Retrieving a response from the model
          int responseCode = connection.getResponseCode();

          String rawResponse = getString(connection, responseCode);
          return new JSONObject(rawResponse);
     }

     @NonNull
     private static String getString(HttpURLConnection connection, int responseCode) throws IOException {
          if (responseCode != HttpURLConnection.HTTP_OK) {
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