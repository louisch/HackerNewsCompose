package dev.louischan.hackernewscompose

import android.content.Context
import androidx.room.*
import java.sql.Timestamp

@Database(entities = [Story::class, TopStoryId::class, Comment::class, CommentId::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    companion object {
        private var INSTANCE: AppDatabase? = null
        fun getInstance(applicationContext: Context): AppDatabase {
            if (INSTANCE == null) {
                INSTANCE = Room.inMemoryDatabaseBuilder(applicationContext, AppDatabase::class.java).build()
            }
            return INSTANCE as AppDatabase
        }
    }

    abstract fun storyDao(): StoryDao
    abstract fun topStoryIdDao(): TopStoryIdDao
    abstract fun commentDao(): CommentDao
    abstract fun commentIdDao(): CommentIdDao
}

class Converters {
    @TypeConverter
    fun timestampFromSeconds(seconds: Long): Timestamp {
        return Timestamp(seconds * 1000)
    }

    @TypeConverter
    fun secondsFromTimestamp(timestamp: Timestamp): Long {
        return timestamp.time / 1000
    }
}