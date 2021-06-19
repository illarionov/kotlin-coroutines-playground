package coroutines

import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job

internal fun Job.dumpOnCompletion(name: String = this[CoroutineName].toString()): Job {
    this.invokeOnCompletion { cause: Throwable? -> log("Coroutine $name complete: $cause") }
    return this
}

internal fun Job.dumpWithChild(prefix: String = "") {
    println("$prefix$this")
    this.children
        .forEach { job -> job.dumpWithChild(prefix + "-") }
}

internal fun coroutineExceptionHandler(name: String = "1") =
    CoroutineExceptionHandler { coroutineContext, throwable ->
        println("CoroutineExceptionHandler `$name` got exception ${throwable}")
    }