package com.erns.alertauni.data.repository

import com.erns.alertauni.data.model.DraftEntity
import kotlinx.coroutines.flow.Flow

/** Borradores locales del Muro de Anuncios (Room). No se envian al backend. */
interface DraftRepository {
    fun observe(ownerId: String): Flow<List<DraftEntity>>
    suspend fun save(draft: DraftEntity): Long
    suspend fun delete(id: Long, ownerId: String)
}
