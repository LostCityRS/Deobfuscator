package org.openrs2.deob.bytecode.transform

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.AbstractInsnNode
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.objectweb.asm.tree.VarInsnNode
import org.openrs2.asm.deleteExpression
import org.openrs2.asm.isPure
import org.openrs2.deob.bytecode.analysis.LiveVariableAnalyzer
import rs.lostcity.asm.transform.Transformer

public class UnusedLocalTransformer : Transformer() {
    private var localsRemoved = 0

    override fun preTransform(classes: List<ClassNode>) {
        localsRemoved = 0
    }

    override fun transformCode(classes: List<ClassNode>, clazz: ClassNode, method: MethodNode): Boolean {
        val analyzer = LiveVariableAnalyzer(clazz.name, method)
        analyzer.analyze()

        val deadStores = mutableListOf<AbstractInsnNode>()

        for (insn in method.instructions) {
            if (insn !is VarInsnNode || !STORE_OPCODES.contains(insn.opcode)) {
                continue
            }

            val live = analyzer.getInSet(insn)?.contains(insn.`var`) ?: false
            if (live) {
                continue
            }

            deadStores += insn
        }

        for (insn in deadStores) {
            if (method.instructions.deleteExpression(insn, AbstractInsnNode::isPure)) {
                localsRemoved++
            }
        }

        return false
    }

    override fun postTransform(classes: List<ClassNode>) {
        System.out.println("Removed $localsRemoved unused local variables")
    }

    private companion object {
        private val STORE_OPCODES = setOf(
            Opcodes.ISTORE,
            Opcodes.LSTORE,
            Opcodes.FSTORE,
            Opcodes.DSTORE,
            Opcodes.ASTORE
        )
    }
}
