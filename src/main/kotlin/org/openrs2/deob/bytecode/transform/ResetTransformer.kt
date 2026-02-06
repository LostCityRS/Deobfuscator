package org.openrs2.deob.bytecode.transform

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.*
import org.openrs2.asm.MemberRef
import org.openrs2.asm.hasCode
import org.openrs2.asm.nextReal
import org.openrs2.asm.removeDeadCode
import rs.lostcity.asm.transform.Transformer

public class ResetTransformer : Transformer() {
    private val resetMethods = mutableSetOf<MemberRef>()

    override fun preTransform(classes: List<ClassNode>) {
        resetMethods.clear()

        for (library in classPath.libraries) {
            for (clazz in library) {
                for (method in clazz.methods) {
                    if (!method.hasCode) {
                        continue
                    }

                    val masterReset = findMasterReset(method) ?: continue
                    System.out.println("Identified master reset method $masterReset")

                    val resetClass = classPath.getClassNode("client!client")!!
                    val resetMethod = resetClass.methods.first {
                        it.name == masterReset.name && it.desc == masterReset.desc
                    }

                    findResetMethods(resetMethods, resetClass, resetMethod)

                    resetMethod.instructions.clear()
                    resetMethod.tryCatchBlocks.clear()
                    resetMethod.instructions.add(InsnNode(Opcodes.RETURN))
                }
            }
        }
    }

    override fun transformClass(classes: List<ClassNode>, clazz: ClassNode): Boolean {
        clazz.methods.removeIf { resetMethods.contains(MemberRef(clazz, it)) }
        return false
    }

    override fun postTransform(classes: List<ClassNode>) {
        System.out.println("Removed ${resetMethods.size} reset methods")
    }

    private companion object {
        private fun findMasterReset(method: MethodNode): MemberRef? {
            var shutdownLdc: AbstractInsnNode? = null
            for (insn in method.instructions) {
                if (insn is LdcInsnNode && insn.cst == "Shutdown complete - clean:") {
                    shutdownLdc = insn
                    break
                }
            }

            var insn = shutdownLdc
            while (insn != null) {
                if (insn !is VarInsnNode || insn.opcode != Opcodes.ALOAD) {
                    insn = insn.previous
                    continue
                }

                if (insn.`var` != 0) {
                    insn = insn.previous
                    continue
                }

                val nextInsn = insn.nextReal
                if (nextInsn !is MethodInsnNode || nextInsn.opcode != Opcodes.INVOKEVIRTUAL) {
                    insn = insn.previous
                    continue
                }

                if (nextInsn.desc != "()V") {
                    insn = insn.previous
                    continue
                }

                return MemberRef(nextInsn)
            }

            return null
        }

        private fun findResetMethods(resetMethods: MutableSet<MemberRef>, clazz: ClassNode, method: MethodNode) {
            method.removeDeadCode(clazz.name)

            for (insn in method.instructions) {
                if (insn is MethodInsnNode && insn.opcode == Opcodes.INVOKESTATIC) {
                    resetMethods.add(MemberRef(insn))
                }
            }
        }
    }
}
