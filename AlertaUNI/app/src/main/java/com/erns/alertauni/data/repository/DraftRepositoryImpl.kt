package com.erns.alertauni.data.repository

import com.erns.alertauni.data.local.DraftDao
import com.erns.alertauni.data.model.DraftEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class DraftRepositoryImpl @Inject constructor(
    private val dao: DraftDao
) : DraftRepository {
    override fun observe(ownerId: String): Flow<List<DraftEntity>> = dao.observe(ownerId)
    override suspend fun save(draft: DraftEntity): Long = dao.upsert(draft)
    override suspend fun delete(id: Long, ownerId: String) = dao.delete(id, ownerId)
}
