package drill

import com.epam.drill.*
import com.epam.drill.common.*
import com.epam.drill.common.ws.*
import com.epam.drill.core.transport.*
import com.epam.drill.core.ws.*
import com.epam.drill.logger.*
import com.epam.drill.logger.api.*
import kotlinx.serialization.protobuf.*
import platform.posix.*

@SharedImmutable
val AGENT_TYPE: AgentType = AgentType.DOTNET

@SharedImmutable
val logger = Logging.logger("NativeAgentLibrary")

const val PLUGIN_ID = "test2code"

fun init(
    agentId: String,
    adminAddr: String,
    buildVersion: String = "unspecified",
    groupId: String = "",
    instanceId: String = "random"
) {
    Logging.logLevel = LogLevel.TRACE
    drillInstallationDir = ""
    agentConfig = AgentConfig(agentId, instanceId, buildVersion, groupId, AGENT_TYPE)
    adminAddress = URL("ws://$adminAddr")
    logger.info { "adminAddress: $adminAddr" }
    logger.info { "agent config: $agentConfig" }
    configureHttp()
    init_sockets()
}

fun WsSocket.connect() {
    connect(adminAddress.toString())
}

fun ByteArray.toWsMessage() = ProtoBuf.decodeFromByteArray(Message.serializer(), this)
