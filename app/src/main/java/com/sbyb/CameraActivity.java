package com.sbyb;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Point;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.PictureCallback;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Environment;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.app.AlertDialog;
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

public class CameraActivity extends AppCompatActivity implements OnClickListener, OnCheckedChangeListener,PictureCallback {

    //FINAL STATIC VARIABLE
    private static final String APP_NAME = "SBYB";
    private static final boolean PHOTO_MODE = false;
    private static final boolean RECORD_MODE = true;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final int REQUEST_TAKE_PHOTO = 100;
    private static final boolean START_RECORDING = true;
    private static final boolean STOP_RECORDING = false;
    private static final String  MEDIA_TAG = "MediaRecording";
    private static final String GALLERY_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + APP_NAME + File.separator;
    private static PreviewCallback previewFaceDetection;
    private static final int MIN_FACE_SCALE = 15;
    private static final int MAX_FACE_SCALE = 2;
    //Other attributes
    private int cWidth; //size of someth3ing?
    private int cHeight;
    private Camera mCamera; //camera-related attributes
    private CameraPreview mCameraPreview;
    private boolean camMode = PHOTO_MODE;
    private static int camId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private MediaRecorder mMediaRecorder; //recorder-related attributes
    private int mAudioSource = MediaRecorder.AudioSource.CAMCORDER; //other: DEFAULT MIC UNPROCESSED
    private int mAudioEncoder = MediaRecorder.AudioEncoder.HE_AAC; //other: DEFAULT AAC AAC_ELD AMR_NB AMR_WB VORBIS(optional)
    private int mVideoEncoder = MediaRecorder.VideoEncoder.MPEG_4_SP; //other: H263 H264 HEVC VP8
    private boolean recordState = STOP_RECORDING;
    private CascadeClassifier faceDetector;
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) { //opencv-callback
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
    }; //opencv callback
    private ImageButton cameraButton; //image buttons
    private ImageButton cameraSwitcher;
    private SwitchCompat modeSwitcher;
    private Animation blink; //animations
    private Animation fadeIn;
    private Animation fadeOut;
    /**********************************************************************************************/

    //SUPPORTING FUNCTIONS
    /**********************************************************************************************/
    private boolean checkPermissions() {
        return (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.VIBRATE) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED);
    }
    private void requestAllPermission() {
        ActivityCompat.requestPermissions(CameraActivity.this,new String[]{
                Manifest.permission.CAMERA,
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.VIBRATE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
        }, PERMISSION_REQUEST_CODE);
        //second check
    }
    private Camera getCamera(){
        Camera camera = null;
        try {
            camera = Camera.open(camId); // attempt to get a Camera instance
        }
        catch (Exception e) {
            AlertDialog.Builder noCameraBuilder = new AlertDialog.Builder(this);
            noCameraBuilder.setTitle(R.string.app_name);
            noCameraBuilder.setIcon(R.mipmap.lotus);
            noCameraBuilder.setMessage("No camera found! Please check your device and try again.");
            noCameraBuilder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    setResult(AppCompatActivity.RESULT_CANCELED);
                    finish();
                }
            });
            AlertDialog noCameraDialog = noCameraBuilder.create();
            noCameraDialog.show();
        }
        return camera; // returns null if camera is unavailable
    }
    private MediaRecorder getMediaRecorder(){
        MediaRecorder recorder = null;
        try{
            recorder = new MediaRecorder();
        }catch(Exception e){
            //TODO: Do something
        }
        return recorder;
    }
    private Pair<Integer,Integer> getScreenSize() {
        Display display = this.getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getRealSize(size);
        return new Pair<>(size.x, size.y);
    }
    private void switchCamera() {
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
            mCamera = this.getCamera();
            mCamera.setPreviewCallback(previewFaceDetection);
            mCameraPreview = new CameraPreview(CameraActivity.this,mCamera);
            ConstraintLayout preview = findViewById(R.id.camera_preview);
            preview.addView(mCameraPreview);
        }
        else{
            //TODO: Implement case when mCamera is null, but it world not probably happend, so it should be logged
        }
    }
    private void switchMode(boolean val) {
        cameraButton.startAnimation(fadeOut);
        if (val == PHOTO_MODE)
            cameraButton.setImageResource(R.mipmap.photo_button);
        else {
            cameraButton.setImageResource(R.mipmap.record_button);
        }
        cameraButton.startAnimation(fadeIn);
        camMode = val;
    }
    private static String getOutputFileName(boolean mode) {
        // Create a media file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        if (mode == PHOTO_MODE)
            return timeStamp + ".jpg";
        else
            return timeStamp + ".mp4";
    }
    private static Bitmap rotateBitmap(Bitmap source, int cameraOrientation) {
        float angle = 270;
        switch(cameraOrientation){
            case Surface.ROTATION_0:
                angle = 0; break;
            case Surface.ROTATION_90:
                angle = 270; break;
            case Surface.ROTATION_180:
                angle = 180; break;
            case Surface.ROTATION_270:
                angle = 90; break;
        }
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
    private void cameraSetUp(){
        mCamera = this.getCamera(); //get camera
        if (previewFaceDetection != null){
            mCamera.setPreviewCallback(previewFaceDetection);
        }else{
            //TODO: Add exception
        }
        mCameraPreview = new CameraPreview(CameraActivity.this,mCamera);
        ConstraintLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mCameraPreview);
    }
    private void recordSetUp() {
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setAudioSource(mAudioSource);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA); //default
        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); //default
        mMediaRecorder.setAudioEncoder(mAudioEncoder);
        mMediaRecorder.setVideoEncoder(mVideoEncoder);
        mMediaRecorder.setOutputFile(getOutputFileName(RECORD_MODE));//set at preparation
        mMediaRecorder.setPreviewDisplay(mCameraPreview.getHolderInstance().getSurface());
        try {
            mMediaRecorder.prepare();
        }catch (Exception e) {
            //TODO: Do something with the exception
        }
    }
    /**********************************************************************************************/

    //CALLBACKS
    /**********************************************************************************************/
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camera_activity);
        //1. Request permission, double check
        if(!checkPermissions()) {
            requestAllPermission();
        }

        //2. Initialize button
        cameraButton = findViewById(R.id.camera_button); cameraButton.setOnClickListener(this);
        cameraSwitcher = findViewById(R.id.camera_switcher); cameraSwitcher.setOnClickListener(this);
        modeSwitcher = findViewById(R.id.mode_switcher); modeSwitcher.setOnCheckedChangeListener(this);
        //3. Check whether there are 2 cameras
        if (Camera.getNumberOfCameras() <= 1){
            cameraSwitcher.setVisibility(View.GONE);
        }
        //4. Set animation
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
    public void onPause(){
        super.onPause();
        //save.
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

    @Override
    public void onClick(View view) {
        switch(view.getId()){
            case R.id.camera_switcher:// switching camera
                view.startAnimation(blink);
                switchCamera();
                break;
            case R.id.camera_button:
                if(camMode == PHOTO_MODE) {
                    Toast.makeText(CameraActivity.this, "Photo mode", Toast.LENGTH_SHORT).show();
                    mCamera.takePicture(null, null, this);
                    //Begin blinking thread
                }
                break;
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton cButton, boolean val){
        switch (cButton.getId()){
            case R.id.mode_switcher:
                switchMode(val);
            default:
                break;
        }
    }

    @Override
    public void onPictureTaken( byte[] data, Camera camera){
        if (data != null) {
            //Check directory existence
            File fileDir = new File(GALLERY_DIR);
            if(!fileDir.exists())
                fileDir.mkdirs();
            //Save to SBYB folder
            Bitmap mBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
            Bitmap mRotatedBitmap = rotateBitmap(mBitmap,mCameraPreview.getCameraOrientation());
            String fileName = getOutputFileName(PHOTO_MODE);
            String destinationPath = GALLERY_DIR + fileName;
            FileOutputStream mOutput;
            try {
                mOutput = new FileOutputStream(destinationPath);
                mRotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, mOutput);
            }catch (Exception e){

            }
            Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            File f = new File(destinationPath);
            Uri contentUri = Uri.fromFile(f);
            mediaScanIntent.setData(contentUri);
            this.sendBroadcast(mediaScanIntent);
            mCamera.stopPreview();
            mCamera.startPreview();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults){
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (!Arrays.asList(grantResults).contains(PackageManager.PERMISSION_DENIED)) {
                //5. Set face detection callback
                previewFaceDetection = new PreviewCallback() {
                    @Override
                    public void onPreviewFrame(byte[] data, Camera camera) {
                        int inputHeight = cHeight + cHeight / 2;
                        int inputWidth = cWidth;
                        //Convert byte to mat
                        Mat m = new Mat(inputHeight, inputWidth, CvType.CV_8UC1);
                        m.put(0, 0, data);
                        MatOfRect faceVectors = new MatOfRect();
                        if (faceDetector != null) {
                            if (!faceDetector.empty()) {
                                int minFace = inputHeight / MIN_FACE_SCALE;
                                int maxFace = inputHeight / MAX_FACE_SCALE;
                                faceDetector.detectMultiScale(m, faceVectors, 2, 1, 0, new Size(minFace, minFace), new Size(maxFace, maxFace));
                            }
                        }
                        Rect[] faceResult = faceVectors.toArray();
                        if (faceResult.length != 0) {
                            //Toast.makeText(CameraActivity.this,faceResult[0].toString(),Toast.LENGTH_SHORT).show();
                            //do something else with this
                        } else {
                            //System.out.println("No detection");
                        }
                    }
                };
                //2. Setting up camera
                cameraSetUp();
                //3. Setting up recorder
                mMediaRecorder = this.getMediaRecorder(); //recorder part
                recordSetUp();
            } else {
                //TOOD: Add warning dialog
                Toast.makeText(getApplicationContext(), "permission is not fully granted", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    //PARAMETERS
    /**********************************************************************************************/
    private SurfaceHolder mHolder; //holder for camera
    private Camera mCamera;
    private int cameraOrientation;
    /**********************************************************************************************/

    //CONSTRUCTOR
    /**********************************************************************************************/
    public CameraPreview(Context context, Camera camera) {
        super(context);
        mCamera = camera;
        // Install a SurfaceHolder.Callback so we get notified when th
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        cameraOrientation = Surface.ROTATION_90;
    }
    /**********************************************************************************************/

    //FUNCTION
    public int getCameraOrientation(){
        return cameraOrientation;
    }

    public SurfaceHolder getHolderInstance(){ return mHolder;}

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
            case Surface.ROTATION_0: degree = 90;
                cameraOrientation = Surface.ROTATION_90;
                break;
            case Surface.ROTATION_90: degree = 0;
                cameraOrientation = Surface.ROTATION_0;
                break;
            case Surface.ROTATION_180: degree = 270;
                cameraOrientation = Surface.ROTATION_270;
                break;
            case Surface.ROTATION_270: degree = 180;
                cameraOrientation = Surface.ROTATION_180;
                break;
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
