package com.example.heartratemonitor;

import android.graphics.Color;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;

import java.util.Random;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.Viewport;
import com.jjoe64.graphview.series.LineGraphSeries;
import com.jjoe64.graphview.series.DataPoint;

import android.widget.TextView;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.imgproc.Imgproc;
import org.w3c.dom.Text;


public class MainActivity extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2 {
    static final String TAG = "Heartbeat Sensor";
    private HeartBeatCameraView mOpenCVCameraView;

    private TextView hrDisplay;

    private int hr_num = 0;

    private int previous_value = 0;
    private int current_value = 0;

    private boolean pos_velocity = false;
    private boolean down_swing = false;

    private int peak_count = 0;

    private int prev_second_avg = 0;

    private int[] prev_second = new int[15];

    // are we on the first second's worth of data
    private boolean calibration_second = true;
    private boolean flashlight_off = true;

    private int second_counter = 0;
    private int sub_second_counter = 0;

    private int lastEntry = 0;
    private LineGraphSeries<DataPoint> heartRate;

    private int colorCount = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mOpenCVCameraView = (HeartBeatCameraView) findViewById(R.id.OpenCVCameraView);
        mOpenCVCameraView.setVisibility(SurfaceView.VISIBLE);
        mOpenCVCameraView.setMaxFrameSize(200, 200);
        mOpenCVCameraView.setCvCameraViewListener(this);

        hrDisplay = (TextView)findViewById(R.id.hrDisplay);

        // initialize the graph
        GraphView graph = (GraphView) findViewById(R.id.graph);

        // initialize the data
        heartRate = new LineGraphSeries<DataPoint>();
        heartRate.setColor(Color.BLUE);
        graph.addSeries(heartRate);

        // customize a little bit viewport
        Viewport viewport = graph.getViewport();
        viewport.setYAxisBoundsManual(true);
        viewport.setMaxY(5800000);
        viewport.setMinY(5300000);

        viewport.setScrollable(true);
    }

    @Override
    protected void onResume()
    {
        super.onResume();

        if (!OpenCVLoader.initDebug()) {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_11, this, mLoaderCallback);
        } else {
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        // testing for real time thread
        new Thread(new Runnable() {
            @Override
            public void run() {
                //numSteps.setText("Count: " + stepCounter);

                // we add 100 new entries
                for (int i = 0; i < 10000000; i++) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            addEntry();
                        }
                    });

                    // slow down the addition of entries
                    try {
                        // approximately 70 milliseconds per frame
                        Thread.sleep(70);

                    } catch (InterruptedException e) {
                        // manage error ...
                    }
                }
            }
        }).start();
    }

    // add random data to graph
    private void addEntry() {
        heartRate.appendData(new DataPoint(lastEntry++, colorCount), false, 100);

        previous_value = current_value;
        current_value = colorCount;

        pos_velocity = (current_value - previous_value) > 0;

        if ((current_value - previous_value) < 0){
            // only reset downswing once it has started declining
            down_swing = true;
        }

        if (!calibration_second) {
            if ((current_value > prev_second_avg) && (pos_velocity) && down_swing){
                peak_count += 1;
                down_swing = false;

                System.out.println(peak_count);
            }
        }

        prev_second[sub_second_counter] = current_value;

        // approximating 15 frames per second
        sub_second_counter += 1;
        if (sub_second_counter == 15){
            if (calibration_second){
                calibration_second = false;
            }
            prev_second_avg = 0;

            // 1 second has passed, approximate the heartbeat
            for(int k = 0; k < 15; k++){
                prev_second_avg += prev_second[k];
            }

            prev_second_avg = prev_second_avg / 15;

            sub_second_counter = 0;
            second_counter += 1;

            if (second_counter == 10){
                hr_num = (peak_count * 6);

                System.out.println("********************printing rate*********************************");
                System.out.println(peak_count * 6);
                hrDisplay.setText("Heart Rate: " + hr_num);
                System.out.println("********************printing rate*********************************");
                peak_count = 0;
                second_counter = 0;
            }

            else{
                if (current_value > 6500000){
                    hrDisplay.setText("Heart Rate: " + hr_num + " Place finger in front of camera, have magnitude within range");
                } else {
                    hrDisplay.setText("Heart Rate: " + hr_num + " ( Calibrating: " + (10 - second_counter) + " more seconds )");
                }
            }
        }

    }

    @Override
    public void onPause()
    {
        super.onPause();
//        mOpenCVCameraView.turnOffFlashlight();
        flashlight_off = true;
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (mOpenCVCameraView != null)
            mOpenCVCameraView.disableView();
    }

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        Mat currentFrame = inputFrame.rgba();
        int cannyThreshold=5;

        //CANNY Edge Detection
        //Imgproc.cvtColor(currentFrame, currentFrame, Imgproc.COLOR_RGBA2GRAY);

        //Mat edgeFrame = currentFrame.clone();
        //Imgproc.Canny(currentFrame, edgeFrame, cannyThreshold / 3, cannyThreshold);

        if (flashlight_off) {
            mOpenCVCameraView.turnOnFlashlight();
        }

//        edgeFrame.reshape(0, 1);
//        Mat ones = Mat.ones(edgeFrame.size(), edgeFrame.type());
//        int numOnes = (int)edgeFrame.dot(ones);

        currentFrame.reshape(0, 1);
        Mat ones = Mat.ones(currentFrame.size(), currentFrame.type());
        int numOnes = (int)currentFrame.dot(ones);

        colorCount = numOnes;

        // if colorCount is outside of our expected range:
        // probably an outlier, do not sync to it
        if ((colorCount > 5800000) || (colorCount < 5300000)){
            calibration_second = true;
            sub_second_counter = 0;
            second_counter = 0;
        }

//        return edgeFrame;
        return currentFrame;
    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {
    }

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCVCameraView.enableView();
                } break;
                default:
                {
                    super.onManagerConnected(status);
                } break;
            }
        }
    };
}
