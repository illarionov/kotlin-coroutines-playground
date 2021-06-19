@file:Suppress("NonAsciiCharacters")

package coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.debug.DebugProbes
import kotlinx.coroutines.flow.*
import kotlin.system.measureTimeMillis

@ExperimentalCoroutinesApi
fun main() {
    DebugProbes.install()
    //`flow not block main thread`()
    //`try to change dispatcher in flow builder`()
    //`use flowOn`()
    //`используем buffer для буфферизации между корутинами`()
    `исключение в onCompletion перевыбрасывается`()
    //`используем collectLatest чтобы получать только последние значения`()
    //`используем collectLatest чтобы получать только последние значения с try`()
    //`разница между collect и catch`()
    //`stateFlow объединяет пришедшие значения`()
}

fun `flow not block main thread`() {
    fun simple(): Flow<Int> = flow { // flow builder
        for (i in 1..3) {
            delay(100) // pretend we are doing something useful here
            emit(i) // emit next value
        }
    }

    runBlocking<Unit> {
        // Launch a concurrent coroutine to check if the main thread is blocked
        launch {
            for (k in 1..3) {
                println("I'm not blocked $k")
                delay(100)
            }
        }
        // Collect the flow
        simple().collect { value -> println(value) }
    }
}

fun `try to change dispatcher in flow builder`() {
    fun simple(): Flow<Int> = flow {
        //val sourceContext = currentCoroutineContext()
        // The WRONG way to change context for CPU-consuming code in flow builder
        withContext(Dispatchers.Default) {
            for (i in 1..3) {
                Thread.sleep(100) // pretend we are computing it in CPU-consuming way
                //withContext(sourceContext) {
                emit(i) // emit next value
            }
        }
    }.map { value ->
        withContext(Dispatchers.Default) {
            delay(1000)
            value * 2
        }
    }

    runBlocking<Unit> {
        simple().collect { value -> println(value) }
    }
}

fun `use flowOn`() {
    fun simple(): Flow<Int> = flow {
        for (i in 1..3) {
            Thread.sleep(100) // pretend we are computing it in CPU-consuming way
            log("Emitting $i")
            emit(i) // emit next value
        }
    }.flowOn(Dispatchers.Default) // RIGHT way to change context for CPU-consuming code in flow builder

    runBlocking<Unit> {
        simple().collect { value ->
            log("Collected $value")
        }
    }
}

fun `используем buffer для буфферизации между корутинами`() {
    fun simple(): Flow<Int> = flow {
        for (i in 1..3) {
            delay(100) // pretend we are asynchronously waiting 100 ms
            emit(i) // emit next value
        }
    }

    runBlocking<Unit> {
        val time = measureTimeMillis {
            simple()
                .buffer()
                .collect { value ->
                    delay(300) // pretend we are processing it for 300 ms
                    println(value)
                }
        }
        println("Collected in $time ms")
    }
}

fun `используем collectLatest чтобы получать только последние значения`() {
    fun simple(): Flow<Int> = flow {
        for (i in 1..3) {
            delay(100) // pretend we are asynchronously waiting 100 ms
            emit(i) // emit next value
        }
    }

    runBlocking<Unit> {
        val time = measureTimeMillis {
            simple()
                .collectLatest { value ->  // cancel & restart on the latest value
                    println("Collecting $value")
                    delay(300) // pretend we are processing it for 300 ms
                    println("Done $value")
                }
        }
        println("Collected in $time ms")
    }
}

fun `используем collectLatest чтобы получать только последние значения с try`() {
    fun simple(): Flow<Int> = flow {
        for (i in 1..3) {
            delay(100) // pretend we are asynchronously waiting 100 ms
            emit(i) // emit next value
        }
    }

    runBlocking<Unit> {
        val time = measureTimeMillis {
            simple()
                .collectLatest { value ->  // cancel & restart on the latest value
                    println("Collecting $value")
                    try {
                        delay(300) // pretend we are processing it for 300 ms
                        println("Done $value")
                    } catch (e: Throwable) {
                        log("отменено с $e")
                    }
                }
        }
        println("Collected in $time ms")
    }
}

fun `разница между collect и catch`() {
    // onCompletion видит как upstream, так и downstream исключения (включая CancellationException) и при этом не
    // перехватывает и не обрабатывает их. Т.е. примерно добавляет finally блок в try { collecct{} }
    // Catch - моэет использоваться для замены исключения на новый элемент
    fun simple(): Flow<Int> = flow {
        for (i in 1..3) {
            delay(100) // pretend we are asynchronously waiting 100 ms
            emit(i) // emit next value
        }
    }

    runBlocking<Unit> {
        simple()
            .onEach { item ->
                check(item % 2 == 1)
            }
            .onCompletion { cause -> cause?.let { log.error("onCompletion Exception $it") } }
            //.catch { cause -> log.error("catch  $cause") }
            .collect { value -> println("value $value") }
    }
}

fun `исключение в onCompletion перевыбрасывается`() {
    fun simple(): Flow<Int> = flow {
        for (i in 1..3) {
            delay(100) // pretend we are asynchronously waiting 100 ms
            emit(i) // emit next value
        }
    }

    runBlocking<Unit> {
        simple()
            .onEach { item ->
                check(item % 2 == 1)
            }
            .onCompletion { cause ->
                log.error("onCompletion Exception $cause")
                // эмитов и исключений из onCompletion мы нигде не увидим
//                delay(100)
//                emit(100) // если оставить это, то приложение упадет с java.lang.IllegalStateException: Check failed.
//                delay(100)
                throw RuntimeException("blabla") // а если только это, то с RuntimeException: blabla
            }
            //.catch { cause -> log.error("catch  $cause") }
            .collect { value -> println("value $value") }
    }
}

fun `stateFlow объединяет пришедшие значения`() {
    val stateFlow = MutableStateFlow<UiState>(UiState.Success)

    runBlocking {
        launch(Dispatchers.IO) {
            delay(10)
            stateFlow.emit(UiState.Error)
            delay(100)
            stateFlow.emit(UiState.Success)
        }

        val job = launch {
            delay(50)
            stateFlow
                .collect {
                    log("Received $it")
                }
        }
        delay(1000)
        job.cancelAndJoin()
    }
}

sealed class UiState {
    object Success : UiState()
    object Error : UiState()
}
