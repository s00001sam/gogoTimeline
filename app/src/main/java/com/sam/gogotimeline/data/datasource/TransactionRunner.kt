package com.sam.gogotimeline.data.datasource

/** Runs [block] as one transaction: all of it is saved, or none of it. */
interface TransactionRunner {
    suspend fun <R> run(block: suspend () -> R): R
}
