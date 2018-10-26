package com.example.mrrobot.recameraapi;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.Toast;

import java.lang.reflect.Array;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    TextureView mTextureView;
    CameraManager mCamera;

    //*********************Size*********************
    Size mSize;

    // ************************** Thread Handler ************************
    HandlerThread mThreadHandler;

    //****************************Handler for looper ************************
    Handler mHandler;

    //******************************CaptureRequestBuilder ******************
    CaptureRequest.Builder builder;

    //***************CameraDevice ***********************************
    CameraDevice mCameraDevice;

    //********************************Camera State callBack *********************
    CameraDevice.StateCallback mStateCallBack = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            mCameraDevice = camera;
            setUpPreview();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    };

    TextureView.SurfaceTextureListener surfaceTextureListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
            setUpCamera(2120,1020);
            connectCamera();
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
            return false;
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {

        }
    };






    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mTextureView = findViewById(R.id.textureView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startBackgroundThread();
        mTextureView.setSurfaceTextureListener(surfaceTextureListener);
    }

    @Override
    protected void onPause() {
        super.onPause();
        closeBackgroundThread();
        closeCamera();
    }



    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        View decorView = getWindow().getDecorView();
        decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            |View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            |View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            |View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            |View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void setUpPreview() {
        SurfaceTexture mPreview = mTextureView.getSurfaceTexture();
        mPreview.setDefaultBufferSize(1200,720);
        Surface surfacePreview = new Surface(mPreview);
        try {
            builder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            builder.addTarget(surfacePreview);
            mCameraDevice.createCaptureSession(Arrays.asList(surfacePreview), new CameraCaptureSession.StateCallback() {
                @Override
                public void onConfigured(@NonNull CameraCaptureSession session) {
                    try {
                        session.setRepeatingRequest(builder.build(),null,mHandler);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                    Toast.makeText(MainActivity.this, "configure failed", Toast.LENGTH_SHORT).show();
                }
            },null);

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }

    }

    private void setUpCamera(int width , int height) {
        CameraManager manager = (CameraManager)getSystemService(CAMERA_SERVICE);
        CameraCharacteristics cameraCharacteristics;
        try {
            cameraCharacteristics = manager.getCameraCharacteristics(String.valueOf(CameraCharacteristics.LENS_FACING_BACK));
            StreamConfigurationMap map = cameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mSize = maximumSize(map.getOutputSizes(Surface.class));
            if (map.getOutputSizes(Surface.class) == null){

            }

        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private Size maximumSize(Size[] mSizeArray){
        try {
            if (mSizeArray == null){
                Log.i("LOG","Array Sizes's is null");
            }else {
                Log.i("LOG","Array Sizes's is not null");
            }
            Size mSampleSize = new Size(1020,720);
            for (int a = 0; a < mSizeArray.length; a++){
                if (mSizeArray[a].getWidth() > mSampleSize.getWidth() && mSizeArray[a].getHeight() > mSampleSize.getHeight()){
                    mSampleSize = mSizeArray[a];
                }
            }
        }catch (NullPointerException e){

        }

        return mSize;
    }

    private void startBackgroundThread(){
        mThreadHandler = new HandlerThread("myHandler");
        mThreadHandler.start();
        mHandler = new Handler(mThreadHandler.getLooper());
    }

    private void connectCamera() {
        mCamera = (CameraManager) getSystemService(CAMERA_SERVICE);
        try{
            if (Build.VERSION.SDK_INT >= 23){
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
                    Toast.makeText(this, "permissions", Toast.LENGTH_SHORT).show();
                    ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.CAMERA},1 );
                }else {
                    mCamera.openCamera(String.valueOf(CameraCharacteristics.LENS_FACING_BACK),mStateCallBack,mHandler);
                }

            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }




    private void closeCamera() {
        if (mCameraDevice != null){
            mCameraDevice = null;
        }
    }

    private void closeBackgroundThread(){
        mThreadHandler.quitSafely();
        try {
            mThreadHandler.join();
            mThreadHandler = null;
            mHandler = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

}
