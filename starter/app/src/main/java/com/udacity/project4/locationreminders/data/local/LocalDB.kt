package com.udacity.project4.locationreminders.data.local

import android.content.Context
import androidx.room.Room


/**
 * Singleton class that is used to create a reminder db
 */
object LocalDB {

    private lateinit var INSTANCE : RemindersDao

    /**
     * static method that creates a reminder class and returns the DAO of the reminder
     */
    fun createRemindersDao(context: Context): RemindersDao {
        return Room.databaseBuilder(
            context.applicationContext,
            RemindersDatabase::class.java, "locationReminders.db"
        ).build().reminderDao()
    }

    fun getDatabase(context: Context) : RemindersDao {
        return synchronized(RemindersDao::class.java) {
            if (!::INSTANCE.isInitialized) INSTANCE = createRemindersDao(context)
            INSTANCE
        }
    }

}