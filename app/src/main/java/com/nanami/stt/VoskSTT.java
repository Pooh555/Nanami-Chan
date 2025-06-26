package com.nanami.stt;

import android.app.Activity;
import android.content.Context;
import android.widget.Toast;

import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.SpeechService;
import org.vosk.android.SpeechStreamService;
import org.vosk.android.StorageService;

import java.io.IOException;

import androidx.annotation.OptIn;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.UnstableApi;

public class VoskSTT implements org.vosk.android.RecognitionListener {
    private static final String TAG = "Vosk";
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private Model model;
    private SpeechService speechService;
    private SpeechStreamService speechStreamService;
    private final ModelCallback callback;

    // Define the callback interface
    public interface ModelCallback {
        void onModelReady();
        void onModelFailed(String errorMessage);
    }

    // VoskSTT constructor
    @OptIn(markerClass = UnstableApi.class)
    public VoskSTT(Context context, ModelCallback callback) {
        this.callback = callback;

        // Load the Vosk STT model
        StorageService.unpack(context, "model-en-us", "model",
                (model) -> {
                    this.model = model;

                    if (this.callback != null) {
                        this.callback.onModelReady();
                    }

                    Log.d(TAG, "Vosk STT model is loaded successfully.");
                },
                (e) -> {
                    Log.e(TAG, "Failed to unpack model: " + e.getMessage());

                    // Display a pop-up message
                    ((Activity) context).runOnUiThread(() ->
                            Toast.makeText(context, "Failed to unpack model: " + e.getMessage(), Toast.LENGTH_LONG).show()
                    );

                    if (this.callback != null) {
                        this.callback.onModelFailed(e.getMessage());
                    }
                });
    }

    // Pause Vosk service
    public void onPause() {
        // TODO: Implement onPause method
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

    // Result
    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onResult(String hypothesis) {
        Log.d(TAG, "Text: " + hypothesis);
    }

    // Final result
    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onFinalResult(String hypothesis) {
        Log.d(TAG, "Final Result: " + hypothesis);

        if (speechStreamService != null) {
            speechStreamService = null;
        }
    }

    // Partial result
    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onPartialResult(String hypothesis) {
        Log.d(TAG, "Partial Result: " + hypothesis);
    }

    // Initialize a speech recognizer for Vosk service
    @OptIn(markerClass = UnstableApi.class)
    public void recognizeMicrophone() {
        // Verify model's existence
        if (model == null) {
            Log.e(TAG, "Model not loaded yet. Cannot start microphone recognition.");

            return;
        }
        // If a speech service is already running, terminate it
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
            speechService = null;
        }

        // Initialize a new recognizer
        try {
            Recognizer rec = new Recognizer(model, 16000.0f);
            speechService = new SpeechService(rec, 16000.0f);

            speechService.startListening(this); // Start listening to the user's speech
        } catch (IOException e) {
            Log.e(TAG, "Error starting microphone recognition: " + e.getMessage());
        }
    }

    // Get microphone permission
    public static int getPermissionsRequestRecordAudio() {
        return PERMISSIONS_REQUEST_RECORD_AUDIO;
    }

    // Callbacks
    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onError(Exception e) {
        Log.e(TAG, "Vosk Error: " + e.getMessage());
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onTimeout() {
        Log.d(TAG, "Vosk Timeout");
        recognizeMicrophone();
    }
}