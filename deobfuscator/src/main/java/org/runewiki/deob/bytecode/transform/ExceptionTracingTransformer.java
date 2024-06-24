package org.runewiki.deob.bytecode.transform;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.runewiki.asm.AsmUtil;
import org.runewiki.asm.InsnMatcher;
import org.runewiki.asm.transform.Transformer;

import java.util.List;
import java.util.Objects;

/*
 * Remove the autogenerated try/catch blocks that report client errors to the server
 */
public class ExceptionTracingTransformer extends Transformer {
    private final InsnMatcher CATCH_MATCHER = InsnMatcher.compile(
        "ASTORE? ALOAD? " +
        "((LDC INVOKESTATIC) | (NEW DUP) " +
        "((LDC INVOKESPECIAL) | (INVOKESPECIAL LDC INVOKEVIRTUAL)) " +
        "((ILOAD | LLOAD | FLOAD | DLOAD | ALOAD | (ALOAD IFNULL LDC GOTO LDC) | BIPUSH | SIPUSH | LDC) INVOKEVIRTUAL)* " +
        "INVOKEVIRTUAL? INVOKEVIRTUAL INVOKESTATIC)? " +
        "(NEW DUP INVOKESPECIAL)? ATHROW"
    );

    /* openrs2 has:
    (ASTORE ALOAD)?
    (LDC INVOKESTATIC | NEW DUP
        (LDC INVOKESPECIAL | INVOKESPECIAL LDC INVOKEVIRTUAL)
        ((ILOAD | LLOAD | FLOAD | DLOAD | (ALOAD IFNULL LDC GOTO LDC) | BIPUSH | SIPUSH | LDC) INVOKEVIRTUAL)*
        INVOKEVIRTUAL INVOKESTATIC
    )?
    ATHROW
    */

    private int tryCatches = 0;

    @Override
    public void preTransform(List<ClassNode> classes) {
        this.tryCatches = 0;
    }

    @Override
    public boolean transformCode(List<ClassNode> classes, ClassNode clazz, MethodNode method) {
        List<List<AbstractInsnNode>> matches = this.CATCH_MATCHER.match(method);

        for (List<AbstractInsnNode> match : matches) {
            boolean foundTryCatch = method.tryCatchBlocks.removeIf(tryCatch ->
                Objects.equals(tryCatch.type, "java/lang/RuntimeException") &&
                AsmUtil.getNextReal(tryCatch.handler).equals(match.get(0)));

            if (foundTryCatch) {
                match.forEach(method.instructions::remove);
                this.tryCatches++;
            }
        }

        return false;
    }

    @Override
    public void postTransform(List<ClassNode> classes) {
        System.out.println("Removed " + this.tryCatches + " tracing try/catch blocks");
    }
}
