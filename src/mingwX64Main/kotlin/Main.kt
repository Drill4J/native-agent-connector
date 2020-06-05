import drill.*
import kotlinx.coroutines.*

fun main(): Unit = runBlocking {
    init(agentId = "nag", adminAddress = "localhost:8090")
    createWebSocket { dest, data ->
        println("<<<dest=$dest\n${data.decodeToString()}\n")
    }.connect()
    while (true) {
        delay(1000L)
    }
}
