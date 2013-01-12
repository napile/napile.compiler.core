/*
 * Copyright 2010-2012 napile.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.napile.compiler.codegen.processors;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.AsmConstants;
import org.napile.asm.Modifier;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.MethodNode;
import org.napile.asm.tree.members.VariableNode;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.bytecode.adapter.ReservedInstruction;
import org.napile.asm.tree.members.bytecode.tryCatch.CatchBlock;
import org.napile.asm.tree.members.bytecode.tryCatch.TryBlock;
import org.napile.asm.tree.members.bytecode.tryCatch.TryCatchBlockNode;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;
import org.napile.asm.tree.members.types.constructors.TypeParameterValueTypeNode;
import org.napile.compiler.codegen.CompilationException;
import org.napile.compiler.codegen.processors.codegen.CallTransformer;
import org.napile.compiler.codegen.processors.codegen.CallableMethod;
import org.napile.compiler.codegen.processors.codegen.FrameMap;
import org.napile.compiler.codegen.processors.codegen.TypeConstants;
import org.napile.compiler.codegen.processors.codegen.loopGen.DoWhileLoopCodegen;
import org.napile.compiler.codegen.processors.codegen.loopGen.ForLoopCodegen;
import org.napile.compiler.codegen.processors.codegen.loopGen.LabelLoopCodegen;
import org.napile.compiler.codegen.processors.codegen.loopGen.LoopCodegen;
import org.napile.compiler.codegen.processors.codegen.loopGen.WhileLoopCodegen;
import org.napile.compiler.codegen.processors.codegen.stackValue.Local;
import org.napile.compiler.codegen.processors.codegen.stackValue.MultiVariable;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.codegen.processors.injection.InjectionCodegen;
import org.napile.compiler.injection.CodeInjection;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.calls.AutoCastReceiver;
import org.napile.compiler.lang.resolve.calls.DefaultValueArgument;
import org.napile.compiler.lang.resolve.calls.ExpressionValueArgument;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.resolve.calls.ResolvedValueArgument;
import org.napile.compiler.lang.resolve.constants.CompileTimeConstant;
import org.napile.compiler.lang.resolve.scopes.receivers.ClassReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.expressions.OperatorConventions;
import com.google.common.collect.Lists;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.Function;

/**
 * @author VISTALL
 * @date 0:24/07.09.12
 * <p/>
 * base code getting from Kotlin
 */
public class ExpressionCodegen extends NapileVisitor<StackValue, StackValue>
{
	@NotNull
	public final BindingTrace bindingTrace;
	@NotNull
	private final Map<NapileElement, Local> tempVariables = new HashMap<NapileElement, Local>();
	@NotNull
	public final InstructionAdapter instructs;
	@NotNull
	public final FrameMap frameMap;
	@NotNull
	private final TypeNode returnType;
	@NotNull
	private final Deque<LoopCodegen<?>> loops = new ArrayDeque<LoopCodegen<?>>();
	@NotNull
	private final Map<VariableDescriptor, StackValue> wrappedVariables = new HashMap<VariableDescriptor, StackValue>();
	@NotNull
	public final ClassNode classNode;

	private final boolean isInstanceConstructor;

	public ExpressionCodegen(@NotNull BindingTrace b, @NotNull TypeNode r, @NotNull ClassNode c)
	{
		bindingTrace = b;
		isInstanceConstructor = false;
		returnType = r;
		classNode = c;
		instructs = new InstructionAdapter();
		frameMap = new FrameMap();
	}

	public ExpressionCodegen(@NotNull BindingTrace b, @NotNull CallableDescriptor d, @NotNull ClassNode c, @NotNull Map<VariableDescriptor, StackValue> w, @Nullable InstructionAdapter adapter)
	{
		bindingTrace = b;
		classNode = c;
		instructs = adapter == null ? new InstructionAdapter() : adapter;
		wrappedVariables.putAll(w);
		isInstanceConstructor = d instanceof ConstructorDescriptor;
		returnType = isInstanceConstructor ? TypeTransformer.toAsmType(bindingTrace, ((ClassDescriptor) d.getContainingDeclaration()).getDefaultType(), classNode) : TypeTransformer.toAsmType(bindingTrace, d.getReturnType(), classNode);
		frameMap = new FrameMap();

		if(!d.isStatic())
			frameMap.enterTemp();

		for(CallParameterDescriptor p : d.getValueParameters())
		{
			int index = frameMap.enter(p);
			if(wrapVariableIfNeed(p))
			{
				StackValue wrapped = wrappedVariables.get(p);

				if(wrapped.receiverSize() == 1)
					instructs.load(0);

				instructs.load(index);

				wrapped.store(wrapped.getType(), instructs);
			}
		}
	}

	@Override
	public StackValue visitVariable(NapileVariable property, StackValue receiver)
	{
		final NapileExpression initializer = property.getInitializer();
		if(initializer == null)
			return StackValue.none();

		initializeLocalVariable(property, new Function<VariableDescriptor, Void>()
		{
			@Override
			public Void fun(VariableDescriptor descriptor)
			{
				TypeNode varType = TypeTransformer.toAsmType(bindingTrace, descriptor.getType(), classNode);
				gen(initializer, varType);
				return null;
			}
		});
		return StackValue.none();
	}

	@Override
	public StackValue visitExpression(NapileExpression expression, StackValue receiver)
	{
		throw new UnsupportedOperationException("Codegen for " + expression + " is not yet implemented");
	}

	@Override
	public StackValue visitDotQualifiedExpression(NapileDotQualifiedExpression expression, StackValue receiver)
	{
		return genQualified(StackValue.none(), expression.getSelectorExpression());
	}

	@Override
	public StackValue visitSafeQualifiedExpression(NapileSafeQualifiedExpression expression, StackValue data)
	{
		return genQualified(StackValue.none(), expression.getSelectorExpression());
	}

	@Override
	public StackValue visitParenthesizedExpression(NapileParenthesizedExpression expression, StackValue receiver)
	{
		return genQualified(receiver, expression.getExpression());
	}

	@Override
	public StackValue visitBlockExpression(NapileBlockExpression expression, StackValue receiver)
	{
		List<NapileElement> statements = expression.getStatements();
		return generateBlock(statements);
	}

	@Override
	public StackValue visitForExpression(NapileForExpression expression, StackValue data)
	{
		return loopGen(new ForLoopCodegen(expression));
	}

	@Override
	public StackValue visitWhileExpression(NapileWhileExpression expression, StackValue data)
	{
		return loopGen(new WhileLoopCodegen(expression));
	}

	@Override
	public StackValue visitDoWhileExpression(NapileDoWhileExpression expression, StackValue data)
	{
		return loopGen(new DoWhileLoopCodegen(expression));
	}

	@Override
	public StackValue visitLabelExpression(NapileLabelExpression expression, StackValue data)
	{
		return loopGen(new LabelLoopCodegen(expression));
	}

	@Override
	public StackValue visitContinueExpression(NapileContinueExpression expression, StackValue data)
	{
		LoopCodegen<?> last = loops.getLast();

		last.addContinue(instructs);

		return StackValue.none();
	}

	@Override
	public StackValue visitTryExpression(NapileTryExpression expression, StackValue data)
	{
		JetType jetType = bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, expression);

		TypeNode expectedAsmType = TypeTransformer.toAsmType(bindingTrace, jetType, classNode);

		final int tryStartIndex = instructs.size();

		gen(expression.getTryBlock());

		List<ReservedInstruction> jumpOutInstructions = new ArrayList<ReservedInstruction>(2);
		jumpOutInstructions.add(instructs.reserve());

		TryBlock tryBlock = new TryBlock(tryStartIndex, instructs.size());
		List<CatchBlock> catchBlocks = new ArrayList<CatchBlock>(2);

		for(NapileCatchClause catchClause : expression.getCatchClauses())
		{
			VariableDescriptor catchParameter = (VariableDescriptor) bindingTrace.safeGet(BindingContext.DECLARATION_TO_DESCRIPTOR, catchClause.getCatchParameter());

			int index = frameMap.enter(catchParameter);

			instructs.visitLocalVariable(catchClause.getName());

			int startCatchIndex = instructs.size();

			gen(catchClause.getCatchBody());

			jumpOutInstructions.add(instructs.reserve());

			catchBlocks.add(new CatchBlock(startCatchIndex, instructs.size(), index, TypeTransformer.toAsmType(bindingTrace, catchParameter.getType(), classNode)));

			frameMap.leave(catchParameter);
		}

		final int nextIndex = instructs.size();
		for(ReservedInstruction r : jumpOutInstructions)
			instructs.replace(r).jump(nextIndex);

		instructs.tryCatch(new TryCatchBlockNode(tryBlock, catchBlocks));

		return StackValue.onStack(expectedAsmType);
	}

	@Override
	public StackValue visitConstantExpression(NapileConstantExpression expression, StackValue data)
	{
		CompileTimeConstant<?> constant = bindingTrace.get(BindingContext.COMPILE_TIME_VALUE, expression);
		if(constant != null)
			return StackValue.constant(constant.getValue(), expressionType(expression));
		else
			throw new UnsupportedOperationException();
	}

	@Override
	public StackValue visitThrowExpression(NapileThrowExpression expression, StackValue data)
	{
		NapileExpression throwExp = expression.getThrownExpression();

		assert throwExp != null;

		gen(throwExp, TypeConstants.EXCEPTION);

		instructs.throwVal();

		return StackValue.onStack(TypeConstants.EXCEPTION);
	}

	@Override
	public StackValue visitAnonymClassExpression(NapileAnonymClassExpression expression, StackValue data)
	{
		return InnerClassCodegen.genAnonym(expression, this);
	}

	@Override
	public StackValue visitAnonymMethodExpression(NapileAnonymMethodExpression expression, StackValue data)
	{
		FqName fqName = bindingTrace.safeGet(BindingContext2.DECLARATION_TO_FQ_NAME, expression.getAnonymMethod());

		SimpleMethodDescriptor methodDescriptor = bindingTrace.safeGet(BindingContext.METHOD, expression);

		boolean isStatic = false;
		if(methodDescriptor.getContainingDeclaration() instanceof SimpleMethodDescriptor)
			isStatic = ((SimpleMethodDescriptor) methodDescriptor.getContainingDeclaration()).isStatic();
		else if(methodDescriptor.getContainingDeclaration() instanceof VariableDescriptor)
			isStatic = ((VariableDescriptor) methodDescriptor.getContainingDeclaration()).isStatic();
		else
			throw new IllegalArgumentException("Unknown owner " + methodDescriptor.getContainingDeclaration());

		// gen method
		MethodNode methodNode = MethodCodegen.gen(methodDescriptor, fqName.shortName(), expression.getAnonymMethod(), bindingTrace, classNode, wrappedVariables);

		classNode.addMember(methodNode);

		JetType jetType = bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, expression);

		instructs.load(0);
		instructs.linkMethod(NodeRefUtil.ref(methodDescriptor, fqName, bindingTrace, classNode));

		return StackValue.onStack(TypeTransformer.toAsmType(bindingTrace, jetType, classNode));
	}

	@Override
	public StackValue visitLinkMethodExpression(NapileLinkMethodExpression expression, StackValue data)
	{
		JetType jetType = bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, expression);

		MethodDescriptor target = (MethodDescriptor) bindingTrace.safeGet(BindingContext.REFERENCE_TARGET, expression.getTarget());

		if(target.isStatic())
			instructs.linkStaticMethod(NodeRefUtil.ref(target, bindingTrace, classNode));
		else
		{
			instructs.load(0);

			instructs.linkMethod(NodeRefUtil.ref(target, bindingTrace, classNode));
		}

		return StackValue.onStack(TypeTransformer.toAsmType(bindingTrace, jetType, classNode));
	}

	@Override
	public StackValue visitBreakExpression(NapileBreakExpression expression, StackValue data)
	{
		LoopCodegen<?> targetLoop = null;
		NapileSimpleNameExpression labelRef = expression.getTargetLabel();
		if(labelRef != null)
		{
			Iterator<LoopCodegen<?>> it = loops.descendingIterator();
			while(it.hasNext())
			{
				LoopCodegen<?> e = it.next();
				if(Comparing.equal(e.getName(), expression.getLabelName()))
				{
					targetLoop = e;
					break;
				}
			}
		}
		else
			targetLoop = loops.getLast();

		assert targetLoop != null;

		targetLoop.addBreak(instructs);

		return StackValue.none();
	}

	@Override
	public StackValue visitInjectionExpression(NapileInjectionExpression expression, StackValue data)
	{
		CodeInjection codeInjection = expression.getCodeInjection();
		if(codeInjection == null)
			return StackValue.none();

		PsiElement block = expression.getBlock();
		if(block == null)
			return StackValue.none();

		InjectionCodegen<?> injectionCodegen = InjectionCodegen.INJECTION_CODEGEN.getValue(codeInjection);
		return injectionCodegen.gen(block, data, this);
	}

	@Override
	public StackValue visitIsExpression(NapileIsExpression expression, StackValue data)
	{
		gen(expression.getLeftHandSide(), TypeTransformer.toAsmType(bindingTrace, bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, expression.getLeftHandSide()), classNode));

		JetType rightType = bindingTrace.safeGet(BindingContext.TYPE, expression.getTypeRef());

		instructs.is(TypeTransformer.toAsmType(bindingTrace, rightType, classNode));

		if(expression.isNegated())
			instructs.invokeVirtual(new MethodRef(NapileLangPackage.BOOL.child(Name.identifier("not")), Collections.<TypeNode>emptyList(), Collections.<TypeNode>emptyList(), AsmConstants.BOOL_TYPE), false);

		return StackValue.onStack(AsmConstants.BOOL_TYPE);
	}

	@Override
	public StackValue visitPrefixExpression(NapilePrefixExpression expression, StackValue receiver)
	{
		DeclarationDescriptor op = bindingTrace.safeGet(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
		ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.safeGet(BindingContext.RESOLVED_CALL, expression.getOperationReference());

		final CallableMethod callableMethod = CallTransformer.transformToCallable(this, resolvedCall, false, false, false);

		if(!(op.getName().getName().equals("inc") || op.getName().getName().equals("dec")))
			return invokeOperation(expression, (MethodDescriptor) op, callableMethod);
		else
		{
			StackValue value = gen(expression.getBaseExpression());
			value.dupReceiver(instructs);
			value.dupReceiver(instructs);

			TypeNode type = expressionType(expression.getBaseExpression());
			value.put(type, instructs);
			callableMethod.invoke(instructs);

			MethodDescriptor methodDescriptor = bindingTrace.get(BindingContext.VARIABLE_CALL, expression);
			if(methodDescriptor != null)
				StackValue.variableAccessor(methodDescriptor, value.getType(), this, false).store(callableMethod.getReturnType(), instructs);
			else
				value.store(callableMethod.getReturnType(), instructs);
			value.put(type, instructs);
			return StackValue.onStack(type);
		}
	}

	@Override
	public StackValue visitThisExpression(NapileThisExpression expression, StackValue receiver)
	{
		final DeclarationDescriptor descriptor = bindingTrace.get(BindingContext.REFERENCE_TARGET, expression.getInstanceReference());
		if(descriptor instanceof ClassDescriptor)
			return StackValue.thisOrOuter(this, (ClassDescriptor) descriptor, false);
		else
		{
			/*if(descriptor instanceof CallableDescriptor)
			{
				return generateReceiver(descriptor);
			}  */
			throw new UnsupportedOperationException("neither this nor receiver");
		}
	}

	@Override
	public StackValue visitSuperExpression(NapileSuperExpression expression, StackValue receiver)
	{
		final DeclarationDescriptor descriptor = bindingTrace.get(BindingContext.REFERENCE_TARGET, expression.getInstanceReference());
		if(descriptor instanceof ClassDescriptor)
			return StackValue.thisOrOuter(this, (ClassDescriptor) descriptor, false);
		else
		{

			throw new UnsupportedOperationException("neither this nor receiver");
			//return StackValue.local(0, asmType(type));
		}
	}

	@Override
	public StackValue visitIfExpression(NapileIfExpression expression, StackValue receiver)
	{
		TypeNode asmType = expressionType(expression);

		NapileExpression thenExpression = expression.getThen();
		NapileExpression elseExpression = expression.getElse();

		if(thenExpression == null && elseExpression == null)
			throw new CompilationException("Both brunches of if/else are null", null, expression);

		if(isEmptyExpression(thenExpression))
		{
			if(isEmptyExpression(elseExpression))
			{
				if(!asmType.equals(AsmConstants.NULL_TYPE))
					throw new CompilationException("Completely empty 'if' is expected to have Null type", null, expression);

				instructs.putNull();
				return StackValue.onStack(asmType);
			}
			StackValue condition = gen(expression.getCondition());
			return generateSingleBranchIf(condition, elseExpression, false);
		}
		else
		{
			if(isEmptyExpression(elseExpression))
			{
				StackValue condition = gen(expression.getCondition());
				return generateSingleBranchIf(condition, thenExpression, true);
			}
		}


		StackValue condition = gen(expression.getCondition());

		condition.put(AsmConstants.BOOL_TYPE, instructs);

		instructs.putTrue();

		ReservedInstruction ifSlot = instructs.reserve();

		gen(thenExpression, asmType);

		ReservedInstruction afterIfSlot = instructs.reserve();

		int elseStartIndex = instructs.size();

		gen(elseExpression, asmType);

		int afterIfStartIndex = instructs.size();

		// replace ifSlot - by jump_if - index is start 'else' block
		instructs.replace(ifSlot).jumpIf(elseStartIndex);
		// at end of 'then' block ignore 'else' block
		instructs.replace(afterIfSlot).jump(afterIfStartIndex);

		return StackValue.onStack(asmType);
	}

	@Override
	public StackValue visitWhenExpression(NapileWhenExpression expression, StackValue data)
	{
		JetType expType = bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, expression);

		NapileExpression subjectExpression = expression.getSubjectExpression();
		if(subjectExpression != null)
			gen(subjectExpression, TypeTransformer.toAsmType(bindingTrace, bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, subjectExpression), classNode));

		List<NapileWhenEntry> whenEntries = expression.getEntries();
		List<ReservedInstruction> jumpToBlocks = new ArrayList<ReservedInstruction>(whenEntries.size());
		List<ReservedInstruction> jumpOut = new ArrayList<ReservedInstruction>(whenEntries.size());

		for(NapileWhenEntry whenEntry : expression.getEntries())
		{
			if(whenEntry.isElse())
			{
				jumpToBlocks.add(instructs.reserve());
			}
			else
			{
				NapileWhenCondition condition = whenEntry.getCondition();
				if(condition instanceof NapileWhenConditionIsPattern)
				{
					instructs.dup();

					TypeNode typeNode = TypeTransformer.toAsmType(bindingTrace, bindingTrace.safeGet(BindingContext.TYPE, ((NapileWhenConditionIsPattern) condition).getTypeRef()), classNode);

					instructs.is(typeNode);

					if(((NapileWhenConditionIsPattern) condition).isNegated())
						instructs.putFalse();
					else
						instructs.putTrue();

					instructs.jumpIf(instructs.size() + 2);

					jumpToBlocks.add(instructs.reserve());
				}
				else if(condition instanceof NapileWhenConditionWithExpression)
				{
					NapileExpression condExp = ((NapileWhenConditionWithExpression) condition).getExpression();

					if(subjectExpression != null)
						instructs.dup();

					gen(condExp, TypeTransformer.toAsmType(bindingTrace, bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, condExp), classNode));

					if(subjectExpression != null)
						instructs.invokeVirtual(BinaryOperationCodegen.ANY_EQUALS, false);

					instructs.putTrue();

					instructs.jumpIf(instructs.size() + 2);

					jumpToBlocks.add(instructs.reserve());
				}
				else if(condition instanceof NapileWhenConditionInRange)
				{
					throw new UnsupportedOperationException("'in' is not supported for now");
				}
			}
		}

		if(whenEntries.size() != jumpToBlocks.size())
			throw new IllegalArgumentException();

		for(int i = 0; i < jumpToBlocks.size(); i++)
		{
			ReservedInstruction reservedInstruction = jumpToBlocks.get(i);
			NapileWhenEntry whenEntry = whenEntries.get(i);

			NapileExpression whenExp = whenEntry.getExpression();

			instructs.replace(reservedInstruction).jump(instructs.size());

			gen(whenExp, TypeTransformer.toAsmType(bindingTrace, bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, whenExp), classNode));

			jumpOut.add(instructs.reserve());
		}

		for(ReservedInstruction instruction : jumpOut)
			instructs.replace(instruction).jump(instructs.size());

		return StackValue.onStack(TypeTransformer.toAsmType(bindingTrace, expType, classNode));
	}

	private StackValue generateSingleBranchIf(StackValue condition, NapileExpression expression, boolean inverse)
	{
		TypeNode expressionType = expressionType(expression);
		TypeNode targetType = expressionType;
		if(!expressionType.equals(AsmConstants.NULL_TYPE))
			targetType = AsmConstants.ANY_TYPE;

		condition.put(AsmConstants.BOOL_TYPE, instructs);

		if(inverse)
			instructs.putTrue();
		else
			instructs.putFalse();

		ReservedInstruction ifSlot = instructs.reserve();

		gen(expression, expressionType);

		StackValue.castTo(expressionType, targetType, instructs);

		instructs.replace(ifSlot).jumpIf(instructs.size());

		return StackValue.onStack(targetType);
	}

	@Override
	public StackValue visitReturnExpression(NapileReturnExpression expression, StackValue receiver)
	{
		final NapileExpression returnedExpression = expression.getReturnedExpression();
		if(returnedExpression != null)
		{
			gen(returnedExpression, returnType);

			doFinallyOnReturn();

			instructs.returnVal();
		}
		else
		{
			if(isInstanceConstructor)
				instructs.load(0);
			else
				instructs.putNull();

			instructs.returnVal();
		}
		return StackValue.none();
	}

	@Override
	public StackValue visitBinaryExpression(NapileBinaryExpression expression, StackValue receiver)
	{
		final IElementType opToken = expression.getOperationReference().getReferencedNameElementType();
		if(opToken == NapileTokens.EQ)
			return BinaryOperationCodegen.genEq(expression, this);
		else if(OperatorConventions.ASSIGNMENT_OPERATION_COUNTERPARTS.containsKey(opToken))
			return BinaryOperationCodegen.genAugmentedAssignment(expression, this, instructs);
		else if(opToken == NapileTokens.ANDAND)
			return BinaryOperationCodegen.genAndAnd(expression, this, instructs);
		else if(opToken == NapileTokens.OROR)
			return BinaryOperationCodegen.genOrOr(expression, this, instructs);
		else if(opToken == NapileTokens.EQEQ || opToken == NapileTokens.EXCLEQ)
			return BinaryOperationCodegen.genEqEq(expression, this);
		else if(opToken == NapileTokens.LT || opToken == NapileTokens.LTEQ || opToken == NapileTokens.GT || opToken == NapileTokens.GTEQ)
			return BinaryOperationCodegen.genGeLe(expression, this);
		else if(opToken == NapileTokens.ELVIS)
			return BinaryOperationCodegen.genElvis(expression, this);
		/*else if(opToken == NapileTokens.IN_KEYWORD || opToken == NapileTokens.NOT_IN)
		{
				return final Type exprType = expressionType(expression);
        JetType type = bindingContext.get(BindingContext.EXPRESSION_TYPE, expression.getLeft());(expression);
		}
		else */
		else
		{
			DeclarationDescriptor op = bindingTrace.get(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
			final CallableMethod callable = CallTransformer.transformToCallable(this, (MethodDescriptor) op, Collections.<TypeNode>emptyList(), false, false, false);

			return invokeOperation(expression, (MethodDescriptor) op, callable);
		}
	}

	@Override
	public StackValue visitPostfixExpression(NapilePostfixExpression expression, StackValue receiver)
	{
		if(expression.getOperationReference().getReferencedNameElementType() == NapileTokens.EXCLEXCL)
			return BinaryOperationCodegen.genSure(expression, this,instructs, receiver);

		DeclarationDescriptor op = bindingTrace.get(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
		if(op instanceof MethodDescriptor)
		{
			if(op.getName().getName().equals("inc") || op.getName().getName().equals("dec"))
			{
				ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.safeGet(BindingContext.RESOLVED_CALL, expression.getOperationReference());

				final CallableMethod callable = CallTransformer.transformToCallable(this, resolvedCall, false, false, false);

				StackValue value = gen(expression.getBaseExpression());
				value.dupReceiver(instructs);

				TypeNode type = expressionType(expression.getBaseExpression());
				value.put(type, instructs);

				switch(value.receiverSize())
				{
					case 0:
						instructs.dup();
						break;
					case 1:
						instructs.dup1x1();
						break;
					default:
						throw new UnsupportedOperationException("Unknown receiver size " + value.receiverSize());
				}

				callable.invoke(instructs);

				MethodDescriptor methodDescriptor = bindingTrace.get(BindingContext.VARIABLE_CALL, expression);
				if(methodDescriptor != null)
					value = StackValue.variableAccessor(methodDescriptor, value.getType(), this, false);
				value.store(callable.getReturnType(), instructs);

				return StackValue.onStack(type);
			}
		}
		throw new UnsupportedOperationException("Don't know how to generate this postfix expression");
	}

	@Override
	public StackValue visitCallExpression(NapileCallExpression expression, StackValue receiver)
	{
		final NapileExpression callee = expression.getCalleeExpression();
		assert callee != null;

		ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.safeGet(BindingContext.RESOLVED_CALL, callee);

		DeclarationDescriptor funDescriptor = resolvedCall.getResultingDescriptor();

		if(!(funDescriptor instanceof MethodDescriptor))
			throw new UnsupportedOperationException("unknown type of callee descriptor: " + funDescriptor);

		//funDescriptor = accessableFunctionDescriptor((FunctionDescriptor) funDescriptor);

		if(funDescriptor instanceof ConstructorDescriptor)
		{
			receiver = StackValue.receiver(resolvedCall, receiver, this, null);
			return generateConstructorCall(expression, (NapileSimpleNameExpression) callee, receiver);
		}
		else
		{
			Pair<VariableDescriptor, ReceiverDescriptor> variableDescriptor = resolvedCall.getVariableCallInfo();
			if(variableDescriptor != null)
			{
				StackValue stackValue = gen(callee);

				stackValue.put(TypeTransformer.toAsmType(bindingTrace, variableDescriptor.getFirst().getType(), classNode), instructs);

				return invokeMethod(receiver, resolvedCall, expression.getParent() instanceof NapileSafeQualifiedExpression, true);
			}
			else
				return invokeMethod(receiver, resolvedCall, expression.getParent() instanceof NapileSafeQualifiedExpression, false);
		}
	}

	@Override
	public StackValue visitArrayAccessExpression(NapileArrayAccessExpressionImpl expression, StackValue receiver)
	{
		MethodDescriptor operationDescriptor = (MethodDescriptor) bindingTrace.safeGet(BindingContext.REFERENCE_TARGET, expression);
		CallableMethod accessor = CallTransformer.transformToCallable(this, operationDescriptor, Collections.<TypeNode>emptyList(), false, false, false);

		boolean isGetter = accessor.getName().endsWith("get");

		ResolvedCall<MethodDescriptor> resolvedGetCall = bindingTrace.get(BindingContext.INDEXED_LVALUE_GET, expression);
		ResolvedCall<MethodDescriptor> resolvedSetCall = bindingTrace.get(BindingContext.INDEXED_LVALUE_SET, expression);

		List<TypeNode> argumentTypes = accessor.getValueParameterTypes();
		int index = 0;

		TypeNode asmType;
		if(isGetter)
		{
			genThisAndReceiverFromResolvedCall(receiver, resolvedGetCall, CallTransformer.transformToCallable(this, resolvedGetCall, false, false, false));

			asmType = accessor.getReturnType();
		}
		else
		{
			genThisAndReceiverFromResolvedCall(receiver, resolvedSetCall, CallTransformer.transformToCallable(this, resolvedSetCall, false, false, false));

			asmType = argumentTypes.get(argumentTypes.size() - 1);
		}

		for(NapileExpression jetExpression : expression.getIndexExpressions())
		{
			gen(jetExpression, argumentTypes.get(index));
			index++;
		}

		return StackValue.collectionElement(asmType, resolvedGetCall, resolvedSetCall, this);
	}

	@Override
	public StackValue visitMultiTypeExpression(NapileMultiTypeExpression expression, StackValue data)
	{
		NapileExpression[] exps = expression.getExpressions();

		instructs.newInt(exps.length);
		instructs.newObject(new TypeNode(false, new ClassTypeNode(NapileLangPackage.MULTI)), Arrays.asList(AsmConstants.INT_TYPE));

		int i = 0;
		for(NapileExpression exp : exps)
		{
			instructs.dup();

			instructs.newInt(i ++);

			gen(exp, TypeTransformer.toAsmType(bindingTrace, bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, exp), classNode));

			instructs.invokeVirtual(MultiVariable.SET_VALUE, false);
			instructs.pop();
		}

		return StackValue.onStack(TypeTransformer.toAsmType(bindingTrace, bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, expression), classNode));
	}

	@Override
	public StackValue visitSimpleNameExpression(NapileSimpleNameExpression expression, StackValue receiver)
	{
		ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.get(BindingContext.RESOLVED_CALL, expression);

		DeclarationDescriptor descriptor;
		if(resolvedCall == null)
			descriptor = bindingTrace.safeGet(BindingContext.REFERENCE_TARGET, expression);
		else
		{
			Pair<VariableDescriptor, ReceiverDescriptor> variableCallInfo = resolvedCall.getVariableCallInfo();
			if(variableCallInfo != null)
			{
				descriptor = variableCallInfo.getFirst();
				receiver = StackValue.receiver(variableCallInfo.getFirst(), variableCallInfo.getSecond(), receiver, this, null);
			}
			else if(resolvedCall.getResultingDescriptor() instanceof MultiTypeEntryVariableDescriptor)
			{
				descriptor = resolvedCall.getResultingDescriptor();

				if(expression.getParent() instanceof NapileDotQualifiedExpression)
				{
					NapileExpression receiverExp = ((NapileDotQualifiedExpression) expression.getParent()).getReceiverExpression();

					StackValue newReceiver = receiverExp.accept(this, StackValue.none());
					newReceiver.put(newReceiver.getType(), instructs);

					return StackValue.multiVariable(this, ((MultiTypeEntryVariableDescriptor) descriptor));
				}
				else
					throw new UnsupportedOperationException("Parent is not NapileDotQualifiedExpression. Found: " + expression.getParent().getClass().getName());
			}
			else
			{
				descriptor = resolvedCall.getResultingDescriptor();
				receiver = StackValue.receiver(resolvedCall, receiver, this, null);
			}
		}

		final DeclarationDescriptor container = descriptor.getContainingDeclaration();

		int index = lookupLocalIndex(descriptor);
		if(index >= 0)
			return stackValueForLocal(descriptor, index);

		if(descriptor instanceof VariableDescriptor)
		{
			StackValue wrappedValue = wrappedVariables.get(descriptor);
			if(wrappedValue != null)
			{
				if(wrappedValue.receiverSize() == 1)
					getInstructs().load(0);

				return wrappedValue;
			}

			VariableDescriptor variableDescriptor = (VariableDescriptor) descriptor;
			boolean directToVar = variableDescriptor instanceof LocalVariableDescriptor && bindingTrace.safeGet(BindingContext.AUTO_CREATED_IT, variableDescriptor);
			VariableDescriptor targetVar = directToVar ? bindingTrace.safeGet(BindingContext.AUTO_CREATED_TO, variableDescriptor) : variableDescriptor;

			boolean isStatic = targetVar.isStatic();
			NapileExpression r = getReceiverForSelector(expression);
			final boolean isSuper = r instanceof NapileSuperExpression;

			final StackValue iValue = intermediateValueForProperty(expression, targetVar, bindingTrace.get(BindingContext.VARIABLE_CALL, expression), directToVar, isSuper ? (NapileSuperExpression) r : null);
			if(!directToVar && resolvedCall != null && !isSuper)
				receiver.put(isStatic ? receiver.getType() : TypeTransformer.toAsmType(bindingTrace, ((ClassDescriptor) container).getDefaultType(), classNode), instructs);
			else
			{
				if(!isStatic)
				{
					if(receiver == StackValue.none())
					{
						if(resolvedCall == null)
							receiver = generateThisOrOuter((ClassDescriptor) targetVar.getContainingDeclaration(), false);
						else
							receiver = generateThisOrOuter((ClassDescriptor) targetVar.getContainingDeclaration(), false);
					}
					JetType receiverType = bindingTrace.get(BindingContext.EXPRESSION_TYPE, r);
					receiver.put(receiverType != null && !isSuper ? TypeTransformer.toAsmType(bindingTrace, receiverType, classNode) : AsmConstants.ANY_TYPE, instructs);
				}
			}
			return iValue;
		}

		if(descriptor instanceof ClassDescriptor)
			return StackValue.none();

		throw new UnsupportedOperationException(descriptor.getClass().getName());
	}

	@Override
	public StackValue visitClassOfExpression(NapileClassOfExpression classOfExpression, StackValue data)
	{
		TypeNode typeNode = expressionType(classOfExpression);

		instructs.classOf(TypeTransformer.toAsmType(bindingTrace, bindingTrace.safeGet(BindingContext.TYPE, classOfExpression.getTypeReference()), classNode));

		return StackValue.onStack(typeNode);
	}

	@Override
	public StackValue visitTypeOfExpression(NapileTypeOfExpression typeOfExpression, StackValue data)
	{
		TypeNode typeNode = expressionType(typeOfExpression);

		instructs.typeOf(TypeTransformer.toAsmType(bindingTrace, bindingTrace.safeGet(BindingContext.TYPE, typeOfExpression.getTypeReference()), classNode));

		return StackValue.onStack(typeNode);
	}

	@Override
	public StackValue visitArrayOfExpression(NapileArrayOfExpression arrayExpression, StackValue data)
	{
		TypeNode typeNode = expressionType(arrayExpression);

		NapileExpression[] expressions = arrayExpression.getValues();

		instructs.newInt(expressions.length);

		instructs.newObject(typeNode, Collections.singletonList(AsmConstants.INT_TYPE));

		// set ref need return 'this' not real type
		MethodRef setRef = new MethodRef(NapileLangPackage.ARRAY.child(Name.identifier("set")), Arrays.<TypeNode>asList(AsmConstants.INT_TYPE, new TypeNode(false, new TypeParameterValueTypeNode(Name.identifier("E")))), Collections.<TypeNode>emptyList(), typeNode);
		for(int i = 0; i < expressions.length; i++)
		{
			NapileExpression expression = expressions[i];

			instructs.newInt(i);

			gen(expression, expressionType(expression));

			instructs.invokeVirtual(setRef, false); // set - put to stack this object
		}

		return StackValue.onStack(typeNode);
	}

	public StackValue intermediateValueForProperty(NapileExpression expression, VariableDescriptor variableDescriptor, @Nullable MethodDescriptor methodDescriptor, final boolean forceField, @Nullable NapileSuperExpression superExpression)
	{
		if(methodDescriptor != null)
			return StackValue.variableAccessor(methodDescriptor, TypeTransformer.toAsmType(bindingTrace, variableDescriptor.getType(), classNode), this, CallTransformer.isNullable(expression));

		if(!forceField)
		{
			//TODO [VISTALL]
			System.out.println("Using old method to invoking variable accessors: " + expression.getParent().getText());
			//throw new UnsupportedOperationException("property");
			return StackValue.simpleVariableAccessor(DescriptorUtils.getFQName(variableDescriptor).toSafe(), TypeTransformer.toAsmType(bindingTrace, variableDescriptor.getType(), classNode), variableDescriptor.isStatic() ? CallableMethod.CallType.STATIC : CallableMethod.CallType.VIRTUAL);
		}
		else
			return StackValue.variable(DescriptorUtils.getFQName(variableDescriptor).toSafe(), TypeTransformer.toAsmType(bindingTrace, variableDescriptor.getType(), classNode), variableDescriptor.isStatic());
	}

	private StackValue generateConstructorCall(NapileCallExpression expression, NapileSimpleNameExpression constructorReference, StackValue receiver)
	{
		DeclarationDescriptor constructorDescriptor = bindingTrace.safeGet(BindingContext.REFERENCE_TARGET, constructorReference);

		//final PsiElement declaration = BindingContextUtils.descriptorToDeclaration(bindingTrace.getBindingContext(), constructorDescriptor);
		TypeNode type;
		if(constructorDescriptor instanceof ConstructorDescriptor)
		{
			//noinspection ConstantConditions
			JetType expressionType = bindingTrace.safeGet(BindingContext.EXPRESSION_TYPE, expression);

			type = TypeTransformer.toAsmType(bindingTrace, expressionType, classNode);

			//final ClassDescriptor classDescriptor = ((ConstructorDescriptor) constructorDescriptor).getContainingDeclaration();

			CallableMethod method = CallTransformer.transformToCallable(this, (ConstructorDescriptor) constructorDescriptor, Collections.<TypeNode>emptyList(), false, false, false);

			receiver.put(receiver.getType(), instructs);

			ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.safeGet(BindingContext.RESOLVED_CALL, expression.getCalleeExpression());

			pushMethodArguments(resolvedCall, method.getValueParameterTypes());

			instructs.newObject(type, method.getValueParameterTypes());
		}
		else
		{
			throw new UnsupportedOperationException("don't know how to generate this new expression");
		}
		return StackValue.onStack(type);
	}

	private StackValue invokeOperation(NapileOperationExpression expression, MethodDescriptor op, CallableMethod callable)
	{
		int functionLocalIndex = lookupLocalIndex(op);
		if(functionLocalIndex >= 0)
			throw new UnsupportedOperationException();
		//stackValueForLocal(op, functionLocalIndex).put(getInternalClassName(op).getAsmType(), v);

		ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.safeGet(BindingContext.RESOLVED_CALL, expression.getOperationReference());

		genThisAndReceiverFromResolvedCall(StackValue.none(), resolvedCall, callable);
		pushMethodArguments(resolvedCall, callable.getValueParameterTypes());
		callable.invoke(instructs);

		return StackValue.onStack(callable.getReturnType());
	}

	private StackValue invokeMethod(StackValue receiver, ResolvedCall<? extends CallableDescriptor> resolvedCall, boolean nullable, boolean anonym)
	{
		boolean requireSpecialCall = false;
		ReceiverDescriptor receiverDescriptor = resolvedCall.getThisObject();
		if(receiverDescriptor instanceof ExpressionReceiver)
		{
			NapileExpression expressionOfReceiver = ((ExpressionReceiver) receiverDescriptor).getExpression();
			if(expressionOfReceiver instanceof NapileSuperExpression)
				requireSpecialCall = true;
		}

		final CallableMethod callableMethod = CallTransformer.transformToCallable(this, resolvedCall, nullable, anonym, requireSpecialCall);

		invokeMethodWithArguments(callableMethod, resolvedCall, receiver);

		final TypeNode callReturnType = callableMethod.getReturnType();

		return StackValue.onStack(callReturnType);
	}

	private void doFinallyOnReturn()
	{
		//TODO [VISTALL] make it
	}

	public void invokeMethodWithArguments(@NotNull CallableMethod callableMethod, NapileCallElement expression, StackValue receiver)
	{
		NapileExpression calleeExpression = expression.getCalleeExpression();

		ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.safeGet(BindingContext.RESOLVED_CALL, calleeExpression);

		invokeMethodWithArguments(callableMethod, resolvedCall, receiver);
	}

	public void invokeMethodWithArguments(@NotNull CallableMethod callableMethod, @NotNull ResolvedCall<? extends CallableDescriptor> resolvedCall, @NotNull StackValue receiver)
	{
		/*final Type calleeType = callableMethod.getGenerateCalleeType();
		if(calleeType != null)
		{
			assert !callableMethod.isNeedsThis();
			gen(call.getCalleeExpression(), calleeType);
		}   */

		//if(resolvedCall instanceof VariableAsMethodResolvedCall)
		//	resolvedCall = ((VariableAsMethodResolvedCall) resolvedCall).getMethodCall();

		if(!(resolvedCall.getResultingDescriptor() instanceof ConstructorDescriptor))  // otherwise already
		{
			receiver = StackValue.receiver(resolvedCall, receiver, this, callableMethod);
			receiver.put(receiver.getType(), instructs);
			/*if(calleeType != null)
			{
				StackValue.onStack(receiver.type).put(boxType(receiver.type), v);
			}    */
		}

		pushMethodArguments(resolvedCall, callableMethod.getValueParameterTypes());

		callableMethod.invoke(instructs);
	}

	private void genThisAndReceiverFromResolvedCall(StackValue receiver, ResolvedCall<? extends CallableDescriptor> resolvedCall, CallableMethod callableMethod)
	{
		receiver = StackValue.receiver(resolvedCall, receiver, this, callableMethod);
		receiver.put(receiver.getType(), instructs);
	}

	private StackValue stackValueForLocal(DeclarationDescriptor descriptor, int index)
	{
		if(descriptor instanceof VariableDescriptor)
		{
			final JetType outType = ((VariableDescriptor) descriptor).getType();

			return StackValue.local(index, TypeTransformer.toAsmType(bindingTrace, outType, classNode));
		}
		else
			return StackValue.local(index, AsmConstants.ANY_TYPE);
	}

	public void pushMethodArguments(@NotNull ResolvedCall<?> resolvedCall, List<TypeNode> valueParameterTypes)
	{
		List<ResolvedValueArgument> valueArguments = resolvedCall.getValueArgumentsByIndex();
		CallableDescriptor fd = resolvedCall.getResultingDescriptor();

		if(fd.getValueParameters().size() != valueArguments.size())
		{
			throw new IllegalStateException();
		}

		int index = 0;

		for(CallParameterDescriptor valueParameterDescriptor : fd.getValueParameters())
		{
			ResolvedValueArgument resolvedValueArgument = valueArguments.get(valueParameterDescriptor.getIndex());
			if(resolvedValueArgument instanceof ExpressionValueArgument)
			{
				ExpressionValueArgument valueArgument = (ExpressionValueArgument) resolvedValueArgument;
				//noinspection ConstantConditions
				gen(valueArgument.getValueArgument().getArgumentExpression(), valueParameterTypes.get(index));
			}
			else if(resolvedValueArgument instanceof DefaultValueArgument)
			{
				DefaultValueArgument valueArgument = (DefaultValueArgument) resolvedValueArgument;
				//noinspection ConstantConditions
				gen(valueArgument.getExpression(), valueParameterTypes.get(index));
			}
			else
			{
				throw new UnsupportedOperationException();
			}
			index++;
		}
	}

	private StackValue generateBlock(List<NapileElement> statements)
	{
		List<Function<StackValue, Void>> leaveTasks = Lists.newArrayList();

		StackValue answer = StackValue.none();

		for(Iterator<NapileElement> iterator = statements.iterator(); iterator.hasNext(); )
		{
			NapileElement statement = iterator.next();

			if(statement instanceof NapileVariable)
				generateLocalVariableDeclaration((NapileVariable) statement, leaveTasks);

			if(!iterator.hasNext())
				answer = gen(statement);
			else
				gen(statement, AsmConstants.NULL_TYPE);
		}

		for(Function<StackValue, Void> task : Lists.reverse(leaveTasks))
			task.fun(answer);

		return answer;
	}

	private <E extends NapileLoopExpression> StackValue loopGen(LoopCodegen<E> l)
	{
		loops.add(l);

		l.gen(this, instructs, bindingTrace);

		loops.getLast();

		return StackValue.none();
	}

	@Nullable
	private static NapileExpression getReceiverForSelector(PsiElement expression)
	{
		if(expression.getParent() instanceof NapileDotQualifiedExpression && !isReceiver(expression))
		{
			final NapileDotQualifiedExpression parent = (NapileDotQualifiedExpression) expression.getParent();
			return parent.getReceiverExpression();
		}
		return null;
	}

	private static boolean isReceiver(PsiElement expression)
	{
		final PsiElement parent = expression.getParent();
		if(parent instanceof NapileQualifiedExpression)
		{
			final NapileExpression receiverExpression = ((NapileQualifiedExpression) parent).getReceiverExpression();
			return expression == receiverExpression;
		}
		return false;
	}

	public void generateFromResolvedCall(@NotNull ReceiverDescriptor descriptor, @NotNull TypeNode type)
	{
		if(descriptor instanceof ClassReceiver)
		{
			TypeNode exprType = TypeTransformer.toAsmType(bindingTrace, descriptor.getType(), classNode);
			ClassReceiver classReceiver = (ClassReceiver) descriptor;
			ClassDescriptor classReceiverDeclarationDescriptor = classReceiver.getDeclarationDescriptor();

			StackValue.thisOrOuter(this, classReceiverDeclarationDescriptor, false).put(type, instructs);
		}
		else if(descriptor instanceof ExpressionReceiver)
		{
			ExpressionReceiver expressionReceiver = (ExpressionReceiver) descriptor;
			NapileExpression expr = expressionReceiver.getExpression();
			gen(expr, type);
		}
		else if(descriptor instanceof AutoCastReceiver)
		{
			AutoCastReceiver autoCastReceiver = (AutoCastReceiver) descriptor;
			TypeNode intermediateType = TypeTransformer.toAsmType(bindingTrace, autoCastReceiver.getType(), classNode);
			generateFromResolvedCall(autoCastReceiver.getOriginal(), intermediateType);
			StackValue.onStack(intermediateType).put(type, instructs);
		}
		else
		{
			throw new UnsupportedOperationException("Unsupported receiver type: " + descriptor);
		}
	}

	public StackValue generateThisOrOuter(@NotNull final ClassDescriptor calleeContainingClass, boolean isSuper)
	{
		//TODO [VISTALL]
		return StackValue.local(0, TypeTransformer.toAsmType(bindingTrace, calleeContainingClass.getDefaultType(), classNode));
	}

	private void generateLocalVariableDeclaration(@NotNull final NapileVariable variableDeclaration, @NotNull List<Function<StackValue, Void>> leaveTasks)
	{
		final VariableDescriptor variableDescriptor = bindingTrace.safeGet(BindingContext.VARIABLE, variableDeclaration);
		if(!wrapVariableIfNeed(variableDescriptor))
		{
			final TypeNode type = TypeTransformer.toAsmType(bindingTrace, variableDescriptor.getType(), classNode);
			int index = frameMap.enter(variableDescriptor);

			leaveTasks.add(new Function<StackValue, Void>()
			{
				@Override
				public Void fun(StackValue answer)
				{
					int index = frameMap.leave(variableDescriptor);

					getInstructs().visitLocalVariable(variableDescriptor.getName().getName());
					return null;
				}
			});
		}
	}

	private void initializeLocalVariable(@NotNull NapileVariable variableDeclaration, @NotNull Function<VariableDescriptor, Void> generateInitializer)
	{
		VariableDescriptor variableDescriptor = bindingTrace.safeGet(BindingContext.VARIABLE, variableDeclaration);

		StackValue wrappedVariable = wrappedVariables.get(variableDescriptor);
		if(wrappedVariable != null)
		{
			if(wrappedVariable.receiverSize() == 1)
				getInstructs().load(0);

			generateInitializer.fun(variableDescriptor);

			wrappedVariable.store(TypeTransformer.toAsmType(bindingTrace, variableDescriptor.getType(), classNode), getInstructs());
		}
		else
		{
			int index = lookupLocalIndex(variableDescriptor);

			if(index < 0)
				throw new IllegalStateException("Local variable not found for " + variableDescriptor);

			generateInitializer.fun(variableDescriptor);

			getInstructs().store(index);
		}
	}

	public int lookupLocalIndex(DeclarationDescriptor descriptor)
	{
		return frameMap.getIndex(descriptor);
	}

	public void gen(NapileElement expr, TypeNode type)
	{
		StackValue value = gen(expr);
		value.put(type, getInstructs());
	}

	public StackValue gen(NapileElement element)
	{
		StackValue tempVar = tempVariables.get(element);
		if(tempVar != null)
			return tempVar;

		if(element instanceof NapileExpression)
		{
			NapileExpression expression = (NapileExpression) element;
			CompileTimeConstant<?> constant = bindingTrace.get(BindingContext.COMPILE_TIME_VALUE, expression);
			if(constant != null)
				return StackValue.constant(constant.getValue(), expressionType(expression));
		}

		return genQualified(StackValue.none(), element);
	}

	public StackValue genQualified(StackValue receiver, NapileElement selector)
	{
		if(tempVariables.containsKey(selector))
		{
			throw new IllegalStateException("Inconsistent state: expression saved to a temporary variable is a selector");
		}

		//if(!(selector instanceof NapileBlockExpression))
		//	markLineNumber(selector);

		try
		{
			return selector.accept(this, receiver);
		}
		catch(ProcessCanceledException e)
		{
			throw e;
		}
		catch(CompilationException e)
		{
			throw e;
		}
		catch(Throwable error)
		{
			String message = error.getMessage();
			throw new CompilationException(message != null ? message : "null", error, selector);
		}
	}

	public void returnExpression(@NotNull NapileExpression expr, boolean macro)
	{
		StackValue lastValue = gen(expr);

		if(macro)
			return;

		NapileExpression lastExp = lastExpressionOfBody(expr);
		if(lastExp instanceof NapileThrowExpression || lastExp instanceof NapileReturnExpression)
		{}
		else if(expr instanceof NapileBlockExpression)
		{
			if(isInstanceConstructor)
				instructs.load(0);
			else
				instructs.putNull();
			instructs.returnVal();
		}
		else
		{
			lastValue.put(returnType, instructs);
			instructs.returnVal();
		}
	}

	@NotNull
	private static NapileExpression lastExpressionOfBody(NapileExpression exp)
	{
		if(exp instanceof NapileBlockExpression)
		{
			final List<NapileElement> statements = ((NapileBlockExpression) exp).getStatements();
			NapileElement last = statements.size() > 0 ? statements.get(statements.size() - 1) : null;
			return last instanceof NapileExpression ? (NapileExpression) last : exp;
		}
		else
			return exp;
	}

	private static boolean isEmptyExpression(NapileElement expr)
	{
		if(expr == null)
			return true;
		if(expr instanceof NapileBlockExpression)
		{
			NapileBlockExpression blockExpression = (NapileBlockExpression) expr;
			List<NapileElement> statements = blockExpression.getStatements();
			if(statements.size() == 0 || statements.size() == 1 && isEmptyExpression(statements.get(0)))
				return true;
		}
		return false;
	}

	private boolean wrapVariableIfNeed(VariableDescriptor variableDescriptor)
	{
		boolean wrappedInClosure = bindingTrace.safeGet(BindingContext.CAPTURED_IN_CLOSURE, variableDescriptor);
		if(wrappedInClosure)
		{
			MethodDescriptor ownerMethod = (MethodDescriptor) variableDescriptor.getContainingDeclaration();
			Name name = Name.identifier(ownerMethod.getName() + AsmConstants.ANONYM_SPLITTER + variableDescriptor.getName());
			VariableDescriptorImpl newVariableDescriptor = new VariableDescriptorImpl(ownerMethod.getContainingDeclaration(), variableDescriptor.getAnnotations(), variableDescriptor.getModality(), Visibility.LOCAL, name, CallableMemberDescriptor.Kind.DECLARATION, ownerMethod.isStatic(), true);
			newVariableDescriptor.setType(variableDescriptor.getType(), variableDescriptor.getTypeParameters(), variableDescriptor.getExpectedThisObject());

			wrappedVariables.put(variableDescriptor, StackValue.simpleVariableAccessor(this, newVariableDescriptor, newVariableDescriptor.isStatic() ? CallableMethod.CallType.STATIC : CallableMethod.CallType.SPECIAL));

			VariableCodegen.getSetterAndGetter(newVariableDescriptor, null, classNode, bindingTrace, false);
			VariableNode variableNode = new VariableNode(newVariableDescriptor.isStatic() ? Modifier.list(Modifier.STATIC, Modifier.MUTABLE) : Modifier.list(Modifier.MUTABLE), newVariableDescriptor.getName(), TypeTransformer.toAsmType(bindingTrace, newVariableDescriptor.getType(), classNode));
			classNode.addMember(variableNode);
			return true;
		}
		else
			return false;
	}

	@NotNull
	public TypeNode expressionType(NapileExpression expr)
	{
		JetType type = bindingTrace.get(BindingContext.EXPRESSION_TYPE, expr);
		return type == null ? StackValue.none().getType() : TypeTransformer.toAsmType(bindingTrace, type, classNode);
	}

	@NotNull
	public TypeNode toAsmType(@NotNull JetType type)
	{
		return TypeTransformer.toAsmType(bindingTrace, type, classNode);
	}

	@NotNull
	public InstructionAdapter getInstructs()
	{
		return instructs;
	}
}
