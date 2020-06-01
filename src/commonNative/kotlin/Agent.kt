package drill

import com.epam.drill.*
import com.epam.drill.common.*
import com.epam.drill.common.ws.*
import com.epam.drill.core.transport.*
import com.epam.drill.core.ws.*
import com.epam.drill.interceptor.logger
import com.epam.drill.logger.*
import kotlinx.cinterop.*
import kotlinx.serialization.protobuf.*
import platform.posix.*
import kotlin.native.concurrent.*

@SharedImmutable
val AGENT_TYPE: AgentType = AgentType.DOTNET

fun init(
    agentId: String,
    adminAddress: String,
    buildVersion: String = "unspecified",
    groupId: String = "",
    instanceId: String = "random"
) {
    println("agentId: $agentId")
    println("adminAddress: $adminAddress")
    println("buildVersion: $buildVersion")
    println("groupId: $groupId")
    println("instanceId: $instanceId")
    logConfig.value = configByLoggerLevel(LogLevel.TRACE).freeze()
    exec {
        this.drillInstallationDir = ""
        this.agentConfig = AgentConfig(agentId, instanceId, buildVersion, groupId, AGENT_TYPE)
        this.adminAddress = URL("ws://$adminAddress").apply { println(this) }
    }
    configureHttp()
    init_sockets()

    WsRouter {

    }
}

fun createWebSocket(handler: (String, ByteArray) -> Unit): WsSocket {
    return WsSocket(
        onBinaryMessage = {
            memScoped {
                val message = it.toWsMessage()
                val destination = message.destination
                handler(destination, message.data)
                Sender.send(
                    Message(
                        MessageType.MESSAGE_DELIVERED,
                        destination
                    )
                )
                logger.debug { "Delivered for $destination" }
            }
        },
        onStringMessage = {},
        onAnyMessage = {}
    )
}


fun WsSocket.connect() {
    val adminAddress = exec { adminAddress }
    println(adminAddress)
    connect(adminAddress.toString())
}


internal fun ByteArray.toWsMessage() = ProtoBuf.load(Message.serializer(), this)
