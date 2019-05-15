package com.sbyb;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.PictureCallback;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.constraint.ConstraintLayout;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.MotionEvent;
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
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.w3c.dom.Text;

import static android.content.ContentValues.TAG;
import static android.content.Context.WINDOW_SERVICE;

public class CameraActivity extends AppCompatActivity implements OnClickListener, OnCheckedChangeListener,PictureCallback {

    //FINAL STATIC VARIABLE
    private static final boolean ON = true;
    private static final boolean OFF = false;
    private static final String APP_NAME = "SBYB";
    private static final boolean PHOTO_MODE = false;
    private static final boolean RECORD_MODE = true;
    private static final int PERMISSION_REQUEST_CODE = 200;
    private static final boolean START_RECORDING = true;
    private static final boolean STOP_RECORDING = false;
    private float mRelativeFaceSize = 0.2f;
    private static final String  MEDIA_TAG = "MediaRecording";
    private static final String VIDEO_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES) + File.separator + APP_NAME + File.separator;
    private static final String GALLERY_DIR = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES) + File.separator + APP_NAME + File.separator;
    private static PreviewCallback previewFaceDetection;
    private static final int MIN_FACE_SCALE = 15;
    private static final int MAX_FACE_SCALE = 1;
    //Other attributes
    private int cWidth; //size of someth3ing?
    private int cHeight;
    private Camera mCamera; //camera-related attributes
    private CameraPreview mPreview;
    private boolean camMode = PHOTO_MODE;
    private static int camId = Camera.CameraInfo.CAMERA_FACING_FRONT;
    private MediaRecorder mMediaRecorder; //recorder-related attributes
    private int mAudioSource = MediaRecorder.AudioSource.CAMCORDER; //other: DEFAULT MIC UNPROCESSED
    private boolean recordMode = STOP_RECORDING;
    private String currentVideoFileName = "";
    private String currentVideoFilePath = "";
    private CascadeClassifier faceDetector;
    private boolean flashLightState = OFF;
    private boolean isFlash = false;
    private List<Camera.Size> previewSizeList;
    private List<Camera.Size> pictureSizeList;
    private Camera.Size currentPreviewSize;
    private Camera.Size currentPictureSize;
    private SurfaceView transparentView;
    private SurfaceHolder mHolderTransparent;
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
    private ImageButton galleryPreview;
    private ImageButton flashLight;
    private ImageButton setting;
    private Switch modeSwitcher;
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
    }
    private Camera getCamera(){
        Camera camera = null;
        try {
            camera = Camera.open(camId); // attempt to get a Camera instance
        }
        catch (Exception e) {
            AlertDialog.Builder noCameraBuilder = new AlertDialog.Builder(this);
            noCameraBuilder.setTitle(R.string.app_name);
            noCameraBuilder.setIcon(R.mipmap.ic_launcher);
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
        if(mCamera == null){

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
        Canvas canvas = mHolderTransparent.lockCanvas(null);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
        mHolderTransparent.unlockCanvasAndPost(canvas);
        if (camId == Camera.CameraInfo.CAMERA_FACING_BACK) { ;
            flashLight.setVisibility(View.GONE);
            camId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        else {
            flashLight.setVisibility(View.VISIBLE);
            camId = Camera.CameraInfo.CAMERA_FACING_BACK;
        }
        if(flashLightState == ON) {
            flashLightState = OFF;
            flashLight.setImageResource(R.mipmap.flashlight_off);
        }
        releaseMediaRecorder();
        releaseCamera();
        cameraSetUp();
    }
    private void switchMode(boolean val) {
        cameraButton.startAnimation(fadeOut);
        releaseMediaRecorder();
        if (val == PHOTO_MODE){
            cameraButton.setImageResource(R.drawable.photo_button_blink);
            modeSwitcher.setThumbResource(R.drawable.camera_mode);
        }else {
            cameraButton.setImageResource(R.drawable.record_button);
            modeSwitcher.setThumbResource(R.drawable.record_mode);
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
            return timeStamp + ".3gp";
    }
    private Bitmap rotateBitmap(Bitmap source) {
        float angle = getPhotoOrientation();
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        return Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(), matrix, true);
    }
    private void cameraSetUp(){
        mCamera = getCamera(); //get camera
        if (mCamera != null){
            mCamera.setPreviewCallback(previewFaceDetection);
        }else{
            Toast.makeText(getApplicationContext(),"Camera is null",Toast.LENGTH_SHORT).show();
            mCamera.release();
            mCamera = null;
        }
        Camera.Parameters mParams = mCamera.getParameters();
        pictureSizeList = mParams.getSupportedPictureSizes();
        previewSizeList = mParams.getSupportedPreviewSizes();
        mParams.setPreviewSize(previewSizeList.get(0).width,previewSizeList.get(0).height);
        mCamera.setParameters(mParams);

        mPreview = new CameraPreview(CameraActivity.this,mCamera);
        ConstraintLayout preview = findViewById(R.id.camera_preview);
        preview.addView(mPreview);

        transparentView = findViewById(R.id.transparent_view);
        transparentView.setZOrderOnTop(true);
        mHolderTransparent = transparentView.getHolder();
        mHolderTransparent.setFormat(PixelFormat.TRANSPARENT);

        Camera.Parameters cameraParameters = mCamera.getParameters();
        cWidth = cameraParameters.getPreviewSize().width;
        cHeight = cameraParameters.getPreviewSize().height;
    }
    private void recordSetUp() {
        try {
            mCamera.setPreviewDisplay(mPreview.getHolder());
        }catch(Exception e){
            Toast.makeText(getApplicationContext(),"Camera preview not set",Toast.LENGTH_SHORT).show();
        }
        mCamera.startPreview();
        mCamera.unlock();
        mMediaRecorder = getMediaRecorder();
        mMediaRecorder.setCamera(mCamera);
        mMediaRecorder.setPreviewDisplay(mPreview.getHolder().getSurface());
        mMediaRecorder.setAudioSource(mAudioSource);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA); //default
        mMediaRecorder.setProfile(CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH));
        currentVideoFileName = getOutputFileName(RECORD_MODE);
        currentVideoFilePath = GALLERY_DIR + currentVideoFileName;
        mMediaRecorder.setOutputFile(currentVideoFilePath);//set at preparation
        mMediaRecorder.setOrientationHint(getRecordOrientation());
        try {
            mMediaRecorder.prepare();
        } catch (Exception e) {
            releaseMediaRecorder();
            Toast.makeText(getApplicationContext(),"Media recorder not started",Toast.LENGTH_SHORT).show();
        }
        Toast.makeText(getApplicationContext(),"OK",Toast.LENGTH_SHORT).show();

    }
    private void releaseCamera(){
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
    }
    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            if (mCamera != null)
                mCamera.lock();           // lock camera for later use
        }
    }

    private void saveVideo() {
        ContentValues values = new ContentValues(3);
        File videoFile = new File(currentVideoFilePath);
        if(videoFile.exists()){
            Toast.makeText(getApplicationContext(),"Exist",Toast.LENGTH_SHORT).show();
        }else{

        }
        values.put(MediaStore.Video.Media.TITLE,currentVideoFileName);
        values.put(MediaStore.Video.Media.MIME_TYPE,"video/3gp");
        values.put(MediaStore.Video.Media.DATA,videoFile.getAbsolutePath());
        Uri savedVideo = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,values);
        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        mediaScanIntent.setData(savedVideo);
        this.sendBroadcast(mediaScanIntent);
    }

    int getRecordOrientation() {
        int camDegree = mPreview.getCameraDegree();
        if(camDegree == 90){
            if(camId == Camera.CameraInfo.CAMERA_FACING_FRONT)
                return 270;
            else
                return 90; //no need to flip
        }
        if(camDegree == 0)
            return 0;
        if(camDegree == 180)
            return 180;
        return 0;
    }
    float getPhotoOrientation() {
        int camDegree = mPreview.getCameraDegree();
        if(camDegree == 90) {
            if(camId == Camera.CameraInfo.CAMERA_FACING_FRONT)
                return 270.0f; //ok
            else
                return 90.0f; //ok
        }
        if(camDegree == 180) {
            if (camId == Camera.CameraInfo.CAMERA_FACING_FRONT)
                return 180.0f; //ok
            else
                return 180.0f; //ok
        }
        if(camDegree == 0) {
            if (camId == Camera.CameraInfo.CAMERA_FACING_FRONT)
                return 0f; //ok
            else
                return 0f; //ok
        }
        return 0f;
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
        galleryPreview = findViewById(R.id.gallery_preview); galleryPreview.setOnClickListener(this);
        flashLight = findViewById(R.id.flash_light); flashLight.setOnClickListener(this);
        setting = findViewById(R.id.setting); setting.setOnClickListener(this);
        //3. Check whether there are 2 cameras
        if (Camera.getNumberOfCameras() <= 1){
            cameraSwitcher.setVisibility(View.GONE);
        }
        //4. Set animation
        blink = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.sbyb_blink);
        fadeIn = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.sbyb_fade_in);
        fadeOut = AnimationUtils.loadAnimation(getApplicationContext(),R.anim.sbyb_fade_out);
        //5.
        previewFaceDetection = new PreviewCallback() {
            @Override
            public void onPreviewFrame(final byte[] data, Camera camera) {
                Canvas canvas = mHolderTransparent.lockCanvas(null);
                canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);
                //FOR FACE DETECTION
                Camera.Parameters cameraParameters = camera.getParameters();
                YuvImage yuvimage=new YuvImage(data,cameraParameters.getPreviewFormat(),cWidth,cHeight,null);
                ByteArrayOutputStream outstr = new ByteArrayOutputStream();
                android.graphics.Rect rect = new android.graphics.Rect(0, 0, cWidth, cHeight);
                yuvimage.compressToJpeg(rect, 100, outstr);
                byte[] bytes = outstr.toByteArray();
                Bitmap bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                Bitmap bitmap = bmp.copy(Bitmap.Config.ARGB_8888,true);
                Bitmap rBitmap = rotateBitmap(bitmap);
                Mat m = new Mat();
                Utils.bitmapToMat(rBitmap,m);
                Mat finalMat = new Mat(m.height(),m.width(),CvType.CV_8UC1);
                Imgproc.cvtColor(m, finalMat, Imgproc.COLOR_RGB2GRAY);
                MatOfRect faceVectors = new MatOfRect();
                if (faceDetector != null) {
                    if (!faceDetector.empty()) {
                        int height = m.rows();
                        int mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
                        faceDetector.detectMultiScale(finalMat, faceVectors, 1.1, 4, 2, new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
                    }
                }
                Rect[] faceResult = faceVectors.toArray();
                if (faceResult.length != 0) {
                    android.graphics.Rect face;
                    Paint  paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                    paint.setStyle(Paint.Style.STROKE);
                    paint.setColor(Color.GREEN);
                    paint.setStrokeWidth(3);
                    for(int i = 0 ; i < faceResult.length; i++){
                        face = new android.graphics.Rect(faceResult[i].x,faceResult[i].y,faceResult[i].x + faceResult[i].width,faceResult[i].y + faceResult[i].height);
                        canvas.drawRect(face,paint);
                        System.out.println(faceResult[i].toString());
                    }
                    //do something else with this
                } else {
                    //System.out.println("No detection");
                }
//                //FOR RECORDING MODE
//                if(recordMode == START_RECORDING) {
//                    cameraButton.setImageResource(R.drawable.record_button);
//                }
                mHolderTransparent.unlockCanvasAndPost(canvas);
            }
        };
        isFlash = getApplicationContext().getPackageManager()
                .hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
        if(!isFlash || camId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            flashLight.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    protected void onStart() {
        //1. Get screen size
        super.onStart();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        cameraSetUp();
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
                AnimationParam fadeOutAnimation = new AnimationParam(getApplicationContext(),view,Arrays.asList(blink));
                new AsyncAnimation().execute(fadeOutAnimation);
                switchCamera();
                break;
            case R.id.camera_button:
                if(camMode == PHOTO_MODE) {
                    Toast.makeText(getApplicationContext() , String.valueOf(mPreview.getCameraDegree()), Toast.LENGTH_SHORT).show();
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
                    mCamera.takePicture(null, null, this);
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                    //Begin blinking thread
                }else{
                    if(recordMode == STOP_RECORDING) {
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_NOSENSOR);
                        Toast.makeText(getApplicationContext(), "Record mode", Toast.LENGTH_SHORT).show();
                        recordSetUp();
                        mMediaRecorder.start();
                        Toast.makeText(getApplicationContext(),String.valueOf(mPreview.getCameraDegree()),Toast.LENGTH_SHORT).show();
                        recordMode = START_RECORDING;
                        //TODO: start animation thread here
                    }else{
                        Toast.makeText(getApplicationContext(), "Stop recording", Toast.LENGTH_SHORT).show();
                        Boolean isSavable = true;
//                        cameraButton.setImageResource(R.drawable.record_button);
                        try {
                            mMediaRecorder.stop();
                        }catch(Exception e){
                            isSavable = false;
                        }
                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
                        if(isSavable)
                            saveVideo();
                        recordMode = STOP_RECORDING;
                    }
                }
                break;
            case R.id.flash_light:
                flashLight.startAnimation(fadeOut);
                Camera.Parameters parameters = mCamera.getParameters();
                if(flashLightState == OFF){
                    if(camId == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                        mCamera.setParameters(parameters);
                    }
//                    flashLight.setImageResource(R.drawable.flashlight_off);
                    flashLightState = ON;
                }else{
                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
                    mCamera.setParameters(parameters);
//                    flashLight.setImageResource(R.drawable.flashlight_off);
                    flashLightState = OFF;
                }
                flashLight.startAnimation(fadeIn);
                flashLight.setVisibility(View.VISIBLE);
                break;
            case R.id.setting:
                //get
                Camera.Parameters camParams = mCamera.getParameters();
                List<Camera.Size> sizes = camParams.getSupportedPictureSizes();
                List<Camera.Size> camSizes = camParams.getSupportedPreviewSizes();
                for(int i = 0 ; i < sizes.size(); i++){
                    Toast.makeText(getApplicationContext(),"Picture size: " + sizes.get(i).width + " " + sizes.get(i).height,Toast.LENGTH_SHORT).show();
                }
                for(int i = 0 ; i < camSizes.size(); i++){
                    Toast.makeText(getApplicationContext(),"Camera size: " + camSizes.get(i).width + " " + camSizes.get(i).height,Toast.LENGTH_SHORT).show();
                }
                break;
            case R.id.gallery_preview:
                Intent intent = new Intent(CameraActivity.this, Gallery.class);
                startActivity(intent);
                break;
            default:
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
            Bitmap mRotatedBitmap = rotateBitmap(mBitmap);
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
                //2. Setting up camera
                cameraSetUp();
            } else {
                //TODO: Add warning dialog
                Toast.makeText(getApplicationContext(), "permission is not fully granted", Toast.LENGTH_SHORT).show();
            }
        }
    }
}

class CameraPreview extends SurfaceView implements SurfaceHolder.Callback {
    //PARAMETERS
    /**********************************************************************************************/
    private static int FOCUS_AREA_SIZE = 300;
    private SurfaceHolder mHolder; //holder for camera
    private Camera mCamera;
    private Context mContext;
    private int cameraOrientation;
    private int cameraDegree;
    private TextView zoomValue;
    private float mDist = 0;
    /**********************************************************************************************/

    //CONSTRUCTOR
    /**********************************************************************************************/
    public CameraPreview(Context context, Camera camera) {
        super(context);
        mContext = context;
        mCamera = camera;
        // Install a SurfaceHolder.Callback so we get notified when th
        // underlying surface is created and destroyed.
        mHolder = getHolder();
        mHolder.addCallback(this);
        // deprecated setting, but required on Android versions prior to 3.0
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
        cameraOrientation = Surface.ROTATION_90;
        zoomValue = ((AppCompatActivity) mContext).findViewById(R.id.zoom_info);
        zoomValue.setVisibility(View.INVISIBLE);
    }
    /**********************************************************************************************/

    //FUNCTION
    public int getCameraDegree() {
        return cameraDegree;
    }
    private float getFingerSpacing(MotionEvent event) {
        // ...
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float)Math.sqrt(x * x + y * y);
    }
    private int clamp(int touchCoordinateInCameraReper, int focusAreaSize) {
        int result;
        if (Math.abs(touchCoordinateInCameraReper)+focusAreaSize/2>1000){
            if (touchCoordinateInCameraReper>0){
                result = 1000 - focusAreaSize/2;
            } else {
                result = -1000 + focusAreaSize/2;
            }
        } else{
            result = touchCoordinateInCameraReper - focusAreaSize/2;
        }
        return result;
    }
    private android.graphics.Rect calculateFocusArea(float x, float y) {
        int left = clamp(Float.valueOf((x / this.getWidth()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);
        int top = clamp(Float.valueOf((y / this.getHeight()) * 2000 - 1000).intValue(), FOCUS_AREA_SIZE);

        return new android.graphics.Rect(left, top, left + FOCUS_AREA_SIZE, top + FOCUS_AREA_SIZE);
    }
    private void handleZoom(MotionEvent event, Camera.Parameters params) {
        int maxZoom = params.getMaxZoom();
        int zoom = params.getZoom();
        float newDist = getFingerSpacing(event);
        if (newDist > mDist) {
            // zoom in
            if (zoom < maxZoom)
                zoom++;
        } else if (newDist < mDist) {
            // zoom out
            if (zoom > 0)
                zoom--;
        }
        mDist = newDist;
        params.setZoom(zoom);
        mCamera.setParameters(params);
    }
    public void handleFocus(MotionEvent event, Camera.Parameters params) { //touch focus
        if (params.getMaxNumMeteringAreas() > 0){
            android.graphics.Rect rect = calculateFocusArea(event.getX(), event.getY());

            List<Camera.Area> meteringAreas = new ArrayList<>();
            meteringAreas.add(new Camera.Area(rect, 800));
            params.setFocusAreas(meteringAreas);
            try {
                mCamera.setParameters(params);
            }catch(Exception e){

            }
            //mCamera.autoFocus(mAutoFocusTakePictureCallback);
        }else {
            //mCamera.autoFocus(mAutoFocusTakePictureCallback);
        }
    }
    //CALLBACKS
    /**********************************************************************************************/
    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        //Rotation bug -> not completely fixed
        // The Surface has been created, now tell the camera where to draw the preview.
        try {
            cameraDegree = 90;
            mCamera.setDisplayOrientation(90);
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
            case Surface.ROTATION_0: degree = 90; cameraDegree = 90;
                cameraOrientation = Surface.ROTATION_90;
                break;
            case Surface.ROTATION_90: degree = 0; cameraDegree = 0;
                cameraOrientation = Surface.ROTATION_0;
                break;
            case Surface.ROTATION_180: degree = 270; cameraDegree = 270;
                cameraOrientation = Surface.ROTATION_270;
                break;
            case Surface.ROTATION_270: degree = 180; cameraDegree = 180;
                cameraOrientation = Surface.ROTATION_180;
                break;
        }
        try {
            mCamera.setDisplayOrientation(degree);
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

    @Override
    public boolean onTouchEvent(MotionEvent event){
        Camera.Parameters params = mCamera.getParameters();
        int action = event.getAction();
        if(event.getPointerCount() > 1){
            //multi-touch event
            if(action == MotionEvent.ACTION_POINTER_DOWN){
                mDist = getFingerSpacing(event);
            }else if(action == MotionEvent.ACTION_MOVE && params.isZoomSupported()){
                mCamera.cancelAutoFocus();
                handleZoom(event,params);
            }
        }else if(action == MotionEvent.ACTION_UP){
            Toast.makeText(getContext(),"Hello",Toast.LENGTH_SHORT).show();
            handleFocus(event,params);
        }
        return true;
    }
}
/**********************************************************************************************/