package com.nanami.llm_wrapper;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;

public class Model {
    protected final String TAG = "LLM_wrapper";
    protected String modelName;
    protected String modelPersonality;
    protected String personalityPath = "personalities";
    protected List<JSONObject> conversationHistory;

    // Model constructor
    public Model(Context context, String modelName, String personalityIdentifier) throws Exception {
        this.modelName = modelName;
        this.conversationHistory = new ArrayList<>();
        this.modelPersonality = this.loadPersonalityFromFile(context, personalityIdentifier);

        // Tell the LLM model to behave like the desired personality
        assert this.modelPersonality != null;
        if (!this.modelPersonality.isEmpty()) {
            JSONObject systemMessage = new JSONObject();

            systemMessage.put("role", "system");
            systemMessage.put("content", this.modelPersonality);
            conversationHistory.add(systemMessage);
        }
    }

    // Load personality file (String), and return it
    private String loadPersonalityFromFile(Context context, String personalityIdentifier) {
        String fileName = "";

        switch (personalityIdentifier) {
            case "Kita Ikuyo":
                fileName = personalityPath + "/kita_ikuyo.txt";
                Log.d(TAG, "Attempting to load the LLM's personality from " + "\"" + fileName + "\"");
                break;
            case "Nanami Ousaka":
                fileName = personalityPath + "/nanami_osaka.txt";
                Log.d(TAG, "Attempting to load the LLM's personality from " + "\"" + fileName + "\"");
                break;
            default:
                Log.e(TAG, "Personality file not found for " + "\"" +personalityIdentifier + "\"");
        }

        // Load the content from the personality file
        try (InputStream is = context.getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            Log.d(TAG, "Attempting to read " + "\"" + fileName + "\"");

            StringBuilder sb = new StringBuilder();
            String line;

            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            return sb.toString().trim();
        } catch (Exception e) {
            Log.e(TAG, "Failed to initialize the LLM's personality:" + e.getMessage());

            return null;
        }
    }

    public void saveConversationHistory() {
        String historyPath = "conversation_history";
        int count = 1;

        // Remove past chat history
        cleanDirectory(historyPath);

        for (JSONObject message : this.conversationHistory) {
            String jsonString = message.toString();

            try (FileWriter file = new FileWriter(
                    historyPath + "/message" + count + ".json")) {
                file.write(jsonString);
                count++;
            } catch (IOException e) {
                Log.e(TAG, "Failed to save the conversation history:" + e.getMessage());
            }
        }
    }

    // Encode JSON string
    protected String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    // Remove all files in a directory
    private void cleanDirectory(String directoryPath) {
        File folder = new File(directoryPath);

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        try {
                            if (!file.delete()) {
                                Log.e(TAG, "Failed to clear the conversation history directory." + directoryPath);
                            }
                        } finally {
                            Log.e(TAG, "Cleared past conversation history.");
                        }
                    }
                }
            }
        } else {
            Log.e(TAG, "Folder does not exist or is not a directory: " + directoryPath);
        }
    }

    // Terminate the LLM process
    public void onDestroy(){
        saveConversationHistory();  // Save the conversation history
    }
}
