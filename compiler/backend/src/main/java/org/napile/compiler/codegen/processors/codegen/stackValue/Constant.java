package org.napile.compiler.codegen.processors.codegen.stackValue;

import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;
import org.napile.compiler.codegen.processors.PositionMarker;
import com.intellij.psi.PsiElement;

public class Constant extends StackValue
{
	private final Object value;

	public Constant(PsiElement target, Object value, TypeNode typeNode)
	{
		super(target, typeNode);

		this.value = value;
	}

	@Override
	public void put(TypeNode type, InstructionAdapter instructionAdapter, PositionMarker positionMarker)
	{
		ClassTypeNode classTypeNode = (ClassTypeNode) getType().typeConstructorNode;

		if(value == null)
			join(instructionAdapter, positionMarker).putNull();
		else if(classTypeNode.className.equals(NapileLangPackage.BYTE))
			join(instructionAdapter, positionMarker).newByte((Byte) value);
		else if(classTypeNode.className.equals(NapileLangPackage.SHORT))
			join(instructionAdapter, positionMarker).newShort((Short) value);
		else if(classTypeNode.className.equals(NapileLangPackage.INT))
			join(instructionAdapter, positionMarker).newInt((Integer) value);
		else if(classTypeNode.className.equals(NapileLangPackage.LONG))
			join(instructionAdapter, positionMarker).newLong((Long) value);
		else if(classTypeNode.className.equals(NapileLangPackage.FLOAT))
			join(instructionAdapter, positionMarker).newFloat((Float) value);
		else if(classTypeNode.className.equals(NapileLangPackage.DOUBLE))
			join(instructionAdapter, positionMarker).newDouble((Double) value);
		else if(classTypeNode.className.equals(NapileLangPackage.CHAR))
			join(instructionAdapter, positionMarker).newChar((Character) value);
		else if(classTypeNode.className.equals(NapileLangPackage.STRING))
			join(instructionAdapter, positionMarker).newString((String) value);
		else if(classTypeNode.className.equals(NapileLangPackage.BOOL))
		{
			if(value == Boolean.TRUE)
				join(instructionAdapter, positionMarker).putTrue();
			else
				join(instructionAdapter, positionMarker).putFalse();
		}
		else
			throw new IllegalArgumentException(value.getClass().getName() + " "  + type);

		castTo(type, instructionAdapter);
	}
}
