
package com.realityexpander.kotlinxrpc

import NewsService
import UserData
import io.ktor.server.testing.*
import kotlinx.coroutines.flow.toList
import kotlinx.rpc.client.withService
import kotlinx.rpc.serialization.json
import kotlinx.rpc.streamScoped
import kotlinx.rpc.transport.ktor.client.installRPC
import kotlinx.rpc.transport.ktor.client.rpc
import kotlinx.rpc.transport.ktor.client.rpcConfig
import kotlin.test.Test
import kotlin.test.assertEquals

class ApplicationTest {
    @Test
    fun testRoot() = testApplication {
        val service = createClient {
            installRPC()
        }.rpc("/api") {
            rpcConfig {
                serialization {
                    json()
                }
            }
        }.withService<NewsService>()

        assertEquals(
            expected = "Nice to meet you Alex, how is it in address1?",
            actual = service.hello("Alex", UserData("address1", "last")),
        )

        streamScoped {
            assertEquals(
                expected = List(10) { "Article number $it" },
                actual = service.subscribeToNews().toList(),
            )
        }
    }
}
