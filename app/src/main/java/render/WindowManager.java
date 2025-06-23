package render;

import java.io.IOException;
import java.nio.IntBuffer;

import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.UnsupportedAudioFileException;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.GLFW_FALSE;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.glfw.GLFW.GLFW_TRUE;
import static org.lwjgl.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.glfw.GLFW.glfwGetWindowSize;
import static org.lwjgl.glfw.GLFW.glfwInit;
import static org.lwjgl.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.glfw.GLFW.glfwSetKeyCallback;
import static org.lwjgl.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.glfw.GLFW.glfwTerminate;
import static org.lwjgl.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.glfw.GLFW.glfwWindowShouldClose;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.glfw.GLFWVidMode;
import org.lwjgl.opengl.GL;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import org.lwjgl.system.MemoryStack;
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

import llm_wrapper.Ollama;
import main.Main;
import stt.VoskSTT;
import tts.ElevenlabsTTS;

public class WindowManager {
    private final String title;
    private final int width;
    private final int height;
    private long window;

    private volatile boolean runningLLMLoop = true;

    public WindowManager(String title, int width, int height) {
        this.title = title;
        this.width = width;
        this.height = height;
    }

    public void run() throws Exception {
        this.init();

        /*
         * Available model
         * - Ollama: llama3:lastest (default)
         * * Available personalities
         * - Kita Ikuyo (From ぼっちざろっく!)
         * - Nanami Osaka (From 現実もたまには嘘をつく) (default)
         */
        Ollama model = new Ollama(Main.modelName, Main.personality);
        /*
         * Available speech-to-text options
         * - Vosk (vosk-model-small-en-us-0.15) (default)
         */
        VoskSTT stt = new VoskSTT();
        /*
         * Available text-to-speech options
         * - Elevenlabs (default)
         */
        ElevenlabsTTS voice = new ElevenlabsTTS();

        // Create and start a new thread for LLM interaction
        Thread llmThread = new Thread(() -> {
            try {
                System.out.println("\nNanami has woken up. You can talk to her now.");

                while (runningLLMLoop && !glfwWindowShouldClose(window)) {
                    String userPrompt = "goodbye";
                    String receivedMessage;

                    // Get user prompt using sound-to-text (STT)
                    try {
                        userPrompt = stt.listenAndTranscribe();
                    } catch (LineUnavailableException | IOException | UnsupportedAudioFileException e) {
                        System.err.println("Error during speech-to-text: " + e.getMessage());

                        runningLLMLoop = false;

                        break;
                    }

                    System.out.println("\nUser: " + userPrompt);

                    // Prompt to exit LLM
                    if (userPrompt.equalsIgnoreCase("good bye") || userPrompt.equalsIgnoreCase("goodbye")) {
                        receivedMessage = model.getResponseText("I have got to go now. Goodbye, see you next time.")
                                + " さようなら!";

                        System.out.println("\nNanami Chan: " + receivedMessage);
                        voice.speak(receivedMessage);

                        runningLLMLoop = false;

                        glfwSetWindowShouldClose(window, true);
                        break;
                    }

                    receivedMessage = model.getResponseText(userPrompt);

                    System.out.println("\nNanami Chan: " + receivedMessage);
                    voice.speak(receivedMessage);
                }
            } catch (IOException e) {
                System.err.println("Unable to run the LLM model: " + e.getMessage());
            } finally {
                try {
                    model.saveConversationHistory();
                } catch (IOException e) {
                    System.err.println("Unable to save the conversation history: " + e.getMessage());
                }
            }
        });

        llmThread.start();

        while (!glfwWindowShouldClose(window)) {
            this.update();
        }

        runningLLMLoop = false;

        llmThread.interrupt();
        llmThread.join();

        glfwFreeCallbacks(window); // Free the window callbacks
        glfwMakeContextCurrent(NULL); // Disassociate the current context
        glfwDestroyWindow(window); // Destroy the window
        glfwTerminate(); // Terminate GLFW
        glfwSetErrorCallback(null).free(); // Free the error callback
        System.out.println("Window manager terminated.");
    }

    private void init() {
        GLFWErrorCallback.createPrint(System.err).set();

        // Initialize GLFW
        if (!glfwInit())
            throw new IllegalStateException("Unable to initialize GLFW");

        // Configure the window
        glfwDefaultWindowHints(); // Get default window hints
        glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE); // Hide the window from the user
        glfwWindowHint(GLFW_RESIZABLE, GLFW_TRUE); // Make the window resizable

        window = glfwCreateWindow(this.width, this.height, this.title, NULL, NULL);

        // Check for any error
        if (window == NULL)
            throw new RuntimeException("Failed to create the GLFW window");

        // Press escape key to close the window
        glfwSetKeyCallback(window, (window, key, scancode, action, mods) -> {
            if (key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE) {
                runningLLMLoop = false;
                glfwSetWindowShouldClose(window, true);
            }
        });

        try (MemoryStack stack = stackPush()) {
            IntBuffer pWidth = stack.mallocInt(1);
            IntBuffer pHeight = stack.mallocInt(1);

            glfwGetWindowSize(window, pWidth, pHeight); // Pass the window's size to glfwCreateWindow

            // Get the resolution of the primary monitor
            GLFWVidMode vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());

            // Center the window
            glfwSetWindowPos(
                    window,
                    (vidmode.width() - pWidth.get(0)) / 2,
                    (vidmode.height() - pHeight.get(0)) / 2);
        }

        glfwMakeContextCurrent(window); // Set context to current
        glfwShowWindow(window); // Make the window visible
        GL.createCapabilities();
        glClearColor(0.0f, 0.0f, 0.0f, 0.0f); // Set initial background color
    }

    private void update() {
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT); // Clear the framebuffer
        glClearColor((float) (Math.sin(System.currentTimeMillis() / 1000.0)),
                (float) (Math.cos(System.currentTimeMillis() / 1000.0)),
                (float) (Math.tan(System.currentTimeMillis() / 1000.0)), 0.0f);
        glfwSwapBuffers(window); // Swap the color buffers to display the rendered image
        glfwPollEvents(); // Process all pending events
    }
}
