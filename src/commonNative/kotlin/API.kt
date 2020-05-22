package drill

import com.epam.drill.common.*
import com.epam.drill.common.ws.URL
import com.epam.drill.core.drillRequest
import com.epam.drill.core.plugin.dto.DrillMessage
import com.epam.drill.core.plugin.dto.MessageWrapper
import com.epam.drill.core.transport.configureHttp
import com.epam.drill.core.ws.Sender
import com.epam.drill.core.ws.WsRouter
import com.epam.drill.core.ws.WsSocket
import com.epam.drill.exec
import com.epam.drill.logger.LogLevel
import com.epam.drill.logger.configByLoggerLevel
import com.epam.drill.logger.logConfig
import kotlinx.cinterop.*
import kotlinx.serialization.protobuf.ProtoBuf
import kotlin.native.concurrent.freeze

@SharedImmutable
val AGENT_TYPE: AgentType = AgentType.DOTNET


@CName("initialize_agent")
fun initializeAgent(
    agentId: String,
    adminAddress: String,
    buildVersion: String = "unspecified",
    groupId: String = "",
    instanceId: String = "random",
    function: CPointer<CFunction<(CPointer<ByteVar>, CPointer<ByteVar>) -> Unit>>
) {
    logConfig.value  = configByLoggerLevel(LogLevel.TRACE).freeze()
    exec {
        this.drillInstallationDir = ""
        this.agentConfig = AgentConfig(agentId, instanceId, buildVersion, groupId, AGENT_TYPE)
        this.adminAddress = URL("ws://$adminAddress")
    }
    configureHttp()

    WsRouter {

    }

    WsSocket({

    }, { rawMessage ->
        memScoped {
            val message = rawMessage.toWsMessage()
            val destination = message.destination
            function.pointed.ptr.invoke(destination.cstr.getPointer(this), message.data.decodeToString().cstr.getPointer(this))
        }
    }, {

    }).connect(exec { this.adminAddress }.toString())
}

private fun String.toWsMessage() = Message.serializer().parse(this)

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