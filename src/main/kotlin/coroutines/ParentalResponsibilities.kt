package coroutines

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

fun main() {
    //`parent-корутина ждет своих child'ов`()
    `withContext и отмены корутин, запущенных в других скоупах`()
}

//
fun `parent-корутина ждет своих child'ов`() = runBlocking {
    val request = launch {
        repeat(3) { i -> // launch a few children jobs
            launch {
                delay((i + 1) * 200L) // variable delay 200ms, 400ms, 600ms
                println("Coroutine $i is done")
            }
        }
        println("request: I'm done and I don't explicitly join my children that are still active")
    }
    //request.join() // wait for completion of the request, including all its children
    println("Now processing of the request is complete")
}

fun `withContext и отмены корутин, запущенных в других скоупах`() {

    // Репозиторий со своим скоупом.
    class Repository: CoroutineScope {
        val job: Job = Job()
        override val coroutineContext: CoroutineContext
            get() =  job + Dispatchers.IO

        suspend fun getRandomNumberAfterDelay(): Float {
            log("getRandomNumber()")
            // Корутины запускаются с Job от Repository в качестве parent'а.
            val result = withContext(coroutineContext) {
                log("inside coroutineContext")
                delay(3000)
                log("delay complete, i'm still alive") // {1}
                42f
            }
            log("getRandomNumber() result: $result")
            return result
        }
    }

    val repository = Repository()
    runBlocking {
        val globalJob = GlobalScope.launch() {
            val newValue = repository.getRandomNumberAfterDelay()
            log("GlobalJob still alive. NewValue: $newValue") // {2}
        }
        delay(300)

        // Если отменить globalJob, то он не отменит delay в getRandomNumberAfterDelay(), так как там withContext с другим parent'ом,
        // но корутина дождется выполения getRandomNumberAfterDelay в состоянии Cancelling (зависнем на 3 секунды на join).
        // {1} выполнится, {2} не выполнится
        globalJob.cancel()
        log("globalJob cancelled")

        // Если же отменить скоуп repository, то ожидающий его globalJob тоже сразу же отменится.
        // {1} и {2} не выполнятся
        //repository.job.cancel()
        //log("Repository job cancelled")

        globalJob.dumpWithChild("globalJob> ")
        globalJob.join()
        log("globalJob.join() complete")
    }
}