package io.em2m.search.core.daos

import io.em2m.search.core.model.SyncDao

abstract class SyncDaoWrapper<T>(val delegate: SyncDao<T>) : SyncDao<T> by delegate
