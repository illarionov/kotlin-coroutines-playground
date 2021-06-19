package coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.channels.produce

fun main() {
    `необычная работа rendevouz`()
    //`channel не закрывается при падении читающей корутины`()
    //`channel не закрывается при падении читающей корутины 2`()
}

fun `необычная работа rendevouz`() = runBlocking {
    // Здесь будет A1, B1, A done, B done, A2
    // Объяснение на https://www.youtube.com/watch?v=HpWQUoVURWQ&t=1s
    // producerB запускается только после того, как producerA засыпает на send(A1)
    // consumer запускается только после того, как producerB засыпает на send(B1)
    // В первые 2 receive producerA и producerB шедулятся на выполнение
    // на 3 receive consumer засыпает, просыпается producerA, выполняет send("A2), который шедулит выполнение consumer,
    // не засыпает и выполняет log("A done")
    // Затем просыпается producerB, затем - consumer

    val channel = Channel<String>()
    val producerA = launch(CoroutineName("Producer A")) {
        channel.send("A1")
        channel.send("A2")
        log("A done")
    }

    val producerB = launch(CoroutineName("Producer B")) {
        channel.send("B1")
        log("B done")
    }

    val consumer = launch {
        for (item in channel) {
            log(item)
        }
    }

    listOf(producerA, producerB).joinAll()
    channel.close()
}

fun `channel не закрывается при падении читающей корутины`() = runBlocking {
    val producer: ReceiveChannel<Int> = produce {
        for (x in 1..10) {
            send(x)
            delay(100)
            log("I'm alive")
        }
        log("Done")
    }

    delay(200)

    try {
        coroutineScope {
            launch {
                for (y in 1..5) {
                    log("received ${producer.receive()}")
                }
                throw RuntimeException()
            }
        }
    } catch (e: Throwable) {
        log("Catched exception $e")
    }

    for (item in producer) {
        log("received2: ${item}")
    }
}


fun `channel не закрывается при падении читающей корутины 2`() = runBlocking {
    val channel = Channel<Int>()

    GlobalScope.launch {
        for (x in 1..10) {
            channel.send(x)
            delay(100)
            log("I'm alive")
        }
        channel.close()
    }

    try {
        coroutineScope {
            launch {
                for (y in 1..5) {
                    log("received ${channel.receive()}")
                }
                throw RuntimeException()
            }
        }
    } catch (e: Throwable) {
        log("Catched exception $e")
    }

    for (item in channel) {
        log("received2: ${item}")
    }
}