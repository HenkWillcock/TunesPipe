package com.example.tunespipe.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.tunespipe.Song

// --- START OF CHANGE ---

// 1. Add Song::class and PlaylistSongCrossRef::class to the entities list.
// 2. Increment the database version from 1 to 2.
@Database(entities = [Playlist::class, Song::class, PlaylistSongCrossRef::class], version = 2, exportSchema = false)
// --- END OF CHANGE ---
abstract class AppDatabase : RoomDatabase() {

    abstract fun playlistDao(): PlaylistDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "tunespipe_database"
                )
                    // --- START OF CHANGE ---
                    // 3. Add this line. It tells Room how to handle a version change.
                    // For development, this is the easiest option: it will simply
                    // delete the old database and create a new one.
                    .fallbackToDestructiveMigration()
                    // --- END OF CHANGE ---
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
