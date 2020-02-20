package edu.duke.cs.ospreyservice.services

import edu.duke.cs.ospreyservice.*
import kotlinx.serialization.Serializable


object AboutService {

	fun registerResponses(registrar: ResponseRegistrar) {
		registrar.addResponse<AboutResponse>()
	}

	fun run(): ServiceResponse<AboutResponse> =
		ServiceResponse.Success(AboutResponse(OspreyService.name, OspreyService.version))
}

@Serializable
data class AboutResponse(
	val name: String,
	val version: String
) : ResponseInfo
