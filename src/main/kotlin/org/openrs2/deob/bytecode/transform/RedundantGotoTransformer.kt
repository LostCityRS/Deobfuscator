package org.openrs2.deob.bytecode.transform

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.JumpInsnNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.nextReal
import org.openrs2.asm.removeDeadCode
import rs.lostcity.asm.transform.Transformer

public class RedundantGotoTransformer : Transformer() {
    private var removed = 0

    override fun preTransform(classes: List<ClassNode>) {
        removed = 0
    }

    override fun preTransformMethod(
        classes: List<ClassNode>,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        method.removeDeadCode(clazz.name)
        return false
    }

    override fun transformCode(classes: List<ClassNode>, clazz: ClassNode, method: MethodNode): Boolean {
        for (instruction in method.instructions) {
            if (instruction.opcode == Opcodes.GOTO) {
                instruction as JumpInsnNode

                if (instruction.nextReal === instruction.label.nextReal) {
                    method.instructions.remove(instruction)
                    removed++
                }
            }
        }

        return false
    }

    override fun postTransform(classes: List<ClassNode>) {
        System.out.println("Removed $removed redundant GOTO instructions")
    }
}
