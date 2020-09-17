package io.em2m.search.core.daos

import io.em2m.search.core.model.*

abstract class AbstractSyncDao<T>(val idMapper: IdMapper<T>) : SyncDao<T> {

    override fun count(query: Query): Long {
        return search(SearchRequest(0, 0, query)).totalItems
    }

    override fun exists(id: String): Boolean {
        return findById(id) != null
    }

    override fun findById(id: String): T? {
        return findOne(TermQuery(idMapper.idField, id))
    }

    override fun findOne(query: Query): T? {
        return search(SearchRequest(0, 1, query).countTotal(false)).items?.firstOrNull()
    }

    override fun saveBatch(entities: List<T>): List<T> {
        entities.forEach { save(idMapper.getId(it), it) }
        return entities
    }

    override fun close() {
    }

    fun generateId(): String {
        return idMapper.generateId()
    }

}