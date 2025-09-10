package rs.lostcity.deob.ast.transform;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.expr.*;
import com.github.javaparser.ast.stmt.ExpressionStmt;

public class IdentityTransformer extends AstTransformer {

	@Override
	public void transformUnit(CompilationUnit unit) {
		walk(unit, UnaryExpr.class, expr -> {
			switch (expr.getOperator()) {
				case PLUS -> {
					if (expr.getExpression() instanceof UnaryExpr inner) {
						if (inner.getOperator() == UnaryExpr.Operator.PLUS) {
							// +(+(...))
							expr.replace(inner.getExpression().clone());
						}
					}
				}
				case MINUS -> {
					if (expr.getExpression() instanceof UnaryExpr inner) {
						if (inner.getOperator() == UnaryExpr.Operator.MINUS) {
							// -(-(...))
							expr.replace(inner.getExpression().clone());
						}
					}
				}
			}
		});

		walk(unit, BinaryExpr.class, expr -> {
			switch (expr.getOperator()) {
				case PLUS -> {
					if (isZero(expr.getLeft())) {
						// 0 + x => x
						expr.replace(expr.getRight().clone());
					} else if (isZero(expr.getRight())) {
						// x + 0 => x
						expr.replace(expr.getLeft().clone());
					}
				}

				case MINUS -> {
					if (isZero(expr.getLeft())) {
						// 0 - x => -x
						expr.replace(new UnaryExpr(expr.getRight().clone(), UnaryExpr.Operator.MINUS));
					} else if (isZero(expr.getRight())) {
						// x - 0 => x
						expr.replace(expr.getLeft().clone());
					}
				}

				case MULTIPLY -> {
					if (isOne(expr.getLeft())) {
						// 1 * x => x
						expr.replace(expr.getRight().clone());
					} else if (isOne(expr.getRight())) {
						// x * 1 => x
						expr.replace(expr.getLeft().clone());
					}
				}

				case DIVIDE -> {
					if (isOne(expr.getRight())) {
						// x / 1 => x
						expr.replace(expr.getLeft().clone());
					}
				}

                case BINARY_AND -> {
                    if (isTrue(expr.getRight())) {
                        // x & true
						expr.replace(expr.getLeft().clone());
                    } else if (isTrue(expr.getLeft())) {
                        // true & x
						expr.replace(expr.getRight().clone());
                    }
                }

                case BINARY_OR -> {
                    if (isFalse(expr.getRight())) {
                        // x | false
						expr.replace(expr.getLeft().clone());
                    } else if (isFalse(expr.getLeft())) {
                        // false | x
						expr.replace(expr.getRight().clone());
                    }
                }
			}
		});

		findAll(unit, AssignExpr.class, expr ->
			switch (expr.getOperator()) {
                // x += 0, x -= 0
				case PLUS, MINUS -> isZero(expr.getValue());

                // x *= 1, x /= 1
				case MULTIPLY, DIVIDE -> isOne(expr.getValue());

                // x &= true
                case BINARY_AND -> isTrue(expr.getValue());

                // x |= false
                case BINARY_OR -> isFalse(expr.getValue());

				default -> false;
			}
		).forEach(expr -> {
			expr.getParentNode().ifPresent(parent -> {
				if (parent instanceof ExpressionStmt) {
					parent.remove();
				} else {
					expr.replace(expr.getTarget().clone());
				}
			});
        });
	}

	private boolean isZero(Expression expr) {
		return switch (expr) {
			case IntegerLiteralExpr intLitExpr -> intLitExpr.asNumber().intValue() == 0;
			case LongLiteralExpr longLitExpr -> longLitExpr.asNumber().longValue() == 0L;
            default -> false;
		};
	}

	private boolean isOne(Expression expr) {
		return switch (expr) {
			case IntegerLiteralExpr intLitExpr -> intLitExpr.asNumber().intValue() == 1;
			case LongLiteralExpr longLitExpr -> longLitExpr.asNumber().longValue() == 1L;
            default -> false;
		};
	}

	private boolean isTrue(Expression expr) {
		return switch (expr) {
			case BooleanLiteralExpr boolLitExpr -> boolLitExpr.getValue() == true;
            default -> false;
		};
	}

	private boolean isFalse(Expression expr) {
		return switch (expr) {
			case BooleanLiteralExpr boolLitExpr -> boolLitExpr.getValue() == false;
            default -> false;
		};
	}
}
