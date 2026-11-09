package com.gitinclo.vibepitch.auth

import androidx.annotation.Keep

/**
 * Explicit authentication guard state used across the repository and domain layers
 * to replace throwing runtime assumptions with type-safe session verification.
 */
@Keep
sealed interface AuthGuardState {
    @Keep
    data class Authenticated(val userId: String) : AuthGuardState

    @Keep
    data class GuestMode(val guestUserId: String = "local_guest_user") : AuthGuardState

    @Keep
    data class Unauthenticated(val reason: String) : AuthGuardState
}

/**
 * Domain exception returned inside Result.failure when a cloud operation is attempted
 * without an active authenticated user session or initialized Firestore instance.
 */
class UnauthenticatedSessionException(message: String) : Exception(message)

/**
 * Explicit sealed result wrapper for repository and domain operations.
 */
@Keep
sealed class DataResult<out T> {
    data class Success<T>(val data: T) : DataResult<T>()
    data class AuthRequired(val reason: String) : DataResult<Nothing>()
    data class Error(val cause: Throwable, val message: String) : DataResult<Nothing>()

    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this !is Success

    fun getOrNull(): T? = (this as? Success)?.data

    fun toResult(): Result<T> = when (this) {
        is Success -> Result.success(data)
        is AuthRequired -> Result.failure(UnauthenticatedSessionException(reason))
        is Error -> Result.failure(cause)
    }
}

fun <T> Result<T>.toDataResult(): DataResult<T> = fold(
    onSuccess = { DataResult.Success(it) },
    onFailure = { err ->
        if (err is UnauthenticatedSessionException) {
            DataResult.AuthRequired(err.message ?: "Authentication required")
        } else {
            DataResult.Error(err, err.localizedMessage ?: "Operation failed")
        }
    }
)
