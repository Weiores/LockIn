package com.lockin.app.data.local.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.lockin.app.data.local.db.entity.ModuleEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ModuleDao {

    @Query("SELECT * FROM modules WHERE is_archived = 0 ORDER BY code ASC")
    fun observeActiveModules(): Flow<List<ModuleEntity>>

    @Query("SELECT * FROM modules ORDER BY code ASC")
    fun observeAllModules(): Flow<List<ModuleEntity>>

    @Query("SELECT * FROM modules WHERE id = :id")
    suspend fun getById(id: Long): ModuleEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(module: ModuleEntity): Long

    @Update
    suspend fun update(module: ModuleEntity)

    @Delete
    suspend fun delete(module: ModuleEntity)

    @Query("UPDATE modules SET is_archived = 1 WHERE id = :id")
    suspend fun archive(id: Long)
}
