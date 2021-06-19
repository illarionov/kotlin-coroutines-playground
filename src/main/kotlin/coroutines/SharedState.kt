package coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.actor
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.atomic.AtomicInteger
import kotlin.system.measureTimeMillis

fun main() {
    DebugProbes.install()
    `volatile int`()
    `atomic int`()
    `confinement thread`()
    `confinement thread coarse-grained`()
    `with mutex`()
    WithActor().run()
}

fun `unsynchropnized state`() = runBlocking {
    println("unsynchropnized state")
    var counter = 0
    withContext(Dispatchers.Default) {
        massiveRun {
            counter++
        }
    }
    println("Counter = $counter")
}

@Volatile
var volatileCounter: Int = 0

fun `volatile int`() = runBlocking {
    println("volatile int")
    withContext(Dispatchers.Default) {
        massiveRun {
            volatileCounter += 1
        }
    }
    println("Counter = ${volatileCounter}")
}

fun `atomic int`() = runBlocking {
    println("atomic int")
    var counter: AtomicInteger = AtomicInteger(0)
    withContext(Dispatchers.Default) {
        massiveRun {
            counter.incrementAndGet()
        }
    }
    println("Counter = ${counter.get()}")
}

fun `confinement thread`() = runBlocking {
    println("confinement thread")
    val counterContext = newSingleThreadContext("CounterContext")
    counterContext.use { dispatcher ->
        var counter = 0
        withContext(Dispatchers.Default) {
            massiveRun {
                // confine each increment to a single-threaded context
                withContext(dispatcher) {
                    counter++
                }
            }
        }
        println("Counter = $counter")
        counterContext.close()
    }
}

fun `confinement thread coarse-grained`() = runBlocking {
    println("confinement thread coarse-grained")
    val counterContext = newSingleThreadContext("CounterContext")
    counterContext.use { dispatcher ->
        var counter = 0
        // confine everything to a single-threaded context
        withContext(dispatcher) {
            massiveRun {
                counter++
            }
        }
        println("Counter = $counter")
    }
}

fun `with mutex`() = runBlocking {
    println("with mutex")
    val mutex = Mutex()
    var counter = 0
    withContext(Dispatchers.Default) {
        massiveRun {
            mutex.withLock {
                counter++
            }
        }
    }
    println("Counter = $counter")
}

sealed class CounterMsg
object IncCounter : CounterMsg() // one-way message to increment counter
class GetCounter(val response: CompletableDeferred<Int>) : CounterMsg() // a request with reply

class WithActor {
    private fun CoroutineScope.counterActor() = actor<CounterMsg> {
        var counter = 0 // actor state
        for (msg in channel) { // iterate over incoming messages
            when (msg) {
                is IncCounter -> counter++
                is GetCounter -> msg.response.complete(counter)
            }
        }
    }

    fun run() = runBlocking<Unit> {
        println("with actor")
        val counter = counterActor() // create the actor
        withContext(Dispatchers.Default) {
            massiveRun {
                counter.send(IncCounter)
            }
        }
        // send a message to get a counter value from an actor
        val response = CompletableDeferred<Int>()
        counter.send(GetCounter(response))
        println("Counter = ${response.await()}")
        counter.close() // shutdown the actor
    }
}

suspend fun massiveRun(action: suspend () -> Unit) {
    val n = 100  // number of coroutines to launch
    val k = 1000 // times an action is repeated by each coroutine
    val time = measureTimeMillis {
        coroutineScope { // scope for coroutines
            repeat(n) {
                launch {
                    repeat(k) { action() }
                }
            }
        }
    }
    println("Completed ${n * k} actions in $time ms")
}
