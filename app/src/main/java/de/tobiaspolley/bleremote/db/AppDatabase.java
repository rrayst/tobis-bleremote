package de.tobiaspolley.bleremote.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {Hub.class, Option.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract HubDao hubDao();
    public abstract OptionDao optionDao();
}
