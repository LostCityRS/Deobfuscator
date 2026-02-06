package org.openrs2.asm.classpath

import org.objectweb.asm.tree.ClassNode
import java.util.*

public class Library(public val name: String) : Iterable<ClassNode> {
    private var classes: SortedMap<String, ClassNode> = TreeMap()

    public constructor(name: String, library: Library) : this(name) {
        for (clazz in library.classes.values) {
            val copy = ClassNode()
            clazz.accept(copy)
            add(copy)
        }
    }

    public operator fun contains(name: String): Boolean {
        return classes.containsKey(name)
    }

    public operator fun get(name: String): ClassNode? {
        return classes[name]
    }

    public fun add(clazz: ClassNode): ClassNode? {
        return classes.put(clazz.name, clazz)
    }

    public fun remove(name: String): ClassNode? {
        return classes.remove(name)
    }

    override fun iterator(): Iterator<ClassNode> {
        return classes.values.iterator()
    }
}
