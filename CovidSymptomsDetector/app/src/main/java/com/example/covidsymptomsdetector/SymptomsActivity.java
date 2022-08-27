package com.example.covidsymptomsdetector;

import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class SymptomsActivity extends AppCompatActivity {
    private UserSymptoms user= new UserSymptoms();
    float[] userSymptomsRatings = new float[10];
    private UserDatabase userDB;
    private UserSymptomsDao userDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_symptoms);

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

        Spinner sympSpinner = findViewById(R.id.spinnerSymptoms);
        RatingBar sympRatingBar = (RatingBar) findViewById(R.id.starRatingBar);
        Button upload = (Button) findViewById(R.id.uploadSymptoms);

        ArrayAdapter<String> userSymptomsArray = new ArrayAdapter<String>(SymptomsActivity.this, android.R.layout.simple_spinner_dropdown_item,getResources().getStringArray(R.array.user_symptoms_array));
        userSymptomsArray.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        sympSpinner.setAdapter(userSymptomsArray);

        sympRatingBar.setOnRatingBarChangeListener(new RatingBar.OnRatingBarChangeListener()
        {

            @Override
            public void onRatingChanged(RatingBar symptomsRatingBar, float f, boolean b) {
                int x = sympSpinner.getSelectedItemPosition();
                userSymptomsRatings[x] = f;
            }
        });

        sympSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                sympRatingBar.setRating(userSymptomsRatings[i]);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                user.cough = userSymptomsRatings[0];
                user.headache = userSymptomsRatings[1];
                user.soreThroat = userSymptomsRatings[2];
                user.fever = userSymptomsRatings[3];
                user.diarrhea = userSymptomsRatings[4];
                user.muscleAche = userSymptomsRatings[5];
                user.nausea = userSymptomsRatings[6];
                user.lossOfTasteOrSmell = userSymptomsRatings[7];
                user.troubleBreathing = userSymptomsRatings[8];
                user.congestion = userSymptomsRatings[9];
                Bundle b = getIntent().getExtras();
                boolean saveUpload=b.getBoolean("saveUpload");
                if (saveUpload==true) {
                    Thread thread2 = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            UserSymptoms user1 = userDB.userDao().retrieveData();
                            user.heartRate = user1.heartRate;
                            user.respiratoryRate = user1.respiratoryRate;
                            user.userId = user1.userId;
                            userDB.userDao().updateData(user);
                        }
                    });
                    thread2.start();

                } else {
                    Thread thread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            userDB.userDao().insertData(user);
                        }
                    });
                    thread.start();
                }
                Toast.makeText(SymptomsActivity.this, "Symptoms updated!", Toast.LENGTH_SHORT).show();


//                }
            }
        });
    }
}