package com.valeo.podometer.activity;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.TextView;

import com.valeo.podometer.R;
import com.valeo.podometer.utils.LogFileUtils;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Date;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private TextView textView_acc_values;
    private TextView textView_gyroscope_values;
    private TextView textView_magnetic_values;
    private TextView textView_gravity_values;
    private TextView textView_lin_acc_values;
    private TextView textView_orientation_values;
    private TextView textView_global_acc_values;
    private SensorManager mSensorManager;

    private float[] mAcceleration = new float[3];
    private float[] mGyroscope = new float[3];
    private float[] mGeomagnetic = new float[3];
    private float[] mGravity = new float[3];
    private float[] mLinAcc = new float[3];
    private float[] mRotationVector = new float[4];
    private float[] mOrientation = new float[3];
    private float[] mGlobalAcc = new float[4];

    private DatagramSocket socket = null;
    private InetAddress IPAddress = null;
    private int PORT;
    private boolean request = false;
    private byte[] receiveData = new byte[16];
    private byte[] sendData = new byte[16];
    private Timer timerUI = new Timer();
    private Timer timerLog = new Timer();
    private Timer timerUDP = new Timer();
    private Thread thread = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Circle", "onCreate");
        setContentView(R.layout.activity_main);
        if (LogFileUtils.createLogFile(this)) {
            LogFileUtils.writeColumnNames();
        }
        mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        textView_acc_values = (TextView) findViewById(R.id.textView_acc_values);
        textView_magnetic_values = (TextView) findViewById(R.id.textView_magnetic_values);
        textView_gyroscope_values = (TextView) findViewById(R.id.textView_gyroscope_values);
        textView_gravity_values = (TextView) findViewById(R.id.textView_gravity_values);
        textView_lin_acc_values = (TextView) findViewById(R.id.textView_lin_acc_values);
        textView_orientation_values = (TextView) findViewById(R.id.textView_orientation_values);
        textView_global_acc_values = (TextView) findViewById(R.id.textView_global_acc_values);


        timerUI.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        updateUI();
                    }
                });
            }
        }, 0, 100);

        timerLog.schedule(new TimerTask() {
            @Override
            public void run() {
                LogFileUtils.appendLogs(mAcceleration, mGeomagnetic, mGyroscope, mGravity, mLinAcc, mOrientation, mGlobalAcc);
            }
        }, 1000, 10);

        timerUDP.schedule(new TimerTask() {
            @Override
            public void run() {
                if (request & socket != null) {
                    String timestamp = LogFileUtils.sdfRssi.format(new Date());
                    String sep = "|";
                    String data = timestamp + "|" + "#ORIENTATION# " + list2str(mOrientation) + sep +
                            "#GLOBALACC# " + list2str(mGlobalAcc);
                    sendData = data.getBytes();
                    DatagramPacket sendPacket =
                            new DatagramPacket(sendData, sendData.length, IPAddress, PORT);
                    try {
                        socket.send(sendPacket);
                        Log.d("socket", "Sent " +
                                sendPacket.getAddress().toString() + " " +
                                sendPacket.getPort() + " " +
                                new String(sendPacket.getData()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }, 1000, 100);

        try {
            socket = new DatagramSocket(8888);
        } catch (SocketException e) {
            e.printStackTrace();
        }

        thread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                        socket.receive(receivePacket);
                        String sentence = new String(receivePacket.getData());
                        Log.d("socket", "Received " + sentence);
                        IPAddress = receivePacket.getAddress();
                        PORT = receivePacket.getPort();
                        request = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        thread.start();

    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("Circle", "onStart");
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Circle", "onResume");
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD),
                SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE),
                SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY),
                SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                SensorManager.SENSOR_DELAY_FASTEST);
        mSensorManager.registerListener(this, mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                SensorManager.SENSOR_DELAY_FASTEST);

    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d("Circle", "onPause");
    }


    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Circle", "onStop");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d("Circle", "onDestroy");
        LogFileUtils.incrementCounter(this);
        LogFileUtils.closeWriter();
        mSensorManager.unregisterListener(this);
        if (socket != null) {
            socket.close();
        }
        timerUI.cancel();
        timerLog.cancel();
        timerUDP.cancel();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {

        switch (event.sensor.getType()) {
            case Sensor.TYPE_ACCELEROMETER:
                mAcceleration = event.values.clone();
                Log.d("sensor", "ACCELEROMETER: " + list2str(mAcceleration));
                break;
            case Sensor.TYPE_MAGNETIC_FIELD:
                mGeomagnetic = event.values.clone();
                Log.d("sensor", "MAGNETIC_FIELD: " + list2str(mGeomagnetic));
                break;
            case Sensor.TYPE_GYROSCOPE:
                mGyroscope = event.values.clone();
                Log.d("sensor", "GYROSCOPE: " + list2str(mGyroscope));
                break;
            case Sensor.TYPE_GRAVITY:
                mGravity = event.values.clone();
                Log.d("sensor", "GRAVITY: " + list2str(mGravity));
                break;
            case Sensor.TYPE_LINEAR_ACCELERATION:
                mLinAcc = event.values.clone();
                Log.d("sensor", "LINEAR_ACCELERATION: " + list2str(mLinAcc));
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                mRotationVector = event.values.clone();
                transformation();
                Log.d("sensor", "ROTATION_VECTOR: " + list2str(mRotationVector));
                break;
            default:
                break;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    private void transformation() {
        float Rotation[] = new float[16];
        SensorManager.getRotationMatrixFromVector(Rotation, mRotationVector);
        SensorManager.getOrientation(Rotation, mOrientation); // mOrientation contains: azimut, pitch and roll
        for (int i = 0; i < mOrientation.length; i++) {
            mOrientation[i] = (mOrientation[i] + (float) Math.PI) / (float) Math.PI * 180;
        }
        Log.d("sensor", "ORIENTATION: " + list2str(mOrientation));

        float[] inv = new float[16];
        float[] deviceAcc = new float[4];
        deviceAcc[0] = mLinAcc[0];
        deviceAcc[1] = mLinAcc[1];
        deviceAcc[2] = mLinAcc[2];
        deviceAcc[3] = 0;
        android.opengl.Matrix.invertM(inv, 0, Rotation, 0);
        android.opengl.Matrix.multiplyMV(mGlobalAcc, 0, inv, 0, deviceAcc, 0);
        Log.d("sensor", "GLOBAL_ACC: " + list2str(mGlobalAcc));
    }

    private String list2str(float[] list) {
        String res = "";
        for (int i = 0; i < list.length; i++) {
            if (i == list.length - 1) {
                res += list[i];
            } else {
                res += list[i] + " ";
            }
        }
        return res;
    }

    private void updateUI() {
        textView_gyroscope_values.setText(String.format(Locale.FRANCE, getString(R.string.values), mGyroscope[0], mGyroscope[1], mGyroscope[2]));
        textView_acc_values.setText(String.format(Locale.FRANCE, getString(R.string.values), mAcceleration[0], mAcceleration[1], mAcceleration[2]));
        textView_magnetic_values.setText(String.format(Locale.FRANCE, getString(R.string.values), mGeomagnetic[0], mGeomagnetic[1], mGeomagnetic[2]));
        textView_lin_acc_values.setText(String.format(Locale.FRANCE, getString(R.string.values), mLinAcc[0], mLinAcc[1], mLinAcc[2]));
        textView_gravity_values.setText(String.format(Locale.FRANCE, getString(R.string.values), mGravity[0], mGravity[1], mGravity[2]));
        textView_orientation_values.setText(String.format(Locale.FRANCE, getString(R.string.orientation_values), mOrientation[0], mOrientation[1], mOrientation[2]));
        textView_global_acc_values.setText(String.format(Locale.FRANCE, getString(R.string.values), mGlobalAcc[0], mGlobalAcc[1], mGlobalAcc[2]));
    }

}
