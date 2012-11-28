package org.napile.compiler.lang.resolve.processors.checkers;

import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.MethodDescriptor;
import org.napile.compiler.lang.descriptors.Modality;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.PropertyDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.diagnostics.Errors;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileClassLike;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileNamedMethod;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BodiesResolveContext;
import org.napile.compiler.lang.resolve.PropertyAccessUtil;
import com.intellij.openapi.util.Pair;

/**
 * @author VISTALL
 * @date 9:54/03.11.12
 */
public class PropertiesChecker
{
	@NotNull
	private BindingTrace trace;

	@Inject
	public void setTrace(@NotNull BindingTrace trace)
	{
		this.trace = trace;
	}

	public void process(@NotNull BodiesResolveContext bodiesResolveContext)
	{
		for(Map.Entry<NapileClass, MutableClassDescriptor> entry : bodiesResolveContext.getClasses().entrySet())
			resolvePropertyAccess(entry.getKey(), entry.getValue());

		for(Map.Entry<NapileVariable, PropertyDescriptor> entry : bodiesResolveContext.getProperties().entrySet())
		{
			NapileVariable variable = entry.getKey();
			PropertyDescriptor propertyDescriptor = entry.getValue();

			Pair<MethodDescriptor, NapileNamedMethod> set = PropertyAccessUtil.get(trace, propertyDescriptor, NapileTokens.SET_KEYWORD);
			Pair<MethodDescriptor, NapileNamedMethod> get = PropertyAccessUtil.get(trace, propertyDescriptor, NapileTokens.GET_KEYWORD);
			Pair<MethodDescriptor, NapileNamedMethod> lazy = PropertyAccessUtil.get(trace, propertyDescriptor, NapileTokens.LAZY_KEYWORD);

			if(variable.getInitializer() != null && lazy != null)
				trace.report(Errors.VARIABLE_WITH_INITIALIZER_AND_LAZY_PROPERTY.on(variable, propertyDescriptor));

			if(propertyDescriptor.getModality() == Modality.FINAL && set != null)
				trace.report(Errors.SET_PROPERTY_WITH_FINAL_VARIABLE.on(set.getSecond(), set.getFirst()));

			if(get != null && lazy != null)
				trace.report(Errors.GET_PROPERTY_WITH_LAZY_PROPERTY.on(get.getSecond(), get.getFirst()));
		}
	}

	private void resolvePropertyAccess(@NotNull NapileClassLike classLike, @NotNull final MutableClassDescriptor mutableClassDescriptor)
	{
		for(NapileDeclaration declaration : classLike.getDeclarations())
		{
			if(!(declaration instanceof NapileNamedMethod))
				continue;
			NapileNamedMethod method = (NapileNamedMethod) declaration;
			if(method.getPropertyAccessType() == null)
				continue;

			VariableDescriptor variableDescriptor = (VariableDescriptor) trace.get(BindingContext.REFERENCE_TARGET, method.getVariableRef());
			if(variableDescriptor == null)
				continue;

			PropertyAccessUtil.record(trace, variableDescriptor, mutableClassDescriptor, method);
		}
	}
}
