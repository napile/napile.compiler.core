package org.napile.asm.adapters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.tree.members.bytecode.Instruction;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.bytecode.VariableRef;
import org.napile.asm.tree.members.bytecode.impl.*;
import org.napile.asm.tree.members.types.TypeNode;

/**
 * @author VISTALL
 * @date 20:25/17.09.12
 */
public class InstructionAdapter implements Iterable<Instruction>
{
	@NotNull
	private final List<Instruction> instructions = new ArrayList<Instruction>();

	private int maxLocals;

	public InstructionAdapter()
	{
	}

	@NotNull
	public LoadInstruction load(int index)
	{
		return add(new LoadInstruction(index));
	}

	@NotNull
	public StoreInstruction store(int index)
	{
		return add(new StoreInstruction(index));
	}

	@NotNull
	public NewIntInstruction newInt(int value)
	{
		return add(new NewIntInstruction(value));
	}

	@NotNull
	public NewStringInstruction newString(@NotNull String value)
	{
		return add(new NewStringInstruction(value));
	}

	@NotNull
	public InvokeSpecialInstruction invokeSpecial(MethodRef methodRef)
	{
		return add(new InvokeSpecialInstruction(methodRef));
	}

	@NotNull
	public InvokeStaticInstruction invokeStatic(MethodRef methodRef)
	{
		return add(new InvokeStaticInstruction(methodRef));
	}

	@NotNull
	public InvokeVirtualInstruction invokeVirtual(MethodRef methodRef)
	{
		return add(new InvokeVirtualInstruction(methodRef));
	}

	@NotNull
	public PutToVariableInstruction putToVar(VariableRef variableRef)
	{
		return add(new PutToVariableInstruction(variableRef));
	}

	@NotNull
	public PutToStaticVariableInstruction putToStaticVar(VariableRef variableRef)
	{
		return add(new PutToStaticVariableInstruction(variableRef));
	}

	@NotNull
	public GetVariableInstruction getVar(VariableRef variableRef)
	{
		return add(new GetVariableInstruction(variableRef));
	}

	@NotNull
	public GetStaticVariableInstruction getStaticVar(VariableRef variableRef)
	{
		return add(new GetStaticVariableInstruction(variableRef));
	}

	@NotNull
	public NewObjectInstruction newObject(TypeNode typeNode)
	{
		return add(new NewObjectInstruction(typeNode));
	}

	@NotNull
	public ReturnInstruction returnVal()
	{
		return add(new ReturnInstruction());
	}

	@NotNull
	public SwapInstruction swap()
	{
		return add(new SwapInstruction());
	}

	@NotNull
	public PopInstruction pop()
	{
		return add(new PopInstruction());
	}

	@NotNull
	public DupInstruction dup()
	{
		return add(new DupInstruction());
	}

	@NotNull
	public ThrowInstruction throwVal()
	{
		return add(new ThrowInstruction());
	}

	@NotNull
	public JumpIfInstruction jumpIf(int val)
	{
		return add(new JumpIfInstruction(val));
	}

	@NotNull
	public JumpInstruction jump(int val)
	{
		return add(new JumpInstruction(val));
	}

	@NotNull
	public ReservedInstruction reserve()
	{
		return add(new ReservedInstruction());
	}

	public int size()
	{
		return instructions.size();
	}

	public void replace(@NotNull Instruction instruction1, @NotNull Instruction instruction2)
	{
		int i = instructions.indexOf(instruction1);
		if(i < 0)
			throw new IndexOutOfBoundsException();

		instructions.set(i, instruction2);
	}

	private <T extends Instruction> T add(T t)
	{
		instructions.add(t);
		return t;
	}

	public void visitLocalVariable(String name)
	{
		maxLocals++;
	}

	@NotNull
	public Collection<Instruction> getInstructions()
	{
		return instructions;
	}

	@Override
	@NotNull
	public Iterator<Instruction> iterator()
	{
		return instructions.iterator();
	}

	public int getMaxLocals()
	{
		return maxLocals;
	}
}
