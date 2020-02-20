package edu.duke.cs.ospreyservice

import edu.duke.cs.ospreyservice.services.AboutService
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.ContentNegotiation
import io.ktor.http.ContentType
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import io.ktor.server.engine.embeddedServer
import io.ktor.server.engine.stop
import io.ktor.server.netty.Netty
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.serializer
import org.slf4j.LoggerFactory
import java.nio.charset.Charset
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.NoSuchElementException
import kotlin.reflect.KClass


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

	val log = LoggerFactory.getLogger(OspreyService::class.java)

	private val service =
		embeddedServer(Netty, 8080) {

			install(ContentNegotiation) {
				serializationForServiceResponse()
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
				service("/about", AboutService::run)
			}
		}

	// register types for each service
	@UseExperimental(ImplicitReflectionSerializer::class)
	val serializationModule = SerializersModule {

		val registrar = ResponseRegistrar()

		// register built-in types
		registrar.addError<InternalError>()
		registrar.addError<RequestError>()

		// ask each service to register their responses and errors
		AboutService.registerResponses(registrar)

		polymorphic<ResponseInfo> {
			for (response in registrar.responses) {
				@Suppress("UNCHECKED_CAST")
				val c = response as KClass<ResponseInfo>
				addSubclass(c, c.serializer())
			}
		}
		polymorphic<ErrorInfo> {
			for (error in registrar.errors) {
				@Suppress("UNCHECKED_CAST")
				val c = error as KClass<ErrorInfo>
				addSubclass(c, c.serializer())
			}
		}
	}

	val json = Json(
		configuration = JsonConfiguration.Stable.copy(
			encodeDefaults = true,
			strictMode = false,
			unquoted = false,
			prettyPrint = false,
			useArrayPolymorphism = true
		),
		context = serializationModule
	)

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
