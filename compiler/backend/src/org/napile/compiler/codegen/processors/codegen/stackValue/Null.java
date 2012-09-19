package org.napile.compiler.codegen.processors.codegen.stackValue;

import org.napile.asm.adapters.InstructionAdapter;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.processors.codegen.TypeConstants;

/**
 * @author VISTALL
 * @date 14:23/19.09.12
 */
public class Null extends StackValue
{
	public static final Null INSTANCE = new Null();

	private final Property propertyStackValue;

	public Null()
	{
		super(TypeConstants.NULL);

		propertyStackValue = new Property(NapileLangPackage.NULL.child(Name.identifier("INSTANCE")), getType(), true);
	}

	@Override
	public void put(TypeNode type, InstructionAdapter instructionAdapter)
	{
		propertyStackValue.put(type, instructionAdapter);
	}
}
