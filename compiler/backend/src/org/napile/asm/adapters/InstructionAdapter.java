package org.napile.asm.adapters;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.asm.Label;
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
	private int maxStacks;

	public InstructionAdapter()
	{}

	public void load(int index)
	{
		instructions.add(new LoadInstruction(index));
	}

	public void store(int index)
	{
		instructions.add(new StoreInstruction(index));
	}

	public void newInt(int value)
	{
		instructions.add(new NewIntInstruction(value));
	}

	public void newString(@NotNull String value)
	{
		instructions.add(new NewStringInstruction(value));
	}

	public void invokeSpecial(MethodRef methodRef)
	{
		instructions.add(new InvokeSpecialInstruction(methodRef));
	}

	public void invokeStatic(MethodRef methodRef)
	{
		instructions.add(new InvokeStaticInstruction(methodRef));
	}

	public void invokeVirtual(MethodRef methodRef)
	{
		instructions.add(new InvokeVirtualInstruction(methodRef));
	}

	public void putToVar(VariableRef variableRef)
	{
		instructions.add(new PutToVariableInstruction(variableRef));
	}

	public void putToStaticVar(VariableRef variableRef)
	{
		instructions.add(new PutToStaticVariableInstruction(variableRef));
	}

	public void getVar(VariableRef variableRef)
	{
		instructions.add(new GetVariableInstruction(variableRef));
	}

	public void getStaticVar(VariableRef variableRef)
	{
		instructions.add(new GetStaticVariableInstruction(variableRef));
	}

	public void newObject(TypeNode typeNode)
	{
		instructions.add(new NewObjectInstruction(typeNode));
	}

	public void returnVal()
	{
		instructions.add(new ReturnInstruction());
	}

	public void swap()
	{
		instructions.add(new SwapInstruction());
	}

	public void pop()
	{
		instructions.add(new PopInstruction());
	}

	public void dup()
	{
		instructions.add(new DupInstruction());
	}

	public void throwVal()
	{
		instructions.add(new ThrowInstruction());
	}

	public void mark(Label label)
	{
		maxStacks ++;
	}

	public void visitLocalVariable(String name)
	{
		maxLocals ++;
	}

	@NotNull
	public Collection<Instruction> getInstructions()
	{
		return instructions;
	}

	@Override
	public Iterator<Instruction> iterator()
	{
		return instructions.iterator();
	}

	public int getMaxLocals()
	{
		return maxLocals;
	}
}
