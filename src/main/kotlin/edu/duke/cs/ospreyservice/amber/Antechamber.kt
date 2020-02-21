package edu.duke.cs.ospreyservice.amber

import edu.duke.cs.ospreyservice.read
import edu.duke.cs.ospreyservice.stream
import edu.duke.cs.ospreyservice.tempFolder
import edu.duke.cs.ospreyservice.write
import java.io.IOException
import java.nio.file.Path
import java.nio.file.Paths


object Antechamber {

	// NOTE: antechamber calls out to the following programs:
	// sqm (or mopac or divcon), atomtype, am1bcc, bondtype, espgen, respgen, prepgen

	data class Results(
		val exitCode: Int,
		val console: List<String>,
		val mol2: String?,
		val sqm: SQM.Results?
	)

	enum class InType(val id: String) {
		Pdb("pdb"),
		Mol2("mol2")
	}

	enum class AtomTypes(val id: String) {
		Gaff("gaff"),
		Gaff2("gaff2"),
		Amber("amber"),
		BCC("bcc"),
		SYBYL("sybyl")
	}

	enum class ChargeMethod(val id: String) {
		RESP("resp"),
		AM1BCC("bcc"),
		CM1("cm1"),
		CM2("cm2"),
		ESP("esp"),
		Mulliken("mul"),
		Gasteiger("gas")
	}

	fun run(
		serviceDir: Path,
		inmol: String,
		inType: InType,
		atomTypes: AtomTypes,
		useACDoctor: Boolean = true,
		generateCharges: ChargeMethod? = null,
		netCharge: Int? = null,
		sqmOptions: SQM.Options = SQM.Options()
	): Results {

		val homePath = Paths.get("ambertools").toAbsolutePath()
		val antechamberPath = homePath.resolve("bin/antechamber").toAbsolutePath()

		tempFolder("antechamber") { cwd ->

			// write the input files
			inmol.write(cwd.resolve("mol.in"))

			val command = mutableListOf(
				antechamberPath.toString(),
				"-i", "mol.in",
				"-fi", inType.id,
				"-o", "mol.mol2",
				"-fo", "mol2",
				"-at", atomTypes.id,
				"-dr", if (useACDoctor) "y" else "n",
				"-ek", sqmOptions.toString()
			)
			if (generateCharges != null) {
				command += listOf("-c", generateCharges.id)
			}
			if (netCharge != null) {
				command += listOf("-nc", netCharge.toString())
			}

			// start antechamber
			val process = ProcessBuilder()
				.command(command)
				.apply {
					environment().apply {
						put("AMBERHOME", homePath.toString())
					}
				}
				.directory(cwd.toFile())
				.stream()
				.waitFor()

			// return the results
			return Results(
				process.exitCode,
				process.console.toList(),
				try {
					cwd.resolve("mol.mol2").read()
				} catch (ex: IOException) {
					null
				},
				// read results from SQM, if any
				try {
					SQM.Results(
						cwd.resolve("sqm.in").read(),
						cwd.resolve("sqm.out").read()
					)
				} catch (ex: IOException) {
					null
				}
			)
		}
	}

	class Exception(val msg: String, val pdb: String, val results: Results) : RuntimeException(StringBuilder().apply {
		append(msg)
		append("\n\n")
		append("Input:\n")
		append(pdb)
		append("\n\n")
		append("console:\n")
		results.console.forEach {
			append(it)
			append("\n")
		}
		if (results.sqm != null) {
			append("\n\n")
			append("SQM:\n")
			append(results.sqm)
		}
	}.toString())
}
