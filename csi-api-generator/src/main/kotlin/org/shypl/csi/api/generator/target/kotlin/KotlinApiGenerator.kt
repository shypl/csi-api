@file:Suppress("DuplicatedCode")

package org.shypl.csi.api.generator.target.kotlin

import org.shypl.csi.api.generator.code.Code
import org.shypl.csi.api.generator.code.DependedCode
import org.shypl.csi.api.generator.model.*
import org.shypl.csi.api.generator.target.LogCallVisitorData
import org.shypl.csi.api.generator.target.TypeAggregator
import org.shypl.csi.api.generator.target.TypeCollector

abstract class KotlinApiGenerator(
	protected val model: Model,
	protected val coders: KotlinCodersGenerator,
	private val targetPackage: ClassPackage,
	private val side: String,
) {
	private val sidePackage = targetPackage.getChild(side)
	
	abstract fun generate(codeSource: KotlinCodeStorage)
	
	protected abstract fun generateApiAdapterDeclaration(code: Code, iaName: String, oaName: String): Code
	
	protected fun generate(innerApi: Api, outerApi: Api, codeSource: KotlinCodeStorage) {
		val loggers = TypeAggregator()
		
		generateApiVersion(codeSource)
		generateInnerApi(codeSource, innerApi)
		generateOuterApi(codeSource, outerApi)
		generateOuterApiImpl(codeSource, outerApi)
		generateApiAdapter(codeSource, innerApi, outerApi)
		generateApiConnection(codeSource, innerApi)
		
		innerApi.services
			.fold(hashSetOf<ServiceDescriptor>()) { a, it -> collectServiceDescriptors(a, it.className) }
			.forEach { s ->
				s.methods.forEach { m -> if (m.arguments.any { it is Method.Argument.Subscription }) generateOuterSubscription(codeSource, s, m, loggers) }
				generateInnerService(codeSource, s, loggers)
			}
		
		outerApi.services
			.fold(hashSetOf<ServiceDescriptor>()) { a, it -> collectServiceDescriptors(a, it.className) }
			.forEach { s ->
				s.methods.forEach { m -> if (m.arguments.any { it is Method.Argument.Subscription }) generateInnerSubscription(codeSource, s, m, loggers) }
				generateOuterService(codeSource, s, loggers)
			}
		
		if (loggers.hasNext()) {
			generateLogging(codeSource, loggers)
		}
	}
	
	private fun generateApiVersion(codeSource: KotlinCodeStorage) {
		codeSource.newFile(targetPackage.getName("_version")).apply {
			body.line("const val API_VERSION = ${model.version.current}")
		}
	}
	
	private fun collectServiceDescriptors(target: HashSet<ServiceDescriptor>, name: ClassName): HashSet<ServiceDescriptor> {
		val descriptor = model.getServiceDescriptor(name)
		if (target.add(descriptor)) {
			descriptor.methods.forEach { m ->
				(m.result as? Method.Result.Service)?.also { collectServiceDescriptors(target, it.className) }
			}
		}
		return target
	}
	
	private fun generateInnerApi(codeSource: KotlinCodeStorage, api: Api) {
		val name = "Internal${api.name.fullValue}"
		
		codeSource.newFile(sidePackage.getName(name)).apply {
			addDependency("org.shypl.csi.api.$side/InnerApi")
			addDependency(api.name)
			
			body.line("interface $name : ${api.name.value}, InnerApi")
		}
	}
	
	private fun generateOuterApi(codeSource: KotlinCodeStorage, api: Api) {
		val name = "Internal${api.name.fullValue}"
		
		codeSource.newFile(sidePackage.getName(name)).apply {
			addDependency("org.shypl.csi.api/OuterApi")
			addDependency(api.name)
			
			body.line("interface $name : ${api.name.value}, OuterApi")
		}
	}
	
	private fun generateOuterApiImpl(codeSource: KotlinCodeStorage, api: Api) {
		val name = "Internal${api.name.fullValue}Impl"
		
		codeSource.newFile(sidePackage.getName(name)).apply {
			addDependency("org.shypl.csi.api/AbstractOuterApi")
			addDependency("org.shypl.csi.api/Context")
			
			body.apply {
				line("internal class $name(")
				ident {
					line("context: Context")
				}
				identBracketsCurly("): AbstractOuterApi(context.connection), Internal${api.name.fullValue} ") {
					api.services.sortedBy(Service::id).forEach {
						addDependency(it.className)
						line("override val ${it.name}: ${it.className.value} = ${it.className.value}Outer(context, false, ${it.id}, \"${it.name}\")")
					}
				}
			}
		}
	}
	
	private fun generateApiAdapter(codeSource: KotlinCodeStorage, innerApi: Api, outerApi: Api) {
		codeSource.newFile(sidePackage.getName("ApiAdapter")).apply {
			addDependency("kotlinx.coroutines/CoroutineScope")
			addDependency("org.shypl.csi.api/CallbacksRegistry")
			addDependency("org.shypl.csi.api/Context")
			addDependency("org.shypl.csi.api.$side/AbstractApiAdapter")
			addDependency("org.shypl.csi.api.$side/ApiSluice")
			addDependency("org.shypl.csi.core.$side/ConnectionHandler")
			addDependency("org.shypl.tool.io/ByteBuffer")
			addDependency("org.shypl.tool.utils.pool/ObjectPool")
			
			val iaName = "Internal" + innerApi.name.fullValue
			val oaName = "Internal" + outerApi.name.fullValue
			
			generateApiAdapterDeclaration(body, iaName, oaName).apply {
				line()
				identBracketsCurly("override fun getLoggerName(): String ") {
					line("return \"$sidePackage\"")
				}
				
				line()
				identBracketsCurly("override fun createConnectionHandler(context: Context, api: $iaName): ConnectionHandler ") {
					line("return ApiConnection(context, api)")
				}
				
				line()
				identBracketsCurly("override fun createOuterApi(context: Context): $oaName ") {
					line("return ${oaName}Impl(context)")
				}
				
				line()
				identBracketsCurly("override fun provideCallbacksRegistry(): CallbacksRegistry ") {
					if (outerApi.services.any { s -> model.getServiceDescriptor(s.className).methods.any { it.result != null } }) {
						addDependency("org.shypl.csi.api/RealCallbacksRegistry")
						line("return RealCallbacksRegistry()")
					}
					else {
						addDependency("org.shypl.csi.api/NothingCallbacksRegistry")
						line("return NothingCallbacksRegistry")
					}
				}
			}
			
		}
	}
	
	private fun generateApiConnection(codeSource: KotlinCodeStorage, api: Api) {
		val iaInternalName = "Internal${api.name.fullValue}"
		
		codeSource.newFile(sidePackage.getName("ApiConnection")).apply {
			addDependency("org.shypl.csi.api/Context")
			addDependency("org.shypl.csi.api/InnerServiceDelegate")
			addDependency("org.shypl.csi.api.$side/AbstractApiConnection")
			
			body.apply {
				line("internal class ApiConnection(")
				ident {
					line("context: Context,")
					line("api: $iaInternalName")
				}
				identBracketsCurly(") : AbstractApiConnection<$iaInternalName>(context, api) ") {
					line()
					val services = api.services.sortedBy(Service::id)
					
					if (services.isEmpty()) {
						identBracketsCurly("override fun findService(serviceId: Int): InnerServiceDelegate<*>? ") {
							line("return null")
						}
					}
					else {
						services.forEach {
							line("private val ${it.name}Delegate = ${it.className.toString('_')}InnerDelegate(context, api.${it.name}, \"${it.name}\")")
						}
						
						line()
						identBracketsCurly("override fun findService(serviceId: Int): InnerServiceDelegate<*>? ") {
							identBracketsCurly("return when (serviceId) ") {
								services.forEach {
									line("${it.id} -> ${it.name}Delegate")
								}
								line("else -> null")
							}
						}
					}
				}
			}
		}
	}
	
	private fun generateInnerService(codeSource: KotlinCodeStorage, serviceDescriptor: ServiceDescriptor, loggers: TypeCollector) {
		val name = "${serviceDescriptor.name.fullValue}InnerDelegate"
		
		codeSource.newFile(sidePackage.getName(name)).apply {
			addDependency("org.shypl.csi.api/Context")
			addDependency("org.shypl.tool.biser/BiserReader")
			addDependency(serviceDescriptor.name)
			
			body.apply {
				line("@Suppress(\"FunctionName\", \"UNUSED_PARAMETER\")")
				line("internal class $name(")
				ident {
					line("context: Context,")
					line("service: ${serviceDescriptor.name.toString('.')},")
					line("name: String")
				}
				
				val delegate = if (serviceDescriptor.closeable) "InnerInstanceServiceDelegate" else "InnerServiceDelegate"
				
				addDependency("org.shypl.csi.api/$delegate")
				
				identBracketsCurly(") : $delegate<${serviceDescriptor.name.toString('.')}>(context, service, name) ") {
					
					line()
					
					identBracketsCurly("override fun callMethod(methodId: Int, message: BiserReader): Boolean ") {
						identBracketsCurly("when (methodId) ") {
							serviceDescriptor.methods.sortedBy(Method::id).forEach {
								line("${it.id} -> call_${it.name}(message)")
							}
							line("else -> return false")
						}
						line("return true")
					}
					
					serviceDescriptor.methods.sortedBy(Method::id).forEach { m ->
						line()
						identBracketsCurly("private fun call_${m.name}(message: BiserReader) ") {
							val result = m.result
							val arguments = m.arguments
							val hasArguments = arguments.isNotEmpty()
							if (result != null) {
								line("val c = message." + coders.provideReadCall(this, Type.Primitive.INT))
							}
							arguments.forEachIndexed { i, a ->
								if (a is Method.Argument.Value) {
									line("val a$i = message." + coders.provideReadCall(this, a.type))
								}
							}
							
							line {
								append("logMethodCall(\"${m.name}\"")
								if (result != null) {
									append(", c")
								}
								if (hasArguments) append(") {")
								else append(") {}")
							}
							if (hasArguments) {
								ident {
									val l = arguments.filterIsInstance<Method.Argument.Value>().size - 1
									var q = 0
									arguments.forEachIndexed { i, a ->
										if (a is Method.Argument.Value) logCall(loggers, q++ == l, a.type, a.name, "a$i")
									}
								}
								line("}")
							}
							
							(if (m.suspend) identBracketsCurly("launchCoroutine ") else this).apply {
								
								when (result) {
									null                       -> {
										line {
											append("service.${m.name}(")
											arguments.indices.joinTo(this) { "a$it" }
											append(')')
										}
									}
									
									is Method.Result.Value     -> {
										line {
											append("val r = service.${m.name}(")
											arguments.indices.joinTo(this) { "a$it" }
											append(")")
										}
										identBracketsCurly("logMethodResponse(\"${m.name}\", c) ") {
											logCall(loggers, true, result.type, null, "r")
										}
										identBracketsCurly("sendMethodResponse(c) ") {
											line(coders.provideWriteCall(this, result.type, "r"))
										}
									}
									
									is Method.Result.Service   -> {
										val hasSubscription = arguments.any { it is Method.Argument.Subscription }
										if (hasSubscription) {
											line("val ss = ${serviceDescriptor.name.fullValue}_${m.name}_OuterSubscription(context, this@${name})")
										}
										line {
											append("val i = service.${m.name}(")
											arguments.forEachIndexed { i, a ->
												when (a) {
													is Method.Argument.Value        -> append("a$i")
													is Method.Argument.Subscription -> append("ss.${a.name}")
												}
												if (i != arguments.lastIndex) append(", ")
											}
											append(")")
										}
										line("val s = registerInstanceService(${result.className.fullValue}InnerDelegate(context, i, \"\$name.${m.name}\"))")
										if (hasSubscription) {
											addDependency("org.shypl.tool.utils/DummyCloseable")
											line("val ssi = registerSubscription(ss, DummyCloseable)")
											
											line("logInstanceServiceResponse(\"${m.name}\", c, s, ssi) ")
											line("sendInstanceServiceResponse(c, s, ssi) ")
											line("ss.ready()")
										}
										else {
											line("logInstanceServiceResponse(\"${m.name}\", c, s) ")
											line("sendInstanceServiceResponse(c, s) ")
										}
									}
									
									Method.Result.Subscription -> {
										line("val s = ${serviceDescriptor.name.fullValue}_${m.name}_OuterSubscription(context, this@${name})")
										line {
											append("val e = service.${m.name}(")
											arguments.forEachIndexed { i, a ->
												when (a) {
													is Method.Argument.Value        -> append("a$i")
													is Method.Argument.Subscription -> append("s.${a.name}")
												}
												if (i != arguments.lastIndex) append(", ")
											}
											append(")")
										}
										line("val i = registerSubscription(s, e)")
										line("logSubscriptionResponse(\"${m.name}\", c, i) ")
										line("sendSubscriptionResponse(c, i) ")
										line("s.ready()")
									}
								}
							}
							
						}
					}
					
				}
			}
		}
	}
	
	private fun generateOuterService(codeSource: KotlinCodeStorage, descriptor: ServiceDescriptor, loggers: TypeCollector) {
		val name = descriptor.name.fullValue
		
		codeSource.newFile(sidePackage.getName("${name}Outer")).apply {
			addDependency(descriptor.name)
			addDependency("org.shypl.csi.api/Context")
			
			body.apply {
				line("@Suppress(\"LocalVariableName\")")
				line("internal class ${name}Outer(")
				ident {
					line("context: Context,")
					line("instance: Boolean,")
					line("serviceId: Int,")
					line("serviceName: String")
				}
				
				val parent = if (descriptor.closeable) {
					addDependency("org.shypl.csi.api/OuterInstanceService")
					"OuterInstanceService"
				}
				else {
					addDependency("org.shypl.csi.api/OuterService")
					"OuterService"
				}
				
				identBracketsCurly(") : $parent(context, instance, serviceId, serviceName), $name ") {
					descriptor.methods.sortedBy(Method::id).forEach { m ->
						
						line()
						line {
							append("override ${if (m.suspend) "suspend " else ""}fun ${m.name}(")
							m.arguments.joinTo(this) { getArgumentDeclaration(this@identBracketsCurly, it) }
							append(")")
							m.result?.also {
								append(": ")
								when (it) {
									is Method.Result.Value     -> append(coders.getTypeName(it.type, this@identBracketsCurly))
									is Method.Result.Service   -> {
										addDependency(it.className)
										append(it.className.value)
									}
									
									Method.Result.Subscription -> {
										addDependency("java.io/Closeable")
										append("Closeable")
									}
								}
							}
							append(" {")
						}
						
						ident {
							line("_checkClosed()")
							
							val r = m.result
							
							(if (r != null) {
								addDependency("kotlin.coroutines/suspendCoroutine")
								addDependency("kotlin.coroutines/resume")
								line("return suspendCoroutine { _continuation ->")
								ident()
							}
							else this).apply {
								
								if (r != null) {
									line("val _callback = _registerCallback { _callbackLocal -> ")
									ident {
										when (r) {
											is Method.Result.Value     -> {
												line("val _result = " + coders.provideReadCall(this, r.type))
												identBracketsCurly("_logMethodResponse(\"${m.name}\", _callbackLocal) ") {
													logCall(loggers, true, r.type, null, "_result")
												}
												line("_continuation.resume(_result)")
											}
											
											is Method.Result.Service   -> {
												val hasSubscription = m.arguments.any { it is Method.Argument.Subscription }
												addDependency(r.className)
												line("val _service = " + coders.provideReadCall(this, Type.Primitive.INT))
												
												if (hasSubscription) {
													line("val _subscription = " + coders.provideReadCall(this, Type.Primitive.INT))
													line("_logInstanceOpen(\"${m.name}\", _callbackLocal, _service, _subscription) ")
												}
												else {
													line("_logInstanceOpen(\"${m.name}\", _callbackLocal, _service) ")
												}
												
												line("val _si = ${r.className.fullValue}Outer(_context, true, _service, \"\$_name.${m.name}[+\$_service]\")")
												
												if (hasSubscription) {
													line {
														append("val t = ${name}_${m.name}_InnerSubscription(_context, this@${name}Outer, _subscription, ")
														m.arguments.filterIsInstance<Method.Argument.Subscription>().joinTo(this) { it.name }
														append(")")
													}
													line("_si.service._registerSubscription(t)")
												}
												line("_continuation.resume(_si)")
												
											}
											
											Method.Result.Subscription -> {
												line("val _subscription = " + coders.provideReadCall(this, Type.Primitive.INT))
												line("_logSubscriptionBegin(\"${m.name}\", _callbackLocal, _subscription) ")
												line {
													append("val t = ${name}_${m.name}_InnerSubscription(_context, this@${name}Outer, _subscription, ")
													m.arguments.filterIsInstance<Method.Argument.Subscription>().joinTo(this) { it.name }
													append(")")
												}
												line("_registerSubscription(t)")
												line("_continuation.resume(t)")
											}
										}
									}
									line("}")
								}
								
								val send: String
								val log: String
								
								if (r == null) {
									log = "_logMethodCall(\"${m.name}\") "
									send = "_callMethod(${m.id})"
								}
								else {
									log = "_logMethodCall(\"${m.name}\", _callback) "
									send = "_callMethod(${m.id}, _callback)"
								}
								
								val arguments = m.arguments.filterIsInstance<Method.Argument.Value>()
								
								identBracketsCurly(log) {
									arguments.forEachIndexed { i, a ->
										logCall(loggers, arguments.lastIndex == i, a.type, a.name)
									}
								}
								
								if (arguments.isEmpty()) {
									line(send)
								}
								else {
									identBracketsCurly("$send ") {
										arguments.forEach { a ->
											line(coders.provideWriteCall(this, a.type, a.name))
										}
									}
								}
							}
							if (r != null) line("}")
						}
						line("}")
					}
				}
			}
		}
	}
	
	private fun generateInnerSubscription(codeSource: KotlinCodeStorage, service: ServiceDescriptor, method: Method, loggers: TypeAggregator) {
		val name = "${service.name.fullValue}_${method.name}_InnerSubscription"
		val arguments = method.arguments.filterIsInstance<Method.Argument.Subscription>()
		codeSource.newFile(sidePackage.getName(name)).apply {
			addDependency("org.shypl.csi.api/Context")
			addDependency("org.shypl.csi.api/OuterService")
			addDependency("org.shypl.csi.api/InnerSubscription")
			addDependency("org.shypl.tool.biser/BiserReader")
			
			body.apply {
				line("@Suppress(\"ClassName\",\"RedundantSuppression\")")
				line("internal class $name(")
				ident {
					line("context: Context,")
					line("service: OuterService,")
					line("id: Int,")
					arguments.forEach {
						line("private val _" + getArgumentDeclaration(this, it) + ",")
					}
				}
				
				identBracketsCurly(") : InnerSubscription(context, service, \"${method.name}\", id) ") {
					identBracketsCurly("override fun call(argumentId: Int, message: BiserReader): Boolean ") {
						
						if (arguments.size == 1) {
							line("if (argumentId != 0) return false")
							val a = arguments.first()
							
							a.parameters.forEachIndexed { i, p ->
								line("val p$i = message." + coders.provideReadCall(this, p.type))
							}
							
							identBracketsCurly("logCall(\"${a.name}\") ") {
								a.parameters.forEachIndexed { i, p ->
									logCall(loggers, i == a.parameters.lastIndex, p.type, p.name, "p$i")
								}
							}
							
							line {
								append("_${a.name}(")
								a.parameters.indices.joinTo(this) { "p$it" }
								append(')')
							}
						}
						else {
							identBracketsCurly("when (argumentId) ") {
								arguments.forEachIndexed { q, a ->
									identBracketsCurly("$q -> ") {
										a.parameters.forEachIndexed { i, p ->
											line("val p$i = message." + coders.provideReadCall(this, p.type))
										}
										
										identBracketsCurly("logCall(\"${a.name}\") ") {
											a.parameters.forEachIndexed { i, p ->
												logCall(loggers, i == a.parameters.lastIndex, p.type, p.name, "p$i")
											}
										}
										
										line {
											append("_${a.name}(")
											a.parameters.indices.joinTo(this) { "p$it" }
											append(')')
										}
									}
								}
								line("else -> return false")
							}
						}
						line("return true")
					}
				}
			}
		}
	}
	
	private fun generateOuterSubscription(codeSource: KotlinCodeStorage, service: ServiceDescriptor, method: Method, loggers: TypeAggregator) {
		val name = "${service.name.fullValue}_${method.name}_OuterSubscription"
		val arguments = method.arguments.filterIsInstance<Method.Argument.Subscription>()
		codeSource.newFile(sidePackage.getName(name)).apply {
			addDependency("org.shypl.csi.api/Context")
			addDependency("org.shypl.csi.api/InnerServiceDelegate")
			addDependency("org.shypl.csi.api/OuterSubscription")
			
			body.apply {
				line("@Suppress(\"ClassName\",\"RedundantSuppression\")")
				line("internal class $name(")
				ident {
					line("context: Context,")
					line("service: InnerServiceDelegate<*>")
				}
				identBracketsCurly(") : OuterSubscription(context, service, \"${method.name}\") ") {
					
					arguments.forEachIndexed { id, a ->
						line()
						line {
							append("val ${a.name} = { ")
							if (a.parameters.isNotEmpty()) {
								a.parameters.forEachIndexed { i, p ->
									append("p").append(i).append(": ").append(coders.getTypeName(p.type, this@identBracketsCurly))
									if (a.parameters.lastIndex != i) append(", ")
								}
								append(" ->")
							}
						}
						ident {
							identBracketsCurly("if (_ready) ") {
								identBracketsCurly("logCall(\"${a.name}\") ") {
									a.parameters.forEachIndexed { i, p ->
										logCall(loggers, a.parameters.lastIndex == i, p.type, p.name, "p$i")
									}
								}
								if (a.parameters.isEmpty()) {
									line("call($id)")
								}
								else {
									identBracketsCurly("call($id) ") {
										a.parameters.forEachIndexed { i, p ->
											line(coders.provideWriteCall(this, p.type, "p$i"))
										}
									}
								}
							}
							identBracketsCurly("else delayCall ") {
								identBracketsCurly("logCall(\"${a.name}\") ") {
									a.parameters.forEachIndexed { i, p ->
										logCall(loggers, a.parameters.lastIndex == i, p.type, p.name, "p$i")
									}
								}
								if (a.parameters.isEmpty()) {
									line("call($id)")
								}
								else {
									identBracketsCurly("call($id) ") {
										a.parameters.forEachIndexed { i, p ->
											line(coders.provideWriteCall(this, p.type, "p$i"))
										}
									}
								}
							}
						}
						line("}")
					}
				}
			}
		}
	}
	
	
	private fun getArgumentDeclaration(depended: DependedCode, argument: Method.Argument): String {
		return StringBuilder().apply {
			append(argument.name).append(": ")
			
			when (argument) {
				is Method.Argument.Value        -> append(coders.getTypeName(argument.type, depended))
				is Method.Argument.Subscription -> {
					append("(")
					argument.parameters.forEachIndexed { i, p ->
						if (p.name != null) append(p.name).append(": ")
						append(coders.getTypeName(p.type, depended))
						if (argument.parameters.lastIndex != i) append(", ")
					}
					append(") -> Unit")
				}
			}
			
		}.toString()
	}
	
	private fun generateLogging(codeSource: KotlinCodeStorage, loggers: TypeAggregator) {
		codeSource.newFile(sidePackage.getName("_logging")).apply {
			body.apply {
				loggers.forEach { type ->
					val name = defineLogName(type)
					identBracketsCurly("internal val $name: StringBuilder.(${coders.getTypeName(type, this)}) -> Unit = ") {
						type.accept(loggingVisitor, GenerateLoggingVisitorData(this, loggers))
					}
					line()
				}
			}
		}
	}
	
	
	private fun Code.logCall(loggers: TypeCollector, last: Boolean, type: Type, argName: String?, valName: String = argName!!) {
		type.accept(logCallVisitor, LogCallVisitorData(loggers, this, argName, valName, !last))
	}
	
	private fun defineLogName(type: Type): String {
		return "log_" + type.accept(logNamesVisitor)
	}
	
	private val logCallVisitor = object : TypeVisitor<Unit, LogCallVisitorData> {
		
		private fun defineFn(data: LogCallVisitorData): String {
			return if (data.sep) {
				data.code.addDependency("org.shypl.csi.api/logS")
				"logS"
			}
			else {
				data.code.addDependency("org.shypl.csi.api/log")
				"log"
			}
		}
		
		private fun visitGenerated(type: Type, data: LogCallVisitorData) {
			val fn = defineFn(data)
			val logger = defineLogName(type)
			data.loggers.add(type)
			
			if (data.argName == null) {
				data.code.line("$fn(${data.argVal}, $logger)")
			}
			else {
				data.code.line("$fn(\"${data.argName}\", ${data.argVal}, $logger)")
			}
		}
		
		override fun visitPrimitiveType(type: Type.Primitive, data: LogCallVisitorData) {
			val fn = defineFn(data)
			if (data.argName == null) {
				data.code.line("$fn(${data.argVal})")
			}
			else {
				data.code.line("$fn(\"${data.argName}\", ${data.argVal})")
			}
		}
		
		override fun visitListType(type: Type.List, data: LogCallVisitorData) {
			visitGenerated(type.element, data)
		}
		
		override fun visitMutableListType(type: Type.MutableList, data: LogCallVisitorData) {
			visitGenerated(type.element, data)
		}
		
		override fun visitMapType(type: Type.Map, data: LogCallVisitorData) {
			visitGenerated(type, data)
		}
		
		override fun visitMutableMapType(type: Type.MutableMap, data: LogCallVisitorData) {
			visitGenerated(type, data)
		}
		
		override fun visitNullableType(type: Type.Nullable, data: LogCallVisitorData) {
			visitGenerated(type, data)
		}
		
		override fun visitEntityType(type: Type.Entity, data: LogCallVisitorData) {
			if (model.getEntity(type.className) is EnumEntity) {
				val fn = defineFn(data)
				if (data.argName == null) {
					data.code.line("$fn(${data.argVal}.name)")
				}
				else {
					data.code.line("$fn(\"${data.argName}\", ${data.argVal}.name)")
				}
			}
			else {
				visitGenerated(type, data)
			}
		}
	}
	
	private val logNamesVisitor = object : TypeVisitor<String, Unit> {
		override fun visitPrimitiveType(type: Type.Primitive, data: Unit): String {
			return type.name
		}
		
		override fun visitEntityType(type: Type.Entity, data: Unit): String {
			return "ENTITY_" + type.className.toStringFull('_')
		}
		
		override fun visitListType(type: Type.List, data: Unit): String {
			return "LIST_" + type.element.accept(this, data)
		}
		
		override fun visitMutableListType(type: Type.MutableList, data: Unit): String {
			return "MUTABLE_LIST_" + type.element.accept(this, data)
		}
		
		override fun visitMapType(type: Type.Map, data: Unit): String {
			return "MAP_" + type.key.accept(this, data) + "__" + type.value.accept(this, data)
		}
		
		override fun visitMutableMapType(type: Type.MutableMap, data: Unit): String {
			return "MUTABLE_MAP_" + type.key.accept(this, data) + "__" + type.value.accept(this, data)
		}
		
		override fun visitNullableType(type: Type.Nullable, data: Unit): String {
			return "NULLABLE_" + type.original.accept(this, data)
		}
	}
	
	private val loggingVisitor = object : TypeVisitor<Unit, GenerateLoggingVisitorData>, EntityVisitor<Unit, GenerateLoggingVisitorData> {
		override fun visitPrimitiveType(type: Type.Primitive, data: GenerateLoggingVisitorData) {
			data.code.addDependency("org.shypl.csi.api/log")
			data.code.line("log(it)")
		}
		
		override fun visitListType(type: Type.List, data: GenerateLoggingVisitorData) {
			data.code.addDependency("org.shypl.csi.api/log")
			data.loggers.add(type.element)
			data.code.line("log(it, ${defineLogName(type.element)})")
		}
		
		override fun visitMutableListType(type: Type.MutableList, data: GenerateLoggingVisitorData) {
			data.code.addDependency("org.shypl.csi.api/log")
			data.loggers.add(type.element)
			data.code.line("log(it, ${defineLogName(type.element)})")
		}
		
		override fun visitMapType(type: Type.Map, data: GenerateLoggingVisitorData) {
			data.code.addDependency("org.shypl.csi.api/log")
			data.loggers.add(type.key)
			data.loggers.add(type.value)
			data.code.line("log(it, ${defineLogName(type.key)}, ${defineLogName(type.value)})")
		}
		
		override fun visitMutableMapType(type: Type.MutableMap, data: GenerateLoggingVisitorData) {
			data.code.addDependency("org.shypl.csi.api/log")
			data.loggers.add(type.key)
			data.loggers.add(type.value)
			data.code.line("log(it, ${defineLogName(type.key)}, ${defineLogName(type.value)})")
		}
		
		override fun visitNullableType(type: Type.Nullable, data: GenerateLoggingVisitorData) {
			data.code.addDependency("org.shypl.csi.api/log")
			data.loggers.add(type.original)
			data.code.line("if (it == null) append(\"NULL\") else log(it, ${defineLogName(type.original)})")
		}
		
		override fun visitEntityType(type: Type.Entity, data: GenerateLoggingVisitorData) {
			model.getEntity(type.className).accept(this, data)
		}
		
		///
		
		override fun visitEnumEntity(entity: EnumEntity, data: GenerateLoggingVisitorData) {
			data.code.addDependency("org.shypl.csi.api/log")
			data.code.line("log(it.name)")
		}
		
		override fun visitStructureEntity(entity: StructureEntity, data: GenerateLoggingVisitorData) {
			data.code.apply {
				if (entity.children.isEmpty()) {
					visitClassEntity0(entity, data)
				}
				else {
					identBracketsCurly("when (it) ") {
						entity.children.forEach {
							val type = model.getEntityType(it)
							data.loggers.add(type)
							line("is ${coders.getTypeName(type, data.code)} -> ${defineLogName(type)}(it)")
						}
						if (entity.abstract) {
							line("else -> throw UnsupportedOperationException()")
						}
						else {
							identBracketsCurly("else -> ") {
								visitClassEntity0(entity, data)
							}
						}
					}
				}
			}
		}
		
		private fun Code.visitClassEntity0(entity: StructureEntity, data: GenerateLoggingVisitorData) {
			line("append('{')")
			val allFields = model.getStructureEntityAllFields(entity)
			allFields.forEachIndexed { i, f ->
				logCall(data.loggers, allFields.lastIndex == i, f.type, f.name, "it.${f.name}")
			}
			line("append('}')")
		}
		
		override fun visitConstantEntity(entity: ConstantEntity, data: GenerateLoggingVisitorData) {
			data.code.addDependency("org.shypl.csi.api/log")
			data.code.line("log(\"${entity.name.fullValue}\")")
		}
	}
	
	private class GenerateLoggingVisitorData(val code: Code, val loggers: TypeCollector)
}
