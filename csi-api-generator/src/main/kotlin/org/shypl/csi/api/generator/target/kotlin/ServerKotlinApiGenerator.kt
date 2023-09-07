package org.shypl.csi.api.generator.target.kotlin

import org.shypl.csi.api.generator.code.Code
import org.shypl.csi.api.generator.model.ClassPackage
import org.shypl.csi.api.generator.model.Model

class ServerKotlinApiGenerator(
	model: Model,
	coders: KotlinCodersGenerator,
	targetPackage: ClassPackage,
) : KotlinApiGenerator(model, coders, targetPackage, "server") {
	
	override fun generate(codeSource: KotlinCodeStorage) {
		generate(model.server, model.client, codeSource)
	}
	
	override fun generateApiAdapterDeclaration(code: Code, iaName: String, oaName: String): Code {
		code.line("class ApiAdapter<I : Any>(")
		code.ident {
			line("sluice: ApiSluice<I, $iaName, $oaName>,")
			line("coroutineScope: CoroutineScope,")
			line("byteBuffers: ObjectPool<ByteBuffer>")
		}
		return code.identBracketsCurly(") : AbstractApiAdapter<I, $iaName, $oaName>(sluice, coroutineScope, byteBuffers) ")
	}
}