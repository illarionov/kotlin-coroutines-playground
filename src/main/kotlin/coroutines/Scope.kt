package coroutines

import kotlinx.coroutines.*

fun main() {
    runBlocking {
        val startTime = System.currentTimeMillis()
        val job = launch(Dispatchers.Default, start = CoroutineStart.UNDISPATCHED) {
            doJob(startTime)
        }
        log("${System.currentTimeMillis()} main: start")
        delay(1300L) // delay a bit
        log("main: I'm tired of waiting!")
        job.cancelAndJoin() // cancels the job and waits for its completion
        log("main: Now I can quit.")
    }
}

private suspend fun CoroutineScope.doJob(startTime: Long) {
    log("coroutine: start")
    var nextPrintTime = startTime
    var i = 0

    try {
        while (i < 5) { // computation loop, just wastes CPU
            delay(500)
            // print a message twice a second
            if (System.currentTimeMillis() >= nextPrintTime) {
                log("job: I'm sleeping ${i++} ...")
                nextPrintTime += 500L
            }
        }
    } catch (e: Throwable) {
        e.printStackTrace()
        log.error("Exception ${e}", e)
    } finally {
        log("Finally")
    }
}


class TestScope {
}