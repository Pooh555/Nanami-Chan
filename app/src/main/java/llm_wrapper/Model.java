package llm_wrapper;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.json.JSONObject;

public class Model {
    protected String modelName;
    protected String modelPersonality;
    protected List<JSONObject> conversationHistory;

    public Model(String modelName, String personalityIdentifier) throws IOException {
        this.modelName = modelName;
        this.conversationHistory = new ArrayList<>();
        this.modelPersonality = this.loadPersonalityFromFile(personalityIdentifier);

        if (!this.modelPersonality.isEmpty()) {
            JSONObject systemMessage = new JSONObject();

            systemMessage.put("role", "system");
            systemMessage.put("content", this.modelPersonality);
            conversationHistory.add(systemMessage);
        }
    }

    protected String loadPersonalityFromFile(String personalityIdentifier) throws IOException {
        // Selecting the personality based on the user's choice
        String personalityPath = "./personalities/";

        switch (personalityIdentifier) {
            case "Kita Ikuyo":
                personalityPath = personalityPath + "kita_ikuyo.txt";
                break;
            case "Nanami Osaka":
                personalityPath = personalityPath + "nanami_osaka.txt";
                break;
            default:
                throw new IOException("Unknown personality identifier: " + personalityIdentifier
                        + ". Provide a valid path or identifier.");
        }

        File personalityFile = new File(personalityPath);

        if (!personalityFile.exists()) {
            throw new IOException("Personality file not found: " + personalityPath);
        }
        if (!personalityFile.canRead()) {
            throw new IOException("Cannot read personality file: " + personalityPath + ". Check permissions.");
        }

        try {
            List<String> lines = Files.readAllLines(Paths.get(personalityPath), StandardCharsets.UTF_8);
            return lines.stream().collect(Collectors.joining(System.lineSeparator()));
        } catch (IOException e) {
            throw new IOException("Error reading personality file '" + personalityPath + "': " + e.getMessage(), e);
        }
    }

    public void saveConversationHistory() {
        int count = 1;

        for (JSONObject message : this.conversationHistory) {
            String jsonString = message.toString();

            try (FileWriter file = new FileWriter(
                    "./conversation_history/message_" + count + ".json")) {
                file.write(jsonString);
                count++;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void launch() throws Exception {
        // Overridden in child class
    }

    protected String escapeJson(String text) {
        return text.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
