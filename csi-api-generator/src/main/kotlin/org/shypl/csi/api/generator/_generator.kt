package org.shypl.csi.api.generator

import org.shypl.csi.api.generator.model.KotlinModelLoader
import org.shypl.csi.api.generator.model.Model
import org.shypl.csi.api.generator.model.ModelStorage
import java.io.File

fun generateCsiApi(sourcePackage: String, storageFile: String, configuration: GeneratorConfiguration.() -> Unit) {
	val config = GeneratorConfigurationImpl(sourcePackage)
	config.configuration()
	
	val model = Model()
	val storage = ModelStorage(model, File(storageFile))
	
	storage.load()
	KotlinModelLoader(sourcePackage, model)
	model.versioning()
	storage.save()
	
	config.targets.forEach { it.generate(model) }
}

