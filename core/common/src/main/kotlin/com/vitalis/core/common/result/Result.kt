package com.vitalis.core.common.result

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart

/**
 * Explicit success/failure/loading, so UI state never has to infer "did this work?"
 * from a null. Named [Outcome] rather than `Result` to avoid colliding with
 * `kotlin.Result` at every call site.
 */
sealed interface Outcome<out T> {
    data class Success<T>(val data: T) : Outcome<T>
    data class Failure(val error: Throwable) : Outcome<Nothing>
    data object Loading : Outcome<Nothing>
}

/** Wraps a flow so downstream collectors see loading and errors as values, not exceptions. */
fun <T> Flow<T>.asOutcome(): Flow<Outcome<T>> = this
    .map<T, Outcome<T>> { Outcome.Success(it) }
    .onStart { emit(Outcome.Loading) }
    .catch { emit(Outcome.Failure(it)) }

inline fun <T, R> Outcome<T>.map(transform: (T) -> R): Outcome<R> = when (this) {
    is Outcome.Success -> Outcome.Success(transform(data))
    is Outcome.Failure -> this
    Outcome.Loading -> Outcome.Loading
}

fun <T> Outcome<T>.dataOrNull(): T? = (this as? Outcome.Success)?.data
