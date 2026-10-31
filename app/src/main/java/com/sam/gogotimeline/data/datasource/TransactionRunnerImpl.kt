package com.sam.gogotimeline.data.datasource

import androidx.room.withTransaction
import com.sam.gogotimeline.data.local.AppDatabase
import javax.inject.Inject

class TransactionRunnerImpl @Inject constructor(private val database: AppDatabase) : TransactionRunner {
    override suspend fun <R> run(block: suspend () -> R): R = database.withTransaction { block() }
}
