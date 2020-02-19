package edu.duke.cs.ospreyservice

import edu.duke.cs.ospreyservice.services.AboutService
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.serialization.serialization
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.NoSuchElementException


object OspreyService {

	const val name = "Osprey Service"

	private val properties =
		Properties().apply {
			getResourceAsStream("build.properties")
				?.use { load(it) }
				?: throw Error("can't find build.properties")
		}

	private fun string(name: String) = properties.getProperty(name) ?: throw NoSuchElementException("no property named $name")

	val version = string("version")

	fun getResourceAsStream(path: String) = OspreyService.javaClass.getResourceAsStream(path)

	fun getResourceAsString(path: String, charset: Charset = Charsets.UTF_8) =
		getResourceAsStream(path).use { stream -> stream.reader(charset).readText() }

	fun getResourceAsBytes(path: String) =
		getResourceAsStream(path).use { stream -> stream.readBytes() }

	private val service =
		embeddedServer(Netty, 8080) {

			install(ContentNegotiation) {
				serialization()
			}

			routing {

				// serve a simple webpage at the root
				get("/") {
					val html = getResourceAsString("index.html")
						.replace("\$name", name)
						.replace("\$version", version)
					call.respondText(html, ContentType.Text.Html)
				}

				// map each service to a URL
				get("/about", AboutService.route)
			}
		}

	fun start(wait: Boolean) =
		service.start(wait)

	fun stop() =
		service.stop(2L, 2L, TimeUnit.SECONDS)

	fun <T> use(block: () -> T): T {
		try {
			start(wait = false)
			return block()
		} finally {
			stop()
		}
	}
}
