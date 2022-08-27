package com.example.covidsymptomsdetector;

import static java.lang.Math.abs;

import android.Manifest;
import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    private TextView heartRateView;
    private TextView respiratoryRateView;
    private static int video_capture=101;
    private int window = 9;
    private Uri fingertip;
    private String path = Environment.getExternalStorageDirectory().getPath();
    private UserDatabase userDB;
    private boolean saveUpload = false;
    private boolean heartRateProcess = false;
    private boolean respiratoryProcess = false;
    private UserSymptoms user=new UserSymptoms();
    private UserSymptomsDao userDao;
    private ArrayList<Integer> accX;
    String csvPath = path + "/x_values.csv";
    long startExecutionTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button symptoms = findViewById(R.id.symptoms);
        Button heartRate = findViewById(R.id.heartRate);
        Button captureVideo = findViewById(R.id.captureVideo);
        Button uploadSigns = findViewById(R.id.uploadSigns);
        Button respiratoryRate = findViewById(R.id.respiratoryRate);
        heartRateView = findViewById(R.id.heartRateView);
        respiratoryRateView = findViewById(R.id.respiratoryRateView);
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try{
                    userDB = UserDatabase.getInstance(getApplicationContext());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        if(!findCam()){
            captureVideo.setEnabled(false);
        }
        handlePermissions(MainActivity.this);
        symptoms.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MainActivity.this,SymptomsActivity.class);
                intent.putExtra("saveUpload", saveUpload);
                startActivity(intent);
            }
        });

        captureVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               if(heartRateProcess == true) {
                    Toast.makeText(MainActivity.this, "Already processing a video!",
                            Toast.LENGTH_SHORT).show();
                } else {
                    initRecording();
                }
            }
        });

        respiratoryRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // Checks if there is an existing respiratory rate detection process running
                if(respiratoryProcess == true) {
                    Toast.makeText(MainActivity.this, "Please wait for the process to complete before starting a new one!",
                            Toast.LENGTH_SHORT).show();

                } else {
                    Toast.makeText(MainActivity.this, "Place the phone on your abdomen \nfor 45s", Toast.LENGTH_LONG).show();
                    respiratoryProcess = true;
                    respiratoryRateView.setText("Sensing...");
                    Intent accelIntent = new Intent(MainActivity.this, AccelerometerSensor.class);
                    startService(accelIntent);
                }
            }
        });

        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Bundle b = intent.getExtras();
                RespiratoryRateDetector runnable = new RespiratoryRateDetector(b.getIntegerArrayList("accX"));
                Thread thread = new Thread(runnable);
                thread.start();
                try {
                    thread.join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                respiratoryRateView.setText(runnable.respiratoryRate + "");
                Toast.makeText(MainActivity.this, "Respiratory rate calculated!", Toast.LENGTH_SHORT).show();
                respiratoryProcess = false;
                b.clear();
                System.gc();

            }
        }, new IntentFilter("BroadcastingAccelerometerData"));

        heartRate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
               File vf = new File(path + "/heartRate.mp4");
                fingertip = Uri.fromFile(vf);

                if(heartRateProcess) {
                    Toast.makeText(MainActivity.this, "Processing...",
                            Toast.LENGTH_SHORT).show();
                } else if (vf.exists()) {

                heartRateProcess = true;
                    heartRateView.setText("Calculating...");


                    startExecutionTime = System.currentTimeMillis();
                    System.gc();
                    Intent hIntent = new Intent(MainActivity.this, HeartService.class);
                    startService(hIntent);

                } else {
                    Toast.makeText(MainActivity.this, "There is no video recorded.", Toast.LENGTH_SHORT).show();
                }

            }
        });

        LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                Bundle bundle = intent.getExtras();
                float heartRate = 0;
                int fail = 0;
                for (int i = 0; i < window; i++) {

                    ArrayList<Integer> heartData = null;
                    heartData = bundle.getIntegerArrayList("heartData"+i);

                    ArrayList<Integer> denoisedRedValues = denoiseXvalues(heartData, 5);

                    float zero_crossing = peakDetection(denoisedRedValues);
                    heartRate += zero_crossing/2;

                    String csvPath = path + "/heartRate" + i + ".csv";
                    saveToCSV(heartData, csvPath);

                    String csvHeartPath = path + "/heart_rate_denoised" + i + ".csv";
                    saveToCSV(denoisedRedValues, csvHeartPath);
                }

                heartRate = (heartRate*12)/ window;
                heartRateView.setText(heartRate + "");
                heartRateProcess = false;
                System.gc();
                bundle.clear();

            }
        }, new IntentFilter("BroadcastingHeartRate"));



        uploadSigns.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View view) {
                saveUpload = true;
                Thread thread2 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        userDB = UserDatabase.getInstance(getApplicationContext());
                        UserSymptoms user1 = new UserSymptoms();
                        user.setHeartRate(Float.parseFloat(heartRateView.getText().toString()));
                        user.setHeartRate(86.4F);
                        user.setRespiratoryRate(Float.parseFloat(respiratoryRateView.getText().toString()));
                        userDB.userDao().insertData(user);
                    }
                });
                thread2.start();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        saveUpload = false;
    }

    public static void handlePermissions(Activity activity) {

        int storagePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int REQUEST_EXTERNAL_STORAGE = 1;

        String[] PERMISSIONS = {
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA

        };

        if (storagePermission != PackageManager.PERMISSION_GRANTED) {
            Log.i("log", "Read/Write Permissions needed!");
        }

        ActivityCompat.requestPermissions(
                activity,
                PERMISSIONS,
                REQUEST_EXTERNAL_STORAGE
        );

        Log.i("log", "Permissions Granted!");

    }


    protected void onActivityResult(int req_code, int result_code, Intent dta) {

        boolean deletefile = false;
        super.onActivityResult(req_code, result_code, dta);
        if (req_code == video_capture) {
            if (result_code == RESULT_OK) {
                MediaMetadataRetriever video_retriever = new MediaMetadataRetriever();
                FileInputStream fis = null;
                try {

                    fis = new FileInputStream(fingertip.getPath());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }

                try {
                    video_retriever.setDataSource(fis.getFD());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                String time_string = video_retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long tme = Long.parseLong(time_string)/1000;
                if(tme<45) {

                    deletefile = true;
                }

            } else if (result_code == RESULT_CANCELED) {

                deletefile = true;
            }
            if(deletefile) {
                File fdelete = new File(fingertip.getPath());
            }
        }
        fingertip = null;
    }

    private boolean findCam() {

        if (getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA_ANY)){
            return true;
        } else {
            return false;
        }
    }


    public void initRecording() {

        File media_file = new File( path + "/heartRate.mp4");
        Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_DURATION_LIMIT,45);
        fingertip = Uri.fromFile(media_file);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, fingertip);
        startActivityForResult(intent,video_capture);
//        startActivityForResult(intent, video_capture);
    }

    public void saveToCSV(ArrayList<Integer> accX, String csvPath) {
        File file = new File(path);
        try {
            FileWriter fw = new FileWriter(file);
            CSVWriter csvWriter = new CSVWriter(fw);
            String[] header = { "Index", "Data"};
            csvWriter.writeNext(header);
            int i = 0;
            for (int d : accX) {
                String valuesX[] = {i + "", d + ""};
                csvWriter.writeNext(valuesX);
                i++;
            }
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public ArrayList<Integer> denoiseXvalues(ArrayList<Integer> accX, int filter) {
        ArrayList<Integer> movingAvgArrayX = new ArrayList<>();
        int movingAvg = 0;
        for(int i=0; i< accX.size(); i++){
            movingAvg += accX.get(i);
            if(i+1 < filter) {
                continue;
            }
            movingAvgArrayX.add((movingAvg)/filter);
            movingAvg -= accX.get(i+1 - filter);
        }
        return movingAvgArrayX;
    }

    public int peakDetection(ArrayList<Integer> denoisedX) {
        int difference, previous, slope = 0, zeroCrossingsX = 0;
        int j = 0;
        previous = denoisedX.get(0);
        //Get initial slope
        while(slope == 0 && j + 1 < denoisedX.size()){
            difference = denoisedX.get(j + 1) - denoisedX.get(j);
            if(difference != 0){
                slope = difference/abs(difference);
            }
            j++;
        }
        //Get total number of zero crossings in data curve
        for(int i = 1; i<denoisedX.size(); i++) {
            difference = denoisedX.get(i) - previous;
            previous = denoisedX.get(i);
            if(difference == 0) continue;
            int slopeOfCurve = difference/abs(difference);
            if(slopeOfCurve == -1* slope){
                slope *= -1;
                zeroCrossingsX++;
            }
        }
        return zeroCrossingsX;
    }

    public class RespiratoryRateDetector implements Runnable{

        public float respiratoryRate;
        ArrayList<Integer> accX;
        private String rootPath = Environment.getExternalStorageDirectory().getPath();

        RespiratoryRateDetector(ArrayList<Integer> accX)
        {
            this.accX=accX;
        }

        @Override
        public void run() {

            String csvPath = rootPath + "/x_values.csv";
            saveToCSV(accX, csvPath);
            ArrayList<Integer> denoisedX = denoiseXvalues(accX, 10);
            csvPath = rootPath + "/denoised_X_values.csv";
            saveToCSV(denoisedX, csvPath);
            int  zeroCrossingsX = peakDetection(denoisedX);
            respiratoryRate = (zeroCrossingsX*60)/90;
            Log.i("log", "Respiratory rate" + respiratoryRate);

        }


    }



}