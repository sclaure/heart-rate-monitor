package com.example.heartratemonitor;

import android.content.Context;
import android.hardware.Camera;
import android.util.AttributeSet;

import org.opencv.android.JavaCameraView;

public class HeartBeatCameraView extends JavaCameraView {

    public HeartBeatCameraView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public void turnOnFlashlight() {
        Camera.Parameters param = mCamera.getParameters();
        param.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
        mCamera.setParameters(param);
    }

    public void turnOffFlashlight() {
        Camera.Parameters param = mCamera.getParameters();
        param.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
        mCamera.setParameters(param);
    }
}
