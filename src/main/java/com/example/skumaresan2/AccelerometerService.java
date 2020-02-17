package com.example.skumaresan2;

import android.app.Service;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Binder;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

public class AccelerometerService extends Service implements SensorEventListener {
    private final IBinder binder= new LocalBinder();
    private SensorManager sensorManager;
    float[] data = new float[]{0,0,0};
    float maxRange=0;


    public class LocalBinder extends Binder {
        AccelerometerService getService(){
            return AccelerometerService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        sensorManager.registerListener(this, sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), sensorManager.SENSOR_DELAY_NORMAL);

        return binder;
    }

    public float[] getAccelerometerData(){
        return data;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            data[0] = event.values[0]+maxRange;
            data[1] = event.values[1]+maxRange;
            data[2] = event.values[2]+maxRange;
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
