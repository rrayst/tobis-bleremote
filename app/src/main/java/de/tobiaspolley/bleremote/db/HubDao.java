package de.tobiaspolley.bleremote.db;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface HubDao {
    @Query("SELECT * FROM hub")
    List<Hub> getAll();

    @Insert
    List<Long> insertAll(Hub... hubs);

    @Delete
    void delete(Hub hub);
}
