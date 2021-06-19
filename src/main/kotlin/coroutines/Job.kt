package coroutines

import kotlinx.coroutines.*
import kotlinx.coroutines.debug.DebugProbes

fun main() {
    DebugProbes.install()
    //testJobLaunch()
    //testJobAsync()
    //testCoroutineScope()
    //testWithContext()
    testJob()
}

fun testJobLaunch() = runBlocking {
    println("root job: ${coroutineContext.job}")

    // возвращаемая job'а из launch - та же, что и внутри launch coroutineContext.job и она - child от job1
    val job1 = Job()
    println("root job1: $job1")
    val launchJob = launch(job1 + CoroutineName("Launch root coroutine")) {
        println("launch job inside launch: ${coroutineContext.job}")
        println("job 1 tree: ")
        job1.dumpWithChild()
    }
    println("launch job outside: $launchJob")
    launchJob.join()

    println()
    // возвращаемая job'а из launch - та же, что и внутри launch coroutineContext.job и она - child от job1
    val scope1: CoroutineScope = CoroutineScope(job1)
    val launchJob2 = launch(job1 + CoroutineName("Launch root coroutine 2")) {
        println("launch 2 job: ${coroutineContext.job}")
        println("job 1 tree: ")
        job1.dumpWithChild()
    }
    println("launch job 2 outside: $launchJob2")

}

fun testJobAsync() = runBlocking {
    println("root job: ${coroutineContext.job}")

    val job1 = Job()
    println("root job1: $job1")
    val launchJob = async(job1 + CoroutineName("Launch root coroutine")) {
        println("launch job inside launch: ${coroutineContext.job}")
        println("job 1 tree: ")
        job1.dumpWithChild()
    }
    println("launch job outside: $launchJob")
    launchJob.join()

    println()
    val scope1: CoroutineScope = CoroutineScope(job1)
    val launchJob2 = async(job1 + CoroutineName("Launch root coroutine 2")) {
        println("launch 2 job: ${coroutineContext.job}")
        println("job 1 tree: ")
        job1.dumpWithChild()
    }
    println("launch job 2 outside: $launchJob2")

}

fun testCoroutineScope() = runBlocking {
    println("root job: ${coroutineContext.job}")

    val rootJob = coroutineContext.job

    val job1 = Job()
    println("root job1: $job1")
    coroutineScope {
        println("scope job: ${coroutineContext.job}")
        val launchJob = launch(CoroutineName("Launch root coroutine")) {
            println("launch job inside launch: ${coroutineContext.job}")
            println("root job tree: ")
            rootJob.dumpWithChild()
        }
        println("launch job outside: $launchJob")
        launchJob.join()
    }
}

fun testWithContext() = runBlocking {
    println("root job: ${coroutineContext.job}")

    val rootJob = coroutineContext.job

    val job1 = Job()
    println("root job1: $job1")
    withContext(job1) {
        println("withContext job: ${coroutineContext.job}")
        val launchJob = launch(CoroutineName("Launch root coroutine")) {
            println("launch job inside launch: ${coroutineContext.job}")
            println("root job tree: ")
            rootJob.dumpWithChild()
            println("job1 tree: ")
            job1.dumpWithChild()
        }
        println("launch job outside: $launchJob")
        launchJob.join()
    }
    //job1.join()

    println()
    println()

    val scope = CoroutineScope(job1)
    withContext(scope.coroutineContext) {
        println("withContext job: ${coroutineContext.job}")
        val launchJob = launch(CoroutineName("Launch root coroutine")) {
            println("launch job inside launch: ${coroutineContext.job}")
            println("root job tree: ")
            rootJob.dumpWithChild()
            println("job1 tree: ")
            job1.dumpWithChild()
        }
        println("launch job outside: $launchJob")
        //launchJob.join()
    }
    delay(100)
    //job1.join()
}

fun testJob() = runBlocking {
    // это бесконечный цикл, так как по дефолту джоба активна, пока не отменена или завершена
    Job().join()
}