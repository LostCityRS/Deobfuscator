package rs.lostcity.deob.bytecode.transform.openrs2;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;
import rs.lostcity.asm.InsnMatcher;
import rs.lostcity.asm.InsnNodeUtil;
import rs.lostcity.asm.MemberRef;
import rs.lostcity.asm.transform.Transformer;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/*
 * Remove opaque predicates
 */
public class OpaquePredicateTransformer extends Transformer {
    private final InsnMatcher FLOW_OBSTRUCTOR_INITIALIZER_MATCHER = InsnMatcher.compile(
        """
        (GETSTATIC | ILOAD)
        IFEQ
        (((GETSTATIC ISTORE)? IINC ILOAD) | ((GETSTATIC | ILOAD) IFEQ ICONST GOTO ICONST))
        PUTSTATIC
        """
    );
    private final InsnMatcher OPAQUE_PREDICATE_MATCHER = InsnMatcher.compile("(GETSTATIC | ILOAD) (IFEQ | IFNE)");
    private final InsnMatcher PUT_MATCHER = InsnMatcher.compile("ICONST PUTSTATIC");
    private final InsnMatcher STORE_MATCHER = InsnMatcher.compile("GETSTATIC ISTORE");

    private final Set<String> flowObfuscators = new HashSet<>();
    private int assignments = 0;
    private int opaquePredicates = 0;

    @Override
    public void preTransform(List<ClassNode> classes) {
        flowObfuscators.clear();
        opaquePredicates = 0;
        assignments = 0;

        for (ClassNode clazz : classes) {
            for (MethodNode method : clazz.methods) {
                if (InsnNodeUtil.hasCode(method)) {
                    findFlowObstructors(method, clazz);
                }
            }
        }

        System.out.println("Identified flow obstructors " + flowObfuscators);
    }

    private void findFlowObstructors(MethodNode method, ClassNode owner) {
        for (List<AbstractInsnNode> match : this.FLOW_OBSTRUCTOR_INITIALIZER_MATCHER.match(method.instructions)) {
            FieldInsnNode putstatic = (FieldInsnNode) match.getLast();

            AbstractInsnNode first = match.getFirst();
            if (first instanceof VarInsnNode) {
                boolean storeFound = false;
                for (List<AbstractInsnNode> storeMatch : this.STORE_MATCHER.match(method.instructions)) {
                    FieldInsnNode getstatic = (FieldInsnNode) storeMatch.getFirst();
                    if (getstatic.name.equals(putstatic.name)) {
                        storeFound = true;
                        break;
                    }
                }

                if (!storeFound) {
                    continue;
                }
            }

            flowObfuscators.add(new MemberRef(putstatic).toString());

            // remove initializer
            match.subList(2, match.size()).forEach(method.instructions::remove);

            // remove field
            // owner.fields.removeIf(field -> field.name.equals(putstatic.name) && field.desc.equals(putstatic.desc));
        }
    }

    private boolean isFlowObstructor(FieldInsnNode insn) {
        return flowObfuscators.contains(new MemberRef(insn).toString());
    }

    private boolean isOpaquePredicate(MethodNode method, List<AbstractInsnNode> match) {
        AbstractInsnNode load = match.getFirst();

        if (load instanceof FieldInsnNode && load.getOpcode() == Opcodes.GETSTATIC) {
            return isFlowObstructor((FieldInsnNode) load);
        }

        VarInsnNode iload = (VarInsnNode) load;
        for (List<AbstractInsnNode> storeMatch : this.STORE_MATCHER.match(method.instructions)) {
            FieldInsnNode getstatic = (FieldInsnNode) storeMatch.get(0);
            VarInsnNode istore = (VarInsnNode) storeMatch.get(1);
            if (isFlowObstructor(getstatic) && iload.var == istore.var) {
                return true;
            }
        }

        return false;
    }

    private boolean isRedundantPut(List<AbstractInsnNode> match) {
        FieldInsnNode putstatic = (FieldInsnNode) match.get(1);
        return isFlowObstructor(putstatic);
    }

    private boolean isRedundantStore(List<AbstractInsnNode> match) {
        FieldInsnNode getstatic = (FieldInsnNode) match.get(0);
        return isFlowObstructor(getstatic);
    }

    @Override
    public boolean transformCode(List<ClassNode> classes, ClassNode clazz, MethodNode method) {
        for (List<AbstractInsnNode> match : this.OPAQUE_PREDICATE_MATCHER.match(method.instructions)) {
            if (isOpaquePredicate(method, match)) {
                JumpInsnNode branch = (JumpInsnNode) match.get(1);
                switch (branch.getOpcode()) {
                    case Opcodes.IFEQ:
                        method.instructions.remove(match.getFirst());
                        branch.setOpcode(Opcodes.GOTO);
                        break;
                    case Opcodes.IFNE:
                        match.forEach(method.instructions::remove);
                        break;
                    default:
                        throw new IllegalStateException("Invalid opcode");
                }

                opaquePredicates++;
            }
        }

        // remove redundant stores
        for (List<AbstractInsnNode> match : this.STORE_MATCHER.match(method.instructions)) {
            if (isRedundantStore(match)) {
                match.forEach(method.instructions::remove);
                assignments++;
            }
        }

        // remove redundant field assignments
        for (List<AbstractInsnNode> match : this.PUT_MATCHER.match(method.instructions)) {
            if (isRedundantPut(match)) {
                match.forEach(method.instructions::remove);
                assignments++;
            }
        }

        return false;
    }

    @Override
    public void postTransform(List<ClassNode> classes) {
        System.out.println("Removed " + opaquePredicates + " opaque predicates and " + assignments + " redundant assignments");
    }
}
