package com.am24.brickstemple.data.repository

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WishlistMutationCoordinator {
    private val localStateMutex = Mutex()
    private val gateMutex = Mutex()

    private val productLocksGuard = Any()
    private val productLocks = mutableMapOf<Int, Mutex>()

    private val itemLocksGuard = Any()
    private val itemLocks = mutableMapOf<Int, Mutex>()

    private var clearInProgress = false
    private var activeMutations = 0
    private var activeMutationsDrained: CompletableDeferred<Unit>? = null

    suspend fun <T> withLocalStateLock(block: () -> T): T =
        localStateMutex.withLock {
            block()
        }

    suspend fun <T> withProductLock(productId: Int, block: suspend () -> T): T =
        productMutex(productId).withLock {
            block()
        }

    suspend fun <T> withItemLock(itemId: Int, block: suspend () -> T): T =
        itemMutex(itemId).withLock {
            block()
        }

    suspend fun <T> runIndividualMutation(block: suspend () -> T): T {
        enterMutation()
        return try {
            block()
        } finally {
            leaveMutation()
        }
    }

    suspend fun beginClear() {
        val waiter = gateMutex.withLock {
            clearInProgress = true
            if (activeMutations == 0) null
            else CompletableDeferred<Unit>().also { activeMutationsDrained = it }
        }
        waiter?.await()
    }

    suspend fun endClear() {
        gateMutex.withLock {
            clearInProgress = false
        }
    }

    private fun productMutex(productId: Int): Mutex =
        synchronized(productLocksGuard) {
            productLocks.getOrPut(productId) { Mutex() }
        }

    private fun itemMutex(itemId: Int): Mutex =
        synchronized(itemLocksGuard) {
            itemLocks.getOrPut(itemId) { Mutex() }
        }

    private suspend fun enterMutation() {
        while (true) {
            gateMutex.withLock {
                if (!clearInProgress) {
                    activeMutations++
                    return
                }
            }
            delay(10)
        }
    }

    private suspend fun leaveMutation() {
        gateMutex.withLock {
            activeMutations--
            if (activeMutations == 0) {
                activeMutationsDrained?.complete(Unit)
                activeMutationsDrained = null
            }
        }
    }
}
