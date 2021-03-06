package drill

import com.epam.drill.common.*
import com.epam.drill.core.ws.*
import com.epam.drill.plugin.api.message.*
import com.epam.drill.ws.*
import kotlinx.cinterop.*
import kotlinx.serialization.json.*


@CName("initialize_agent")
fun initializeAgent(
    agentId: String,
    adminAddress: String,
    buildVersion: String = "unspecified",
    groupId: String = "",
    instanceId: String = "random",
    function: CPointer<CFunction<(CPointer<ByteVar>, CPointer<ByteVar>) -> Unit>>
) {
    init(
        agentId = agentId,
        adminAddr = adminAddress,
        buildVersion = buildVersion,
        groupId = groupId,
        instanceId = instanceId
    )
    val wsock = WsSocket()
    wsock.connect()
    ws.value?.onBinaryMessage { rawMessage ->
        val message = rawMessage.toWsMessage()
        val dest = message.destination
        val data = message.data
        memScoped {
            function.pointed.ptr.invoke(
                dest.cstr.getPointer(this),
                data.decodeToString().cstr.getPointer(this)
            )
        }
    } ?: logger.warn { "WebSocket was not initialized" }
}

@CName("sendPluginMessage")
fun sendPluginMessage(pluginId: String, content: String) {
    val drillMessage = DrillMessage(content)
    Sender.send(
        Message(
            type = MessageType.PLUGIN_DATA,
            data = Json.encodeToString(
                MessageWrapper.serializer(), MessageWrapper(pluginId, drillMessage)
            ).encodeToByteArray()
        )
    )
}

@CName("sendMessage")
fun sendMessage(messageType: String, destination: String, content: String) {
    Sender.send(
        Message(
            MessageType.valueOf(messageType),
            destination,
            content.encodeToByteArray()
        )
    )
}
