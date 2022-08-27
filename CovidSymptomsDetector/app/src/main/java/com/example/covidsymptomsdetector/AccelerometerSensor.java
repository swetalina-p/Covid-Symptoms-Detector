package com.example.covidsymptomsdetector;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.util.ArrayList;

public class AccelerometerSensor extends Service implements SensorEventListener{
    private SensorManager accSensorManager;
    private Sensor accSensor;
    private ArrayList<Integer> accX = new ArrayList<>();
    private ArrayList<Integer> accY = new ArrayList<>();
    private ArrayList<Integer> accZ = new ArrayList<>();

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    @Override
    public void onCreate(){
        Toast.makeText(this, "Service Started", Toast.LENGTH_LONG).show();
        accSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        accSensor = accSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        accSensorManager.registerListener(this, accSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        accX.clear();
        accY.clear();
        accZ.clear();
        return START_STICKY_COMPATIBILITY;
    }


    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        Sensor sensor = sensorEvent.sensor;
        if(sensor.getType()==Sensor.TYPE_ACCELEROMETER)
        {
            accX.add((int) (sensorEvent.values[0]*100));
            accY.add((int) (sensorEvent.values[0]*100));
            accZ.add((int) (sensorEvent.values[0]*100));
            if(accX.size()>=230)
            {
                stopSelf();
            }
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
    @Override
    public void onDestroy(){

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {

                //Unregisters accelerometer sensor data listener
                accSensorManager.unregisterListener(AccelerometerSensor.this);
                Log.i("service", "Service stopping");

                //Broadcasts accelerometer X values
                Intent intent = new Intent("BroadcastingAccelerometerData");
                Bundle b = new Bundle();
                b.putIntegerArrayList("accX", accX);
                intent.putExtras(b);
                LocalBroadcastManager.getInstance(AccelerometerSensor.this).sendBroadcast(intent);

            }
        });
        thread.start();
    }
}
