package com.nanami.stt;

import android.content.Context;

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
    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final String TAG = "Vosk";

    private Model model;
    private SpeechService speechService;
    private SpeechStreamService speechStreamService;
    private final ModelCallback callback;

    // Define the callback interface
    public interface ModelCallback {
        void onModelReady();
        void onModelFailed(String errorMessage);
    }

    @OptIn(markerClass = UnstableApi.class)
    public VoskSTT(Context context, ModelCallback callback) {
        this.callback = callback;   // Accept callback

        /*
         * The Vosk models are located in /models.
         */
        StorageService.unpack(context, "model-en-us", "model",
                (model) -> {
                    this.model = model;
                    if (this.callback != null) {
                        this.callback.onModelReady();
                    }
                },
                (exception) -> {
                    Log.e(TAG, "Failed to unpack model: " + exception.getMessage());

                    if (this.callback != null) {
                        this.callback.onModelFailed(exception.getMessage());
                    }
                });
    }

    public static int getPermissionsRequestRecordAudio() {
        return PERMISSIONS_REQUEST_RECORD_AUDIO;
    }

    private void pause(boolean checked) {
        if (speechService != null) {
            speechService.setPause(checked);
        }
    }

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

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onResult(String hypothesis) {
        Log.d(TAG, "Text: " + hypothesis);

    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onFinalResult(String hypothesis) {
        Log.d(TAG, "Final Result: " + hypothesis);

        if (speechStreamService != null) {
            speechStreamService = null;
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onPartialResult(String hypothesis) {
        Log.d(TAG, "Partial Result: " + hypothesis);
    }

    @OptIn(markerClass = UnstableApi.class)
    private void recognizeFile(Context context) {
        if (model == null) {
            Log.e(TAG, "Model not loaded yet for file recognition.");
            return;
        }
        if (speechStreamService != null) {
            speechStreamService.stop();
            speechStreamService = null;
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    public void recognizeMicrophone() {
        if (model == null) {
            Log.e(TAG, "Model not loaded yet. Cannot start microphone recognition.");

            return;
        }
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
            speechService = null;
        }
        try {
            Recognizer rec = new Recognizer(model, 16000.0f);
            speechService = new SpeechService(rec, 16000.0f);

            speechService.startListening(this); // Start listening to the user's speech
        } catch (IOException e) {
            Log.e(TAG, "Error starting microphone recognition: " + e.getMessage());
        }
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onError(Exception exception) {
        Log.e(TAG, "Vosk Error: " + exception.getMessage());
    }

    @OptIn(markerClass = UnstableApi.class)
    @Override
    public void onTimeout() {
        Log.d(TAG, "Vosk Timeout");
        recognizeMicrophone();
    }
}