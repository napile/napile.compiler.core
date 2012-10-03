package org.napile.compiler.codegen.processors.codegen.stackValue;

import org.napile.asm.adapters.InstructionAdapter;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;

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
		ClassTypeNode classTypeNode = (ClassTypeNode) getType().typeConstructorNode;

		if(value == null)
			StackValue.putNull(instructionAdapter);
		else if(classTypeNode.className.equals(NapileLangPackage.INT))
			instructionAdapter.newInt((Integer) value);
		else if(classTypeNode.className.equals(NapileLangPackage.STRING))
			instructionAdapter.newString((String) value);
		else if(classTypeNode.className.equals(NapileLangPackage.BOOL))
		{
			if(value == Boolean.TRUE)
				StackValue.putTrue(instructionAdapter);
			else
				StackValue.putFalse(instructionAdapter);
		}
		else
			throw new IllegalArgumentException(value.getClass().getName() + " "  + type);

		castTo(type, instructionAdapter);
	}
}