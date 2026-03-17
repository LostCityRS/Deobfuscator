package rs.lostcity.deob.ast.transform;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.resolution.UnsolvedSymbolException;

public class RedundantThisTransformer extends AstTransformer {

    @Override
    public void transformUnit(CompilationUnit unit) {
        var fields = findAll(unit, FieldAccessExpr.class,
                        expr -> isUnqualifiedThis(expr.getScope()));

        for (var expr : fields) {
            var nameExpr = new NameExpr(expr.getNameAsString());
            expr.replace(nameExpr);

            try {
                if (nameExpr.resolve().isField()) {
                    // still resolves to a field, `this` was redundant
                    continue;
                }
            } catch (UnsolvedSymbolException e) {
                // can't resolve, revert to be safe
                // System.err.println("[" + getName() + "] WARNING: " + e.getMessage());
            }

            // revert since replacing `this.someName` with `someName` did not resolve to a field (shadowed)
            nameExpr.replace(expr);
        }

        walk(unit, MethodCallExpr.class, expr -> {
            // this.someMethod()
            if (expr.getScope().filter(RedundantThisTransformer::isUnqualifiedThis).isPresent()) {
                expr.setScope(null);
            }
        });
    }

    private static boolean isUnqualifiedThis(Expression expr) {
        return expr instanceof ThisExpr thisExpr && thisExpr.getTypeName().isEmpty();
    }
}
