package com.sbyb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.objdetect.CascadeClassifier;

import static android.content.ContentValues.TAG;
import static android.content.Context.WINDOW_SERVICE;

public class CameraActivity extends AppCompatActivity implements OnClickListener, OnCheckedChangeListener {

    //PARAMETERS
    /**********************************************************************************************/
    private static final boolean PHOTO_MODE = false;
    private static final boolean RECORD_MODE = true;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private Camera mCamera; //get camera
    private CameraPreview mPreview;
    private CascadeClassifier faceDetector;
    private int cWidth;
    private int cHeight;
    private final int MIN_FACE_SCALE = 15;
    private final int MAX_FACE_SCALE = 2;
    public static int camId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    public static PreviewCallback previewFaceDetection;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    try{
                        InputStream is = getResources().openRawResource(R.raw.haarcascade_frontal_face);
                        File cascadeDir = getDir("cascade", Context.MODE_APPEND);
                        File mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();
                        faceDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        // Load the cascade classifier
                    } catch (Exception e) {
                        Log.e("OpenCVActivity", "Error loading cascade", e);
                    }
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };
    //buttons
    ImageButton photoButton;
    ImageButton cameraSwitcher;
    ImageButton recordButton;
    SwitchCompat modeSwitcher;
    //ImageButton switchMode;
    //animation
    Animation blink;
    Animation fadeIn;
    Animation fadeOut;
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
            camera = Camera.open(camId); // attempt to get a Camera instance
        }
        catch (Exception e){
            //TODO: Implement handler for camera being unavailable (Showing notification...)
        }
        return camera; // returns null if camera is unavailable
    }

    public Pair<Integer,Integer> getScreenSize() {
        Display display = this.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        return new Pair<>(size.x, size.y);
    }
    public void switchCamera() {
        if (mCamera != null) {
            try {
                mCamera.stopPreview();
                mCamera.setPreviewCallback(null);
                mCamera.release();
                mCamera = null;
            } catch (Exception e){
                //TODO: Implement handler
            }
            if (camId == Camera.CameraInfo.CAMERA_FACING_BACK)
                camId = Camera.CameraInfo.CAMERA_FACING_FRONT;
            else
                camId = Camera.CameraInfo.CAMERA_FACING_BACK;
            mCamera = getCamera(this);
            mCamera.setPreviewCallback(previewFaceDetection);
            mPreview = new CameraPreview(CameraActivity.this,mCamera);
            ConstraintLayout preview = findViewById(R.id.camera_preview);
            preview.addView(mPreview);
        }
        else{
            //TODO: Implement case when mCamera is null, but it world not probably happend, so it should be logged
        }
    }
    public void animationSwitchMode(boolean val) {
        if (val == PHOTO_MODE) {
            photoButton.setVisibility(View.INVISIBLE);
            recordButton.startAnimation(fadeOut);
            recordButton.setVisibility(View.GONE);
            photoButton.startAnimation(fadeIn);
        }else{
            recordButton.setVisibility(View.INVISIBLE);
            photoButton.startAnimation(fadeOut);
            photoButton.setVisibility(View.GONE);
            recordButton.startAnimation(fadeIn);
        }

    }
    /**********************************************************************************************/

    //CALLBACKS
    /**********************************************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);
        //1. Request permission
        if(!checkCameraPermission()) {
            requestAllPermission();
        }
        //2. Initialize button
        photoButton = findViewById(R.id.photo_button); photoButton.setOnClickListener(this);
        recordButton = findViewById(R.id.record_button); recordButton.setOnClickListener(this);
        cameraSwitcher = findViewById(R.id.camera_switcher); cameraSwitcher.setOnClickListener(this);
        modeSwitcher = findViewById(R.id.mode_switcher); modeSwitcher.setOnCheckedChangeListener(this);
        //3. Check whether there are 2 cameras
        if (Camera.getNumberOfCameras() <= 1){
            cameraSwitcher.setVisibility(View.GONE);
        }
        //4. Default: photo mode
        recordButton.setVisibility(View.GONE);
        //5. Set animation
        blink = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.sbyb_blink);
        fadeIn = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.sbyb_fade_in);
        fadeOut = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.sbyb_fade_out);
    }

    @Override
    protected void onStart() {
        //1. Get screen size
        super.onStart();
        Pair<Integer,Integer> size = this.getScreenSize();
        cWidth = size.first;
        cHeight = size.second;
        //2. Setting up camera
        mCamera = getCamera(this); //get camera
        previewFaceDetection = new PreviewCallback() {
            @Override
            public void onPreviewFrame(byte[] data, Camera camera) {
                int inputHeight = cHeight + cHeight / 2;
                int inputWidth = cWidth;
                //Convert byte to mat
                Mat m = new Mat(inputHeight, inputWidth, CvType.CV_8UC1);
                m.put(0, 0, data);
                MatOfRect faceVectors = new MatOfRect();
                if (faceDetector != null){
                    if (!faceDetector.empty()) {
                        int minFace = inputHeight / MIN_FACE_SCALE;
                        int maxFace = inputHeight / MAX_FACE_SCALE;
                        faceDetector.detectMultiScale(m, faceVectors, 2, 1,0,new Size(minFace, minFace), new Size(maxFace, maxFace));
                    }
                }
                Rect[] faceResult = faceVectors.toArray();
                if (faceResult.length != 0) {
                    //Toast.makeText(CameraActivity.this,faceResult[0].toString(),Toast.LENGTH_SHORT).show();
                    //do something else with this
                }
                else
                    System.out.println("No detection");
            }
        };
        mCamera.setPreviewCallback(previewFaceDetection);
        mPreview = new CameraPreview(CameraActivity.this,mCamera);
        ConstraintLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mPreview);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
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
    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.camera_switcher:// switching camera
                Toast.makeText(CameraActivity.this,"switch!",Toast.LENGTH_SHORT).show();
                view.startAnimation(blink);
                switchCamera();
                break;
            case R.id.photo_button:
                break;
            case R.id.record_button:
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton cButton, boolean val){
        switch (cButton.getId()){
            case R.id.mode_switcher:
                animationSwitchMode(val);
                break;
            default:
                break;
        }
    }
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
        if (mHolder.getSurface() == null) {
            // preview surface does not exist
            return;
        }
        // stop preview before making changes
        try {
            mCamera.stopPreview();
        } catch (Exception e) {
            // ignore: tried to stop a non-existent preview
        }
        WindowManager mWindowManager = (WindowManager) this.getContext().getSystemService(WINDOW_SERVICE);
        Display mDisplay = mWindowManager.getDefaultDisplay();
        int orientation = mDisplay.getRotation();
        int degree = 0;
        switch (orientation) {
            case Surface.ROTATION_0: degree = 90; break;
            case Surface.ROTATION_90: degree = 0; break;
            case Surface.ROTATION_180: degree = 270; break;
            case Surface.ROTATION_270: degree = 180; break;
        }
        mCamera.setDisplayOrientation(degree);
        try {
            mCamera.setPreviewDisplay(mHolder);
            mCamera.startPreview();
        } catch (Exception e) {
            Log.d(TAG, "Error starting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.e(TAG, "surfaceDestroyed()");
        mCamera.stopPreview();
        mCamera.setPreviewCallback(null);
        mCamera.release();
        mCamera = null;
    }
}
/**********************************************************************************************/
