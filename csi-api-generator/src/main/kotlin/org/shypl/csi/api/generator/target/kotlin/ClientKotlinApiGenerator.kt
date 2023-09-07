package org.shypl.csi.api.generator.target.kotlin

import org.shypl.csi.api.generator.code.Code
import org.shypl.csi.api.generator.model.ClassPackage
import org.shypl.csi.api.generator.model.Model

class ClientKotlinApiGenerator(
	model: Model,
	coders: KotlinCodersGenerator,
	targetPackage: ClassPackage,
) : KotlinApiGenerator(model, coders, targetPackage, "client") {
	
	override fun generate(codeSource: KotlinCodeStorage) {
		generate(model.client, model.server, codeSource)
	}
	
	override fun generateApiAdapterDeclaration(code: Code, iaName: String, oaName: String): Code {
		code.line("class ApiAdapter(")
		code.ident {
			line("sluice: ApiSluice<$iaName, $oaName>,")
			line("coroutineScope: CoroutineScope,")
			line("byteBuffers: ObjectPool<ByteBuffer>")
		}
		return code.identBracketsCurly(") : AbstractApiAdapter<$iaName, $oaName>(sluice, coroutineScope, byteBuffers) ")
	}
}