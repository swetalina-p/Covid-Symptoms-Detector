package com.example.covidsymptomsdetector;


import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = "UserSymptoms")
public class UserSymptoms {


    @PrimaryKey (autoGenerate = true)
    public int userId;
    @ColumnInfo(name="cough")
    public float cough;
    @ColumnInfo(name="headache")
    public float headache;
    @ColumnInfo(name="soreThroat")
    public float soreThroat;
    @ColumnInfo(name="fever")
    public float fever;
    @ColumnInfo(name="diarrhea")
    public float diarrhea;
    @ColumnInfo(name="muscleAche")
    public float muscleAche;
    @ColumnInfo(name="nausea")
    public float nausea;
    @ColumnInfo(name="lossOfTasteOrSmell")
    public float lossOfTasteOrSmell;
    @ColumnInfo(name="troubleBreathing")
    public float troubleBreathing;
    @ColumnInfo(name="congestion")
    public float congestion;
    @ColumnInfo(name="heartRate")
    public float heartRate;
    @ColumnInfo(name="respiratoryRate")
    public float respiratoryRate;


    public UserSymptoms() {
        cough = 0;
        headache = 0;
        soreThroat = 0;
        fever = 0;
        diarrhea = 0;
        muscleAche = 0;
        nausea = 0;
        lossOfTasteOrSmell = 0;
        congestion = 0;
        troubleBreathing = 0;
        heartRate=0;
        respiratoryRate=0;
    }

    public void setHeartRate(float heartRate) {
        this.heartRate = heartRate;
    }

    public void setRespiratoryRate(float respiratoryRate) {
        this.respiratoryRate = respiratoryRate;
    }
}
