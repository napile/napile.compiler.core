# Napile Programming Language

Napile - programming language what based on Java/Kotlin logics, that running on VM prototype.

It have some special futures:

 * multiple inheritance
 * runtime generics (type parameters constructors, see [it](https://github.com/napile-lang/napile.classpath.draft/blob/master/src/testgen/napile/codegenTest/typeParameterTest/TWithConstructorTest.ns)
 * runtime type info
 * mutable & immutable types
 * and many others


# Developing

 * U need installed IntelliJ IDEA 12
 * Downloaded sources of
    * [napile.compiler](https://github.com/napile-lang/napile.compiler) - base source, compiler + IDEA plugin
    * [napile.classpath.draft](https://github.com/napile-lang/napile.classpath.draft) - draft classlib in napile
    * [napile.jvm](https://github.com/napile-lang/napile.jvm) - only for running, and bytecode improvements
    * [napile.asm](https://github.com/napile-lang/napile.asm) - only for bytecode improvements