package org.shypl.csi.api.generator.model

sealed interface Type {
	
	val name: String
	
	fun <R, D> accept(visitor: TypeVisitor<R, D>, data: D): R
	fun <R> accept(visitor: TypeVisitor<R, Unit>): R = accept(visitor, Unit)
	
	
	enum class Primitive(val array: Boolean = false) : Type {
		BOOLEAN,
		BYTE,
		INT,
		LONG,
		DOUBLE,
		STRING,
		DATE_TIME,
		
		BOOLEAN_ARRAY(true),
		BYTE_ARRAY(true),
		INT_ARRAY(true),
		LONG_ARRAY(true),
		DOUBLE_ARRAY(true);
		
		
		override fun <R, D> accept(visitor: TypeVisitor<R, D>, data: D): R {
			return visitor.visitPrimitiveType(this, data)
		}
	}
	
	data class Entity(val className: ClassName) : Type {
		override val name: String
			get() = className.full
		
		override fun <R, D> accept(visitor: TypeVisitor<R, D>, data: D): R {
			return visitor.visitEntityType(this, data)
		}
	}
	
	data class List(val element: Type) : Type {
		override val name: String
			get() = "List<${element.name}>"
		
		override fun <R, D> accept(visitor: TypeVisitor<R, D>, data: D): R {
			return visitor.visitListType(this, data)
		}
	}
	
	data class MutableList(val element: Type) : Type {
		override val name: String
			get() = "MutableList<${element.name}>"
		
		override fun <R, D> accept(visitor: TypeVisitor<R, D>, data: D): R {
			return visitor.visitMutableListType(this, data)
		}
	}
	
	data class Map(val key: Type, val value: Type) : Type {
		override val name: String
			get() = "Map<${key.name},${value.name}>"
		
		override fun <R, D> accept(visitor: TypeVisitor<R, D>, data: D): R {
			return visitor.visitMapType(this, data)
		}
	}
	
	data class MutableMap(val key: Type, val value: Type) : Type {
		override val name: String
			get() = "MutableMap<${key.name},${value.name}>"
		
		override fun <R, D> accept(visitor: TypeVisitor<R, D>, data: D): R {
			return visitor.visitMutableMapType(this, data)
		}
	}
	
	data class Nullable(val original: Type) : Type {
		override val name: String
			get() = "${original.name}?"
		
		override fun <R, D> accept(visitor: TypeVisitor<R, D>, data: D): R {
			return visitor.visitNullableType(this, data)
		}
	}
}


interface TypeVisitor<R, D> {
	fun visitPrimitiveType(type: Type.Primitive, data: D): R
	fun visitEntityType(type: Type.Entity, data: D): R
	fun visitListType(type: Type.List, data: D): R
	fun visitMutableListType(type: Type.MutableList, data: D): R
	fun visitMapType(type: Type.Map, data: D): R
	fun visitMutableMapType(type: Type.MutableMap, data: D): R
	fun visitNullableType(type: Type.Nullable, data: D): R
}