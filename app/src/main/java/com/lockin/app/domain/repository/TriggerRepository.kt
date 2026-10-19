package com.lockin.app.domain.repository

import com.lockin.app.domain.model.TriggerProfile
import com.lockin.app.domain.model.TriggerType
import kotlinx.coroutines.flow.Flow

interface TriggerRepository {
    fun observeAll(): Flow<List<TriggerProfile>>
    fun observeByType(type: TriggerType): Flow<List<TriggerProfile>>
    fun observeAutoPromptWifiTriggers(): Flow<List<TriggerProfile>>
    suspend fun getById(id: Long): TriggerProfile?
    suspend fun findByTypeAndValue(type: TriggerType, value: String): TriggerProfile?
    suspend fun insert(trigger: TriggerProfile): Long
    suspend fun update(trigger: TriggerProfile)
    suspend fun delete(trigger: TriggerProfile)
}

