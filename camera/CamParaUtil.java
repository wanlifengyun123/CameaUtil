package com.yajun.dex.camera;

import android.content.Context;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.Log;
import android.view.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CamParaUtil {

    private static final String TAG = CamParaUtil.class.getSimpleName();

    private Context context;
    private Camera mCamera;
    private boolean isPreview;
    private float oldDist = 1f;

    static CamParaUtil myCamPara = null;
    private CamParaUtil(){

    }
    public static CamParaUtil getInstance(){
        if(myCamPara == null){
            myCamPara = new CamParaUtil();
        }
        return myCamPara;
    }

    public synchronized void initCamera(Context context){
        this.context = context;
    }

    public synchronized void openDriver(SurfaceHolder holder)
            throws IOException {
        if (mCamera == null) {
            // 获取手机背面的摄像头
            mCamera = openCamera();
            if (mCamera == null) {
                throw new IOException();
            }
        }
        int degrees = getOrientation(context);
        setDefaultParameters(mCamera,degrees);
        isPreview = true;
//        mCamera.autoFocus(null);
        // 设置摄像头预览view
        mCamera.setDisplayOrientation(degrees);
        mCamera.setPreviewDisplay(holder);
        mCamera.startPreview();

    }

    public synchronized boolean isOpen() {
        return mCamera != null && isPreview;
    }

    /**
     * 检测手机上摄像头的个数，如果有两个摄像头，则取背面的摄像头
     * @return
     */
    public static Camera openCamera() {
        int numCameras = Camera.getNumberOfCameras();
        if (numCameras == 0) {
            Log.w(TAG, "No cameras!");
            return null;
        }
        int index = 0;
        while (index < numCameras) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(index, cameraInfo);
            // CAMERA_FACING_BACK：手机背面的摄像头
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                break;
            }
            index++;
        }

        Camera camera;
        if (index < numCameras) {
            Log.i(TAG, "Opening camera #" + index);
            camera = Camera.open(index);
        } else {
            Log.i(TAG, "No camera facing back; returning camera #0");
            camera = Camera.open(0);
        }
        return camera;
    }

    public synchronized void closeCamera() {
        if (mCamera != null) {
            if(isPreview){
                mCamera.stopPreview();
            }
            isPreview = false;
            mCamera.release();
            mCamera = null;
        }
    }

    /**
     * 拍照
     * Shutter是快门按下时的回调，
     * raw是获取拍照原始数据的回调，
     * jpeg是获取经过压缩成jpg格式的图像数据
     *  Camera.PictureCallback 该回调接口包含了一个onPictureTaken(byte[]data, Camera camera)方法。
     *                      在这个方法中可以保存图像数据。
     */
    public synchronized void takeCamera(){
        mCamera.takePicture(null, null, new Camera.PictureCallback() {
            @Override
            public void onPictureTaken(byte[] data, Camera camera) {
                // 保存图片到本地
            }
        });
    }

    /**
     * 获取手机方向角度
     * @param context
     * @return
     */
    private static int getOrientation(Context context) {
        Display display = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
        int rotation = display.getRotation();
        int orientation;
        boolean expectPortrait;
        switch (rotation) {
            case Surface.ROTATION_0:
            default:
                orientation = 90;
                expectPortrait = true;
                break;
            case Surface.ROTATION_90:
                orientation = 0;
                expectPortrait = false;
                break;
            case Surface.ROTATION_180:
                orientation = 270;
                expectPortrait = true;
                break;
            case Surface.ROTATION_270:
                orientation = 180;
                expectPortrait = false;
                break;
        }
        boolean isPortrait = display.getHeight() > display.getWidth();
        if (isPortrait != expectPortrait) {
            orientation = (orientation + 270) % 360;
        }
        return orientation;
    }

    /**
     * 设置相机默认预览，照片尺寸大小
     * @param mCamera
     * @param degrees
     */
    private void setDefaultParameters(Camera mCamera,int degrees){
        Camera.Parameters parameters = mCamera.getParameters();
        if (parameters.getSupportedFocusModes().contains(
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
        }
        Camera.Size previewSize = getLargePreviewSize(mCamera);
        parameters.setPreviewSize(previewSize.width, previewSize.height);
        Camera.Size pictureSize = getLargePictureSize(mCamera);
        parameters.setPictureSize(pictureSize.width, pictureSize.height);
        parameters.setRotation(degrees);
        parameters.setPictureFormat(PixelFormat.JPEG);//设置照片输出的格式
        mCamera.setParameters(parameters);
    }

    private Camera.Size getLargePictureSize(Camera camera){
        if(camera != null){
            List<Camera.Size> sizes = camera.getParameters().getSupportedPictureSizes();
            Camera.Size temp = sizes.get(0);
            for(int i = 1;i < sizes.size();i ++){
                float scale = (float)(sizes.get(i).height) / sizes.get(i).width;
                if(temp.width < sizes.get(i).width && scale < 0.6f && scale > 0.5f)
                    temp = sizes.get(i);
            }
            return temp;
        }
        return null;
    }

    private Camera.Size getLargePreviewSize(Camera camera){
        if(camera != null){
            List<Camera.Size> sizes = camera.getParameters().getSupportedPreviewSizes();
            Camera.Size temp = sizes.get(0);
            for(int i = 1;i < sizes.size();i ++){
                if(temp.width < sizes.get(i).width)
                    temp = sizes.get(i);
            }
            return temp;
        }
        return null;
    }

    /**
     * 设置缩放
     * @param isZoomIn true放大;false放小;
     * @param camera
     */
    private void handleZoom(boolean isZoomIn, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        if (params.isZoomSupported()) {
            int maxZoom = params.getMaxZoom();
            int zoom = params.getZoom();
            if (isZoomIn && zoom < maxZoom) {
                zoom++;
            } else if (zoom > 0) {
                zoom--;
            }
            params.setZoom(zoom);
            camera.setParameters(params);
        } else {
            Log.i(TAG, "zoom not supported");
        }
    }

    /**
     * 手指间距
     * @param event
     * @return
     */
    private float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        return (float) Math.sqrt(x * x + y * y);
    }

    /**
     * 设置对焦区域
     * @param event
     * @param camera
     */
    private void handleFocus(MotionEvent event, Camera camera) {
        Camera.Parameters params = camera.getParameters();
        Camera.Size previewSize = params.getPreviewSize();
        Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f, previewSize);

        camera.cancelAutoFocus();
        //判断相机是否支持设定手动测光点
        if (params.getMaxNumFocusAreas() > 0) {
            List<Camera.Area> focusAreas = new ArrayList<>();
            focusAreas.add(new Camera.Area(focusRect, 800));
            params.setFocusAreas(focusAreas);
        } else {
            Log.i(TAG, "focus areas not supported");
        }
//        final String currentFocusMode = params.getFocusMode();
//        params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
//        camera.setParameters(params);

        camera.autoFocus(new Camera.AutoFocusCallback() {
            @Override
            public void onAutoFocus(boolean success, Camera camera) {
                if(success){
                    Camera.Parameters params = camera.getParameters();
                    params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
                    camera.setParameters(params);
                    camera.autoFocus(null);
                }
            }
        });
    }

    /**
     * 获取焦点 Rect
     * @param x
     * @param y
     * @param coefficient
     * @param previewSize
     * @return
     */
    private Rect calculateTapArea(float x, float y, float coefficient, Camera.Size previewSize) {
        float focusAreaSize = 300;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) (x / previewSize.width - 1000);
        int centerY = (int) (y / previewSize.height - 1000);

        int left = clamp(centerX - areaSize / 2, -1000, 1000);
        int top = clamp(centerY - areaSize / 2, -1000, 1000);

        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);

        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

    /**
     * 捕获触摸事件
     * @param event
     * @return
     */
    public boolean onTouchEvent(MotionEvent event) {
        if (event.getPointerCount() == 1) {
            handleFocus(event, mCamera);
        }else {
            switch (event.getAction() & MotionEvent.ACTION_MASK) {
                case MotionEvent.ACTION_POINTER_DOWN:
                    oldDist = getFingerSpacing(event);
                    break;
                case MotionEvent.ACTION_MOVE:
                    float newDist = getFingerSpacing(event);
                    if (newDist > oldDist) {
                        handleZoom(true, mCamera);
                    } else if (newDist < oldDist) {
                        handleZoom(false, mCamera);
                    }
                    oldDist = newDist;
                    break;
            }

        }
        return true;
    }
}

