package dev.rockyj.example.demo.utils

sealed class Result<out T> {
    data class Success<T>(
        val value: T,
    ) : Result<T>()

    data class Failure(
        val error: Throwable,
    ) : Result<Nothing>()

    companion object Factory {
        fun <T> success(value: T): Result<T> = Success(value)

        fun failure(error: Throwable): Result<Nothing> = Failure(Exception("error!", error))
        fun failure(message: String): Result<Nothing> = Failure(Exception(message))

        inline fun <T> runCatching(block: () -> T): Result<T> =
            try {
                success(block())
            } catch (e: Throwable) {
                failure(e)
            }
    }

    // Check if result is success/failure
    val isSuccess: Boolean get() = this is Success
    val isFailure: Boolean get() = this is Failure

    // Get value or null
    fun getOrNull(): T? =
        when (this) {
            is Success -> value
            is Failure -> null
        }

    // Get error or null
    fun errorOrNull(): Throwable? =
        when (this) {
            is Success -> null
            is Failure -> error
        }


    // Get error or null
    fun getOrError(): T =
        when (this) {
            is Success -> value
            is Failure -> throw Exception("error!", error)
        }
}

// ==================== PART 2: FACTORY FUNCTIONS ====================

// Create success result
fun <T> success(value: T): Result<T> = Result.Success(value)

// Create failure result
fun failure(error: Throwable): Result<Nothing> = Result.Failure(error)

fun failure(message: String): Result<Nothing> = Result.Failure(Exception(message))

// Wrap a potentially throwing operation
fun <T> runWithSafety(block: () -> T): Result<T> =
    try {
        success(block())
    } catch (e: Throwable) {
        failure(e)
    }

// Convert nullable to Result
fun <T : Any> T?.toResult(errorMessage: String = "Value is null"): Result<T> =
    this?.let { success(it) } ?: failure(errorMessage)

// Convert boolean to Result
fun Boolean.toResult(errorMessage: String = "Condition failed"): Result<Unit> =
    if (this) success(Unit) else failure(errorMessage)

// Convert with success value if condition is true
fun <T> Boolean.toResult(
    value: T,
    errorMessage: String = "Condition failed",
): Result<T> = if (this) success(value) else failure(errorMessage)

// Convert any value to success
fun <T> T.toSuccess(): Result<T> = success(this)

// Convert exception to failure
fun Throwable.toFailure(): Result<Nothing> = failure(this)

// ==================== PART 3: CORE MONAD OPERATIONS ====================

// MAP: Transform success value, keep failure as-is
inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> =
    when (this) {
        is Result.Success -> success(transform(value))
        is Result.Failure -> this
    }

// FLAT MAP: Transform success value to another Result, flatten nested Results
inline fun <T, R> Result<T>.flatMap(transform: (T) -> Result<R>): Result<R> =
    when (this) {
        is Result.Success -> transform(value)
        is Result.Failure -> this
    }

// INFIX version of flatMap for better chaining
inline infix fun <T, R> Result<T>.then(transform: (T) -> Result<R>): Result<R> = flatMap(transform)

// ==================== PART 4: ERROR HANDLING ====================

// Map errors to different types
inline fun <T> Result<T>.mapError(transform: (Throwable) -> Throwable): Result<T> =
    when (this) {
        is Result.Success -> this
        is Result.Failure -> failure(transform(error))
    }

// Recover from failure with a default value
inline fun <T> Result<T>.recover(recovery: (Throwable) -> T): Result<T> =
    when (this) {
        is Result.Success -> this
        is Result.Failure -> success(recovery(error))
    }

// Recover from failure with another Result
inline fun <T> Result<T>.recoverWith(recovery: (Throwable) -> Result<T>): Result<T> =
    when (this) {
        is Result.Success -> this
        is Result.Failure -> recovery(error)
    }

// ==================== PART 5: INFIX OPERATIONS FOR CHAINING ====================

// Infix version of map
inline infix fun <T, R> Result<T>.mapTo(transform: (T) -> R): Result<R> = map(transform)

// Infix version of recover
infix fun <T> Result<T>.orElse(defaultValue: T): T =
    when (this) {
        is Result.Success -> value
        is Result.Failure -> defaultValue
    }

// Infix version of recoverWith
inline infix fun <T> Result<T>.orTry(recovery: (Throwable) -> Result<T>): Result<T> = recoverWith(recovery)

// ==================== PART 6: SIDE EFFECTS ====================

// Execute side effect on success
inline fun <T> Result<T>.onSuccess(action: (T) -> Unit): Result<T> {
    if (this is Result.Success) action(value)
    return this
}

// Execute side effect on failure
inline fun <T> Result<T>.onFailure(action: (Throwable) -> Unit): Result<T> {
    if (this is Result.Failure) action(error)
    return this
}

// Infix versions for natural language
inline infix fun <T> Result<T>.ifSuccess(action: (T) -> Unit): Result<T> = onSuccess(action)

inline infix fun <T> Result<T>.ifFailure(action: (Throwable) -> Unit): Result<T> = onFailure(action)

// ==================== PART 7: FOLD AND TERMINAL OPERATIONS ====================

// Fold: Handle both success and failure cases
inline fun <T, R> Result<T>.fold(
    onSuccess: (T) -> R,
    onFailure: (Throwable) -> R,
): R =
    when (this) {
        is Result.Success -> onSuccess(value)
        is Result.Failure -> onFailure(error)
    }

// Get value or throw exception
fun <T> Result<T>.getOrThrow(): T =
    when (this) {
        is Result.Success -> value
        is Result.Failure -> throw error
    }

// Get value or return default
fun <T> Result<T>.getOrDefault(default: T): T =
    when (this) {
        is Result.Success -> value
        is Result.Failure -> default
    }

// ==================== PART 8: COLLECTION OPERATIONS ====================

// Transform list of Results to Result of list (fails fast)
fun <T> List<Result<T>>.sequence(): Result<List<T>> {
    val values = mutableListOf<T>()
    for (result in this) {
        when (result) {
            is Result.Success -> values.add(result.value)
            is Result.Failure -> return result
        }
    }
    return success(values)
}

// Transform list with a function that returns Result
inline fun <T, R> List<T>.mapResult(transform: (T) -> Result<R>): Result<List<R>> = map(transform).sequence()

// ==================== PART 9: DSL FOR RESULT BUILDING ====================

class ResultBuilder<T> {
    private var result: Result<T>? = null

    infix fun tryOperation(operation: () -> T) {
        result = runWithSafety(operation)
    }

    infix fun <R> thenMap(transform: (T) -> R): ResultBuilder<R> {
        val newBuilder = ResultBuilder<R>()
        newBuilder.result = result?.map(transform)
        return newBuilder
    }

    infix fun <R> thenFlatMap(transform: (T) -> Result<R>): ResultBuilder<R> {
        val newBuilder = ResultBuilder<R>()
        newBuilder.result = result?.flatMap(transform)
        return newBuilder
    }

    fun build(): Result<T> = result ?: failure("No operation specified")
}

fun <T> buildResult(init: ResultBuilder<T>.() -> Unit): Result<T> = ResultBuilder<T>().apply(init).build()
