package edu.duke.cs.ospreyservice

import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.request.receive
import io.ktor.response.respond
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.Serializable


object HelloService {

	val route: suspend PipelineContext<Unit,ApplicationCall>.(Unit) -> Unit = {
		val request = call.receive<HelloRequest>()
		call.respond(hello(request.name))
	}

	fun hello(name: String) = HelloResponse(OspreyService.name, OspreyService.version, "Hello, $name")
}

@Serializable
data class HelloRequest(
	val name: String
)

@Serializable
data class HelloResponse(
	val name: String,
	val version: String,
	val msg: String
)
