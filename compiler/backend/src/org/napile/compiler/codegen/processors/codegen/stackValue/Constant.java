package org.napile.compiler.codegen.processors.codegen.stackValue;

import org.napile.asm.adapters.InstructionAdapter;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.tree.members.types.ClassTypeNode;
import org.napile.asm.tree.members.types.TypeNode;

public class Constant extends StackValue
{
	private Object value;

	public Constant(Object value, TypeNode typeNode)
	{
		super(typeNode);

		this.value = value;
	}

	@Override
	public void put(TypeNode type, InstructionAdapter instructionAdapter)
	{
		ClassTypeNode classTypeNode = (ClassTypeNode) type.typeConstructorNode;

		if(classTypeNode.getClassName().equals(NapileLangPackage.INT))
			instructionAdapter.newInt((Integer) value);
		else if(classTypeNode.getClassName().equals(NapileLangPackage.STRING))
			instructionAdapter.newString((String) value);
		else
			throw new IllegalArgumentException(value.getClass().getName() + " "  + type);

		castTo(type, instructionAdapter);
	}
}
