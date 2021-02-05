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

import com.epam.drill.common.*
import com.epam.drill.core.*
import com.epam.drill.core.plugin.dto.*
import com.epam.drill.core.ws.*
import kotlinx.cinterop.*


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
            type = MessageType.PLUGIN_DATA,
            data = json.stringify(
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
