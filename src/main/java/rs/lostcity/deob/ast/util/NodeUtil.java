package rs.lostcity.deob.ast.util;

import com.github.javaparser.ast.Node;
import com.github.javaparser.resolution.types.ResolvedType;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static com.github.javaparser.ast.Node.TreeTraversal;

public class NodeUtil {
	public static boolean isClass(ResolvedType type) {
		return type.isReferenceType() && type.asReferenceType().getQualifiedName().equals("java.lang.Class");
	}

	public static boolean isString(ResolvedType type) {
		return type.isReferenceType() && type.asReferenceType().getQualifiedName().equals("java.lang.String");
	}

    public static <T extends Node> List<T> findAll(Node node, Class<T> nodeType, Predicate<T> predicate) {
        return node.findAll(nodeType, predicate);
    }

	public static <T extends Node> void walk(Node node, Class<T> nodeType, Consumer<T> consumer) {
		node.walk(TreeTraversal.POSTORDER, n -> {
			if (nodeType.isAssignableFrom(n.getClass())) {
				consumer.accept(nodeType.cast(n));
			}
		});
	}
}
