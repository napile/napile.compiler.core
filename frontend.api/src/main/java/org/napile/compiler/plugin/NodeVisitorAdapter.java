package org.napile.compiler.plugin;

import org.napile.asm.tree.members.AnnotationNode;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.MacroNode;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.NodeVisitor;
import org.napile.asm.tree.members.TypeParameterNode;
import org.napile.asm.tree.members.VariableNode;
import org.napile.asm.tree.members.bytecode.tryCatch.TryCatchBlockNode;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;
import org.napile.asm.tree.members.types.constructors.MethodTypeNode;
import org.napile.asm.tree.members.types.constructors.MultiTypeNode;
import org.napile.asm.tree.members.types.constructors.ThisTypeNode;
import org.napile.asm.tree.members.types.constructors.TypeParameterValueTypeNode;

/**
 * @author VISTALL
 * @since 22:33/15.05.13
 */
public class NodeVisitorAdapter<A, R> implements NodeVisitor<A, R>
{
	@Override
	public R visitAnnotationNode(AnnotationNode annotationNode, A a2)
	{
		return null;
	}

	@Override
	public R visitClassNode(ClassNode classNode, A a2)
	{
		return null;
	}

	@Override
	public R visitMethodNode(MethodNode methodNode, A a2)
	{
		return null;
	}

	@Override
	public R visitMacroNode(MacroNode methodNode, A a2)
	{
		return null;
	}

	@Override
	public R visitVariableNode(VariableNode variableNode, A a2)
	{
		return null;
	}

	@Override
	public R visitMethodParameterNode(MethodParameterNode methodParameterNode, A a2)
	{
		return null;
	}

	@Override
	public R visitTypeParameter(TypeParameterNode typeParameterNode, A a2)
	{
		return null;
	}

	@Override
	public R visitTypeNode(TypeNode typeNode, A a2)
	{
		return null;
	}

	@Override
	public R visitClassTypeNode(ClassTypeNode classTypeNode, A a2)
	{
		return null;
	}

	@Override
	public R visitThisTypeNode(ThisTypeNode thisTypeNode, A a2)
	{
		return null;
	}

	@Override
	public R visitMethodTypeNode(MethodTypeNode methodTypeNode, A a2)
	{
		return null;
	}

	@Override
	public R visitMultiTypeNode(MultiTypeNode multiTypeNode, A a2)
	{
		return null;
	}

	@Override
	public R visitTypeParameterValueTypeNode(TypeParameterValueTypeNode typeParameterValueTypeNode, A a2)
	{
		return null;
	}

	@Override
	public R visitTryCatchBlockNode(TryCatchBlockNode tryCatchBlockNode, A a2)
	{
		return null;
	}
}
