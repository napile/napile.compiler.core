# Overview

Napile - programming language what based on Java/Kotlin logics, that running on VM prototype.

It have some special futures:

 * pseudo multiple inheritance (pseudo ill be removed)
 * runtime generics (type parameters constructors, see [it](https://github.com/napile-lang/napile.classpath.draft/blob/master/src/testgen/napile/codegenTest/typeParameterTest/TWithConstructorTest.ns))
 * runtime type info
 * mutable & immutable types
 * and many others

# Hello World

```
import napile.logging.Logger

class Main
{
	static meth main(val arg : Array<String>)
	{
		Logger.System.info("Hello World!!!")
	}
}
```

# Developing

 * Need [IntelliJ IDEA 12.1 EAP](http://confluence.jetbrains.com/display/IDEADEV/IDEA+12.1+EAP)
 * Downloaded sources of
    * [napile.compiler](https://github.com/napile-lang/napile.compiler) - base source, compiler + IDEA plugin
    * [napile.classpath.draft](https://github.com/napile-lang/napile.classpath.draft) - draft classlib in napile
    * [napile.jvm](https://github.com/napile-lang/napile.jvm) - only for running, and bytecode improvements
    * [napile.asm](https://github.com/napile-lang/napile.asm) - only for bytecode improvements

# Help & Development

I am working on napile itself. I have free time. But my expirence in some futures is small(ex: VM on C++).

If you wana help me, email me.