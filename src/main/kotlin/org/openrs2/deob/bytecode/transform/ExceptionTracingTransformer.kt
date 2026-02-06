package org.openrs2.deob.bytecode.transform

import org.objectweb.asm.tree.ClassNode
import org.objectweb.asm.tree.MethodNode
import org.openrs2.asm.InsnMatcher
import org.openrs2.asm.nextReal
import rs.lostcity.asm.transform.Transformer

/**
 * A [Transformer] responsible for removing Jagex's exception tracing. Jagex
 * inserts a try/catch block around every method that catches
 * [RuntimeException]s, wraps them with a custom [RuntimeException]
 * implementation and re-throws them. The wrapped exception's message contains
 * the values of the method's arguments. While this is for debugging and not
 * obfuscation, it is clearly automatically-generated and thus we remove these
 * exception handlers too.
 */
public class ExceptionTracingTransformer : Transformer() {
    private var tracingTryCatches = 0

    override fun preTransform(classes: List<ClassNode>) {
        tracingTryCatches = 0
    }

    override fun transformCode(
        classes: List<ClassNode>,
        clazz: ClassNode,
        method: MethodNode
    ): Boolean {
        for (match in CATCH_MATCHER.match(method)) {
            val foundTryCatch = method.tryCatchBlocks.removeIf { tryCatch ->
                tryCatch.type == "java/lang/RuntimeException" && tryCatch.handler.nextReal === match[0]
            }

            if (foundTryCatch) {
                match.forEach(method.instructions::remove)
                tracingTryCatches++
            }
        }

        return false
    }

    override fun postTransform(classes: List<ClassNode>) {
        System.out.println("Removed $tracingTryCatches tracing try/catch blocks")
    }

    private companion object {
        private val CATCH_MATCHER = InsnMatcher.compile(
            """
            (ASTORE ALOAD)?
            (LDC INVOKESTATIC |
                NEW DUP
                (LDC INVOKESPECIAL | INVOKESPECIAL LDC INVOKEVIRTUAL)
                ((ILOAD | LLOAD | FLOAD | DLOAD | (ALOAD IFNULL LDC GOTO LDC) | BIPUSH | SIPUSH | LDC) INVOKEVIRTUAL)*
                INVOKEVIRTUAL INVOKESTATIC
            )?
            ATHROW
        """
        )
    }
}
