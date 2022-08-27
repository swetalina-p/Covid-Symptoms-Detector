package com.example.covidsymptomsdetector;

import android.content.Context;
import android.widget.Toast;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;


@Database(entities = {UserSymptoms.class}, version = 1)
public abstract class UserDatabase extends RoomDatabase {
    public abstract UserSymptomsDao userDao();
    private static UserDatabase userDataBaseInstance;
    public static synchronized UserDatabase getInstance(Context context){
        //Create new database with last name for a name if none exist
        if(userDataBaseInstance == null){
            userDataBaseInstance = Room
                    .databaseBuilder(context, UserDatabase.class, "kandala")
                    .build();
            Toast.makeText(context, "Database Created Kandala", Toast.LENGTH_SHORT).show();
        }
        return userDataBaseInstance;
    }

}