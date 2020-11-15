package lander

import io.ktor.application.*
import io.ktor.features.*
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.serialization.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.websocket.*

fun main() {
    embeddedServer(Netty, 9090){
        routing {
            route("/") {
                get("/lander") {
                    call.respondText(this::class.java.classLoader.getResource("lander.html")!!.readText(),
                    ContentType.Text.Html)
                }
            }
            static("/") {
                resources("")
            }
        }
        install (WebSockets)

        install(Routing) {
            webSocket("/lander") {

            }
        }
        install(ContentNegotiation) {
            json()
        }
        install(CORS) {
            method(HttpMethod.Get)
            method(HttpMethod.Post)
            method(HttpMethod.Delete)
            anyHost()
        }
        install(Compression) {
            gzip()
        }
    }.start(wait = false)
}