package com.lockin.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lockin.app.data.local.db.entity.SavedTriggerEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TriggerDao {

    @Query("SELECT * FROM saved_triggers ORDER BY created_at DESC")
    fun observeAll(): Flow<List<SavedTriggerEntity>>

    @Query("SELECT * FROM saved_triggers WHERE type = :type ORDER BY label ASC")
    fun observeByType(type: String): Flow<List<SavedTriggerEntity>>

    @Query("SELECT * FROM saved_triggers WHERE id = :id")
    suspend fun getById(id: Long): SavedTriggerEntity?

    @Query("SELECT * FROM saved_triggers WHERE type = :type AND value = :value LIMIT 1")
    suspend fun findByTypeAndValue(type: String, value: String): SavedTriggerEntity?

    @Query("SELECT * FROM saved_triggers WHERE auto_prompt = 1 AND type = 'WIFI'")
    fun observeAutoPromptWifiTriggers(): Flow<List<SavedTriggerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(trigger: SavedTriggerEntity): Long

    @Update
    suspend fun update(trigger: SavedTriggerEntity)

    @Delete
    suspend fun delete(trigger: SavedTriggerEntity)
}
