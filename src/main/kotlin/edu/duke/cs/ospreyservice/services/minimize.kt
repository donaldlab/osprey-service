package edu.duke.cs.ospreyservice.services

import edu.duke.cs.ospreyservice.*
import edu.duke.cs.ospreyservice.amber.Sander
import kotlinx.serialization.Serializable


object MinimizeService {

	fun registerResponses(registrar: ResponseRegistrar) {
		registrar.addResponse<MinimizeResponse>()
		registrar.addError<MinimizeError>()
	}

	fun run(instance: OspreyService.Instance, request: MinimizeRequest): ServiceResponse<MinimizeResponse> {

		val commands = ArrayList<String>()

		// TODO: report progress info to the caller somehow
		val reportEveryCycles = 10

		commands += listOf(
			"imin=1", // do cartesian minimization
			"maxcyc=${request.numCycles}",
			"ntpr=$reportEveryCycles",
			"ntxo=1" // format the output coords in ASCII
		)

		// TODO: expose more options?
		// ntmin     minimization type

		if (request.restraintMask != null) {

			// alas, the restraint mask has a limited size
			if (request.restraintMask.length > Sander.maxRestraintMaskSize) {
				return ServiceResponse.Failure(RequestError(
					"Alas, the restraintmask for sander can only be ${Sander.maxRestraintMaskSize} characters." +
					" ${request.restraintMask.length} characters is too long."
				))
			}

			commands += listOf(
				"ntr=1", // turn on cartesian restraints
				"restraint_wt=${request.restraintWeight}",
				"restraintmask='${Sander.sanitizeRestraintMask(request.restraintMask)}'"
			)
		}

		commands += listOf(
			"igb=1" // use generalized borne solvation
		)
		// TODO: expose more solvation options?

		val results = Sander.run(
			instance.dir,
			request.top,
			request.crd,
			"""
				|Header
				|&cntrl
				|${commands.joinToString(",\n")}
				|/
			""".trimMargin()
		)

		val coords = results.coords
			?: return ServiceResponse.Failure(MinimizeError(
				"Sander didn't produce output coordinates",
				results.console.joinToString("\n")
			))

		return ServiceResponse.Success(MinimizeResponse(coords))
	}
}

@Serializable
data class MinimizeRequest(
	val top: String,
	val crd: String,
	val numCycles: Int,
	val restraintMask: String? = null,
	val restraintWeight: Double = 1.0
)

@Serializable
data class MinimizeResponse(
	val coords: List<Point3d>
) : ResponseInfo

@Serializable
data class MinimizeError(
	override val msg: String,
	val sanderLog: String
) : ErrorInfo {
	override fun message() = "$msg\n\nSander:\n$sanderLog"
}
