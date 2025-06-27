package com.nanami;

import com.nanami.keys.API_keys;

import android.app.Activity;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import com.nanami.frontend.GLRenderer;
import com.nanami.frontend.LAppDelegate;
import com.nanami.llm_wrapper.Ollama;
import com.nanami.stt.VoskSTT;
import com.nanami.tts.ElevenlabsTTS;

import java.util.Objects;

public class MainActivity extends Activity {
    private GLSurfaceView glSurfaceView;
    private GLRenderer glRenderer;
    private Ollama ollamaModel;
    private VoskSTT voskModel;
    private ElevenlabsTTS elevenlabsModel;

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

        // Launch Live2D renderer, Vosk STT, Elevenlabs TTS and Ollama
        LAppDelegate.getInstance().onStart(this);
        voskModel = VoskSTT.getInstance(this);
        voskModel.onStart(this);
        elevenlabsModel = ElevenlabsTTS.getInstance();
        elevenlabsModel.onStart(this);
        ollamaModel = Ollama.getInstance(this, API_keys.OLLAMA_MODEL_NAME, API_keys.PERSONALITY);
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
        Ollama.getInstance(this, API_keys.OLLAMA_MODEL_NAME, API_keys.PERSONALITY).onDestroy();   // Terminate LLM process
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
}