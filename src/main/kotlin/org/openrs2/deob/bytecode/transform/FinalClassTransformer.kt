package org.openrs2.deob.bytecode.transform

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import rs.lostcity.asm.transform.Transformer

public class FinalClassTransformer : Transformer() {
    private val superClasses = mutableListOf<String>()

    override fun preTransform(classes: List<ClassNode>) {
        superClasses.clear()
    }

    override fun transformClass(classes: List<ClassNode>, clazz: ClassNode): Boolean {
        val superClass = clazz.superName
        if (superClass != null) {
            superClasses += superClass
        }

        superClasses.addAll(clazz.interfaces)

        return false
    }

    private fun isClassFinal(clazz: ClassNode): Boolean {
        if ((clazz.access and (Opcodes.ACC_ABSTRACT or Opcodes.ACC_INTERFACE)) != 0) {
            return false
        }

        return !superClasses.contains(clazz.name)
    }

    override fun postTransform(classes: List<ClassNode>) {
        var classesChanged = 0

        for (library in classPath.libraries) {
            for (clazz in library) {
                val access = clazz.access

                if (isClassFinal(clazz)) {
                    clazz.access = access or Opcodes.ACC_FINAL
                } else {
                    clazz.access = access and Opcodes.ACC_FINAL.inv()
                }

                if (clazz.access != access) {
                    classesChanged++
                }
            }
        }

        System.out.println("Updated final modifier on $classesChanged classes")
    }
}
