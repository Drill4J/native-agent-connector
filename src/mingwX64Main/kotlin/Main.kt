import drill.*
import kotlinx.coroutines.*

fun main(): Unit = runBlocking {
    init(agentId = "nag", adminAddress = "localhost:8090")
    createWebSocket { dest, _ ->
        println(dest)
    }.connect()
    delay(15000)
}
