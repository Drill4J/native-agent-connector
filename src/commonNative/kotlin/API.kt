package drill

import com.epam.drill.common.*
import com.epam.drill.core.*
import com.epam.drill.core.plugin.dto.*
import com.epam.drill.core.ws.*
import kotlinx.cinterop.*
import kotlinx.serialization.protobuf.*


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

@CName("sendMessage")
fun sendMessage(pluginId: String, content: String) {
    val drillMessage = DrillMessage(drillRequest()?.drillSessionId ?: "", content)
    Sender.send(
        Message(
            MessageType.PLUGIN_DATA,
            "",
            ProtoBuf.dump(MessageWrapper.serializer(), MessageWrapper(pluginId, drillMessage))
        )
    )
}
