package com.example.covidsymptomsdetector;


import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface UserSymptomsDao {

    @Query("Select user.* from USERSYMPTOMS user where user.userId=(Select max(user1.userId) from UserSymptoms user1)")
    UserSymptoms retrieveData();

    @Insert
    long insertData(UserSymptoms user);

    @Update
    int updateData(UserSymptoms user);
}
