import com.epam.drill.core.ws.*
import com.epam.drill.ws.*
import drill.*
import kotlinx.coroutines.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*

@Serializable
sealed class CoverMessage

@SerialName("INIT")
@Serializable
data class InitInfo(
    val classesCount: Int,
    val message: String,
    val init: Boolean = false
) : CoverMessage()

@SerialName("INIT_DATA_PART")
@Serializable
data class InitDataPart(val astEntities: List<AstEntity>) : CoverMessage()

@SerialName("INITIALIZED")
@Serializable
data class Initialized(val msg: String = "") : CoverMessage()

@Serializable
data class AstEntity(
    val path: String,
    val name: String,
    val methods: List<AstMethod>
)

@Serializable
data class AstMethod(
    val name: String,
    val params: List<String>,
    val returnType: String,
    val count: Int = 0,
    val probes: List<Int> = emptyList()
)

fun main(): Unit = runBlocking {
    logger.info { Json.encodeToString(CoverMessage.serializer(), InitInfo(1, "", true)) }
    init(agentId = "nag", adminAddr = "localhost:8090")
    WsSocket().connect()
    ws.value?.onBinaryMessage { rawMessage ->
        logger.info { "MESSAGE: ${rawMessage.decodeToString()}" }
        val message = rawMessage.toWsMessage()
        val dest = message.destination
        logger.info { "<<<dest=$dest\n${message.data.decodeToString()}\n" }
        when (dest) {
            "/agent/load" -> {
                InitInfo(0, "", true).send()
                InitDataPart(
                    astEntities = listOf(
                        AstEntity(
                            path = "foo/bar", name = "Baz", methods = listOf(
                                AstMethod(name = "foo", params = emptyList(), returnType = "void")
                            )
                        )
                    )
                ).send()
                Initialized("").send()
            }
        }
    }
    while (true) {
        delay(1000L)
    }
}


fun CoverMessage.send() {
    val json = Json.encodeToString(CoverMessage.serializer(), this)
    sendPluginMessage(PLUGIN_ID, json)
    logger.info { ">>>plugin: test2code, json=$json" }
}
