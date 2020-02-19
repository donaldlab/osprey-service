package edu.duke.cs.ospreyservice.services

import edu.duke.cs.ospreyservice.OspreyService
import io.ktor.application.ApplicationCall
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.util.pipeline.PipelineContext
import kotlinx.serialization.Serializable


object AboutService {

	val route: suspend PipelineContext<Unit, ApplicationCall>.(Unit) -> Unit = {
		call.respond(AboutResponse(OspreyService.name, OspreyService.version))
	}
}

@Serializable
data class AboutResponse(
	val name: String,
	val version: String
)
