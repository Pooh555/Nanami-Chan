package com.nanami.stt;

import android.app.Activity;
import android.content.Context;
// import android.widget.Toast;

import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

import java.io.IOException;

import androidx.annotation.OptIn;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;

@UnstableApi
public class VoskSTT implements org.vosk.android.RecognitionListener {
    private static final String TAG = "Vosk";
    private Model model;
    private SpeechService speechService;
    private SpeechStreamService speechStreamService;
    private static VoskSTT voskInstance;
    private VoskSTTListener listener;
    private boolean finalizationScheduled = false;
    private final int endOfSpeechWaitTime = 500;
    private String lastValidResult = null;

    private final android.os.Handler handler = new android.os.Handler(android.os.Looper.getMainLooper());

    private final Runnable forceStopRunnable = () -> {
        if (speechService != null) {
            Log.d(TAG, "Forcing speechService stop to trigger final result");
            speechService.stop(); // triggers onFinalResult
        }
    };

    // Define an interface for callbacks
    public interface VoskSTTListener {
        void onVoskFinalResult(String result);
        void onVoskPartialResult(String partialResult);
        void onVoskError(Exception e);
        void onVoskReady();
    }

    // Set Vosk listener
    public void setListener(VoskSTTListener listener) {
        this.listener = listener;
    }

    // Get instance
    public static VoskSTT getInstance(Context context) {
        if (voskInstance == null) {
            voskInstance = new VoskSTT(context);
        }
        return voskInstance;
    }

    // VoskSTT constructor
    @OptIn(markerClass = UnstableApi.class)
    public VoskSTT(Context context) {
        // Load the Vosk STT model
        StorageService.unpack(context, "model-en-us", "model",
                (model) -> {
                    this.model = model;

                    Log.d(TAG, "Vosk STT model is loaded successfully.");

                    ((Activity) context).runOnUiThread(() -> {
                        if (listener != null) {
                            listener.onVoskReady(); // Notify listener that model is ready
                        }

                        recognizeMicrophone();  // Start listening
                        // Toast.makeText(context, "Vosk STT started listening!", Toast.LENGTH_SHORT).show();
                    });
                },
                (e) -> {
                    Log.e(TAG, "Failed to unpack model: " + e.getMessage());

                    // Display a pop-up message
                    ((Activity) context).runOnUiThread(() -> {
                        // Toast.makeText(context, "Failed to unpack model: " + e.getMessage(), Toast.LENGTH_LONG).show();

                        if (listener != null) {
                            listener.onVoskError(e); // Notify listener about the error
                        }
                    });
                });
    }

    // Launch Vosk service
    @OptIn(markerClass = UnstableApi.class)
    public void onStart() {
        Log.d(TAG, "Vosk service is initiated.");

        recognizeMicrophone();
    }

    // Stop vosk service temporary
    public void onPause() {
        if (speechService != null) {
            speechService.stop();
        }
    }

    // Terminate Vosk service
    public void onDestroy() {
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
            speechService = null;
        }
        if (speechStreamService != null) {
            speechStreamService.stop();
            speechStreamService = null;
        }
    }

    // Partial result
    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onPartialResult(String hypothesis) {
        Log.d(TAG, "Partial Result: " + hypothesis);

        // Pass the partial result to the listener
        if (listener != null) {
            listener.onVoskPartialResult(hypothesis);
        }
    }

    // Immediate Result
    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onResult(String hypothesis) {
        Log.d(TAG, "Text: " + hypothesis);

        try {
            JSONObject json = new JSONObject(hypothesis);
            String text = json.optString("text", "");

            if (!text.isEmpty()) {
                lastValidResult = text;

                // Reset timer
                handler.removeCallbacks(forceStopRunnable);
                handler.postDelayed(forceStopRunnable, endOfSpeechWaitTime);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse intermediate result: " + e.getMessage());
        }
    }


    // Final result
    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onFinalResult(String hypothesis) {
        Log.d(TAG, "Final Result: " + hypothesis);

        try {
            JSONObject json = new JSONObject(hypothesis);
            String finalText = json.optString("text", "");

            // Fallback if empty
            if (finalText.isEmpty() && lastValidResult != null) {
                finalText = lastValidResult;
                Log.w(TAG, "Using last known text from onResult as fallback: " + finalText);
            }

            if (listener != null && !finalText.isEmpty()) {
                listener.onVoskFinalResult(finalText);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse final result: " + e.getMessage());

            if (listener != null) {
                listener.onVoskError(e);
            }
        }
    }


    // Initialize a speech recognizer for Vosk service
    @OptIn(markerClass = UnstableApi.class)
    public void recognizeMicrophone() {
        // Verify model's existence
        if (model == null) {
            Log.e(TAG, "Model not loaded yet. Cannot start microphone recognition.");

            if (listener != null) {
                listener.onVoskError(new IllegalStateException("Model not loaded yet. Cannot start microphone recognition."));
            }

            return;
        }
        // If a speech service is already running, terminate it
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
            speechService = null;
        }

        finalizationScheduled = false;

        // Initialize a new recognizer
        try {
            Recognizer rec = new Recognizer(model, 16000.0f);
            speechService = new SpeechService(rec, 16000.0f);

            speechService.startListening(this); // Start listening to the user's speech

            // Force final result after 2 seconds
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (speechService != null && finalizationScheduled) {
                    Log.d(TAG, "Forcing speechService stop to trigger final result");
                    speechService.stop(); // This triggers onFinalResult
                }
            }, endOfSpeechWaitTime); // Delay in milliseconds
        } catch (IOException e) {
            Log.e(TAG, "Error starting microphone recognition: " + e.getMessage());

            if (listener != null) {
                listener.onVoskError(e);
            }
        }
    }

    // Callbacks
    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onError(Exception e) {
        Log.e(TAG, "Vosk Error: " + e.getMessage());

        if (listener != null) {
            listener.onVoskError(e);
        }
    }

    // Timeout
    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onTimeout() {
        Log.d(TAG, "Vosk Timeout");
        recognizeMicrophone();
    }

    // Get the final result
    public String getLastValidResult() {
        return lastValidResult != null ? lastValidResult : "";
    }

}