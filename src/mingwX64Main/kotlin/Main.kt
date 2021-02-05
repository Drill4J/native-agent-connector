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
import com.epam.drill.common.*
import drill.*
import kotlinx.coroutines.*
import kotlinx.serialization.*

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
    println(json.stringify(CoverMessage.serializer(), InitInfo(1, "", true)))
    init(agentId = "nag", adminAddress = "localhost:8090")
    createWebSocket { dest, data ->
        println("<<<dest=$dest\n${data.decodeToString()}\n")
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
    }.connect()
    while (true) {
        delay(1000L)
    }
}


private fun CoverMessage.send() {
    val json = json.stringify(CoverMessage.serializer(), this)
    sendPluginMessage("test2code", json)
    println(">>>plugin: test2code, json=$json")
}
