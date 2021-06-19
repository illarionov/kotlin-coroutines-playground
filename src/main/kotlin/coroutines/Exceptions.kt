@file:Suppress("UNREACHABLE_CODE", "FunctionName", "UNUSED_ANONYMOUS_PARAMETER")

package coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.debug.DebugProbes
import java.io.IOException
import kotlin.coroutines.EmptyCoroutineContext

fun main() {
    DebugProbes.install()
//    exceptions()
//    exceptionsWithHandler()
//    cancellationOfParent()
//    exceptionOrder()
//    `cancellation Exceptions Are Transparent`()
//    `supervisor job`()
//    `бесполезный supervisorJob`()
//    `бесполезный supervisorScope 2`()
//    `скоуп перевыбрасывает исключение`()
//    `скоуп после отмены или получения исключения больше не может использоваться для запуска корутин`()
//    `скоуп после отмены или получения исключения больше не может использоваться для запуска корутин 2`()
//    `withContext перевыбрасывает исключение`()
//    `supervisorScope не перевыбрасывает исключения child корутин`()
//    `supervisorScope перевыбрасывает свои исключения`()
//    `Handler на top scope обрабатывает исключение`()
//    `root async не прокидывает исключение вверх`()
//    `не-root async в coroutineScope не выбрасывает исключение в await, catch не ловит exception`()
//    `не-root async в launch прокидывает исключение вверх и в await`()
//    `не-root async в launch при исключении в его child launch передает исключение в await и в ceh`()
//    `в случае parent-child скоупов исключение попадает в CEH child-скоупа и не передается в parent`()
//    `в случае скоупа - child от корутины исключение попадает в CEH parent-скоупа`()
//    `в случае parent-child скоупов отмена parent отменяет все child`()
//    `root корутина - первая корутина скоупа или хендлер в контексте 2`()
//    `паззлер от VasiliyZukanov`()
//    `паззлер от VasiliyZukanov 2`()
//    `async передает исключение в parent, а не выбрасывает в await, если он не в top-level`()
//    `под вопросом- async в async - перехват исключений`()
//    `под вопросом- async в async - перехват исключений 2`()
//    `тестируем handler на scoped корутине`()
}

fun exceptions() = runBlocking {

    val job = GlobalScope.launch { // root coroutine with launch
        println("Throwing exception from launch")
        throw IndexOutOfBoundsException() // Will be printed to the console by Thread.defaultUncaughtExceptionHandler
    }
    job.join()
    println("Joined failed job")

    val job2 = CoroutineScope(EmptyCoroutineContext).launch { // root coroutine with launch
        println("Throwing exception from launch2")
        throw IndexOutOfBoundsException() // Will be printed to the console by Thread.defaultUncaughtExceptionHandler
    }
    job2.join()
    println("Joined failed job 2")

    val deferred: Deferred<Unit> = GlobalScope.async { // root coroutine with async
        println("Throwing exception from async")
        throw ArithmeticException() // Nothing is printed, relying on user to call await
    }
    try {
        deferred.await()
        println("Unreached")
    } catch (e: ArithmeticException) {
        println("Caught ArithmeticException")
    }
}


fun exceptionsWithHandler() {
    val globalHandler = coroutineExceptionHandler("1")
    runBlocking(globalHandler) {
        val handler = coroutineExceptionHandler("2")

        val job = GlobalScope.launch(handler) { // root coroutine, running in GlobalScope
            println("Throwing exception from launch")
            throw IndexOutOfBoundsException()
        }
        job.join()
        println("Joined failed job")

        val job2 = CoroutineScope(handler).launch {
            println("Throwing exception from launch2")
            throw IndexOutOfBoundsException()
        }
        job2.join()
        println("Joined failed job 2")

        val deferred: Deferred<Unit> = GlobalScope.async(handler) { // also root, but async instead of launch
            println("Throwing exception from async")
            throw ArithmeticException() // Nothing is printed, relying on user to call await
        }
        DebugProbes.dumpCoroutines()
        try {
            deferred.await()
            println("Unreached")
        } catch (e: ArithmeticException) {
            println("Caught ArithmeticException")
        }
    }
}

fun `Handler на top scope обрабатывает исключение`() = runBlocking() {
    val globalHandler = coroutineExceptionHandler()

    // В гугловских статьях указывается, что CoroutineExceptionHandler срабатывает из root-корутины
    // (первой корутины скоупа), либо из Скоупа, а в оф. доках - что только из первой корутины скоупа.
    // Проверяем - на самом деле, только из первой корутины скоупа, просто при её запуске обычно
    // CoroutineExceptionHandler наследуется из скоупа.

    println("Root корутина унаследовала CoroutineExceptionHandler из скоупа")
    var scope = CoroutineScope(globalHandler)
    // сработает globalHandler, который унаследовался в конекст
    val job1 = scope.launch {
        throw RuntimeException("exception")
    }
    job1.join()

    println("Переопределяем ExceptionHandler в корутине. ")
    scope = CoroutineScope(globalHandler)
    // сработает CoroutineExceptionHandler 2
    val handler2 = coroutineExceptionHandler("2")
    val job2 = scope.launch(handler2) {
        throw RuntimeException("exception 2")
    }
    job2.join()

    println("Запускаем корутину с парентом у скоупа, но без ExceptionHandler'а")
    // В первой child корутине нет ExceptionHandler'а, поэтмоу globalHandler из скоупа не сработает
    scope = CoroutineScope(globalHandler)
    val job3 = GlobalScope.launch(scope.coroutineContext.job) {
        throw RuntimeException("exception 3")
    }
    job3.join()

    scope.coroutineContext.job.join()
}

fun `в случае parent-child скоупов исключение попадает в CEH child-скоупа и не передается в parent`() = runBlocking {
    val globalHandler1 = coroutineExceptionHandler("parent")
    val globalHandler2 = coroutineExceptionHandler("child")

    val scopeJob1 = Job()
    val scope = CoroutineScope(scopeJob1 + globalHandler1)
    val scopeJob2 = Job(scopeJob1)
    val scope2 = CoroutineScope(scopeJob2 + globalHandler2)

    // Исключение попадет в globalHandler2. "Scope 2 child coroutine" - по прежнему первая корутина, хоть и имеет
    // 2 parent-скоупа
    // JobImpl{Active}@195a689e (ScopeJob1)
    // -JobImpl{Active}@1b2bdfd7 (ScopeJob2)
    // --"Scope 2 child coroutine#3":StandaloneCoroutine{Active}@51bbbc5f
    // -"Scope 1 child coroutine#2":StandaloneCoroutine{Active}@4e99c5cc
    scope.launch(CoroutineName("Scope 1 child coroutine")) {
        scope2.launch(CoroutineName("Scope 2 child coroutine")) {
            scopeJob1.dumpWithChild()
            DebugProbes.printScope(scope)
            throw RuntimeException("error")
        }.join()
    }

    scopeJob1.join()
}

fun `в случае скоупа - child от корутины исключение попадает в CEH parent-скоупа`() = runBlocking {
    val globalHandler1 = coroutineExceptionHandler("parent")
    val globalHandler2 = coroutineExceptionHandler("child")

    val scopeJob1 = Job()
    val scope = CoroutineScope(scopeJob1 + globalHandler1)

    scope.launch(CoroutineName("Scope 1 child coroutine")) {
        val scopeJob2 = Job(coroutineContext[Job])
        val scope2 = CoroutineScope(scopeJob2 + globalHandler2)
        scope2.launch(CoroutineName("Scope 2 child coroutine")) {
            scopeJob1.dumpWithChild()
            throw RuntimeException("error")
        }
    }

    scopeJob1.join()
}


fun `в случае parent-child скоупов отмена parent отменяет все child`() = runBlocking {
    val scopeJob1 = Job()
    val scope = CoroutineScope(scopeJob1 + CoroutineName("Parent scope"))
    val scopeJob2 = Job(scopeJob1)
    val scope2 = CoroutineScope(scopeJob2 + CoroutineName("Child scope"))

    // Исключение попадет в globalHandler2
    scope.launch(CoroutineName("Parent")) {
        log("parent scope.launch started")
        scope2.launch(CoroutineName("Child")) {
            log("child scope.launch started")
            delay(1000)
        }.dumpOnCompletion("child")
        delay(1000)
    }.dumpOnCompletion("parent")

    delay(100)

    scopeJob1.cancelAndJoin()
}

fun `root async не прокидывает исключение вверх`() = runBlocking() {
    val globalHandler = coroutineExceptionHandler()

    // job - async root корутина, исключение обработается в try-catch и не попадет в globalHandler
    // Если сделать job.join(), то исключение вообще никуда не попадет
    val scope = CoroutineScope(globalHandler)
    val job: Deferred<Unit> = scope.async {
        throw RuntimeException("blabla")
    }
    try {
        job.await()
    } catch (e: Throwable) {
        log("Catched exception $e")
    }

    scope.coroutineContext.job.join()
}

fun `не-root async в coroutineScope не выбрасывает исключение в await, catch не ловит exception`() = runBlocking {
    // hint: если заменить coroutineScope на supervisorsScope, то исключение будет поймано
    coroutineScope {
        try {
            val deferred: Deferred<Unit> = async {
                delay(10)
                throw RuntimeException("blabla")
            }
            // Если заменить await на join, то ничего не меняется
            deferred.await()
        } catch (e: Exception) {
            log("exception $e")
            // Exception thrown in async WILL NOT be caught here
            // but propagated up to the scope
        }
    }
}

fun `не-root async в launch прокидывает исключение вверх и в await`() = runBlocking() {
    val globalHandler = coroutineExceptionHandler()

    // job - async не-root корутина, исключение обработается в try-catch плюс попадет в globalHandler
    // Если scope.launch заменить на async, то исключение попадет во второй try-catch и не попадет в globalHandler
    val scope = CoroutineScope(globalHandler)
    val job = scope.launch {
        val job2: Deferred<Unit> = async {
            throw RuntimeException("blabla")
        }
        try {
            job2.await()
        } catch (e: Throwable) {
            log("Catched exception 2 $e")
        }
    }
    try {
        job.join()
    } catch (e: Throwable) {
        log("Catched exception $e")
    }

    scope.coroutineContext.job.join()
}

fun `не-root async в launch при исключении в его child launch передает исключение в await и в ceh`() = runBlocking {
    val globalHandler = coroutineExceptionHandler()

    val scopeJob = Job()
    val scope = CoroutineScope(scopeJob)
    scope.launch(globalHandler) {
        val async1Job = async {
            launch {
                throw RuntimeException("error")
            }
        }

        try {
            async1Job.await()
        } catch (e: Throwable) {
            log("Catched in try-catch $e")
        }
    }
    scopeJob.join()
}

fun `скоуп после отмены или получения исключения больше не может использоваться для запуска корутин`() = runBlocking {
    val globalHandler = coroutineExceptionHandler()
    val scope = CoroutineScope(globalHandler)
    val job1 = scope.launch {
        throw RuntimeException("exception")
    }
    job1.join()

    val job2 = scope.launch() {
        log("This will not be executed")
    }
    job2.join()
    delay(100)
}

fun `скоуп после отмены или получения исключения больше не может использоваться для запуска корутин 2`() = runBlocking {
    val globalHandler = coroutineExceptionHandler()

    val scope = CoroutineScope(globalHandler)
    scope.launch {
        delay(100)
        throw RuntimeException("exception")
    }
    delay(110)

    println("I'm alive")
    // Скоуп уже отменен, корутина не будет запущена
    scope.launch() {
        delay(110)
        log("This will not be executed")
    }

    delay(100)
}

fun `скоуп перевыбрасывает исключение`() {
    val globalHandler = coroutineExceptionHandler()

    runBlocking() {
        coroutineScope {
            launch {
                try {
                    coroutineScope {
                        try {
                            launch(globalHandler) {
                                try {
                                    launch() {
                                        delay(10)
                                        throw RuntimeException("blabla")
                                    }
                                } catch (e: Throwable) {
                                    log("Inner launch 1: $e")
                                }
                            }
                        } catch (e: Throwable) {
                            log("Inner launch 2: $e")
                        }
                    }
                } catch (e: Throwable) {
                    log("Inner coroutineScope 3: $e")
                }
            }.join()
        }
    }
}

fun `withContext перевыбрасывает исключение`() = runBlocking {
    launch {
        try {
            withContext(Dispatchers.IO) {
                launch {
                    throw RuntimeException("blabla")
                }
            }
        } catch (e: Throwable) {
            log("Catch exception: $e")
        }
    }
}

fun `supervisorScope не перевыбрасывает исключения child корутин`() = runBlocking {
    try {
        supervisorScope {
            launch {
                throw RuntimeException("blabla")
            }
        }
    } catch (e: Throwable) {
        log("Catch exception: $e")
    }
}

fun `supervisorScope перевыбрасывает свои исключения`() = runBlocking {
    println("Example 1")
    try {
        supervisorScope {
            throw RuntimeException("blabla")
        }
    } catch (e: Throwable) {
        log("Catch exception: $e")
    }

    println("Example 2")
    try {
        supervisorScope {
            coroutineScope {
                launch {
                    throw RuntimeException("blabla")
                }
            }
        }
    } catch (e: Throwable) {
        log("Catch exception: $e")
    }
}

fun `root корутина - первая корутина скоупа или хендлер в контексте 2`() {
    val globalHandler = coroutineExceptionHandler()

    runBlocking() {
        coroutineScope {
            launch {
                GlobalScope.launch(globalHandler) {
                    launch() {
                        delay(10)
                        throw RuntimeException("blabla")
                    }
                }.join()
            }
        }
    }
}

fun cancellationOfParent() = runBlocking {
    val job = launch {
        val child = launch {
            try {
                delay(Long.MAX_VALUE)
            } finally {
                log("Child is cancelled")
            }
        }
        yield()
        log("Cancelling child")
        child.cancel()
        child.join()
        yield()
        log("Parent is not cancelled")
    }
    job.join()
}

fun exceptionOrder() = runBlocking {
    val handler = coroutineExceptionHandler()
    val job = GlobalScope.launch(handler) {
        launch { // the first child
            try {
                delay(Long.MAX_VALUE)
            } finally {
                withContext(NonCancellable) {
                    println("Children are cancelled, but exception is not handled until all children terminate")
                    delay(100)
                    println("The first child finished its non cancellable block")
                }
            }
        }
        launch { // the second child
            delay(10)
            println("Second child throws an exception")
            throw ArithmeticException()
        }
    }
    job.join()
}

fun `cancellation Exceptions Are Transparent`() = runBlocking {
    val handler = coroutineExceptionHandler("1")
    val handler2 = coroutineExceptionHandler("2")

    val job = GlobalScope.launch(handler) {
        val inner = launch { // all this stack of coroutines will get cancelled
            launch {
                launch(handler2) {
                    throw IOException() // the original exception
                }
            }
        }
        try {
            inner.join()
        } catch (e: CancellationException) {
            println("Rethrowing CancellationException with original cause")
            throw e // cancellation exception is rethrown, yet the original IOException gets to the handler
        }
    }.dumpOnCompletion("globalscope")
    job.join()
}

fun `supervisor job`() = runBlocking {
    val supervisor = Job().dumpOnCompletion("supervisor")
    //with(CoroutineScope(coroutineContext + supervisor)) {
    supervisorScope {
        // launch the first child -- its exception is ignored for this example (don't do this in practice!)
        val firstChild = launch(CoroutineExceptionHandler { _, _ -> log("firstChild failed handler") }) {
            log("The first child is failing")
            throw AssertionError("The first child is cancelled")
        }.dumpOnCompletion("firstChild")
        // launch the second child
        val secondChild = launch {
            firstChild.join()
            // Cancellation of the first child is not propagated to the second child
            log("The first child is cancelled: ${firstChild.isCancelled}, but the second one is still active")
            try {
                delay(Long.MAX_VALUE)
            } finally {
                // But cancellation of the supervisor is propagated
                log("The second child is cancelled because the supervisor was cancelled")
            }
        }.dumpOnCompletion("secondChild")
        // wait until the first child fails & completes
        firstChild.join()
        log("Cancelling the supervisor")
        supervisor.cancel()
        secondChild.join()
    }
}

fun `бесполезный supervisorJob`() = runBlocking {
    val handler = coroutineExceptionHandler()

    val scopeJob = Job()
    val supervisor = SupervisorJob()
    val scope = CoroutineScope(scopeJob)

    // Здесь SupervisorJob бесполезен, потому что launch создает свою обычную Job и в качестве parent'а берет supervisor
    val job = scope.launch(supervisor + handler + CoroutineName("Parent 1")) {
        // new coroutine -> can suspend
        launch(CoroutineName("Child 1")) {
            log("child 1 started")
            scopeJob.dumpWithChild()
            scope.coroutineContext.job.dumpWithChild()
            supervisor.dumpWithChild()
            // Child 1
            delay(10)
            throw RuntimeException("child 1 failed")
        }.dumpOnCompletion("child 1")
        launch(CoroutineName("Child 2")) {
            log("child 2 started")
            // Child 2
            delay(20)
            log("Child 2 alive")
        }.dumpOnCompletion("child 2")
    }.dumpOnCompletion("job")
    try {
        job.join()
    } catch (e: Throwable) {
        log("join failed: e")
    }
}

fun `бесполезный supervisorScope 2`() = runBlocking {
    val supervisor = SupervisorJob()
    val handler = coroutineExceptionHandler()

    // Здесь supervisorScope бесполезен, потому что launch создает свою обычную Job и в качестве parent'а берет supervisor
    // и child2 всё равно будет отменен. Если handler стоит на launch, то он поймает исключение
    supervisorScope {
        val job = launch(supervisor + handler + CoroutineName("Parent 1")) {
            // new coroutine -> can suspend
            launch(CoroutineName("Child 1")) {
                log("child 1 started")
                // Child 1
                delay(10)
                throw RuntimeException("child 1 failed")
            }.dumpOnCompletion("child 1")
            launch(CoroutineName("Child 2")) {
                log("child 2 started")
                // Child 2
                delay(20)
                log("Child 2 alive")
            }.dumpOnCompletion("child 2")
        }.dumpOnCompletion("job")
        try {
            job.join()
        } catch (e: Throwable) {
            log("join failed: e")
        }
    }
}

fun `async передает исключение в parent, а не выбрасывает в await, если он не в top-level`() = runBlocking {
    val handler = coroutineExceptionHandler()

    // JobCancellationException попадет в handler, join завершится с JobCancellationException
    val topLevelScope = CoroutineScope(SupervisorJob() + handler)
    topLevelScope.launch {
        val job = async {
            throw RuntimeException()
        }
        try {
            job.join()
        } catch (e: Throwable) {
            log("Catch throwable: $e")
        }
    }

    delay(100)
}

fun `паззлер от VasiliyZukanov`() {
    val handler = coroutineExceptionHandler()

    val externalJob = Job()
    val externalScope = CoroutineScope(externalJob + handler)

    // Исключение не попадет в handler и будет перевыброшено
    suspend fun doWork() {
        withContext(Dispatchers.IO) {
            delay(500)
            withContext(externalScope.coroutineContext) {
                throw RuntimeException()
            }
        }

    }

    runBlocking {
        val scope = CoroutineScope(Job())
        val job = scope.launch() {
            doWork()
        }
        job.join()
    }
}

fun `паззлер от VasiliyZukanov 2`() {
    val handler = coroutineExceptionHandler()

    val externalJob = Job()
    val externalJob2 = Job()
    val externalScope = CoroutineScope(externalJob + handler + CoroutineName("External scope"))

    //
    suspend fun doWork() {
        withContext(Dispatchers.IO + CoroutineName("DoWork WithContext")) {
            delay(500)
            externalJob.dumpWithChild()
            externalJob2.dumpWithChild()
            val job3 = launch(externalScope.coroutineContext + CoroutineName("Inner launch")) {
                externalJob.dumpWithChild()
                externalJob2.dumpWithChild()
                //DebugProbes.dumpCoroutines()
                delay(100)
                throw RuntimeException()
            }
            job3.join()
        }
    }

    runBlocking {
        val scope = CoroutineScope(externalJob2 + CoroutineName("Scope"))
        val job = scope.launch(CoroutineName("Outer launch")) {
            doWork()
        }
        job.join()
        externalJob.join()
    }
}

fun `под вопросом- async в async - перехват исключений`() {
    val handler = coroutineExceptionHandler()

    // runBlocking перевыбрасывает пришедшее исключение. В catch приходят JobCancellationException
    runBlocking(handler) {
        launch(handler) {
            val job1 = async(handler) {
                val job2 = async(handler) {
                    //delay(10)
                    throw RuntimeException("my exception")
                }
                try {
                    job2.join()
                } catch (e: Throwable) {
                    log("Inner catch throwable: $e")
                }
            }
            try {
                job1.join()
            } catch (e: Throwable) {
                log("Outer catch throwable: $e")
            }
        }
    }
}

fun `под вопросом- async в async - перехват исключений 2`() {
    val handler = coroutineExceptionHandler()

    // await перевыбрасывает исключение, кроме того, исключение передается вверх по цепочке
    // runBlocking перевыбрасывает пришедшее исключение. В catch приходят RuntimeException
    runBlocking(handler) {
        launch(handler) {
            val job1 = async(handler) {
                val job2: Deferred<Unit> = async(handler) {
                    //delay(10)
                    throw RuntimeException("my exception")
                }
                try {
                    job2.await()
                } catch (e: Throwable) {
                    log("Inner catch throwable: $e")
                }
            }
            try {
                job1.await()
            } catch (e: Throwable) {
                log("Outer catch throwable: $e")
            }
        }
    }
}

fun `runBlocking(handler) вообще никогда не срабатывает`() {
    val handler = coroutineExceptionHandler()

    // Пишут, что runBlocking сoздает child-корутину, для которых exceptionHandler не вызывается.
    // Но он должен передавать исключение parent-корутине (которой нет), либо должен вызываться handler из скопа
    // Hint: ещё BlockingCoroutine - это scoped-корутина, они перевыбрасывают исключения
    runBlocking(handler) {
        val blockingJob = coroutineContext.job
        launch(handler) {
            blockingJob.dumpWithChild()
            throw RuntimeException()
        }
    }
}

fun `тестируем handler на scoped корутине`() {
    val handler = coroutineExceptionHandler()
    // Пишут, что runBlocking сoздает child-корутину, для которых exceptionHandler не вызывается.
    // Но он должен передавать исключение parent-корутине (которой нет), либо должен вызываться handler из скоупа
    // Hint: ещё BlockingCoroutine - это scoped-корутина, они перевыбрасывают исключения
    runBlocking(handler) {
        val scope = CoroutineScope(handler)
        val job = scope.launch() {
            scope.coroutineContext.job.dumpWithChild()
            delay(10)
            throw RuntimeException()
        }
        scope.coroutineContext.job.join()
    }
}