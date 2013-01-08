package org.napile.compiler.codegen.processors;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.tree.members.AbstractMemberNode;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.TypeParameterNode;
import org.napile.compiler.lang.descriptors.CallParameterDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.types.JetType;

/**
 * @author VISTALL
 * @date 19:57/08.10.12
 */
public class TypeParameterCodegen
{
	public static void gen(@NotNull List<TypeParameterDescriptor> typeParameters, @NotNull AbstractMemberNode<?> node, @NotNull BindingTrace bindingTrace, @NotNull ClassNode classNode)
	{
		for(TypeParameterDescriptor typeParameterDescriptor : typeParameters)
		{
			TypeParameterNode typeParameterNode = new TypeParameterNode(typeParameterDescriptor.getName());
			for(JetType superType : typeParameterDescriptor.getUpperBounds())
				typeParameterNode.supers.add(TypeTransformer.toAsmType(bindingTrace, superType, classNode));

			for(ConstructorDescriptor constructorDescriptor : typeParameterDescriptor.getConstructors())
			{
				List<MethodParameterNode> parameterNodes = new ArrayList<MethodParameterNode>(constructorDescriptor.getValueParameters().size());
				for(CallParameterDescriptor declaration : constructorDescriptor.getValueParameters())
				{
					MethodParameterNode methodParameterNode = new MethodParameterNode(ModifierCodegen.gen(declaration), declaration.getName(), TypeTransformer.toAsmType(bindingTrace, declaration.getType(), classNode));

					parameterNodes.add(methodParameterNode);
				}

				typeParameterNode.constructors.add(parameterNodes);
			}

			node.typeParameters.add(typeParameterNode);
		}
	}
}
