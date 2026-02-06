package org.openrs2.deob.bytecode.transform

import org.objectweb.asm.Type
import org.objectweb.asm.tree.*
import rs.lostcity.asm.transform.Transformer

public class EmptyClassTransformer : Transformer() {
    private var removedClasses = 0
    private val emptyClasses = mutableSetOf<String>()
    private val referencedClasses = mutableSetOf<String>()

    override fun preTransform(classes: List<ClassNode>) {
        removedClasses = 0
        emptyClasses.clear()
    }

    override fun prePass(classes: List<ClassNode>): Boolean {
        referencedClasses.clear()
        return false
    }

    override fun transformClass(classes: List<ClassNode>, clazz: ClassNode): Boolean {
        if (clazz.fields.isEmpty() && clazz.methods.isEmpty()) {
            emptyClasses.add(clazz.name)
        }

        if (clazz.superName != null) {
            referencedClasses.add(clazz.superName)
        }

        for (superInterface in clazz.interfaces) {
            referencedClasses.add(superInterface)
        }

        return false
    }

    private fun addTypeReference(type: Type) {
        when (type.sort) {
            Type.OBJECT -> referencedClasses.add(type.internalName)
            Type.ARRAY -> addTypeReference(type.elementType)
            Type.METHOD -> {
                type.argumentTypes.forEach(::addTypeReference)
                addTypeReference(type.returnType)
            }
        }
    }

    override fun transformField(classes: List<ClassNode>, clazz: ClassNode, field: FieldNode): Boolean {
        addTypeReference(Type.getType(field.desc))
        return false
    }

    override fun preTransformMethod(
        classes: List<ClassNode>,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        addTypeReference(Type.getType(method.desc))
        return false
    }

    override fun transformCode(classes: List<ClassNode>, clazz: ClassNode, method: MethodNode): Boolean {
        for (insn in method.instructions) {
            when (insn) {
                is LdcInsnNode -> {
                    val cst = insn.cst
                    if (cst is Type) {
                        addTypeReference(cst)
                    }
                }

                is TypeInsnNode -> referencedClasses.add(insn.desc)
            }
        }

        return false
    }

    override fun postPass(classes: List<ClassNode>): Boolean {
        var changed = false

        for (name in emptyClasses.subtract(referencedClasses)) {
            for (library in classPath.libraries) {
                if (library.remove(name) != null) {
                    removedClasses++
                    changed = true
                }
            }
        }

        return changed
    }

    override fun postTransform(classes: List<ClassNode>) {
        System.out.println("Removed $removedClasses unused classes")
    }
}
