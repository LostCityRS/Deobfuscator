package org.openrs2.deob.bytecode.transform

import org.objectweb.asm.Opcodes
import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.nextReal
import rs.lostcity.asm.transform.Transformer

/**
 * A [Transformer] responsible for removing [ZKM](http://www.zelix.com/klassmaster/)'s
 * [exception obfuscation](https://www.zelix.com/klassmaster/featuresExceptionObfuscation.html),
 * which inserts exception handlers that catch any type of exception and
 * immediately re-throw them. The exception handlers are inserted in locations
 * where there is no Java source code equivalent, confusing decompilers.
 */
public class ExceptionObfuscationTransformer : Transformer() {
    private var handlers = 0

    override fun preTransform(classes: List<ClassNode>) {
        handlers = 0
    }

    override fun transformCode(classes: List<ClassNode>, clazz: ClassNode, method: MethodNode): Boolean {
        for (insn in method.instructions) {
            if (insn.opcode != Opcodes.ATHROW) {
                continue
            }

            val foundTryCatch = method.tryCatchBlocks.removeIf { tryCatch ->
                tryCatch.handler.nextReal === insn
            }

            if (foundTryCatch) {
                method.instructions.remove(insn)
                handlers++
            }
        }

        return false
    }

    override fun postTransform(classes: List<ClassNode>) {
        System.out.println("Removed $handlers exception obfuscation handlers")
    }
}
