package com.nanami.llm_wrapper;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;

public class Model {
    protected String modelName;
    protected String modelPersonality;
    protected String personalityPath = "./personalities/";
    protected List<JSONObject> conversationHistory;

    public Model(Context context, String modelName, String personalityIdentifier) throws Exception {
        this.modelName = modelName;
        this.conversationHistory = new ArrayList<>();
        this.modelPersonality = this.loadPersonalityFromFile(context, personalityIdentifier);

        if (!this.modelPersonality.isEmpty()) {
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", this.modelPersonality);
            conversationHistory.add(systemMessage);
        }
    }


    private String loadPersonalityFromFile(Context context, String personalityIdentifier) throws IOException {
        String fileName;

        switch (personalityIdentifier) {
            case "Kita Ikuyo":
                fileName = "personalities/kita_ikuyo.txt";
                break;
            case "Nanami Osaka":
                fileName = "personalities/nanami_osaka.txt";
                break;
            default:
                throw new IOException("Unknown personality identifier: " + personalityIdentifier);
        }

        try (InputStream is = context.getAssets().open(fileName);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {

            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }

            return sb.toString().trim();
        }
    }


    public List<String> readAllLinesCompat(File file) throws IOException {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }

    public void saveConversationHistory() throws IOException {
        String historyPath = "./conversation_history";
        int count = 1;

        // Remove past chat history
        this.cleanDirectory(historyPath);

        for (JSONObject message : this.conversationHistory) {
            String jsonString = message.toString();

            try (FileWriter file = new FileWriter(
                    historyPath + "/message" + count + ".json")) {
                file.write(jsonString);
                count++;
            } catch (IOException e) {
                throw new IOException("Invalid URI syntax for Ollama API: " + e.getMessage(), e);
            }
        }
    }

    protected String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private void cleanDirectory(String directoryPath) {
        File folder = new File(directoryPath);

        if (folder.exists() && folder.isDirectory()) {
            File[] files = folder.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isFile()) {
                        try {
                            file.delete();
                        } finally {
                        }
                    }
                }
            }
        } else {
            System.err.println("Folder does not exist or is not a directory: " + directoryPath);
        }
    }
}
