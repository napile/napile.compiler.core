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

import gnu.trove.TIntArrayList;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.AsmConstants;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.ClassNode;
import org.napile.asm.tree.members.MethodParameterNode;
import org.napile.asm.tree.members.bytecode.Instruction;
import org.napile.asm.tree.members.bytecode.InstructionInCodePosition;
import org.napile.asm.tree.members.bytecode.MethodRef;
import org.napile.asm.tree.members.bytecode.adapter.InstructionAdapter;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;
import org.napile.asm.tree.members.types.constructors.ThisTypeNode;
import org.napile.asm.tree.members.types.constructors.TypeParameterValueTypeNode;
import org.napile.compiler.codegen.CompilationException;
import org.napile.compiler.codegen.processors.codegen.CallTransformer;
import org.napile.compiler.codegen.processors.codegen.CallableMethod;
import org.napile.compiler.codegen.processors.codegen.FrameMap;
import org.napile.compiler.codegen.processors.codegen.stackValue.MultiVariable;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.codegen.processors.codegen.stackValue.WrappedVar;
import org.napile.compiler.codegen.processors.injection.InjectionCodegen;
import org.napile.compiler.codegen.processors.visitors.BinaryCodegenVisitor;
import org.napile.compiler.codegen.processors.visitors.ClosureCodegenVisitor;
import org.napile.compiler.codegen.processors.visitors.ConditionCodegenVisitor;
import org.napile.compiler.codegen.processors.visitors.LoopCodegenVisitor;
import org.napile.compiler.codegen.processors.visitors.TryThrowCodegenVisitor;
import org.napile.compiler.injection.CodeInjection;
import org.napile.compiler.lang.descriptors.CallParameterDescriptor;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.LocalVariableDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.MultiTypeEntryVariableDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
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
import com.google.common.collect.Lists;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.Function;

/**
 * @author VISTALL
 * @date 0:24/07.09.12
 * <p/>
 * base code getting from Kotlin
 */
public class ExpressionCodegen extends NapileVisitor<StackValue, StackValue> implements PositionMarker
{
	@NotNull
	public final BindingTrace bindingTrace;
	@NotNull
	public final InstructionAdapter instructs;
	@NotNull
	public final FrameMap frameMap;
	@NotNull
	private final TypeNode returnType;

	@NotNull
	public final ClassNode classNode;
	@NotNull
	public final ExpressionCodegenContext context;

	private final ClosureCodegenVisitor closureCodegenVisitor = new ClosureCodegenVisitor(this);
	private final BinaryCodegenVisitor binaryCodegenVisitor = new BinaryCodegenVisitor(this);
	private final ConditionCodegenVisitor conditionCodegenVisitor = new ConditionCodegenVisitor(this);
	private final LoopCodegenVisitor loopCodegenVisitor = new LoopCodegenVisitor(this);
	private final TryThrowCodegenVisitor tryThrowCodegenVisitor = new TryThrowCodegenVisitor(this);

	private TIntArrayList refParameters = null;

	public ExpressionCodegen(@NotNull BindingTrace b, @NotNull TypeNode r, @NotNull ClassNode c)
	{
		bindingTrace = b;
		returnType = r;
		classNode = c;
		instructs = new InstructionAdapter();
		frameMap = new FrameMap();
		context = ExpressionCodegenContext.empty();
		context.gen = this;
	}

	public ExpressionCodegen(@NotNull BindingTrace b, @Nullable MethodDescriptor d, @NotNull ClassNode c, @NotNull ExpressionCodegenContext codegenContext, @Nullable InstructionAdapter adapter)
	{
		bindingTrace = b;
		classNode = c;
		instructs = adapter == null ? new InstructionAdapter() : adapter;
		context = codegenContext;
		context.gen = this;
		frameMap = new FrameMap();

		if(d != null)
		{
			returnType = d instanceof ConstructorDescriptor ? new TypeNode(false, new ThisTypeNode()) : TypeTransformer.toAsmType(bindingTrace, d.getReturnType(), classNode);

			if(!d.isStatic())
			{
				frameMap.enterTemp();
				instructs.visitLocalVariable("this");
			}

			refParameters = new TIntArrayList();
			for(CallParameterDescriptor p : d.getValueParameters())
			{
				int index = frameMap.enter(p);
				instructs.visitLocalVariable(p.getName().getName());

				if(p.isRef())
					refParameters.add(index);

				if(context.wrapVariableIfNeed(p))
				{
					WrappedVar wrapped = context.wrappedVariables.get(p);

					wrapped.putReceiver(this);

					instructs.localGet(index);

					wrapped.store(wrapped.getType(), instructs, this);
				}
			}
		}
		else
			returnType = AsmConstants.NULL_TYPE;
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
	public StackValue visitBinaryWithTypeRHSExpression(NapileBinaryExpressionWithTypeRHS expression, StackValue data)
	{
		return expression.accept(binaryCodegenVisitor, data);
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
		return expression.accept(loopCodegenVisitor, data);
	}

	@Override
	public StackValue visitWhileExpression(NapileWhileExpression expression, StackValue data)
	{
		return expression.accept(loopCodegenVisitor, data);
	}

	@Override
	public StackValue visitDoWhileExpression(NapileDoWhileExpression expression, StackValue data)
	{
		return expression.accept(loopCodegenVisitor, data);
	}

	@Override
	public StackValue visitLabelExpression(NapileLabelExpression expression, StackValue data)
	{
		return expression.accept(loopCodegenVisitor, data);
	}

	@Override
	public StackValue visitContinueExpression(NapileContinueExpression expression, StackValue data)
	{
		return expression.accept(loopCodegenVisitor, data);
	}

	@Override
	public StackValue visitTryExpression(NapileTryExpression expression, StackValue data)
	{
		return expression.accept(tryThrowCodegenVisitor, data);
	}

	@Override
	public StackValue visitConstantExpression(NapileConstantExpression expression, StackValue data)
	{
		CompileTimeConstant<?> constant = bindingTrace.get(BindingContext.COMPILE_TIME_VALUE, expression);
		if(constant != null)
			return StackValue.constant(expression,  constant.getValue(), expressionType(expression));
		else
			throw new UnsupportedOperationException();
	}

	@Override
	public StackValue visitThrowExpression(NapileThrowExpression expression, StackValue data)
	{
		return expression.accept(tryThrowCodegenVisitor, data);
	}

	@Override
	public StackValue visitAnonymClassExpression(NapileAnonymClassExpression expression, StackValue data)
	{
		return InnerClassCodegen.genAnonym(expression, this);
	}

	@Override
	public StackValue visitAnonymMethodExpression(NapileAnonymMethodExpression expression, StackValue data)
	{
		return expression.accept(closureCodegenVisitor, data);
	}

	@Override
	public StackValue visitLinkMethodExpression(NapileLinkMethodExpression expression, StackValue data)
	{
		return expression.accept(closureCodegenVisitor, data);
	}

	@Override
	public StackValue visitBreakExpression(NapileBreakExpression expression, StackValue data)
	{
		return expression.accept(loopCodegenVisitor, data);
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
		return expression.accept(conditionCodegenVisitor, data);
	}

	@Override
	public StackValue visitPrefixExpression(NapilePrefixExpression expression, StackValue receiver)
	{
		return expression.accept(binaryCodegenVisitor, receiver);
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
		return expression.accept(conditionCodegenVisitor, receiver);
	}

	@Override
	public StackValue visitWhenExpression(NapileWhenExpression expression, StackValue data)
	{
		return expression.accept(conditionCodegenVisitor, data);
	}

	@Override
	public StackValue visitReturnExpression(NapileReturnExpression expression, StackValue receiver)
	{
		final NapileExpression returnedExpression = expression.getReturnedExpression();

		visitReturn(returnedExpression);

		return StackValue.none();
	}

	@Override
	public StackValue visitBinaryExpression(NapileBinaryExpression expression, StackValue receiver)
	{
		return expression.accept(binaryCodegenVisitor, receiver);
	}

	@Override
	public StackValue visitPostfixExpression(NapilePostfixExpression expression, StackValue receiver)
	{
		return expression.accept(binaryCodegenVisitor, receiver);
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
			if(variableDescriptor != null || callee instanceof NapileAnonymMethodExpression)
			{
				StackValue stackValue = gen(callee);

				stackValue.put(expressionType(callee), instructs, this);

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
					newReceiver.put(newReceiver.getType(), instructs, this);

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
			return stackValueForLocal(expression, descriptor, index);

		if(descriptor instanceof VariableDescriptor)
		{
			WrappedVar wrappedValue = context.wrappedVariables.get(descriptor);
			if(wrappedValue != null)
			{
				wrappedValue.putReceiver(this);

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
				receiver.put(isStatic ? receiver.getType() : TypeTransformer.toAsmType(bindingTrace, ((ClassDescriptor) container).getDefaultType(), classNode), instructs, this);
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
					receiver.put(receiverType != null && !isSuper ? TypeTransformer.toAsmType(bindingTrace, receiverType, classNode) : AsmConstants.ANY_TYPE, instructs, this);
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
		MethodRef setRef = new MethodRef(NapileLangPackage.ARRAY.child(Name.identifier("set")), Arrays.<MethodParameterNode>asList(AsmNodeUtil.parameterNode("index", AsmConstants.INT_TYPE), AsmNodeUtil.parameterNode("element", new TypeNode(false, new TypeParameterValueTypeNode(Name.identifier("E"))))), Collections.<TypeNode>emptyList(), typeNode);
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
			return StackValue.variableAccessor(methodDescriptor, TypeTransformer.toAsmType(bindingTrace, variableDescriptor.getType(), classNode), this, CallTransformer.isNullable(expression), expression);

		if(!forceField)
		{
			throw new IllegalArgumentException("Using old method to invoking variable accessors: " + expression.getParent().getText());
			//return StackValue.simpleVariableAccessor(FqNameGenerator.getFqName(variableDescriptor, bindingTrace), TypeTransformer.toAsmType(bindingTrace, variableDescriptor.getType(), classNode), variableDescriptor.isStatic() ? CallableMethod.CallType.STATIC : CallableMethod.CallType.VIRTUAL);
		}
		else
			return StackValue.variable(FqNameGenerator.getFqName(variableDescriptor, bindingTrace), TypeTransformer.toAsmType(bindingTrace, variableDescriptor.getType(), classNode), variableDescriptor.isStatic());
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

			receiver.put(receiver.getType(), instructs, this);

			ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.safeGet(BindingContext.RESOLVED_CALL, expression.getCalleeExpression());

			pushMethodArguments(resolvedCall, method.getValueParameterTypes());

			marker(expression).newObject(type, method.getValueParameterTypes());
		}
		else
		{
			throw new UnsupportedOperationException("don't know how to generate this new expression");
		}
		return StackValue.onStack(type);
	}

	public StackValue invokeOperation(NapileOperationExpression expression, MethodDescriptor op, CallableMethod callable)
	{
		int functionLocalIndex = lookupLocalIndex(op);
		if(functionLocalIndex >= 0)
			throw new UnsupportedOperationException();
		//stackValueForLocal(op, functionLocalIndex).put(getInternalClassName(op).getAsmType(), v);

		ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.safeGet(BindingContext.RESOLVED_CALL, expression.getOperationReference());

		genThisAndReceiverFromResolvedCall(StackValue.none(), resolvedCall, callable);
		pushMethodArguments(resolvedCall, callable.getValueParameterTypes());
		callable.invoke(instructs, this, expression.getOperationReference());

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

	private void putFinallyBlocks()
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
			receiver.put(receiver.getType(), instructs, this);
			/*if(calleeType != null)
			{
				StackValue.onStack(receiver.type).put(boxType(receiver.type), v);
			}    */
		}

		pushMethodArguments(resolvedCall, callableMethod.getValueParameterTypes());

		callableMethod.invoke(instructs, PositionMarker.EMPTY, null);

		setRefs(resolvedCall);
	}

	private void setRefs(@NotNull ResolvedCall<? extends CallableDescriptor> resolvedCall)
	{
		CallableDescriptor fd = resolvedCall.getResultingDescriptor();
		List<ResolvedValueArgument> valueArguments = resolvedCall.getValueArgumentsByIndex();

		for(CallParameterDescriptor valueParameterDescriptor : fd.getValueParameters())
		{
			if(!valueParameterDescriptor.isRef())
				continue;

			ResolvedValueArgument resolvedValueArgument = valueArguments.get(valueParameterDescriptor.getIndex());
			if(resolvedValueArgument instanceof ExpressionValueArgument)
			{
				ExpressionValueArgument valueArgument = (ExpressionValueArgument) resolvedValueArgument;

				NapileExpression expression = valueArgument.getValueArgument().getArgumentExpression();

				if(expression instanceof NapileSimpleNameExpression)
				{
					DeclarationDescriptor referenceDescriptor = bindingTrace.get(BindingContext.REFERENCE_TARGET, (NapileSimpleNameExpression) expression);
					if(referenceDescriptor instanceof LocalVariableDescriptor)
					{
						instructs.localPut(frameMap.getIndex(referenceDescriptor));
						continue;
					}
				}

			}

			instructs.pop();
		}
	}

	private void genThisAndReceiverFromResolvedCall(StackValue receiver, ResolvedCall<? extends CallableDescriptor> resolvedCall, CallableMethod callableMethod)
	{
		receiver = StackValue.receiver(resolvedCall, receiver, this, callableMethod);
		receiver.put(receiver.getType(), instructs, this);
	}

	private StackValue stackValueForLocal(PsiElement target, DeclarationDescriptor descriptor, int index)
	{
		if(descriptor instanceof VariableDescriptor)
		{
			final JetType outType = ((VariableDescriptor) descriptor).getType();

			return StackValue.local(target, index, TypeTransformer.toAsmType(bindingTrace, outType, classNode));
		}
		else
			return StackValue.local(target, index, AsmConstants.ANY_TYPE);
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

			generateThisOrOuter(classReceiverDeclarationDescriptor, false).put(type, instructs, this);
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
			StackValue.onStack(intermediateType).put(type, instructs, this);
		}
		else
		{
			throw new UnsupportedOperationException("Unsupported receiver type: " + descriptor);
		}
	}

	public StackValue generateThisOrOuter(@NotNull final ClassDescriptor calleeContainingClass, boolean isSuper)
	{
		StackValue wrappedOuter = context.wrappedOuterClasses.get(calleeContainingClass);
		if(wrappedOuter != null)
			return wrappedOuter;

		return StackValue.local(null, 0, TypeTransformer.toAsmType(bindingTrace, calleeContainingClass.getDefaultType(), classNode));
	}

	private void generateLocalVariableDeclaration(@NotNull final NapileVariable variableDeclaration, @NotNull List<Function<StackValue, Void>> leaveTasks)
	{
		final VariableDescriptor variableDescriptor = bindingTrace.safeGet(BindingContext.VARIABLE, variableDeclaration);
		if(!context.wrapVariableIfNeed(variableDescriptor))
		{
			final TypeNode type = TypeTransformer.toAsmType(bindingTrace, variableDescriptor.getType(), classNode);
			int index = frameMap.enter(variableDescriptor);

			leaveTasks.add(new Function<StackValue, Void>()
			{
				@Override
				public Void fun(StackValue answer)
				{
					int index = frameMap.leave(variableDescriptor);

					instructs.visitLocalVariable(variableDescriptor.getName().getName());
					return null;
				}
			});
		}
	}

	private void initializeLocalVariable(@NotNull NapileVariable variableDeclaration, @NotNull Function<VariableDescriptor, Void> generateInitializer)
	{
		VariableDescriptor variableDescriptor = bindingTrace.safeGet(BindingContext.VARIABLE, variableDeclaration);

		WrappedVar wrappedVariable = context.wrappedVariables.get(variableDescriptor);
		if(wrappedVariable != null)
		{
			wrappedVariable.putReceiver(this);

			generateInitializer.fun(variableDescriptor);

			wrappedVariable.store(TypeTransformer.toAsmType(bindingTrace, variableDescriptor.getType(), classNode), instructs, this);
		}
		else
		{
			int index = lookupLocalIndex(variableDescriptor);

			if(index < 0)
				throw new IllegalStateException("Local variable not found for " + variableDescriptor);

			generateInitializer.fun(variableDescriptor);

			instructs.localPut(index);
		}
	}

	public int lookupLocalIndex(DeclarationDescriptor descriptor)
	{
		return frameMap.getIndex(descriptor);
	}

	public void gen(NapileElement expr, TypeNode type)
	{
		StackValue value = gen(expr);
		if(value == null)
			return;
		value.put(type, instructs, this);
	}

	public StackValue gen(NapileElement element)
	{
		if(element instanceof NapileExpression)
		{
			NapileExpression expression = (NapileExpression) element;
			CompileTimeConstant<?> constant = bindingTrace.get(BindingContext.COMPILE_TIME_VALUE, expression);
			if(constant != null)
				return StackValue.constant(expression, constant.getValue(), expressionType(expression));
		}

		return genQualified(StackValue.none(), element);
	}

	public StackValue genQualified(StackValue receiver, NapileElement selector)
	{
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
		else if(expr instanceof NapileBlockExpression && (expr.getParent() instanceof NapileNamedMethodOrMacro || expr.getParent() instanceof NapileConstructor))
		{
			visitReturn(null);
		}
		else
		{
			lastValue.put(returnType, instructs, this);
			instructs.returnValues(1);
		}
	}

	public void visitReturn(@Nullable NapileExpression exp)
	{
		putFinallyBlocks();

		int size = putRefVariables();

		if(exp != null)
		{
			gen(exp, returnType);
		}
		else
		{
			if(returnType.typeConstructorNode instanceof ThisTypeNode)
				instructs.localGet(0);
			else
				instructs.putNull();
		}

		instructs.returnValues(size);
	}

	public int putRefVariables()
	{
		if(refParameters == null)
			throw new IllegalArgumentException("Ref parameters is null but is require return");
		if(refParameters.isEmpty())
			return 1;
		for(int i : refParameters.toNativeArray())
			instructs.localGet(i);
		return refParameters.size() + 1;
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

	public static boolean isEmptyExpression(NapileElement expr)
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

	public InstructionAdapter marker(final PsiElement element)
	{
		if(element == null)
			return instructs;

		return new InstructionAdapter()
		{
			@Override
			protected <T extends Instruction> T add(T t)
			{
				instructs.getInstructions().add(t);
				mark(t, element);
				return t;
			}
		};
	}

	@Override
	@NotNull
	public  <T extends Instruction> T mark(@NotNull T instruction, PsiElement target)
	{
		if(target == null)
			return instruction;
		PsiFile containingFile = target.getContainingFile();
		Document document = containingFile.getViewProvider().getDocument();
		if(document != null)
		{
			TextRange textRange = target.getTextRange();
			int lineNumber = document.getLineNumber(textRange.getStartOffset());

			int lineStartOffset = document.getLineStartOffset(lineNumber);
			int column = textRange.getStartOffset() - lineStartOffset;

			String path = null;
			if(containingFile instanceof NapileFile)
			{
				StringBuilder builder = new StringBuilder();
				String packageName = ((NapileFile) containingFile).getPackageName();
				if(packageName != null)
					builder.append(packageName.replace(".", "/")).append("/");
				builder.append(containingFile.getVirtualFile().getName());
				path = builder.toString();
			}
			else
				throw new UnsupportedOperationException(containingFile.getClass().getName());

			instruction.position = new InstructionInCodePosition(path, lineNumber + 1, column + 1);
		}

		return instruction;
	}
}
