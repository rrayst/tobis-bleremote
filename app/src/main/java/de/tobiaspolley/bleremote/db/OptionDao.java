package de.tobiaspolley.bleremote.db;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface OptionDao {
    @Query("SELECT * FROM option WHERE name = :name")
    Option getOption(String name);

    @Insert
    void insertAll(Option... options);

    @Delete
    void delete(Option option);
}
