package drill

import com.epam.drill.common.Message
import com.epam.drill.common.MessageType
import com.epam.drill.core.drillRequest
import com.epam.drill.core.plugin.dto.DrillMessage
import com.epam.drill.core.plugin.dto.MessageWrapper
import com.epam.drill.core.ws.Sender
import kotlinx.cinterop.*
import kotlinx.serialization.protobuf.ProtoBuf


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
        adminAddress = adminAddress,
        buildVersion = buildVersion,
        groupId = groupId,
        instanceId = instanceId
    )
    val wsock = createWebSocket { dest, data ->
        memScoped {
            function.pointed.ptr.invoke(
                dest.cstr.getPointer(this),
                data.decodeToString().cstr.getPointer(this)
            )
        }
    }
    wsock.connect()
}

@CName("sendPluginMessage")
fun sendPluginMessage(pluginId: String, content: String) {
    val drillMessage = DrillMessage(drillRequest()?.drillSessionId ?: "", content)
    Sender.send(
        Message(
            MessageType.PLUGIN_DATA,
            "",
            ProtoBuf.dump(MessageWrapper.serializer(), MessageWrapper(pluginId, drillMessage))
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
