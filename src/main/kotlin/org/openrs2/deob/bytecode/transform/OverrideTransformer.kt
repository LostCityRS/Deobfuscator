package org.openrs2.deob.bytecode.transform

import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import org.objectweb.asm.tree.AnnotationNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.MemberDesc
import rs.lostcity.asm.transform.Transformer

public class OverrideTransformer : Transformer() {
    private var overrides = 0

    override fun preTransform(classes: List<ClassNode>) {
        overrides = 0
    }

    override fun transformCode(
        classes: List<ClassNode>,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        if (method.name == "<init>" || method.name == "<clinit>" || method.access and Opcodes.ACC_STATIC != 0) {
            return false
        }

        if (!classPath[clazz.name]!!.isOverride(MemberDesc(method))) {
            return false
        }

        if (method.invisibleAnnotations != null && method.invisibleAnnotations.any { it.desc == OVERRIDE_DESC }) {
            return false
        }

        if (method.invisibleAnnotations == null) {
            method.invisibleAnnotations = mutableListOf()
        }
        method.invisibleAnnotations.add(AnnotationNode(OVERRIDE_DESC))
        overrides++

        return false
    }

    override fun postTransform(classes: List<ClassNode>) {
        System.out.println("Added $overrides override annotations")
    }

    private companion object {
        private val OVERRIDE_DESC = Type.getDescriptor(Override::class.java)
    }
}
