package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
  @Query("SELECT * FROM projects ORDER BY lastEditedTime DESC")
  fun getAllProjects(): Flow<List<ProjectEntity>>

  @Query("SELECT * FROM projects WHERE isDraft = 1 ORDER BY lastEditedTime DESC")
  fun getDrafts(): Flow<List<ProjectEntity>>

  @Query("SELECT * FROM projects WHERE id = :id LIMIT 1")
  suspend fun getProjectById(id: String): ProjectEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertProject(project: ProjectEntity)

  @Update
  suspend fun updateProject(project: ProjectEntity)

  @Query("DELETE FROM projects WHERE id = :id")
  suspend fun deleteProjectById(id: String)

  @Query("UPDATE projects SET name = :newName, lastEditedTime = :time WHERE id = :id")
  suspend fun renameProject(id: String, newName: String, time: Long = System.currentTimeMillis())
}

@Dao
interface ExportedVideoDao {
  @Query("SELECT * FROM exported_videos ORDER BY timestamp DESC")
  fun getAllExportedVideos(): Flow<List<ExportedVideoEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun insertExportedVideo(video: ExportedVideoEntity)

  @Query("DELETE FROM exported_videos WHERE id = :id")
  suspend fun deleteExportedVideo(id: String)
}
