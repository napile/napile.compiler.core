package org.napile.compiler.codegen.processors.codegen;

import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;

/**
 * @author VISTALL
 * @date 20:37/17.09.12
 */
public interface TypeConstants
{
	TypeNode NULL = new TypeNode(false, new ClassTypeNode(NapileLangPackage.NULL));

	TypeNode ANY = new TypeNode(false, new ClassTypeNode(NapileLangPackage.ANY));
}
