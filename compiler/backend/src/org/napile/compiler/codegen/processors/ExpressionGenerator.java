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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.Label;
import org.napile.asm.adapters.InstructionAdapter;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.compiler.codegen.CompilationException;
import org.napile.compiler.codegen.processors.codegen.CallTransformer;
import org.napile.compiler.codegen.processors.codegen.Callable;
import org.napile.compiler.codegen.processors.codegen.CallableMethod;
import org.napile.compiler.codegen.processors.codegen.FrameMap;
import org.napile.compiler.codegen.processors.codegen.IntrinsicMethod;
import org.napile.compiler.codegen.processors.codegen.TypeConstants;
import org.napile.compiler.codegen.processors.codegen.stackValue.Local;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.CallableMemberDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.ConstructorDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.ParameterDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.calls.AutoCastReceiver;
import org.napile.compiler.lang.resolve.calls.ExpressionValueArgument;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.resolve.calls.ResolvedValueArgument;
import org.napile.compiler.lang.resolve.calls.VariableAsFunctionResolvedCall;
import org.napile.compiler.lang.resolve.constants.CompileTimeConstant;
import org.napile.compiler.lang.resolve.scopes.receivers.ClassReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lexer.JetTokens;
import com.google.common.collect.Lists;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.util.Function;

/**
 * @author VISTALL
 * @date 0:24/07.09.12
 * <p/>
 * base code getting from Kotlin
 */
public class ExpressionGenerator extends NapileVisitor<StackValue, StackValue>
{
	@NotNull
	private final BindingTrace bindingTrace;
	@NotNull
	private final Map<NapileElement, Local> tempVariables = new HashMap<NapileElement, Local>();
	@NotNull
	private final InstructionAdapter instructs = new InstructionAdapter();
	@NotNull
	private final FrameMap myFrameMap;
	@NotNull
	private final TypeNode returnType;
	private final boolean isInstanceConstructor;

	public ExpressionGenerator(@NotNull BindingTrace b, @NotNull CallableDescriptor d)
	{
		bindingTrace = b;
		isInstanceConstructor = d instanceof ConstructorDescriptor;
		returnType = isInstanceConstructor ? TypeTransformer.toAsmType(((ClassDescriptor) d.getContainingDeclaration()).getDefaultType()) : TypeTransformer.toAsmType(d.getReturnType());
		myFrameMap = new FrameMap();

		if(!d.isStatic())
			myFrameMap.enterTemp(TypeConstants.ANY);
		for(ParameterDescriptor p : d.getValueParameters())
			myFrameMap.enter(p, null);
	}

	@Override
	public StackValue visitProperty(NapileProperty property, StackValue receiver)
	{
		final NapileExpression initializer = property.getInitializer();
		if(initializer == null)
			return StackValue.none();

		initializeLocalVariable(property, new Function<VariableDescriptor, Void>()
		{
			@Override
			public Void fun(VariableDescriptor descriptor)
			{
				TypeNode varType = asmType(descriptor.getType());
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
				StackValue.local(0, returnType);
			else
				StackValue.nullInstance().put(returnType, instructs);

			instructs.returnVal();
		}
		return StackValue.none();
	}

	@Override
	public StackValue visitStringTemplateExpression(NapileStringTemplateExpression expression, StackValue receiver)
	{
		StringBuilder constantValue = new StringBuilder("");
		for(NapileStringTemplateEntry entry : expression.getEntries())
		{
			if(entry instanceof NapileLiteralStringTemplateEntry)
				constantValue.append(entry.getText());
			else if(entry instanceof NapileEscapeStringTemplateEntry)
				constantValue.append(((NapileEscapeStringTemplateEntry) entry).getUnescapedValue());
			else
			{
				constantValue = null;
				break;
			}
		}
		if(constantValue != null)
		{
			final TypeNode type = expressionType(expression);
			return StackValue.constant(constantValue.toString(), type);
		}
		else
			throw new UnsupportedOperationException();
	}

	@Override
	public StackValue visitBinaryExpression(NapileBinaryExpression expression, StackValue receiver)
	{
		final IElementType opToken = expression.getOperationReference().getReferencedNameElementType();
		if(opToken == JetTokens.EQ)
			return generateAssignmentExpression(expression);
		/*else if(JetTokens.AUGMENTED_ASSIGNMENTS.contains(opToken))
		{
			return generateAugmentedAssignment(expression);
		}
		else if(opToken == JetTokens.ANDAND)
		{
			return generateBooleanAnd(expression);
		}
		else if(opToken == JetTokens.OROR)
		{
			return generateBooleanOr(expression);
		}
		else if(opToken == JetTokens.EQEQ || opToken == JetTokens.EXCLEQ ||
				opToken == JetTokens.EQEQEQ || opToken == JetTokens.EXCLEQEQEQ)
		{
			return generateEquals(expression.getLeft(), expression.getRight(), opToken);
		}
		else if(opToken == JetTokens.LT || opToken == JetTokens.LTEQ ||
				opToken == JetTokens.GT || opToken == JetTokens.GTEQ)
		{
			return generateCompareOp(expression.getLeft(), expression.getRight(), opToken, expressionType(expression.getLeft()));
		}
		else if(opToken == JetTokens.ELVIS)
		{
			return generateElvis(expression);
		}
		else if(opToken == JetTokens.IN_KEYWORD || opToken == JetTokens.NOT_IN)
		{
			return generateIn(expression);
		}
		else */
		{
			DeclarationDescriptor op = bindingTrace.get(BindingContext.REFERENCE_TARGET, expression.getOperationReference());
			final Callable callable = CallTransformer.transformToCallable((MethodDescriptor) op);
			if(callable instanceof IntrinsicMethod)
			{
				IntrinsicMethod intrinsic = (IntrinsicMethod) callable;

				return intrinsic.generate(this, instructs, expressionType(expression), expression, Arrays.asList(expression.getLeft(), expression.getRight()), receiver);
			}
			else
				return invokeOperation(expression, (MethodDescriptor) op, (CallableMethod) callable);
		}
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
			Call call = bindingTrace.get(BindingContext.CALL, expression.getCalleeExpression());
			if(resolvedCall instanceof VariableAsFunctionResolvedCall)
			{
				throw new UnsupportedOperationException();
				//	VariableAsFunctionResolvedCall variableAsFunctionResolvedCall = (VariableAsFunctionResolvedCall) resolvedCall;
				//	ResolvedCallWithTrace<FunctionDescriptor> functionCall = variableAsFunctionResolvedCall.getFunctionCall();
				//	return invokeFunction(call, receiver, functionCall);
			}
			else
				return invokeFunction(call, receiver, resolvedCall);
		}
	}

	@Override
	public StackValue visitArrayAccessExpression(NapileArrayAccessExpression expression, StackValue receiver)
	{
		MethodDescriptor operationDescriptor = (MethodDescriptor) bindingTrace.safeGet(BindingContext.REFERENCE_TARGET, expression);
		CallableMethod accessor = CallTransformer.transformToCallable(operationDescriptor);

		boolean isGetter = accessor.getName().endsWith("get");

		ResolvedCall<MethodDescriptor> resolvedGetCall = bindingTrace.get(BindingContext.INDEXED_LVALUE_GET, expression);
		ResolvedCall<MethodDescriptor> resolvedSetCall = bindingTrace.get(BindingContext.INDEXED_LVALUE_SET, expression);

		List<TypeNode> argumentTypes = accessor.getValueParameterTypes();
		int index = 0;

		TypeNode asmType;
		if(isGetter)
		{
			genThisAndReceiverFromResolvedCall(receiver, resolvedGetCall, CallTransformer.transformToCallable(resolvedGetCall.getResultingDescriptor()));

			asmType = accessor.getReturnType();
		}
		else
		{
			genThisAndReceiverFromResolvedCall(receiver, resolvedSetCall, CallTransformer.transformToCallable(resolvedSetCall.getResultingDescriptor()));

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
	public StackValue visitSimpleNameExpression(NapileSimpleNameExpression expression, StackValue receiver)
	{
		ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.get(BindingContext.RESOLVED_CALL, expression);

		DeclarationDescriptor descriptor;
		if(resolvedCall == null)
			descriptor = bindingTrace.get(BindingContext.REFERENCE_TARGET, expression);
		else
		{
			if(resolvedCall instanceof VariableAsFunctionResolvedCall)
			{
				VariableAsFunctionResolvedCall call = (VariableAsFunctionResolvedCall) resolvedCall;
				resolvedCall = call.getVariableCall();
			}
			receiver = StackValue.receiver(resolvedCall, receiver, this, null);
			descriptor = resolvedCall.getResultingDescriptor();
		}

		IntrinsicMethod intrinsic = null;
		if(descriptor instanceof CallableMemberDescriptor)
		{
			CallableMemberDescriptor memberDescriptor = (CallableMemberDescriptor) descriptor;
			memberDescriptor = unwrapFakeOverride(memberDescriptor);

			intrinsic = CallTransformer.findIntrinsicMethod(memberDescriptor);
		}

		if(intrinsic != null)
		{
			final TypeNode expectedType = expressionType(expression);
			return intrinsic.generate(this, instructs, expectedType, expression, Collections.<NapileExpression>emptyList(), receiver);
		}

		assert descriptor != null;
		final DeclarationDescriptor container = descriptor.getContainingDeclaration();

		int index = lookupLocalIndex(descriptor);
		if(index >= 0)
			return stackValueForLocal(descriptor, index);

		if(descriptor instanceof PropertyDescriptor)
		{
			PropertyDescriptor propertyDescriptor = (PropertyDescriptor) descriptor;

			boolean isStatic = propertyDescriptor.isStatic();
			final boolean directToField = expression.getReferencedNameElementType() == JetTokens.FIELD_IDENTIFIER;
			NapileExpression r = getReceiverForSelector(expression);
			final boolean isSuper = r instanceof NapileSuperExpression;

			final StackValue iValue = intermediateValueForProperty(propertyDescriptor, directToField, isSuper ? (NapileSuperExpression) r : null);
			if(!directToField && resolvedCall != null && !isSuper)
				receiver.put(isStatic ? receiver.getType() : TypeTransformer.toAsmType(((ClassDescriptor) container).getDefaultType()), instructs);
			else
			{
				if(!isStatic)
				{
					if(receiver == StackValue.none())
					{
						if(resolvedCall == null)
							receiver = generateThisOrOuter((ClassDescriptor) propertyDescriptor.getContainingDeclaration(), false);
						else
							receiver = generateThisOrOuter((ClassDescriptor) propertyDescriptor.getContainingDeclaration(), false);
					}
					JetType receiverType = bindingTrace.get(BindingContext.EXPRESSION_TYPE, r);
					receiver.put(receiverType != null && !isSuper ? asmType(receiverType) : TypeConstants.ANY, instructs);
				}
			}
			return iValue;
		}

		throw new UnsupportedOperationException();
	}

	private StackValue generateAssignmentExpression(NapileBinaryExpression expression)
	{
		StackValue stackValue = gen(expression.getLeft());
		gen(expression.getRight(), stackValue.getType());
		stackValue.store(stackValue.getType(), instructs);
		return StackValue.none();
	}

	public StackValue intermediateValueForProperty(PropertyDescriptor propertyDescriptor, final boolean forceField, @Nullable NapileSuperExpression superExpression)
	{
		boolean isSuper = superExpression != null;

		//TODO [VISTALL] super?

		if(!forceField)
			return StackValue.property(DescriptorUtils.getFQName(propertyDescriptor).toSafe(), TypeTransformer.toAsmType(propertyDescriptor.getType()), propertyDescriptor.isStatic());
		else
			return StackValue.variable(DescriptorUtils.getFQName(propertyDescriptor).toSafe(), TypeTransformer.toAsmType(propertyDescriptor.getType()), propertyDescriptor.isStatic());
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

			type = TypeTransformer.toAsmType(expressionType);
			instructs.newObject(type);
			instructs.dup();

			//final ClassDescriptor classDescriptor = ((ConstructorDescriptor) constructorDescriptor).getContainingDeclaration();

			CallableMethod method = CallTransformer.transformToCallable((ConstructorDescriptor) constructorDescriptor);

			receiver.put(receiver.getType(), instructs);

			invokeMethodWithArguments(method, expression, StackValue.none());
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
		return returnValueAsStackValue(op, callable.getReturnType());
	}

	private StackValue invokeFunction(Call call, StackValue receiver, ResolvedCall<? extends CallableDescriptor> resolvedCall)
	{
		MethodDescriptor fd = (MethodDescriptor) resolvedCall.getResultingDescriptor();

		//TODO [VISTALL] super<A>.foo();

		Callable callable = CallTransformer.transformToCallable(fd);
		final CallableMethod callableMethod = (CallableMethod) callable;

		invokeMethodWithArguments(callableMethod, resolvedCall, call, receiver);

		final TypeNode callReturnType = callableMethod.getReturnType();

		return returnValueAsStackValue(fd, callReturnType);
	}

	private void doFinallyOnReturn()
	{
		//TODO [VISTALL] make it
	}

	public void invokeMethodWithArguments(CallableMethod callableMethod, NapileCallElement expression, StackValue receiver)
	{
		NapileExpression calleeExpression = expression.getCalleeExpression();
		Call call = bindingTrace.safeGet(BindingContext.CALL, calleeExpression);
		ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.safeGet(BindingContext.RESOLVED_CALL, calleeExpression);

		invokeMethodWithArguments(callableMethod, resolvedCall, call, receiver);
	}

	protected void invokeMethodWithArguments(@NotNull CallableMethod callableMethod, @NotNull ResolvedCall<? extends CallableDescriptor> resolvedCall, @NotNull Call call, @NotNull StackValue receiver)
	{
		/*final Type calleeType = callableMethod.getGenerateCalleeType();
		if(calleeType != null)
		{
			assert !callableMethod.isNeedsThis();
			gen(call.getCalleeExpression(), calleeType);
		}   */

		if(resolvedCall instanceof VariableAsFunctionResolvedCall)
			resolvedCall = ((VariableAsFunctionResolvedCall) resolvedCall).getFunctionCall();

		if(!(resolvedCall.getResultingDescriptor() instanceof ConstructorDescriptor))  // otherwise already
		{
			receiver = StackValue.receiver(resolvedCall, receiver, this, callableMethod);
			receiver.put(receiver.getType(), instructs);
			/*if(calleeType != null)
			{
				StackValue.onStack(receiver.type).put(boxType(receiver.type), v);
			}    */
		}

		int mask = pushMethodArguments(resolvedCall, callableMethod.getValueParameterTypes());
		if(mask == 0)
			callableMethod.invoke(instructs);
		else
			callableMethod.invokeWithDefault(instructs, mask);
	}

	private StackValue returnValueAsStackValue(MethodDescriptor fd, TypeNode callReturnType)
	{
		return StackValue.none(); //TODO
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

			return StackValue.local(index, asmType(outType));
		}
		else
			return StackValue.local(index, TypeConstants.ANY);
	}

	private int pushMethodArguments(@NotNull ResolvedCall resolvedCall, List<TypeNode> valueParameterTypes)
	{
		@SuppressWarnings("unchecked") List<ResolvedValueArgument> valueArguments = resolvedCall.getValueArgumentsByIndex();
		CallableDescriptor fd = resolvedCall.getResultingDescriptor();

		if(fd.getValueParameters().size() != valueArguments.size())
		{
			throw new IllegalStateException();
		}

		int index = 0;
		int mask = 0;

		for(ParameterDescriptor valueParameterDescriptor : fd.getValueParameters())
		{
			ResolvedValueArgument resolvedValueArgument = valueArguments.get(valueParameterDescriptor.getIndex());
			if(resolvedValueArgument instanceof ExpressionValueArgument)
			{
				ExpressionValueArgument valueArgument = (ExpressionValueArgument) resolvedValueArgument;
				//noinspection ConstantConditions
				gen(valueArgument.getValueArgument().getArgumentExpression(), valueParameterTypes.get(index));
			}
			/*else if(resolvedValueArgument instanceof DefaultValueArgument)
			{
				Type type = valueParameterTypes.get(index);
				if(type.getSort() == Type.OBJECT || type.getSort() == Type.ARRAY)
				{
					v.aconst(null);
				}
				else if(type.getSort() == Type.FLOAT)
				{
					v.aconst(0f);
				}
				else if(type.getSort() == Type.DOUBLE)
				{
					v.aconst(0d);
				}
				else if(type.getSort() == Type.LONG)
				{
					v.aconst(0l);
				}
				else
				{
					v.iconst(0);
				}
				mask |= (1 << index);
			}    */
			/*else if(resolvedValueArgument instanceof VarargValueArgument)
			{
				VarargValueArgument valueArgument = (VarargValueArgument) resolvedValueArgument;

				genVarargs(valueParameterDescriptor, valueArgument);
			}  */
			else
			{
				throw new UnsupportedOperationException();
			}
			index++;
		}
		return mask;
	}

	private StackValue generateBlock(List<NapileElement> statements)
	{
		final Label blockEnd = new Label();

		List<Function<StackValue, Void>> leaveTasks = Lists.newArrayList();

		StackValue answer = StackValue.none();

		for(Iterator<NapileElement> iterator = statements.iterator(); iterator.hasNext(); )
		{
			NapileElement statement = iterator.next();

			if(statement instanceof NapileProperty)
				generateLocalVariableDeclaration((NapileProperty) statement, null, leaveTasks);

			if(!iterator.hasNext())
				answer = gen(statement);
			else
				gen(statement, TypeConstants.NULL);
		}

		instructs.mark(blockEnd);

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
			TypeNode exprType = asmType(descriptor.getType());
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
			TypeNode intermediateType = asmType(autoCastReceiver.getType());
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
		return StackValue.local(0, TypeTransformer.toAsmType(calleeContainingClass.getDefaultType()));
	}

	private void generateLocalVariableDeclaration(@NotNull final NapileProperty variableDeclaration, final @Nullable Label blockEnd, @NotNull List<Function<StackValue, Void>> leaveTasks)
	{
		final VariableDescriptor variableDescriptor = bindingTrace.get(BindingContext.VARIABLE, variableDeclaration);
		assert variableDescriptor != null;

		final Label scopeStart = new Label();
		instructs.mark(scopeStart);

		final TypeNode type = asmType(variableDescriptor.getType());
		int index = myFrameMap.enter(variableDescriptor, type);

		leaveTasks.add(new Function<StackValue, Void>()
		{
			@Override
			public Void fun(StackValue answer)
			{
				int index = myFrameMap.leave(variableDescriptor);

				getInstructs().visitLocalVariable(variableDescriptor.getName().getName());
				return null;
			}
		});
	}

	private void initializeLocalVariable(@NotNull NapileProperty variableDeclaration, @NotNull Function<VariableDescriptor, Void> generateInitializer)
	{
		VariableDescriptor variableDescriptor = bindingTrace.get(BindingContext.VARIABLE, variableDeclaration);

		int index = lookupLocalIndex(variableDescriptor);

		if(index < 0)
			throw new IllegalStateException("Local variable not found for " + variableDescriptor);

		assert variableDescriptor != null;

		generateInitializer.fun(variableDescriptor);

		getInstructs().store(index);
	}

	public int lookupLocalIndex(DeclarationDescriptor descriptor)
	{
		return myFrameMap.getIndex(descriptor);
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

	public void returnExpression(@NotNull NapileExpression expr)
	{
		StackValue lastValue = gen(expr);

		if(lastValue.getType() != TypeConstants.NULL)
		{
			lastValue.put(returnType, instructs);
			instructs.returnVal();
		}
		else if(!endsWithReturn(expr))
		{
			if(isInstanceConstructor)
				StackValue.local(0, returnType).put(returnType, instructs);
			else
				StackValue.nullInstance().put(TypeConstants.NULL, instructs);
			instructs.returnVal();
		}
	}

	@SuppressWarnings("unchecked")
	public static <T extends CallableMemberDescriptor> T unwrapFakeOverride(T member)
	{
		while(member.getKind() == CallableMemberDescriptor.Kind.FAKE_OVERRIDE)
			member = (T) member.getOverriddenDescriptors().iterator().next();
		return member;
	}

	private static boolean endsWithReturn(NapileElement bodyExpression)
	{
		if(bodyExpression instanceof NapileBlockExpression)
		{
			final List<NapileElement> statements = ((NapileBlockExpression) bodyExpression).getStatements();
			return statements.size() > 0 && statements.get(statements.size() - 1) instanceof NapileReturnExpression;
		}

		return bodyExpression instanceof NapileReturnExpression;
	}

	@NotNull
	private static TypeNode asmType(@NotNull JetType jetType)
	{
		return TypeTransformer.toAsmType(jetType);
	}

	@NotNull
	public TypeNode expressionType(NapileExpression expr)
	{
		JetType type = bindingTrace.get(BindingContext.EXPRESSION_TYPE, expr);
		return type == null ? StackValue.none().getType() : TypeTransformer.toAsmType(type);
	}

	@NotNull
	public InstructionAdapter getInstructs()
	{
		return instructs;
	}
}
