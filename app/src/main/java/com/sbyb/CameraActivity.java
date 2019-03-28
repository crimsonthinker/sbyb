package com.sbyb;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import java.io.IOException;

import static android.content.ContentValues.TAG;

public class CameraActivity extends AppCompatActivity {

    //PARAMETERS
    /**********************************************************************************************/
    private static final int PERMISSION_REQUEST_CODE = 200;
    private Camera mCamera;
    private CameraPreview mPreview;
    /**********************************************************************************************/

    //SUPPORTING FUNCTIONS
    /**********************************************************************************************/
    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            return false;
        }
        return true;
    }
    private void requestAllPermission() {
        ActivityCompat.requestPermissions(this,new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        }, PERMISSION_REQUEST_CODE);
    }
    public static Camera getCamera(Context context){
        Camera camera = null;
        try {
            camera = Camera.open(); // attempt to get a Camera instance
        }
        catch (Exception e){
            //TODO: Implement handler for camera being unavailable (Showing notification...)
        }
        return camera; // returns null if camera is unavailable
    }
    /**********************************************************************************************/

    //CALLBACKS
    /**********************************************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);
    }

    @Override
    protected void onStart() {
        super.onStart();
        //1. Request permission
        if(!checkCameraPermission()) {
            requestAllPermission();
        }
        //2. Setting up camera
        mCamera = getCamera(this); //get camera
        //3.setting up preview
        mPreview = new CameraPreview(CameraActivity.this,mCamera);
        ConstraintLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }
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
    /**********************************************************************************************/
}

class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {

    //PARAMETERS
    /**********************************************************************************************/
    private SurfaceHolder mHolder; //holder for camera
    private Camera mCamera;
    /**********************************************************************************************/

    //CONSTRUCTOR
    /**********************************************************************************************/
    public CameraPreview(Context context, Camera camera) {
        super(context);

        mCamera = camera;
        // Install a SurfaceHolder.Callback so we get notified when the
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
    }
    /**********************************************************************************************/

    //CALLBACKS
    /**********************************************************************************************/
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //Rotation bug -> not completely fixed
        mCamera.setDisplayOrientation(90);
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            mCamera.setPreviewDisplay(holder);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.d(TAG, "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int w, int h) {


        // Make sure to stop the preview before resizing or reformatting it.
        if (mHolder.getSurface() == null){
            // preview surface does not exist
            return;
        }
        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e){
            // ignore: tried to stop a non-existent preview
        }
        // set preview size and make any resize, rotate or
        // reformatting changes here
        // start preview with new settings
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e){
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e("TABACT", "surfaceDestroyed()");
        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera = null;
    }

    /**********************************************************************************************/
}
