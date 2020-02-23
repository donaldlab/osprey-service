package edu.duke.cs.ospreyservice.services

import edu.duke.cs.ospreyservice.*
import edu.duke.cs.ospreyservice.amber.Antechamber
import edu.duke.cs.ospreyservice.amber.Leap
import kotlinx.serialization.Serializable


object BondsService {

	fun registerResponses(registrar: ResponseRegistrar) {
		registrar.addResponse<BondsResponse>()
		registrar.addError<BondsLeapError>()
		registrar.addError<BondsAntechamberError>()
	}

	fun run(instance: OspreyService.Instance, request: BondsRequest): ServiceResponse<BondsResponse> {

		// if we got a forcefield name, use LEaP
		val ffname = request.ffname
		if (ffname != null) {

			// run LEaP to infer all the missing atoms
			val results = Leap.run(
				instance.dir,
				filesToWrite = mapOf("in.pdb" to request.pdb),
				commands = """
					|verbosity 2
					|source leaprc.${Leap.sanitizeToken(ffname)}
					|mol = loadPDB in.pdb
					|saveMol2 mol out.mol2 0
				""".trimMargin(),
				filesToRead = listOf("out.mol2")
			)

			val mol2 = results.files["out.mol2"]
				?: return ServiceResponse.Failure(BondsLeapError(
					"LEaP didn't produce an output molecule",
					results.console.joinToString("\n")
				))

			return ServiceResponse.Success(BondsResponse(mol2))

		// we didn't get a forcefield, so use antechamber
		} else {

			val results = Antechamber.run(
				instance.dir,
				request.pdb,
				Antechamber.InType.Pdb,
				Antechamber.AtomTypes.SYBYL
			)

			val mol2 = results.mol2
				?: return ServiceResponse.Failure(BondsAntechamberError(
					"Antechamber didn't produce an output molecule",
					results.console.joinToString("\n")
				))

			return ServiceResponse.Success(BondsResponse(mol2))
		}
	}
}


@Serializable
data class BondsRequest(
	val pdb: String,
	val ffname: String?
)

@Serializable
data class BondsResponse(
	val mol2: String
) : ResponseInfo

@Serializable
data class BondsLeapError(
	override val msg: String,
	val leapLog: String
) : ErrorInfo {
	override fun message() = "$msg\n\nLEaP:\n$leapLog"
}

@Serializable
data class BondsAntechamberError(
	override val msg: String,
	val antechamberLog: String
) : ErrorInfo {
	override fun message() = "$msg\n\nAntechamber:\n$antechamberLog"
}
