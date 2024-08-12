package com.realityexpander.kotlinxrpc

import NewsService
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.netty.*
import io.ktor.server.plugins.cors.routing.*
import io.ktor.server.response.respond
import io.ktor.server.routing.*
import kotlinx.coroutines.launch
import kotlinx.rpc.serialization.json
import kotlinx.rpc.transport.ktor.server.RPC
import kotlinx.rpc.transport.ktor.server.rpc

fun main(args: Array<String>): Unit = EngineMain.main(args)

@Suppress("unused")
fun Application.module() {
    install(RPC)
    installCORS()

    routing {
        rpc("/api") {

            // Get the client authorization header.
            val authHeader = call.request.headers[HttpHeaders.Authorization]
            if(authHeader != "Basic YWRtaW46YWRtaW4=") {
                launch {
                    call.respond(HttpStatusCode.Unauthorized)
                }
                return@rpc
            }

            rpcConfig {
                serialization {
                    json()
                }
            }

            registerService<NewsService> { ctx -> NewsServiceImpl(ctx) }
        }
    }
}

fun Application.installCORS() {
    install(CORS) {
        allowMethod(HttpMethod.Options)
        allowMethod(HttpMethod.Put)
        allowMethod(HttpMethod.Delete)
        allowMethod(HttpMethod.Patch)
        allowHeader(HttpHeaders.Authorization)
        allowHeader(HttpHeaders.AccessControlAllowOrigin)
        allowHeader(HttpHeaders.Upgrade)
        allowNonSimpleContentTypes = true
        allowCredentials = true
        allowSameOrigin = true

        // webpack-dev-server and local development
        val allowedHosts =
            listOf("localhost:3000", "localhost:8080", "127.0.0.1:8080", "192.1.168.69:8080")
        allowedHosts.forEach { host ->
            allowHost(host, listOf("http", "https", "ws", "wss"))
        }
    }
}
