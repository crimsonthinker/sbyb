package com.sbyb;

import android.content.Intent;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;

public class SplashScreen extends AppCompatActivity {
    private final int splashDisplayLength = 2000; //2 seconds
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        //SHOW SPLASH SCREEN
        super.onCreate(savedInstanceState);
        setContentView(R.layout.splash_screen);

        //WAIT THEN MOVE ON TO NEW ACTIVITY
        Handler waitHandler=  new Handler();
        Runnable cameraActivity = new Runnable() {
            public void run() {
                Intent cameraIntent = new Intent(SplashScreen.this, CameraActivity.class);
                startActivity(cameraIntent);
            };
        };
        waitHandler.postDelayed(cameraActivity,splashDisplayLength);
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
}
