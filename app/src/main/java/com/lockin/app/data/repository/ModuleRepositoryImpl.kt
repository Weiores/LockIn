package com.lockin.app.data.repository

import com.lockin.app.data.local.db.dao.ModuleDao
import com.lockin.app.data.local.db.entity.ModuleEntity
import com.lockin.app.domain.model.CourseModule
import com.lockin.app.domain.repository.ModuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ModuleRepositoryImpl @Inject constructor(
    private val moduleDao: ModuleDao,
) : ModuleRepository {

    override fun observeActiveModules(): Flow<List<CourseModule>> =
        moduleDao.observeActiveModules().map { list -> list.map { it.toDomain() } }

    override fun observeAllModules(): Flow<List<CourseModule>> =
        moduleDao.observeAllModules().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): CourseModule? =
        moduleDao.getById(id)?.toDomain()

    override suspend fun insert(module: CourseModule): Long =
        moduleDao.insert(module.toEntity())

    override suspend fun update(module: CourseModule) =
        moduleDao.update(module.toEntity())

    override suspend fun delete(module: CourseModule) =
        moduleDao.delete(module.toEntity())

    override suspend fun archive(id: Long) =
        moduleDao.archive(id)

    private fun ModuleEntity.toDomain() = CourseModule(
        id = id,
        code = code,
        name = name,
        colorHex = colorHex,
        weeklyGoalMinutes = weeklyGoalMinutes,
        goalNote = goalNote,
        createdAt = createdAt,
        isArchived = isArchived,
    )

    private fun CourseModule.toEntity() = ModuleEntity(
        id = id,
        code = code,
        name = name,
        colorHex = colorHex,
        weeklyGoalMinutes = weeklyGoalMinutes,
        goalNote = goalNote,
        createdAt = createdAt,
        isArchived = isArchived,
    )
}

