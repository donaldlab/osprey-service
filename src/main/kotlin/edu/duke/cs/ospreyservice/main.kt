package edu.duke.cs.ospreyservice

import java.nio.file.Paths


fun main() {
	val cwd = Paths.get(System.getProperty("user.dir"))
	OspreyService.start(cwd, wait = true)
}
