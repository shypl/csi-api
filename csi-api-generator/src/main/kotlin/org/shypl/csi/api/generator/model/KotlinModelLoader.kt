package org.shypl.csi.api.generator.model

import org.reflections.Reflections
import org.reflections.util.ConfigurationBuilder
import java.io.Closeable
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.declaredMemberFunctions
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.full.superclasses
import kotlin.reflect.full.valueParameters
import kotlin.reflect.jvm.jvmErasure

class KotlinModelLoader(
	pkg: String,
	private val model: Model,
) {
	private val reflections = Reflections(ConfigurationBuilder().forPackage(""))
	private val loadedClasses = HashSet<ClassName>()
	
	init {
		val classLoader = javaClass.classLoader
		loadApi(model.client, classLoader.loadClass("${pkg}.ClientApi").kotlin)
		loadApi(model.server, classLoader.loadClass("${pkg}.ServerApi").kotlin)
		model.removeUnusedClasses(loadedClasses)
	}
	
	private fun loadApi(api: Api, clazz: KClass<*>) {
		api.name = clazz.toClassName()
		val services = mutableListOf<String>()
		for (property in clazz.declaredMemberProperties) {
			val descriptor = provideServiceDescriptor(property.returnType.jvmErasure)
			val service = api.provideService(property.name, descriptor)
			services.add(service.name)
		}
		api.removeUnusedServices(services)
	}
	
	private fun provideServiceDescriptor(clazz: KClass<*>): ClassName {
		val name = clazz.toClassName()
		if (!loadedClasses.contains(name)) {
			loadedClasses.add(name)
			
			val service = model.provideServiceDescriptor(name, clazz.isSubclassOf(Closeable::class))
			val methods = mutableListOf<String>()
			
			for (function in clazz.declaredMemberFunctions) {
				val arguments = function.valueParameters.map(::extractMethodArgument)
				
				val returnClassifier = function.returnType.classifier as KClass<*>
				val returnCloseable = returnClassifier.isSubclassOf(Closeable::class)
				
				if (arguments.any { it is Method.Argument.Subscription }) {
					check(returnCloseable) { "Method '${function.name}' require return Closeable" }
				}
				
				val result = if (returnCloseable) {
					if (returnClassifier == Closeable::class) {
						Method.Result.Subscription
					}
					else {
						Method.Result.Service(provideServiceDescriptor(returnClassifier))
					}
				}
				else if (returnClassifier == Unit::class) {
					null
				}
				else {
					Method.Result.Value(extractType(function.returnType))
				}
				
				if (result != null) {
					require(function.isSuspend) { "Method mast by suspend when it has result (${service.name.full}:${function.name})" }
				}
				
				service.provideMethod(
					function.name,
					function.isSuspend,
					arguments,
					result
				)
				
				methods.add(function.name)
			}
			
			service.removeUnusedMethods(methods)
		}
		
		return name
	}
	
	private fun extractMethodArgument(parameter: KParameter): Method.Argument {
		val name = requireNotNull(parameter.name)
		val classifier = parameter.type.classifier
		if (classifier is KClass<*> && classifier.java.packageName == "kotlin.jvm.functions") {
			require(requireNotNull(parameter.type.arguments.last().type).classifier == Unit::class)
			
			val parameters = parameter.type.arguments.dropLast(1).map {
				val type = it.type
				Method.Argument.Subscription.Parameter(
					(requireNotNull(type).annotations[0] as ParameterName).name,
					extractType(type)
				)
			}
			
			return Method.Argument.Subscription(name, parameters)
		}
		return Method.Argument.Value(name, extractType(parameter.type))
	}
	
	private fun extractType(type: KType): Type {
		val t = when (val clazz = type.jvmErasure) {
			Boolean::class      -> Type.Primitive.BOOLEAN
			Byte::class         -> Type.Primitive.BYTE
			Int::class          -> Type.Primitive.INT
			Long::class         -> Type.Primitive.LONG
			Double::class       -> Type.Primitive.DOUBLE
			String::class       -> Type.Primitive.STRING
			BooleanArray::class -> Type.Primitive.BOOLEAN_ARRAY
			ByteArray::class    -> Type.Primitive.BYTE_ARRAY
			IntArray::class     -> Type.Primitive.INT_ARRAY
			LongArray::class    -> Type.Primitive.LONG_ARRAY
			DoubleArray::class  -> Type.Primitive.DOUBLE_ARRAY
			
			List::class         -> model.getListType(extractType(requireNotNull(type.arguments[0].type)))
			
			Map::class          -> model.getMapType(
				extractType(requireNotNull(type.arguments[0].type)),
				extractType(requireNotNull(type.arguments[1].type))
			)
			
			else                -> model.getEntityType(loadEntity(clazz))
		}
		
		if (type.isMarkedNullable) {
			return model.getNullableType(t)
		}
		
		return t
	}
	
	private fun loadEntity(clazz: KClass<*>): ClassName {
		val name = clazz.toClassName()
		if (!loadedClasses.contains(name)) {
			loadedClasses.add(name)
			
			if (clazz.isSubclassOf(Enum::class)) {
				@Suppress("UNCHECKED_CAST")
				loadEnumEntity(name, clazz as KClass<Enum<*>>)
			}
			else if (clazz.objectInstance != null) {
				loadConstantEntity(name, clazz)
			}
			else {
				loadStructureEntity(name, clazz)
			}
			
		}
		return name
	}
	
	private fun loadEnumEntity(name: ClassName, clazz: KClass<Enum<*>>) {
		model.provideEnumEntity(name, clazz.java.enumConstants.map { it.name })
	}
	
	private fun loadConstantEntity(name: ClassName, clazz: KClass<*>) {
		model.provideConstantEntity(name, getEntityParent(clazz))
	}
	
	private fun loadStructureEntity(name: ClassName, clazz: KClass<*>) {
		val parent = getEntityParent(clazz)
		val abstract = clazz.isAbstract || clazz.isSealed
		val sealed = clazz.isSealed
		
		val properties = clazz.declaredMemberProperties.map { it.name }
		
		val fields = checkNotNull(clazz.primaryConstructor).valueParameters
			.filter { it.name in properties }
			.map {
				StructureEntity.Field(
					checkNotNull(it.name),
					extractType(it.type)
				)
			}
		
		val children = reflections.getSubTypesOf(clazz.java)
			.filter { it.superclass == clazz.java }
			.map { loadEntity(it.kotlin) }
		
		model.provideStructureEntity(name, parent, abstract, sealed, fields, children)
	}
	
	private fun getEntityParent(clazz: KClass<*>): ClassName? {
		val superclasses = clazz.superclasses
		require(superclasses.size == 1)
		val parentClass = superclasses[0]
		
		return if (parentClass == Any::class) null
		else loadEntity(parentClass)
	}
	
	
	private fun KClass<*>.toClassName(): ClassName {
		val packageName = java.packageName
		val qualifiedName = requireNotNull(qualifiedName)
		return model.getClassName(packageName, qualifiedName.substringAfter("$packageName."))
	}
}