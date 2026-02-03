package org.shypl.csi.api.generator.model

class ModelUniform {
	var version = Version()
	var lastEntityId: Int = 0
	val client: Api = Api()
	val server: Api = Api()
	val serviceDescriptors = mutableListOf<ServiceDescriptor>()
	val entities: Entities = Entities()
	
	class Api {
		var lastServiceId: Int = 0
		val services: MutableList<Service> = mutableListOf()
		
		class Service(val id: Int, val name: String, val descriptor: String)
	}
	
	class ServiceDescriptor(
		val name: String,
		val lastMethodId: Int,
		val closeable: Boolean,
		val methods: List<Method>,
	) {
		
		class Method(val id: Int, val name: String, val suspend: Boolean, val arguments: List<Argument>, val result: String?) {
			class Argument(
				val name: String,
				val type: String?,
				val parameters: List<Parameter>?,
			)
		}
	}
	
	class Parameter(val name: String?, val type: String)
	
	class Entities(
		val enums: MutableList<EnumEntity> = mutableListOf(),
		val structures: MutableList<StructureEntity> = mutableListOf(),
		val constant: MutableList<ConstantEntity> = mutableListOf(),
	) {
		
		class EnumEntity(
			val name: String,
			val values: List<String>,
		)
		
		class StructureEntity(
			val id: Int,
			val name: String,
			val parent: String? = null,
			val abstract: Boolean = false,
			val sealed: Boolean = false,
			val fields: List<Field>,
			val children: List<String>
		) {
			class Field(
				val name: String,
				val type: String,
				val const: Boolean
			)
		}
		
		class ConstantEntity(
			val id: Int,
			val name: String,
			val parent: String? = null,
		)
	}
}