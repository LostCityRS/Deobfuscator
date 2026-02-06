package org.openrs2.deob.bytecode.transform

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodInsnNode
import org.openrs2.asm.MemberRef
import org.openrs2.asm.classpath.ClassPath
import org.openrs2.asm.filter.GlobMemberFilter
import org.openrs2.asm.filter.MemberFilter
import org.openrs2.asm.filter.UnionMemberFilter
import org.openrs2.asm.hasCode
import org.openrs2.asm.removeDeadCode
import org.openrs2.deob.bytecode.filter.ReflectedConstructorFilter
import org.openrs2.util.collect.DisjointSet
import org.openrs2.util.collect.UniqueQueue
import rs.lostcity.asm.transform.Transformer

public class UnusedMethodTransformer : Transformer() {
    private lateinit var inheritedMethodSets: DisjointSet<MemberRef>
    private lateinit var entryPoints: MemberFilter
    private val pendingMethods = UniqueQueue<MemberRef>()
    private val usedMethods = mutableSetOf<DisjointSet.Partition<MemberRef>>()

    override fun preTransform(classes: List<ClassNode>) {
        inheritedMethodSets = classPath.createInheritedMethodSets()
        entryPoints = UnionMemberFilter(GlobMemberFilter(listOf()), ReflectedConstructorFilter.create(classPath))

        queueEntryPoints(classPath)

        while (true) {
            val method = pendingMethods.removeFirstOrNull() ?: break
            analyzeMethod(classPath, method)
        }
    }

    private fun analyzeMethod(classPath: ClassPath, ref: MemberRef) {
        // find ClassNode/MethodNode
        val owner = classPath.getClassNode(ref.owner) ?: return
        val method = owner.methods.singleOrNull { it.name == ref.name && it.desc == ref.desc } ?: return
        if (!method.hasCode) {
            return
        }

        // iterate over non-dead call instructions
        method.removeDeadCode(owner.name)

        for (insn in method.instructions) {
            if (insn !is MethodInsnNode) {
                continue
            }

            val invokedRef = MemberRef(insn.owner, insn.name, insn.desc)
            val partition = inheritedMethodSets[invokedRef] ?: continue
            if (usedMethods.add(partition)) {
                pendingMethods += partition
            }
        }
    }

    private fun queueEntryPoints(classPath: ClassPath) {
        for (partition in inheritedMethodSets) {
            if (isEntryPoint(classPath, partition)) {
                pendingMethods.addAll(partition)
            }
        }
    }

    private fun isEntryPoint(classPath: ClassPath, partition: DisjointSet.Partition<MemberRef>): Boolean {
        for (method in partition) {
            val clazz = classPath[method.owner]!!

            if (entryPoints.matches(method) || clazz.dependency) {
                return true
            }
        }

        return false
    }

    override fun postTransform(classes: List<ClassNode>) {
        var methodsRemoved = 0

        for (library in classPath.libraries) {
            for (clazz in library) {
                val it = clazz.methods.iterator()

                while (it.hasNext()) {
                    val method = it.next()
                    if (method.access and Opcodes.ACC_NATIVE != 0) {
                        continue
                    } else if (entryPoints.matches(clazz.name, method.name, method.desc)) {
                        continue
                    }

                    val member = MemberRef(clazz, method)
                    val partition = inheritedMethodSets[member]!!

                    if (partition.any { classPath[it.owner]!!.dependency }) {
                        continue
                    } else if (usedMethods.contains(partition)) {
                        continue
                    }

                    it.remove()
                    methodsRemoved++
                }
            }
        }

        System.out.println("Removed $methodsRemoved unused methods")
    }
}
