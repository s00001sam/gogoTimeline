package com.sam.gogotimeline.data.repository

import com.sam.gogotimeline.data.datasource.TransactionRunner

/** Runs blocks directly and records whether a call happens inside a transaction. */
class RecordingTransactionRunner : TransactionRunner {
    var active = false
        private set
    var runs = 0
        private set

    override suspend fun <R> run(block: suspend () -> R): R {
        runs++
        active = true
        try {
            return block()
        } finally {
            active = false
        }
    }
}
