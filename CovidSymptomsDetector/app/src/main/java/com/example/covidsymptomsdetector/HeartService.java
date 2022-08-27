package com.example.covidsymptomsdetector;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.bytedeco.javacv.AndroidFrameConverter;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.Frame;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class HeartService extends Service {

    private Bundle bundle = new Bundle();
    private String path = Environment.getExternalStorageDirectory().getPath();
    private int window = 9;

    @Override
    public int onStartCommand(Intent intent, int flag, int startId) {

        System.gc();
        Toast.makeText(this, "Your video is being processed..", Toast.LENGTH_LONG).show();

        HeartRateSplittingWindow the_runnable = new HeartRateSplittingWindow();
        Thread thread = new Thread(the_runnable);
        thread.start();

        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public class HeartRateSplittingWindow implements Runnable {

        @Override
        public void run() {

            ExecutorService executor = Executors.newFixedThreadPool(6);

            List<ExtractFrame> list = new ArrayList<>();
            for (int i = 0; i < window; i++) {
                ExtractFrame extractedFrame = new ExtractFrame(i * 5);
                list.add(extractedFrame);
            }

            List<Future<ArrayList<Integer>>> resultList = null;
            try {
                resultList = executor.invokeAll(list);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            executor.shutdown();
            System.gc();

            for (int k = 0; k < resultList.size(); k++) {

                Future<ArrayList<Integer>> the_future = resultList.get(k);
                try {
                    bundle.putIntegerArrayList("heartData" + k, the_future.get());
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                    e.getCause();
                }
            }
            stopSelf();
        }
    }

    public class ExtractFrame implements Callable<ArrayList<Integer>> {
        private int startingTime;

        ExtractFrame(int startingTime) {
            this.startingTime = startingTime;
        }
        @RequiresApi(api = Build.VERSION_CODES.P)
        private ArrayList<Integer> retrieveFrames() {
            Bitmap bitMap = null;
            try {
                String videoPath = path+"/heartRate.mp4";
                ArrayList<Integer> avgColorArray = new ArrayList<>();
                FFmpegFrameGrabber grabber = new FFmpegFrameGrabber(videoPath);
                AndroidFrameConverter convert_to_bit_map = new AndroidFrameConverter();
                grabber.start();
                grabber.setTimestamp(startingTime * 1000000);
                double frameRate = grabber.getFrameRate();
                for (int J = 0; J < 5 * frameRate; ) {
                    Frame frame = grabber.grabFrame();
                    if (frame == null) {
                        break;
                    }
                    if (frame.image == null) {
                        continue;
                    }
                    J++;
                    System.gc();
                    bitMap = convert_to_bit_map.convert(frame);
                    int avgColor = getAvgColor(bitMap);

                    avgColorArray.add(avgColor);
                }
                return avgColorArray;
            }
            catch(Exception e) {
                Log.e("FrameError", e.toString());
                System.out.println(e.toString());
            }
            return null;
        }

        @RequiresApi(api = Build.VERSION_CODES.P)
        @Override
        public ArrayList<Integer> call() {

            ArrayList<Integer> rednessData = new ArrayList<>();
            try {
                rednessData = retrieveFrames();

            } catch (Exception e) {
                e.printStackTrace();
            }
            return rednessData;
        }


    }
    private int getAvgColor(Bitmap bit_map) {

        long redBucket = 0;
        long pixel = 0;

        for (int X = 0; X < bit_map.getHeight(); X += 5) {
            for (int Y = 0; Y < bit_map.getWidth(); Y += 5) {
                int C = bit_map.getPixel(Y, X);
                pixel++;
                redBucket += Color.red(C);
            }
        }
        return (int) (redBucket / pixel);
    }

    @Override
    public void onDestroy() {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent("BroadcastingHeartRate");
                intent.putExtras(bundle);
                LocalBroadcastManager.getInstance(HeartService.this).sendBroadcast(intent);
                bundle.clear();
                System.gc();
            }
        });

        thread.start();
    }
}
