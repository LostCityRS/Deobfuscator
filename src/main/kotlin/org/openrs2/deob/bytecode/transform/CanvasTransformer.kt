package org.openrs2.deob.bytecode.transform

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import rs.lostcity.asm.transform.Transformer

public class CanvasTransformer : Transformer() {
    override fun transformClass(
        classes: List<ClassNode>,
        clazz: ClassNode
    ): Boolean {
        if (clazz.superName != "java/awt/Canvas") {
            return false
        }

        if (clazz.access and Opcodes.ACC_FINAL == 0) {
            return false
        }

        clazz.interfaces.remove("java/awt/event/FocusListener")
        return false
    }
}
