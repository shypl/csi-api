package org.shypl.csi.api.generator.target.typescript

import org.shypl.csi.api.generator.code.Code
import org.shypl.csi.api.generator.code.CodeStorage
import org.shypl.csi.api.generator.code.DependedCode
import org.shypl.csi.api.generator.model.*
import org.shypl.csi.api.generator.target.LogCallVisitorData
import org.shypl.csi.api.generator.target.TypeAggregator
import org.shypl.csi.api.generator.target.TypeCollector

@Suppress("DuplicatedCode")
class ClientTypescriptApiGenerator(
	private val model: Model,
	private val coders: TypescriptCodersGenerator,
	private val libPackage: ClassPackage,
	private val genPackage: ClassPackage,
) {
	private val internalPackage = genPackage.getChild("_internal")
	
	fun generate(codeStorage: CodeStorage) {
		generate(model.client, model.server, codeStorage)
	}
	
	private fun generate(innerApi: Api, outerApi: Api, codeStorage: CodeStorage) {
		val loggers = TypeAggregator()
		
		generateApiVersion(codeStorage)
		generateInnerApi(codeStorage, innerApi)
		generateOuterApi(codeStorage, outerApi)
		generateOuterApiImpl(codeStorage, outerApi)
		generateApiAdapter(codeStorage, innerApi, outerApi)
		generateApiConnection(codeStorage, innerApi)
		
		val allServices = hashSetOf<ServiceDescriptor>()
		
		innerApi.services
			.fold(hashSetOf<ServiceDescriptor>()) { a, it -> collectServiceDescriptors(a, it.descriptor) }
			.forEach { s ->
				allServices.add(s)
				s.methods.forEach { m -> if (m.arguments.any { it is Method.Argument.Subscription }) generateOuterSubscription(codeStorage, s, m, loggers) }
				generateInnerService(codeStorage, s, loggers)
			}
		
		outerApi.services
			.fold(hashSetOf<ServiceDescriptor>()) { a, it -> collectServiceDescriptors(a, it.descriptor) }
			.forEach { s ->
				allServices.add(s)
				s.methods.forEach { m -> if (m.arguments.any { it is Method.Argument.Subscription }) generateInnerSubscription(codeStorage, s, m, loggers) }
				generateOuterService(codeStorage, s, loggers)
			}
		
		if (loggers.hasNext()) {
			generateLogging(codeStorage, loggers)
		}
		
		
		generateSourceApi(codeStorage, innerApi)
		generateSourceApi(codeStorage, outerApi)
		
		allServices.forEach { generateSourceService(codeStorage, it) }
	}
	
	private fun generateApiVersion(codeStorage: CodeStorage) {
		codeStorage.newFile(internalPackage.getName("_version")).apply {
			body.line("export const API_VERSION = ${model.version.current}")
		}
	}
	
	private fun generateSourceApi(codeStorage: CodeStorage, api: Api) {
		codeStorage.newFile(api.name).apply {
			body.identBracketsCurly("export interface ${api.name.value} ") {
				api.services.sortedBy(Service::id).forEach {
					addDependency(it.descriptor)
					line("readonly ${it.name}: ${it.descriptor.value}")
				}
			}
		}
	}
	
	private fun generateSourceService(codeStorage: CodeStorage, descriptor: ServiceDescriptor) {
		codeStorage.newFile(descriptor.name).apply {
			
			val closeable = if (descriptor.closeable) {
				addDependency(libPackage.getName("tool.utils/Closeable"))
				"extends Closeable "
			} else ""
			
			body.identBracketsCurly("export interface ${descriptor.name.value} $closeable") {
				descriptor.methods.forEach { m ->
					line {
						append("${m.name}(")
						m.arguments.forEachIndexed { i, a ->
							if (i != 0) append(", ")
							append(getArgumentDeclaration(this@identBracketsCurly, a))
						}
						append("): ")
						when (val r = m.result) {
							is Method.Result.Value     -> append("Promise<${coders.getTypeName(r.type, this@identBracketsCurly)}>")
							is Method.Result.Service   -> {
								addDependency(r.descriptor)
								append("Promise<${r.descriptor.value}>")
							}
							
							Method.Result.Subscription -> {
								addDependency(libPackage.getName("tool.utils/Closeable"))
								append("Promise<Closeable>")
							}
							
							null                       -> append("void")
						}
					}
					line()
				}
			}
		}
	}
	
	private fun collectServiceDescriptors(target: HashSet<ServiceDescriptor>, descriptorName: ClassName): HashSet<ServiceDescriptor> {
		val descriptor = model.getServiceDescriptor(descriptorName)
		if (target.add(descriptor)) {
			descriptor.methods.forEach { m ->
				(m.result as? Method.Result.Service)?.also { collectServiceDescriptors(target, it.descriptor) }
			}
		}
		return target
	}
	
	private fun generateInnerApi(codeStorage: CodeStorage, api: Api) {
		val name = "Internal${api.name.value}"
		
		codeStorage.newFile(internalPackage.getName(name)).apply {
			addDependency(libPackage.getName("csi.api.client/InnerApi"))
			addDependency(api.name)
			
			body.line("export interface $name extends ${api.name.value}, InnerApi {}")
		}
	}
	
	private fun generateOuterApi(codeStorage: CodeStorage, api: Api) {
		val name = "Internal${api.name.value}"
		
		codeStorage.newFile(internalPackage.getName(name)).apply {
			addDependency(libPackage.getName("csi.api/OuterApi"))
			addDependency(api.name)
			
			body.line("export interface $name extends ${api.name.value}, OuterApi {}")
		}
	}
	
	private fun generateOuterApiImpl(codeStorage: CodeStorage, api: Api) {
		val name = "Internal${api.name.value}Impl"
		
		codeStorage.newFile(internalPackage.getName(name)).apply {
			addDependency(libPackage.getName("csi.api/AbstractOuterApi"))
			addDependency(libPackage.getName("csi.api/Context"))
			addDependency(internalPackage.getName("InternalServerApi"))
			
			body.apply {
				identBracketsCurly("export class $name extends AbstractOuterApi implements Internal${api.name.value}") {
					val services = api.services.sortedBy(Service::id)
					
					services.forEach {
						addDependency(it.descriptor)
						line("readonly ${it.name}: ${it.descriptor.value}")
					}
					line()
					identBracketsCurly("constructor(context: Context) ") {
						line("super(context.connection)")
						
						services.forEach {
							addDependency(internalPackage.getName("${it.descriptor.value}Outer"))
							line("this.${it.name} = new ${it.descriptor.value}Outer(context, false, ${it.id}, \"${it.name}\")")
						}
					}
				}
			}
		}
	}
	
	private fun generateApiAdapter(codeStorage: CodeStorage, innerApi: Api, outerApi: Api) {
		codeStorage.newFile(internalPackage.getName("ApiAdapter")).apply {
			addDependency(libPackage.getName("csi.api/CallbacksRegistry"))
			addDependency(libPackage.getName("csi.api/Context"))
			addDependency(libPackage.getName("csi.api.client/AbstractApiAdapter"))
			addDependency(libPackage.getName("csi.api.client/ApiSluice"))
			addDependency(libPackage.getName("csi.core.client/ConnectionHandler"))
			addDependency(libPackage.getName("tool.io/ByteBuffer"))
			addDependency(libPackage.getName("tool.utils.pool/ObjectPool"))
			
			val iaName = "Internal" + innerApi.name.value
			val oaName = "Internal" + outerApi.name.value
			
			addDependency(internalPackage.getName(iaName))
			addDependency(internalPackage.getName(oaName))
			addDependency(internalPackage.getName("ApiConnection"))
			addDependency(internalPackage.getName("${oaName}Impl"))
			
			
			generateApiAdapterDeclaration(body, iaName, oaName).apply {
				line()
				identBracketsCurly("getLoggerName(): string ") {
					line("return 'csi'")
				}
				
				line()
				identBracketsCurly("createConnectionHandler(context: Context, api: $iaName): ConnectionHandler ") {
					line("return new ApiConnection(context, api)")
				}
				
				line()
				identBracketsCurly("createOuterApi(context: Context): $oaName ") {
					line("return new ${oaName}Impl(context)")
				}
				
				line()
				identBracketsCurly("provideCallbacksRegistry(): CallbacksRegistry ") {
					if (outerApi.services.any { s -> model.getServiceDescriptor(s.descriptor).methods.any { it.result != null } }) {
						addDependency(libPackage.getName("csi.api/RealCallbacksRegistry"))
						line("return new RealCallbacksRegistry()")
					}
					else {
						addDependency(libPackage.getName("csi.api/NothingCallbacksRegistry"))
						line("return new NothingCallbacksRegistry()")
					}
				}
			}
			
		}
	}
	
	private fun generateApiAdapterDeclaration(code: Code, iaName: String, oaName: String): Code {
		return code.identBracketsCurly("export class ApiAdapter extends AbstractApiAdapter<$iaName, $oaName> ").apply {
			line("constructor(")
			ident {
				line("sluice: ApiSluice<$iaName, $oaName>,")
				line("byteBuffers: ObjectPool<ByteBuffer>")
			}
			line(") {")
			ident {
				line("super(sluice, byteBuffers)")
			}
			line("}")
		}
	}
	
	private fun generateApiConnection(codeStorage: CodeStorage, api: Api) {
		val iaInternalName = "Internal${api.name.value}"
		
		codeStorage.newFile(internalPackage.getName("ApiConnection")).apply {
			addDependency(libPackage.getName("csi.api/Context"))
			addDependency(libPackage.getName("csi.api/InnerServiceDelegate"))
			addDependency(libPackage.getName("csi.api.client/AbstractApiConnection"))
			addDependency(internalPackage.getName(iaInternalName))
			
			body.apply {
				identBracketsCurly("export class ApiConnection extends AbstractApiConnection<$iaInternalName> ") {
					
					val services = api.services.sortedBy(Service::id)
					services.forEach {
						addDependency(internalPackage.getName("${it.descriptor.value}InnerDelegate"))
						line("private readonly ${it.name}Delegate: ${it.descriptor.value}InnerDelegate")
					}
					line()
					
					line("constructor(")
					ident {
						line("context: Context,")
						line("api: $iaInternalName")
					}
					line(") {")
					ident {
						line("super(context, api)")
						
						services.forEach {
							line("this.${it.name}Delegate = new ${it.descriptor.value}InnerDelegate(context, api.${it.name}, \"${it.name}\")")
						}
					}
					line("}")
					line()
					
					identBracketsCurly("findService(serviceId: number): InnerServiceDelegate<any> | null ") {
						identBracketsCurly("switch (serviceId) ") {
							services.forEach {
								line("case ${it.id}: return this.${it.name}Delegate")
							}
							line("default: return null")
						}
					}
					
				}
			}
		}
	}
	
	private fun generateInnerService(codeStorage: CodeStorage, descriptor: ServiceDescriptor, loggers: TypeCollector) {
		val name = "${descriptor.name.value}InnerDelegate"
		
		codeStorage.newFile(internalPackage.getName(name)).apply {
			addDependency(libPackage.getName("csi.api/Context"))
			addDependency(libPackage.getName("csi.api/InnerServiceDelegate"))
			addDependency(libPackage.getName("tool.biser/BiserReader"))
			addDependency(descriptor.name)
			
			body.identBracketsCurly("export class $name extends InnerServiceDelegate<${descriptor.name.value}> ") {
				
				line("constructor(")
				ident {
					line("context: Context,")
					line("service: ${descriptor.name.value},")
					line("name: string")
				}
				line(") {")
				ident {
					line("super(context, service, name)")
				}
				line("}")
				line()
				
				identBracketsCurly("callMethod(methodId: number, message: BiserReader): boolean ") {
					identBracketsCurly("switch (methodId) ") {
						descriptor.methods.sortedBy(Method::id).forEach {
							line("case ${it.id}: this.call_${it.name}(message); break")
						}
						line("default: return false")
					}
					line("return true")
				}
				
				descriptor.methods.sortedBy(Method::id).forEach { m ->
					line()
					identBracketsCurly("private call_${m.name}(message: BiserReader) ") {
						val result = m.result
						val arguments = m.arguments
						val hasArguments = arguments.isNotEmpty()
						if (result != null) {
							line("const c = message." + coders.provideReadCall(this, Type.Primitive.INT))
						}
						arguments.forEachIndexed { i, a ->
							if (a is Method.Argument.Value) {
								line("const a$i = message." + coders.provideReadCall(this, a.type))
							}
						}
						
						line {
							if (result == null) {
								append("this.logMethodCall(\"${m.name}\", lb => {")
							}
							else {
								append("this.logMethodCallWithCallback(\"${m.name}\", c, lb => {")
							}
							if (!hasArguments) append("})")
						}
						if (hasArguments) {
							ident {
								arguments.forEachIndexed { i, a ->
									if (a is Method.Argument.Value) logCall(loggers, i == arguments.lastIndex, a.type, a.name, "a$i")
								}
							}
							line("})")
						}
						
						when (result) {
							null                       -> {
								line {
									append("this.service.${m.name}(")
									arguments.indices.joinTo(this) { "a$it" }
									append(')')
								}
							}
							
							is Method.Result.Value     -> {
								line {
									append("this.service.${m.name}(")
									arguments.indices.joinTo(this) { "a$it" }
									append(").then(r => {")
								}
								ident {
									line("this.logMethodResponse(\"${m.name}\", c, lb => {")
									ident {
										logCall(loggers, true, result.type, null, "r")
									}
									line("})")
									
									line("this.sendMethodResponse(c, w => {")
									ident {
										line("w." + coders.provideWriteCall(this, result.type, "r"))
									}
									line("})")
								}
								line("})")
							}
							
							is Method.Result.Service   -> {
								val hasSubscription = arguments.any { it is Method.Argument.Subscription }
								if (hasSubscription) {
									line("const ss = ${descriptor.name.value}_${m.name}_OuterSubscription(context, this@${name})")
								}
								
								line {
									append("this.service.${m.name}(")
									arguments.forEachIndexed { i, a ->
										when (a) {
											is Method.Argument.Value        -> append("a$i")
											is Method.Argument.Subscription -> append("ss.${a.name}")
										}
										if (i != arguments.lastIndex) append(", ")
									}
									append(").then(i => {")
								}
								ident {
									line("const s = this.registerInstanceService(i, ${result.descriptor.value}InnerDelegate(context, i.service, \"\$name.${m.name}\"))")
									if (hasSubscription) {
										addDependency(libPackage.getName("tool.utils/Closeable"))
										line("const ssi = this.registerSubscription(ss, Closeable.DUMMY)")
										
										line("this.logInstanceServiceSubscriptionResponse(\"${m.name}\", c, s, ssi) ")
										line("this.sendInstanceServiceSubscriptionResponse(c, s, ssi) ")
									}
									else {
										line("this.logInstanceServiceResponse(\"${m.name}\", c, s) ")
										line("this.sendInstanceServiceResponse(c, s) ")
									}
								}
								line("})")
							}
							
							Method.Result.Subscription -> {
								line("const s = new ${descriptor.name.value}_${m.name}_OuterSubscription(context, this@${name})")
								line {
									append("this.service.${m.name}(")
									arguments.forEachIndexed { i, a ->
										when (a) {
											is Method.Argument.Value        -> append("a$i")
											is Method.Argument.Subscription -> append("s.${a.name}")
										}
										if (i != arguments.lastIndex) append(", ")
									}
									append(").then(e => {")
								}
								ident {
									line("const i = this.registerSubscription(s, e)")
									line("this.logSubscriptionResponse(\"${m.name}\", c, i) ")
									line("this.sendSubscriptionResponse(c, i) ")
								}
								line("})")
							}
						}
						
					}
				}
				
			}
		}
	}
	
	private fun generateOuterService(codeStorage: CodeStorage, descriptor: ServiceDescriptor, loggers: TypeCollector) {
		val name = descriptor.name.value
		
		codeStorage.newFile(internalPackage.getName("${name}Outer")).apply {
			addDependency(descriptor.name)
			addDependency(libPackage.getName("csi.api/Context"))
			
			val parent = if (descriptor.closeable) {
				addDependency(libPackage.getName("csi.api/OuterInstanceService"))
				"OuterInstanceService"
			}
			else {
				addDependency(libPackage.getName("csi.api/OuterService"))
				"OuterService"
			}
			
			body.line("// noinspection JSUnusedLocalSymbols")
			body.identBracketsCurly("export class ${name}Outer extends $parent implements $name ") {
				line("constructor(")
				ident {
					line("context: Context,")
					line("instance: boolean,")
					line("serviceId: number,")
					line("serviceName: string")
				}
				identBracketsCurly(") ") {
					line("super(context, instance, serviceId, serviceName)")
				}
				
				descriptor.methods.sortedBy(Method::id).forEach { m ->
					
					line()
					line {
						append("${m.name}(")
						m.arguments.joinTo(this) { getArgumentDeclaration(this@identBracketsCurly, it) }
						append(")")
						m.result?.also {
							append(": Promise<")
							when (it) {
								is Method.Result.Value     -> append(coders.getTypeName(it.type, this@identBracketsCurly))
								is Method.Result.Service   -> {
									addDependency(it.descriptor)
									append(it.descriptor.value)
								}
								
								Method.Result.Subscription -> {
									addDependency(libPackage.getName("tool.utils/Closeable"))
									append("Closeable")
								}
							}
							append(">")
						}
						append(" {")
					}
					
					ident {
						line("this._checkClosed()")
						
						val r = m.result
						
						(if (r != null) {
							line("return new Promise(_continuation => {")
							ident()
						}
						else this).apply {
							
							if (r != null) {
								line("const _callback = this._registerCallback((_reader, _callbackLocal) => {")
								ident {
									when (r) {
										is Method.Result.Value     -> {
											line("const _result = _reader." + coders.provideReadCall(this, r.type))
											line("this._logMethodResponse(\"${m.name}\", _callbackLocal, lb => {")
											ident {
												logCall(loggers, true, r.type, null, "_result")
											}
											line("})")
											line("_continuation(_result)")
										}
										
										is Method.Result.Service   -> {
											val hasSubscription = m.arguments.any { it is Method.Argument.Subscription }
											addDependency(r.descriptor)
											addDependency(internalPackage.getName("${r.descriptor.value}Outer"))
											line("const _service = _reader." + coders.provideReadCall(this, Type.Primitive.INT))
											
											if (hasSubscription) {
												line("const _subscription = _reader." + coders.provideReadCall(this, Type.Primitive.INT))
												line("this._logInstanceSubscriptionOpen(\"${m.name}\", _callbackLocal, _service, _subscription) ")
											}
											else {
												line("this._logInstanceOpen(\"${m.name}\", _callbackLocal, _service) ")
											}
											
											line("const _si = new ${r.descriptor.value}Outer(this._context, true, _service, `\${this._name}.${m.name}[+\${_service}]`)")
											if (hasSubscription) {
												line {
													addDependency(internalPackage.getName("${name}_${m.name}_InnerSubscription"))
													append("const t = new ${name}_${m.name}_InnerSubscription(this._context, this, _subscription, ")
													m.arguments.filterIsInstance<Method.Argument.Subscription>().joinTo(this) { it.name }
													append(")")
												}
												line("_si._registerSubscription(t)")
											}
											line("_continuation(_si)")
											
										}
										
										Method.Result.Subscription -> {
											line("const _subscription = _reader." + coders.provideReadCall(this, Type.Primitive.INT))
											line("this._logSubscriptionBegin(\"${m.name}\", _callbackLocal, _subscription) ")
											line {
												addDependency(internalPackage.getName("${name}_${m.name}_InnerSubscription"))
												append("const t = new ${name}_${m.name}_InnerSubscription(this._context, this, _subscription, ")
												m.arguments.filterIsInstance<Method.Argument.Subscription>().joinTo(this) { it.name }
												append(")")
											}
											line("this._registerSubscription(t)")
											line("_continuation(t)")
										}
									}
								}
								line("})")
							}
							
							val send: String
							val log: String
							
							if (r == null) {
								log = "this._logMethodCall(\"${m.name}\", lb => {"
								send = "this._callMethod(${m.id}, w => {"
							}
							else {
								log = "this._logMethodCallWithCallback(\"${m.name}\", _callback, lb => {"
								send = "this._callMethodWithCallback(${m.id}, _callback, w => {"
							}
							
							val arguments = m.arguments.filterIsInstance<Method.Argument.Value>()
							
							if (arguments.isEmpty()) {
								line("$log})")
							}
							else {
								line(log)
								ident {
									arguments.forEachIndexed { i, a ->
										logCall(loggers, arguments.lastIndex == i, a.type, a.name)
									}
								}
								line("})")
							}
							
							if (arguments.isEmpty()) {
								line("$send})")
							}
							else {
								line(send)
								ident {
									arguments.forEach { a ->
										line("w." + coders.provideWriteCall(this, a.type, a.name))
									}
								}
								line("})")
							}
						}
						if (r != null) line("})")
					}
					line("}")
				}
				
			}
		}
	}
	
	private fun generateInnerSubscription(codeStorage: CodeStorage, service: ServiceDescriptor, method: Method, loggers: TypeAggregator) {
		val name = "${service.name.value}_${method.name}_InnerSubscription"
		val arguments = method.arguments.filterIsInstance<Method.Argument.Subscription>()
		codeStorage.newFile(internalPackage.getName(name)).apply {
			addDependency(libPackage.getName("csi.api/Context"))
			addDependency(libPackage.getName("csi.api/OuterService"))
			addDependency(libPackage.getName("csi.api/InnerSubscription"))
			addDependency(libPackage.getName("tool.biser/BiserReader"))
			
			body.identBracketsCurly("export class $name extends InnerSubscription ") {
				line("constructor(")
				ident {
					line("context: Context,")
					line("service: OuterService,")
					line("id: number,")
					arguments.forEach {
						line("private readonly _" + getArgumentDeclaration(this, it) + ",")
					}
				}
				identBracketsCurly(") ") {
					line("super(context, service, \"${method.name}\", id)")
				}
				line()
				
				identBracketsCurly("call(argumentId: number, message: BiserReader): boolean ") {
					
					if (arguments.size == 1) {
						line("if (argumentId != 0) return false")
						val a = arguments.first()
						
						a.parameters.forEachIndexed { i, p ->
							line("const p$i = message." + coders.provideReadCall(this, p.type))
						}
						
						line("this.logCall(\"${a.name}\", lb => {")
						ident {
							a.parameters.forEachIndexed { i, p ->
								logCall(loggers, i == a.parameters.lastIndex, p.type, p.name, "p$i")
							}
						}
						line("})")
						
						line {
							append("this._${a.name}(")
							a.parameters.indices.joinTo(this) { "p$it" }
							append(')')
						}
					}
					else {
						identBracketsCurly("switch (argumentId) ") {
							arguments.forEachIndexed { q, a ->
								identBracketsCurly("case $q: ") {
									a.parameters.forEachIndexed { i, p ->
										line("const p$i = message." + coders.provideReadCall(this, p.type))
									}
									
									line("this.logCall(\"${a.name}\", lb => {")
									ident {
										a.parameters.forEachIndexed { i, p ->
											logCall(loggers, i == a.parameters.lastIndex, p.type, p.name, "p$i")
										}
									}
									line("})")
									
									line {
										append("this._${a.name}(")
										a.parameters.indices.joinTo(this) { "p$it" }
										append(')')
									}
									line("break")
								}
							}
							line("default: return false")
						}
					}
					line("return true")
				}
				
			}
		}
	}
	
	private fun generateOuterSubscription(codeStorage: CodeStorage, service: ServiceDescriptor, method: Method, loggers: TypeAggregator) {
		val name = "${service.name.value}_${method.name}_OuterSubscription"
		val arguments = method.arguments.filterIsInstance<Method.Argument.Subscription>()
		codeStorage.newFile(internalPackage.getName(name)).apply {
			addDependency(libPackage.getName("csi.api/Context"))
			addDependency(libPackage.getName("csi.api/InnerServiceDelegate"))
			addDependency(libPackage.getName("csi.api/OuterSubscription"))
			
			body.apply {
				line("@Suppress(\"ClassName\")")
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
						append(p.name ?: "p$i").append(": ")
						append(coders.getTypeName(p.type, depended))
						if (argument.parameters.lastIndex != i) append(", ")
					}
					append(") => void")
				}
			}
			
		}.toString()
	}
	
	private fun generateLogging(codeStorage: CodeStorage, loggers: TypeAggregator) {
		codeStorage.newFile(internalPackage.getName("_logging")).apply {
			addDependency(libPackage.getName("csi.api/LogBuilder"))
			header.line("// noinspection DuplicatedCode,JSUnusedLocalSymbols")
			header.line()
			body.apply {
				loggers.forEach { type ->
					val name = defineLogName(type)
					identBracketsCurly("export function $name(lb: LogBuilder, v: ${coders.getTypeName(type, this)}) ") {
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
		
		override fun visitPrimitiveType(type: Type.Primitive, data: LogCallVisitorData) {
			var fn = if (type == Type.Primitive.BYTE_ARRAY) "logByteArray" else if (type.array) "logArray" else "log"
			if (data.sep) fn += "Sep"
			
			if (data.argName == null) {
				data.code.line("lb.$fn(${data.argVal})")
			}
			else {
				data.code.line("lb.${fn}Arg(\"${data.argName}\", ${data.argVal})")
			}
		}
		
		override fun visitListType(type: Type.List, data: LogCallVisitorData) {
			if (type.element.let { it is Type.Primitive && !it.array }) {
				val fn = if (data.sep) "logArraySep" else "logArray"
				if (data.argName == null) {
					data.code.line("lb.$fn(${data.argVal})")
				}
				else {
					data.code.line("lb.${fn}Arg(\"${data.argName}\", ${data.argVal})")
				}
			}
			else {
				val fn = if (data.sep) "logSep" else "log"
				val logger = defineLogName(type)
				data.loggers.add(type)
				data.code.addDependency(internalPackage.getName("_logging"), logger)
				if (data.argName == null) {
					data.code.line("lb.${fn}With(${data.argVal}, $logger)")
				}
				else {
					data.code.line("lb.${fn}WithArg(\"${data.argName}\", ${data.argVal}, $logger)")
				}
			}
		}
		
		override fun visitMapType(type: Type.Map, data: LogCallVisitorData) {
			val fn = if (data.sep) "logSep" else "log"
			val logger = defineLogName(type)
			data.loggers.add(type)
			data.code.addDependency(internalPackage.getName("_logging"), logger)
			if (data.argName == null) {
				data.code.line("lb.${fn}With(${data.argVal}, $logger)")
			}
			else {
				data.code.line("lb.${fn}WithArg(\"${data.argName}\", ${data.argVal}, $logger)")
			}
		}
		
		override fun visitNullableType(type: Type.Nullable, data: LogCallVisitorData) {
			val fn = if (data.sep) "logSepWith" else "logWith"
			val logger = defineLogName(type)
			data.loggers.add(type)
			data.code.addDependency(internalPackage.getName("_logging"), logger)
			if (data.argName == null) {
				data.code.line("lb.${fn}(${data.argVal}, $logger)")
			}
			else {
				data.code.line("lb.${fn}Arg(\"${data.argName}\", ${data.argVal}, $logger)")
			}
		}
		
		override fun visitEntityType(type: Type.Entity, data: LogCallVisitorData) {
			if (model.getEntity(type.className) is EnumEntity) {
				val fn = if (data.sep) "logSep" else "log"
				val n = coders.getTypeName(type, data.code)
				if (data.argName == null) {
					data.code.line("lb.$fn($n[${data.argVal}])")
				}
				else {
					data.code.line("lb.${fn}Arg(\"${data.argName}\", $n[${data.argVal}])")
				}
			}
			else {
				val fn = if (data.sep) "logSepWith" else "logWith"
				val logger = defineLogName(type)
				data.loggers.add(type)
				data.code.addDependency(internalPackage.getName("_logging"), logger)
				if (data.argName == null) {
					data.code.line("lb.${fn}(${data.argVal}, $logger)")
				}
				else {
					data.code.line("lb.${fn}Arg(\"${data.argName}\", ${data.argVal}, $logger)")
				}
			}
		}
	}
	
	private val logNamesVisitor = object : TypeVisitor<String, Unit> {
		override fun visitPrimitiveType(type: Type.Primitive, data: Unit): String {
			return type.name
		}
		
		override fun visitEntityType(type: Type.Entity, data: Unit): String {
			return "ENTITY_" + type.className.path.joinToString("_")
		}
		
		override fun visitListType(type: Type.List, data: Unit): String {
			return "list_" + type.element.accept(this, data)
		}
		
		override fun visitMapType(type: Type.Map, data: Unit): String {
			return "MAP_" + type.key.accept(this, data) + "__" + type.value.accept(this, data)
		}
		
		override fun visitNullableType(type: Type.Nullable, data: Unit): String {
			return "NULLABLE_" + type.original.accept(this, data)
		}
	}
	
	private val loggingVisitor = object : TypeVisitor<Unit, GenerateLoggingVisitorData>, EntityVisitor<Unit, GenerateLoggingVisitorData> {
		override fun visitPrimitiveType(type: Type.Primitive, data: GenerateLoggingVisitorData) {
			data.code.line(if (type.array) "lb.logArray(v)" else "lb.log(v)")
		}
		
		override fun visitListType(type: Type.List, data: GenerateLoggingVisitorData) {
			data.loggers.add(type.element)
			val logger = defineLogName(type.element)
			data.code.addDependency(internalPackage.getName("_logging"), logger)
			data.code.line("lb.logArrayWith(v, $logger)")
		}
		
		override fun visitMapType(type: Type.Map, data: GenerateLoggingVisitorData) {
			data.loggers.add(type.key)
			data.loggers.add(type.value)
			val loggerK = defineLogName(type.key)
			val loggerV = defineLogName(type.value)
			data.code.addDependency(internalPackage.getName("_logging"), loggerK, loggerV)
			data.code.line("lb.logMapWith(v, $loggerK, $loggerV)")
		}
		
		override fun visitNullableType(type: Type.Nullable, data: GenerateLoggingVisitorData) {
			data.loggers.add(type.original)
			val logger = defineLogName(type.original)
			data.code.addDependency(internalPackage.getName("_logging"), logger)
			data.code.line("if (v === null) lb.log(\"NULL\"); else lb.logWith(v, $logger)")
		}
		
		override fun visitEntityType(type: Type.Entity, data: GenerateLoggingVisitorData) {
			model.getEntity(type.className).accept(this, data)
		}
		
		///
		
		override fun visitEnumEntity(entity: EnumEntity, data: GenerateLoggingVisitorData) {
			data.code.line("lb.log(${coders.getTypeName(model.getEntityType(entity.name), data.code)}[v])")
		}
		
		override fun visitStructureEntity(entity: StructureEntity, data: GenerateLoggingVisitorData) {
			data.code.apply {
				if (entity.children.isEmpty()) {
					visitStructureEntity0(entity, data)
				}
				else {
					identBracketsCurly("switch (true) ") {
						entity.children.forEach {
							val type = model.getEntityType(it)
							data.loggers.add(type)
							val typeName = coders.getTypeName(type, data.code)
							line("case v instanceof $typeName: ${defineLogName(type)}(lb, v as $typeName); break")
						}
						if (entity.abstract) {
							data.code.addDependency(libPackage.getName("lang.exceptions/UnsupportedOperationException"))
							line("default: throw new UnsupportedOperationException()")
						}
						else {
							identBracketsCurly("default:  ") {
								visitStructureEntity0(entity, data)
								line("break")
							}
						}
					}
				}
			}
		}
		
		private fun Code.visitStructureEntity0(entity: StructureEntity, data: GenerateLoggingVisitorData) {
			line("lb.log('{')")
			val allFields = model.getStructureEntityAllFields(entity)
			allFields.forEachIndexed { i, f ->
				logCall(data.loggers, allFields.lastIndex == i, f.type, f.name, "v.${f.name}")
			}
			line("lb.log('}')")
		}
		
		override fun visitConstantEntity(entity: ConstantEntity, data: GenerateLoggingVisitorData) {
			data.code.line("lb.log(\"${entity.name.value}\")")
		}
	}
	
	private class GenerateLoggingVisitorData(val code: Code, val loggers: TypeCollector)
}
