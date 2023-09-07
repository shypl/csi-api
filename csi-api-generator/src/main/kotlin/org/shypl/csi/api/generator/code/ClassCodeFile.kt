package org.shypl.csi.api.generator.code

import org.shypl.csi.api.generator.model.ClassName

class ClassCodeFile(val name: ClassName) : CodeFile(name.rootPackage)