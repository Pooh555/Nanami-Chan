package com.nanami;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.nanami.frontend.GLRenderer;
import com.nanami.frontend.LAppDelegate;
import com.nanami.llm_wrapper.Ollama;
import com.nanami.stt.VoskSTT;
import com.nanami.tts.ElevenlabsTTS;

import java.util.Objects;

public class MainActivity extends Activity {
    private static final String TAG = "MainActivity";
    private GLSurfaceView glSurfaceView;
    private GLRenderer glRenderer;
    private Ollama ollamaModel;
    private final String modelName = "llama3:latest";
    private final String personality = "Nanami Osaka";
    private VoskSTT voskModel;
    private ElevenlabsTTS elevenlabModel;


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
    }

    @Override
    protected void onStart() {
        super.onStart();

        try {
            ollamaModel = new Ollama(this, modelName, personality);
        } catch (Exception e) {
            Log.d(TAG, "Failed to initialize Ollama.");
        }

        LAppDelegate.getInstance().onStart(this);   // Launch Live2D render
        this.initializeAndStartVosk();  // Launch Vosk STT
        this.initializeAndStartElevenlab(); // Launch Elevenlab TTS
        ollamaModel.onStart(this);
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

        if (voskModel != null) {
            voskModel.onDestroy();
            voskModel = null;
        }
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

        // Terminate STT process
        if (voskModel != null) {
            voskModel.onDestroy();
            voskModel = null;
        }

        // Terminate TTS process
        if (elevenlabModel != null) {
            elevenlabModel.onDestroy();
            elevenlabModel = null;
        }

        // Terminate LLM process
        if (ollamaModel != null) {
            ollamaModel.onDestroy();
            ollamaModel = null;
        }
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

    private void initializeAndStartVosk() {
        if (voskModel == null) {
            // Initialize Vosk model
            voskModel = new VoskSTT(this, new VoskSTT.ModelCallback() {

                @Override
                public void onModelReady() {
                    runOnUiThread(() -> {
                        if (voskModel != null) {
                            voskModel.recognizeMicrophone();    // Begin listening
                            // Toast.makeText(MainActivity.this, "Vosk STT started listening!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

                @Override
                public void onModelFailed(String errorMessage) {
                    runOnUiThread(() -> {
                        Toast.makeText(MainActivity.this, "Failed to load Vosk model: " + errorMessage,
                                Toast.LENGTH_LONG).show();
                        Log.e(TAG, "Vosk model failed: " + errorMessage);
                    });
                }
            });
        } else {
            voskModel.recognizeMicrophone();
        }
    }

    // Request microphone permission
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == VoskSTT.getPermissionsRequestRecordAudio()) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeAndStartVosk();
            } else {
                Toast.makeText(this, "Audio recording permission denied. Speech recognition will not work.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    private void initializeAndStartElevenlab() {
        if (elevenlabModel == null) {
            elevenlabModel = new ElevenlabsTTS(this);
            elevenlabModel.speak("Hello! my name is Nanami Osaka.");
        } else {
            elevenlabModel.speak("Hello! my name is Nanami Osaka.");
        }
    }
}