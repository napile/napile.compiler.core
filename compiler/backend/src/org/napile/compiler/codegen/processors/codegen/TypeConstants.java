package org.napile.compiler.codegen.processors.codegen;

import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;
import org.napile.compiler.CodeTodo;

/**
 * @author VISTALL
 * @date 20:37/17.09.12
 */
public interface TypeConstants
{
	TypeNode BOOL = new TypeNode(false, new ClassTypeNode(NapileLangPackage.BOOL));

	TypeNode NULL = new TypeNode(false, new ClassTypeNode(NapileLangPackage.NULL));

	TypeNode ANY = new TypeNode(false, new ClassTypeNode(NapileLangPackage.ANY));

	TypeNode ITERATOR__ANY__ = new TypeNode(false, new ClassTypeNode(CodeTodo.ITERATOR)).visitArgument(ANY);
}
