package com.yajun.dex;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.*;
import com.yajun.dex.camera.CamParaUtil;

import java.io.IOException;
import java.util.List;

/**
 * Created by yajun on 2017/4/12.
 *
 */
public class CameraSurfacePreview extends SurfaceView implements SurfaceHolder.Callback {

    private SurfaceHolder mHolder;
    private Camera mCamera;

    private int degrees;
    private boolean mIsPortrait = true;

    public CameraSurfacePreview(Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
        mHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        CamParaUtil.getInstance().initCamera(context);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        // Open the Camera in preview mode
        if(CamParaUtil.getInstance().isOpen()){
            CamParaUtil.getInstance().closeCamera();
        }
        try {
            CamParaUtil.getInstance().openDriver(holder);
        } catch (IOException e) {
            Log.d("Dennis", "Error setting camera preview: " + e.getMessage());
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        Log.d("Dennis", "surfaceChanged() is called");
//        try {
//            mCamera.startPreview();
//        } catch (Exception e){
//            Log.d("Dennis", "Error starting camera preview: " + e.getMessage());
//        }

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        Log.d("Dennis", "surfaceDestroyed() is called");
        CamParaUtil.getInstance().closeCamera();
    }

    /**
     * 拍照
     * Shutter是快门按下时的回调，
     * raw是获取拍照原始数据的回调，
     * jpeg是获取经过压缩成jpg格式的图像数据
     * @param imageCallback 该回调接口包含了一个onPictureTaken(byte[]data, Camera camera)方法。
     *                      在这个方法中可以保存图像数据。
     */
    public void takePicture(Camera.PictureCallback imageCallback) {
        mCamera.takePicture(null, null, imageCallback);
    }

    @Override
    protected void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if(newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE){
            mIsPortrait = false;  // 横屏
        }else {
            mIsPortrait = true;  // 竖屏
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return CamParaUtil.getInstance().onTouchEvent(event);
    }
}
