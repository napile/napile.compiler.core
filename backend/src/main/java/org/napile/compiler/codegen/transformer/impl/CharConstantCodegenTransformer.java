package org.napile.compiler.codegen.transformer.impl;

import org.jetbrains.annotations.Nullable;
import org.napile.asm.AsmConstants;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.asm.resolve.name.Name;
import org.napile.compiler.codegen.processors.codegen.stackValue.StackValue;
import org.napile.compiler.codegen.transformer.CodegenTransformer;
import org.napile.compiler.lang.descriptors.CallableDescriptor;
import org.napile.compiler.lang.descriptors.ClassDescriptor;
import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import org.napile.compiler.lang.psi.NapileCallExpression;
import org.napile.compiler.lang.psi.NapileConstantExpression;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.calls.ResolvedCall;
import org.napile.compiler.lang.resolve.constants.CharValue;
import org.napile.compiler.lang.resolve.constants.CompileTimeConstant;
import org.napile.compiler.lang.resolve.constants.StringValue;
import org.napile.compiler.lang.resolve.scopes.receivers.ExpressionReceiver;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;

/**
 * @author VISTALL
 * @since 19:04/12.05.13
 */
public class CharConstantCodegenTransformer implements CodegenTransformer
{
	private static final Name NAME = Name.identifier("toChar");

	@Nullable
	@Override
	public StackValue doTransform(BindingTrace bindingTrace, NapileExpression expression)
	{
		if(!(expression instanceof NapileCallExpression))
		{
			return null;
		}

		ResolvedCall<? extends CallableDescriptor> resolvedCall = bindingTrace.safeGet(BindingTraceKeys.RESOLVED_CALL, ((NapileCallExpression) expression).getCalleeExpression());

		final CallableDescriptor candidateDescriptor = resolvedCall.getCandidateDescriptor();

		if(!NAME.equals(candidateDescriptor.getName()))
		{
			return null;
		}

		final DeclarationDescriptor containingDeclaration = candidateDescriptor.getContainingDeclaration();

		ClassDescriptor classDescriptor = (ClassDescriptor) containingDeclaration;
		if(!NapileLangPackage.STRING.equals(DescriptorUtils.getFQName(classDescriptor)))
		{
			return null;
		}

		final ReceiverDescriptor thisObject = resolvedCall.getThisObject();
		if(thisObject instanceof ExpressionReceiver)
		{
			final NapileExpression receiverExpression = ((ExpressionReceiver) thisObject).getExpression();
			if(receiverExpression instanceof NapileConstantExpression)
			{
				final CompileTimeConstant<?> compileTimeConstant = bindingTrace.safeGet(BindingTraceKeys.COMPILE_TIME_VALUE, receiverExpression);
				if(compileTimeConstant instanceof StringValue)
				{
					final String value = ((StringValue)compileTimeConstant).getValue();
					return StackValue.constant(receiverExpression, value.charAt(0), AsmConstants.CHAR_TYPE);
				}
				else if(compileTimeConstant instanceof CharValue)
				{
					final Character value = ((CharValue)compileTimeConstant).getValue();
					return StackValue.constant(receiverExpression, value, AsmConstants.CHAR_TYPE);
				}
			}
		}
		return null;
	}
}
