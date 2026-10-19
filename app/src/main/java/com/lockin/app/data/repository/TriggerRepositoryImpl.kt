package com.lockin.app.data.repository

import com.lockin.app.data.local.db.dao.TriggerDao
import com.lockin.app.data.local.db.entity.SavedTriggerEntity
import com.lockin.app.domain.model.TriggerProfile
import com.lockin.app.domain.model.TriggerType
import com.lockin.app.domain.repository.TriggerRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TriggerRepositoryImpl @Inject constructor(
    private val triggerDao: TriggerDao,
) : TriggerRepository {

    override fun observeAll(): Flow<List<TriggerProfile>> =
        triggerDao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeByType(type: TriggerType): Flow<List<TriggerProfile>> =
        triggerDao.observeByType(type.name).map { list -> list.map { it.toDomain() } }

    override fun observeAutoPromptWifiTriggers(): Flow<List<TriggerProfile>> =
        triggerDao.observeAutoPromptWifiTriggers().map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): TriggerProfile? =
        triggerDao.getById(id)?.toDomain()

    override suspend fun findByTypeAndValue(type: TriggerType, value: String): TriggerProfile? =
        triggerDao.findByTypeAndValue(type.name, value)?.toDomain()

    override suspend fun insert(trigger: TriggerProfile): Long =
        triggerDao.insert(trigger.toEntity())

    override suspend fun update(trigger: TriggerProfile) =
        triggerDao.update(trigger.toEntity())

    override suspend fun delete(trigger: TriggerProfile) =
        triggerDao.delete(trigger.toEntity())

    private fun SavedTriggerEntity.toDomain() = TriggerProfile(
        id = id,
        type = TriggerType.valueOf(type),
        label = label,
        value = value,
        isStrict = isStrict,
        autoPrompt = autoPrompt,
        createdAt = createdAt,
    )

    private fun TriggerProfile.toEntity() = SavedTriggerEntity(
        id = id,
        type = type.name,
        label = label,
        value = value,
        isStrict = isStrict,
        autoPrompt = autoPrompt,
        createdAt = createdAt,
    )
}

