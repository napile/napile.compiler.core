package org.napile.compiler.lang.resolve.processors.checkers;

import java.util.Map;

import javax.inject.Inject;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileClassLike;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileNamedMethod;
import org.napile.compiler.lang.resolve.BindingContext;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BodiesResolveContext;
import org.napile.compiler.lang.resolve.PropertyAccessUtil;

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
