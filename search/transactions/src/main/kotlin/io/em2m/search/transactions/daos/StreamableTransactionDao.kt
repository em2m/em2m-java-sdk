package io.em2m.search.transactions.daos

import io.em2m.search.core.model.DocSort
import io.em2m.search.core.model.Field
import io.em2m.search.core.model.Query
import io.em2m.search.core.model.StreamItemsRequest
import io.em2m.search.core.model.StreamRowsRequest
import io.em2m.search.core.model.StreamableDao
import io.em2m.search.core.model.SyncDao
import io.em2m.transactions.Transaction
import io.em2m.transactions.TransactionConfig
import io.em2m.transactions.TransactionContext
import io.em2m.transactions.TransactionPrecedence
import io.em2m.utils.OperationType

open class StreamableTransactionDao<T: Any, DAO>(vararg delegates: DAO?, config: Map<Class<*>, TransactionConfig> = mutableMapOf())
    : TransactionDao<T, DAO>(delegates = delegates.filterNotNull().toList(), config= config), StreamableDao<T> where DAO : SyncDao<T>, DAO: StreamableDao<T> {

    protected open val streamRowsTransaction: Transaction<DAO, StreamRowsRequest, Iterator<List<Any?>>> by lazy {
        val transaction = Transaction.Builder<DAO, StreamRowsRequest, Iterator<List<Any?>>>()
            .main { delegate, context ->
                delegate.streamRows(context.input!!)
            }
            .type(OperationType.SEARCH)
            .precedence(TransactionPrecedence.ANY)
            .build()
        transaction
    }

    override fun streamRows(
        fields: List<Field>,
        query: Query,
        sorts: List<DocSort>,
        params: Map<String, Any>
    ): Iterator<List<Any?>> {
        val context = TransactionContext<DAO, StreamRowsRequest, Iterator<List<Any?>>>(
            delegates= this.delegates as List<DAO>,
            transaction = streamRowsTransaction
        )
        return handler(context) as? Iterator<List<Any?>> ?: emptyList<List<Any>>().iterator()
    }

    protected open val streamItemsTransaction: Transaction<DAO, StreamItemsRequest, Iterator<T>> by lazy {
        val transaction = Transaction.Builder<DAO, StreamItemsRequest, Iterator<T>>()
            .main { delegate, context ->
                delegate.streamItems(context.input!!)
            }
            .type(OperationType.SEARCH)
            .precedence(TransactionPrecedence.ANY)
            .build()
        transaction
    }

    override fun streamItems(
        query: Query,
        sorts: List<DocSort>,
        params: Map<String, Any>
    ): Iterator<T> {
        val context = TransactionContext<DAO, StreamItemsRequest, Iterator<T>>(
            delegates= this.delegates as List<DAO>,
            transaction = streamItemsTransaction
        )
        return handler(context) as? Iterator<T> ?: emptyList<T>().iterator()
    }
}
