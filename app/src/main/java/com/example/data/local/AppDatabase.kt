package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
  entities = [ProjectEntity::class, ExportedVideoEntity::class, CrashRecoveryEntity::class],
  version = 2,
  exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun projectDao(): ProjectDao
  abstract fun exportedVideoDao(): ExportedVideoDao
  abstract fun crashRecoveryDao(): CrashRecoveryDao

  companion object {
    @Volatile
    private var INSTANCE: AppDatabase? = null

    val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        // Add new project configuration and metadata columns with safe default values
        db.execSQL("ALTER TABLE projects ADD COLUMN sampleRate INTEGER NOT NULL DEFAULT 48000")
        db.execSQL("ALTER TABLE projects ADD COLUMN canvasColor INTEGER NOT NULL DEFAULT -16777216")
        db.execSQL("ALTER TABLE projects ADD COLUMN hasMissingMedia INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE projects ADD COLUMN extraMetadataJson TEXT NOT NULL DEFAULT '{}'")

        // Create crash recovery table to protect unsaved timeline changes
        db.execSQL("""
          CREATE TABLE IF NOT EXISTS crash_recovery (
            id TEXT NOT NULL PRIMARY KEY,
            projectId TEXT NOT NULL,
            projectName TEXT NOT NULL,
            timestamp INTEGER NOT NULL,
            timelineJson TEXT NOT NULL,
            settingsJson TEXT NOT NULL,
            isDirty INTEGER NOT NULL DEFAULT 1
          )
        """.trimIndent())
      }
    }

    fun getDatabase(context: Context): AppDatabase {
      return INSTANCE ?: synchronized(this) {
        val instance = Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "ah_video_studio.db"
        )
        .addMigrations(MIGRATION_1_2)
        .fallbackToDestructiveMigration()
        .build()
        INSTANCE = instance
        instance
      }
    }
  }
}

