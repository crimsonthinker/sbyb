package com.sbyb;

import android.app.Activity;
import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class SplashScreen extends AppCompatActivity {
    private final int SPLASH_DISPLAY_LENGTH = 2000; //2 seconds
    private final int CAMERA_CODE = 1000;
    private final int EDITOR_CODE = 2000;

    // Open up camera when the app started
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        //SHOW SPLASH SCREEN
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.splash_screen);
//
//        //WAIT THEN MOVE ON TO NEW ACTIVITY
//        Handler waitHandler=  new Handler();
//        Runnable cameraActivity = new Runnable() {
//            public void run() {
//                Intent cameraIntent = new Intent(SplashScreen.this, CameraActivity.class);
//                startActivityForResult(cameraIntent,CAMERA_CODE);
//            };
//        };
//        waitHandler.postDelayed(cameraActivity,SPLASH_DISPLAY_LENGTH);
//    }

    // Open up photo editor when the app started
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //SHOW SPLASH SCREEN
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        //WAIT THEN MOVE ON TO NEW ACTIVITY
        Handler waitHandler=  new Handler();
        Runnable cameraActivity = new Runnable() {
            public void run() {
                Intent editorIntent = new Intent(SplashScreen.this, PhotoEditorActivity.class);
                startActivityForResult(editorIntent,EDITOR_CODE);
            };
        };
        waitHandler.postDelayed(cameraActivity,SPLASH_DISPLAY_LENGTH);
    }

    //Immersive mode: Use if you want to hide default buttons
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            View decorView = getWindow().getDecorView();
            decorView.setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        if (requestCode == CAMERA_CODE){
            if (resultCode == AppCompatActivity.RESULT_CANCELED){
                finish();
            }
            if (resultCode == AppCompatActivity.RESULT_OK){

            }
        }
    }
}
