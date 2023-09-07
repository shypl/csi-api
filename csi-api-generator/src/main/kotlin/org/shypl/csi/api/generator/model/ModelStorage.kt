package org.shypl.csi.api.generator.model

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import java.io.File

class ModelStorage(
	private val model: Model,
	private val file: File,
) {
	
	private val mapper = ObjectMapper(YAMLFactory().disable(YAMLGenerator.Feature.WRITE_DOC_START_MARKER)).registerKotlinModule()
	
	fun save() {
		val uniform = ModelUniform()
		
		saveApi(model.client, uniform.client)
		saveApi(model.server, uniform.server)
		saveServiceDescriptors(uniform)
		saveEntities(uniform)
		
		uniform.version.current = model.version.current
		uniform.version.compatible = model.version.compatible
		uniform.lastEntityId = model.lastEntityId
		
		mapper.writeValue(file, uniform)
	}
	
	private fun saveApi(api: Api, uniform: ModelUniform.Api) {
		api.services.forEach {
			uniform.services.add(
				ModelUniform.Api.Service(
					it.id,
					it.name,
					it.descriptor.full
				)
			)
		}
		
		uniform.lastServiceId = api.lastServiceId
	}
	
	private fun saveServiceDescriptors(uniform: ModelUniform) {
		model.services.forEach { service ->
			uniform.serviceDescriptors.add(ModelUniform.ServiceDescriptor(
				service.name.full,
				service.lastMethodId,
				service.closeable,
				service.methods.map {
					ModelUniform.ServiceDescriptor.Method(
						it.id,
						it.name,
						it.suspend,
						it.arguments.map { a ->
							when (a) {
								is Method.Argument.Value        -> ModelUniform.ServiceDescriptor.Method.Argument(a.name, a.type.name, null)
								is Method.Argument.Subscription -> ModelUniform.ServiceDescriptor.Method.Argument(a.name, null, a.parameters.map { p ->
									ModelUniform.Parameter(p.name, p.type.name)
								})
							}
						},
						it.result.let { r ->
							when (r) {
								null                       -> null
								is Method.Result.Value     -> r.type.name
								is Method.Result.Service   -> "@" + r.descriptor.full
								Method.Result.Subscription -> "@"
							}
						}
					)
				}
			))
		}
	}
	
	private fun saveEntities(uniform: ModelUniform) {
		val visitor = object : EntityVisitor<Unit, ModelUniform> {
			override fun visitEnumEntity(entity: EnumEntity, data: ModelUniform) {
				data.entities.enums.add(ModelUniform.Entities.EnumEntity(entity.name.full, entity.values))
			}
			
			override fun visitStructureEntity(entity: StructureEntity, data: ModelUniform) {
				data.entities.structures.add(ModelUniform.Entities.StructureEntity(
					entity.id,
					entity.name.full,
					entity.parent?.full,
					entity.abstract,
					entity.sealed,
					entity.fields.map { ModelUniform.Entities.StructureEntity.Field(it.name, it.type.name) },
					entity.children.map { it.full }
				))
			}
			
			override fun visitConstantEntity(entity: ConstantEntity, data: ModelUniform) {
				data.entities.constant.add(
					ModelUniform.Entities.ConstantEntity(
						entity.id,
						entity.name.full,
						entity.parent?.full,
					)
				)
			}
		}
		
		model.entities
			.sortedBy { it.name.full }
			.forEach { it.accept(visitor, uniform) }
	}
	
	//
	
	fun load() {
		val uniform = if (file.exists()) mapper.readValue(file, ModelUniform::class.java) else ModelUniform()
		
		loadApi(model.client, uniform.client)
		loadApi(model.server, uniform.server)
		loadServiceDescriptors(uniform)
		loadEntities(uniform)
		
		model.version.current = uniform.version.current
		model.version.compatible = uniform.version.compatible
		
		model.commitRecover(uniform.lastEntityId)
	}
	
	private fun loadApi(api: Api, uniform: ModelUniform.Api) {
		uniform.services.forEach {
			api.recoverService(it.id, it.name, it.descriptor.toClassName())
		}
		
		api.commitRecover(uniform.lastServiceId)
	}
	
	private fun loadServiceDescriptors(uniform: ModelUniform) {
		uniform.serviceDescriptors.forEach {
			val service = model.recoverServiceDescriptor(it.name.toClassName(), it.closeable)
			
			it.methods.forEach { m ->
				service.recoverMethod(m.id, m.name,
					m.suspend,
					m.arguments.map { a ->
						if (a.type != null)
							Method.Argument.Value(a.name, a.type.toType())
						else
							Method.Argument.Subscription(a.name, a.parameters!!.map { p ->
								Method.Argument.Subscription.Parameter(p.name, p.type.toType())
							})
					},
					m.result?.let { r ->
						when {
							r == "@"          -> Method.Result.Subscription
							r.startsWith('@') -> Method.Result.Service(r.substring(1).toClassName())
							else              -> Method.Result.Value(r.toType())
						}
					}
				)
			}
			
			service.commitRecover(it.lastMethodId)
		}
	}
	
	private fun loadEntities(uniform: ModelUniform) {
		uniform.entities.enums.forEach {
			model.recoverEnumEntity(it.name.toClassName(), it.values)
		}
		
		uniform.entities.structures.forEach { e ->
			model.recoverStructureEntity(
				e.id,
				e.name.toClassName(),
				e.parent?.toClassName(),
				e.abstract,
				e.sealed,
				e.fields.map {
					StructureEntity.Field(
						it.name,
						it.type.toType()
					)
				},
				e.children.map { it.toClassName() }
			)
		}
		
		uniform.entities.constant.forEach {
			model.recoverConstantEntity(it.id, it.name.toClassName(), it.parent?.toClassName())
		}
	}
	
	
	private val primitiveTypeValues = Type.Primitive.entries.associateBy { it.name }
	
	private fun String.toType(): Type {
		return when {
			this in primitiveTypeValues.keys -> primitiveTypeValues.getValue(this)
			endsWith('?')                    -> model.getNullableType(substring(0, length - 1).toType())
			startsWith("List<")              -> model.getListType(substring(5, length - 1).toType())
			startsWith("Map<")               -> indexOf(',').let {
				model.getMapType(
					substring(4, it).toType(),
					substring(it + 1, length - 1).toType()
				)
			}
			
			else                             -> model.getEntityType(toClassName())
		}
	}
	
	private fun String.toClassName(): ClassName {
		return model.getClassName(substringBefore('/'), substringAfter('/'))
	}
}