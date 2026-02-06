package org.openrs2.deob.bytecode.transform

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.InsnNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.nextReal
import rs.lostcity.asm.transform.Transformer

public class FernflowerExceptionTransformer : Transformer() {
    private var nopsInserted = 0

    override fun preTransform(classes: List<ClassNode>) {
        nopsInserted = 0
    }

    override fun transformCode(classes: List<ClassNode>, clazz: ClassNode, method: MethodNode): Boolean {
        if (method.tryCatchBlocks.any { it.end.nextReal == null }) {
            method.instructions.add(InsnNode(Opcodes.NOP))
            nopsInserted++
        }

        return false
    }

    override fun postTransform(classes: List<ClassNode>) {
        System.out.println("Inserted $nopsInserted NOPs to correct Fernflower's exception generation")
    }
}
