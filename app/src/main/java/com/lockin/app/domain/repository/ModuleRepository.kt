package com.lockin.app.domain.repository

import com.lockin.app.domain.model.CourseModule
import kotlinx.coroutines.flow.Flow

interface ModuleRepository {
    fun observeActiveModules(): Flow<List<CourseModule>>
    fun observeAllModules(): Flow<List<CourseModule>>
    suspend fun getById(id: Long): CourseModule?
    suspend fun insert(module: CourseModule): Long
    suspend fun update(module: CourseModule)
    suspend fun delete(module: CourseModule)
    suspend fun archive(id: Long)
}
