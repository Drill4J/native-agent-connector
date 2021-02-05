/**
 * Copyright 2020 EPAM Systems
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package drill

import com.epam.drill.*
import com.epam.drill.common.*
import com.epam.drill.common.ws.*
import com.epam.drill.core.transport.*
import com.epam.drill.core.ws.*
import com.epam.drill.interceptor.*
import com.epam.drill.logger.*
import kotlinx.cinterop.*
import kotlinx.serialization.protobuf.*
import platform.posix.*
import kotlin.native.concurrent.*

@SharedImmutable
val AGENT_TYPE: AgentType = AgentType.DOTNET

const val PLUGIN_ID = "test2code"

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
                logger.debug { "Binary,  dest=$destination" }
                handler(destination, message.data)
                delivered(destination)
            }
        },
        onStringMessage = { str ->
            memScoped {
                val messageJson = json.parseJson(str).jsonObject
                val destination = messageJson.getPrimitive("destination").content
                logger.debug { "Text,  dest=$destination" }
                val text = messageJson.getPrimitive("text").content
                handler(destination, text.encodeToByteArray())
                delivered(destination)
                if (destination == "/agent/load") {
                    delivered("/agent/plugin/$PLUGIN_ID/loaded")
                }
            }
        },
        onAnyMessage = {}
    )
}

private fun delivered(destination: String) {
    Sender.send(
        Message(
            MessageType.MESSAGE_DELIVERED,
            destination
        )
    )
    logger.debug { "Delivered for $destination" }
}


fun WsSocket.connect() {
    val adminAddress = exec { adminAddress }
    println(adminAddress)
    connect(adminAddress.toString())
}


internal fun ByteArray.toWsMessage() = ProtoBuf.load(Message.serializer(), this)
