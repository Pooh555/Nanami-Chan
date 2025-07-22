package com.nanami;

import com.nanami.keys.API_keys;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
// import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.media3.common.util.UnstableApi;

import com.nanami.frontend.GLRenderer;
import com.nanami.frontend.LAppDelegate;
import com.nanami.llm_wrapper.Ollama;
import com.nanami.stt.VoskSTT;
import com.nanami.tts.ElevenlabsTTS;

import java.util.Objects;

@UnstableApi
public class MainActivity extends Activity implements VoskSTT.VoskSTTListener {
    private static final String TAG = "MainActivity";
    private GLSurfaceView glSurfaceView;
    private GLRenderer glRenderer;
    private Ollama ollamaModel;
    private VoskSTT voskModel;
    private ElevenlabsTTS elevenlabsModel;
    private final String keywordSong = "*sing*";

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Setting up Live2D frontend
        glSurfaceView = new GLSurfaceView(this);
        glRenderer = new GLRenderer();

        glSurfaceView.setEGLContextClientVersion(2); // OpenGL ES 2.0を利用
        glSurfaceView.setRenderer(glRenderer);
        glSurfaceView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);

        setContentView(glSurfaceView);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        } else {
            Objects.requireNonNull(getWindow().getInsetsController()).hide(WindowInsets.Type.navigationBars() | WindowInsets.Type.statusBars());
            getWindow().getInsetsController()
                    .setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }

        onStart();
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Launch Live2D renderer, Vosk STT, Elevenlabs TTS and Ollama
        LAppDelegate.getInstance().onStart(this);
        voskModel = VoskSTT.getInstance(this);
        voskModel.onStart(this);
        ollamaModel = Ollama.getInstance(this, API_keys.OLLAMA_MODEL_NAME, API_keys.PERSONALITY);
        ollamaModel.onStart(this);
        elevenlabsModel = ElevenlabsTTS.getInstance();
        elevenlabsModel.onStart();
        elevenlabsModel.setAudioLevelListener(level -> {
            if (LAppDelegate.getInstance() != null) {
                LAppDelegate.getInstance().setLipSyncValue(level);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();

        glSurfaceView.onResume();

        View decor = this.getWindow().getDecorView();
        decor.setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override
    protected void onPause() {
        super.onPause();

        glSurfaceView.onPause();
        LAppDelegate.getInstance().onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        LAppDelegate.getInstance().onStop();    // Stop Live2D render process
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        LAppDelegate.getInstance().onDestroy(); // Terminate Live2D process
        VoskSTT.getInstance(this).onDestroy();  // Terminate STT process
        ElevenlabsTTS.getInstance().onDestroy();    // Terminate TTS process
        Ollama.getInstance(this, API_keys.OLLAMA_MODEL_NAME, API_keys.PERSONALITY).onDestroy(this);   // Terminate LLM process
    }

    @Override
    public boolean onTouchEvent(final MotionEvent event) {
        final float pointX = event.getX();
        final float pointY = event.getY();

        glSurfaceView.queueEvent(
                () -> {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            LAppDelegate.getInstance().onTouchBegan(pointX, pointY);
                            break;
                        case MotionEvent.ACTION_UP:
                            LAppDelegate.getInstance().onTouchEnd(pointX, pointY);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            LAppDelegate.getInstance().onTouchMoved(pointX, pointY);
                            break;
                    }
                });

        return super.onTouchEvent(event);
    }

    // Vosk --------------------------------------------------------------------------------------------------------------------

    // Return final result to Ollama
    @Override
    public void onVoskFinalResult(String result) {
        runOnUiThread(() -> {
            Log.d(TAG, "Vosk Final Result received in MainActivity: " + result);

            // Input the Vosk result into Ollama
            if (ollamaModel != null && !result.isEmpty() && !result.equals("huh")) {
                Log.d(TAG, "Received message from Vosk, attempting to generate response.");

                // Pause Vosk IMMEDIATELY. VoskSTT.onFinalResult will no longer restart it.
                voskModel.onPause();

                ollamaModel.generateResponse(result, new Ollama.OllamaCallback() {
                    @Override
                    public void onSuccess(String ollamaResponse) {
                        runOnUiThread(() -> {
                            Log.d(TAG, "Ollama Response: " + ollamaResponse);

                            if (keywordSong.equalsIgnoreCase(ollamaResponse.substring(0, keywordSong.length()))) {
                                Log.d(TAG, "Attempting to sing a song.");

                                elevenlabsModel.speak(MainActivity.this, ollamaResponse, new ElevenlabsTTS.SpeechCompletionListener() {
                                    @Override
                                    public void onSpeechFinished() {
                                        runOnUiThread(() -> {
                                            Log.d(TAG, "ElevenLabs speech finished. Resuming Vosk listening.");
                                        });

                                        // Sing a song from the asset
                                        elevenlabsModel.singAssetSong(MainActivity.this, "suki_dakara", new ElevenlabsTTS.SpeechCompletionListener() {
                                            @Override
                                            public void onSpeechFinished() {
                                                runOnUiThread(() -> {
                                                    Log.d(TAG, "Finished singing a song.");
                                                    voskModel.recognizeMicrophone(); // Resume listening here
                                                });
                                            }

                                            @Override
                                            public void onSpeechError(Exception e) {
                                                runOnUiThread(() -> {
                                                    Log.e(TAG, "Sing a song error: " + e.getMessage());
                                                    // Decide: Should Vosk restart even if speech failed? Usually yes.
                                                    voskModel.recognizeMicrophone();
                                                });
                                            }
                                        });
                                    }

                                    @Override
                                    public void onSpeechError(Exception e) {
                                        runOnUiThread(() -> {
                                            Log.e(TAG, "ElevenLabs Speech Error: " + e.getMessage());
                                            voskModel.recognizeMicrophone();
                                        });
                                    }
                                });

                            } else {
                                Log.d(TAG, "Attempting to speak.");

                                elevenlabsModel.speak(MainActivity.this, ollamaResponse, new ElevenlabsTTS.SpeechCompletionListener() {
                                    @Override
                                    public void onSpeechFinished() {
                                        runOnUiThread(() -> {
                                            Log.d(TAG, "ElevenLabs speech finished. Resuming Vosk listening.");
                                            voskModel.recognizeMicrophone(); // Resume listening here
                                        });
                                    }

                                    @Override
                                    public void onSpeechError(Exception e) {
                                        runOnUiThread(() -> {
                                            Log.e(TAG, "ElevenLabs Speech Error: " + e.getMessage());
                                            // Decide: Should Vosk restart even if speech failed? Usually yes.
                                            voskModel.recognizeMicrophone();
                                        });
                                    }
                                });
                            }
                        });
                    }

                    @Override
                    public void onError(Exception e) {
                        runOnUiThread(() -> {
                            Log.e(TAG, "Ollama Error: " + e.getMessage(), e);
                            // Toast.makeText(MainActivity.this, "Ollama Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                            // If Ollama has an error, immediately restart Vosk so the user can try again.
                            voskModel.recognizeMicrophone();
                        });
                    }
                });
            } else {
                Log.e(TAG, "Input speech is invalid or 'huh'. Vosk should ideally remain active or be restarted if paused by default.");
                // If the input is invalid or "huh", and Vosk was perhaps paused by onPause(),
                // you might want to ensure it's listening again for the next user input.
                // If onVoskFinalResult is only called for valid results, this branch might not need a Vosk restart.
                // If it could be called even for "silent" or "invalid" results that don't go to Ollama,
                // then you might want: voskModel.recognizeMicrophone(); here too.
            }
        });
    }

    // Show partial result
    @Override
    public void onVoskPartialResult(String partialResult) {
        runOnUiThread(() -> {
            // Log.d(TAG, "Vosk Partial Result: " + partialResult);
            // Toast.makeText(this, "Vosk Partial Result: " + partialResult, Toast.LENGTH_LONG).show();
        });
    }

    // Show error
    @Override
    public void onVoskError(Exception e) {
        runOnUiThread(() -> {
            Log.e(TAG, "Vosk Error in MainActivity: " + e.getMessage(), e);
            // Toast.makeText(this, "Vosk Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        });
    }

    // Show that Vosk is ready
    @Override
    public void onVoskReady() {
        runOnUiThread(() -> {
            Log.d(TAG, "Vosk Model is ready.");
            // Toast.makeText(this, "Vosk STT Model Ready!", Toast.LENGTH_SHORT).show();
        });
    }

    // -------------------------------------------------------------------------------------------------------------------------
}