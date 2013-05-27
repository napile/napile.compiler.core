package org.napile.compiler.plugin.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.napile.asm.AsmConstants;
import org.napile.asm.resolve.name.FqName;
import org.napile.asm.resolve.name.Name;
import org.napile.asm.tree.members.types.TypeNode;
import org.napile.asm.tree.members.types.constructors.ClassTypeNode;
import org.napile.compiler.lang.descriptors.CallableMemberDescriptor;
import org.napile.compiler.lang.descriptors.Modality;
import org.napile.compiler.lang.descriptors.MutableClassDescriptor;
import org.napile.compiler.lang.descriptors.TypeParameterDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptor;
import org.napile.compiler.lang.descriptors.VariableDescriptorImpl;
import org.napile.compiler.lang.descriptors.Visibility;
import org.napile.compiler.lang.descriptors.annotations.AnnotationDescriptor;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.psi.NapileVisitorVoid;
import org.napile.compiler.lang.resolve.AnnotationUtils;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.BindingTraceKeys;
import org.napile.compiler.lang.resolve.TopDownAnalysisContext;
import org.napile.compiler.lang.resolve.scopes.NapileScope;
import org.napile.compiler.lang.types.NapileType;
import org.napile.compiler.lang.types.TypeUtils;
import org.napile.compiler.plugin.CompilerPlugin;

/**
 * @author VISTALL
 * @since 22:01/15.05.13
 */
public class VariableChangeListenerCompilerPlugin implements CompilerPlugin
{
	public static final Name VARIABLE_NAME = Name.identifier("variableChangeListener");
	public static final FqName ANNOTATION_FQ_NAME = new FqName("napile.annotation.VariableChangeListener");

	public static final TypeNode TYPE = new TypeNode(false, new ClassTypeNode(new FqName("napile.util.listeners.ListenerHolder"))).visitArgument(new TypeNode(false, new ClassTypeNode(new FqName("napile.util.listeners.VariableChangeEvent"))).visitArgument(AsmConstants.ANY_TYPE));

	@Override
	public void process(final NapileScope scope, final BindingTrace trace, final TopDownAnalysisContext context, final Collection<? extends NapileFile> declarations)
	{
		for(Map.Entry<NapileClass, MutableClassDescriptor> entry : context.getClasses().entrySet())
		{
			final NapileClass key = entry.getKey();
			final MutableClassDescriptor value = entry.getValue();

			final Map<NapileVariable, VariableDescriptor> variablesWithAnnotations = new LinkedHashMap<NapileVariable, VariableDescriptor>();

			for(NapileDeclaration declaration : key.getDeclarations())
			{
				declaration.accept(new NapileVisitorVoid()
				{
					@Override
					public void visitVariable(NapileVariable property)
					{
						final VariableDescriptor variableDescriptor = context.getVariables().get(property);
						if(variableDescriptor != null && AnnotationUtils.hasAnnotation(variableDescriptor, ANNOTATION_FQ_NAME))
						{
							variablesWithAnnotations.put(property, variableDescriptor);
						}
					}
				});
			}

			if(!variablesWithAnnotations.isEmpty())
			{
				NapileType napileType = TypeUtils.toCompilerType(scope, TYPE);
				System.out.println(napileType);
				VariableDescriptorImpl newVariable = new VariableDescriptorImpl(value, Collections.<AnnotationDescriptor>emptyList(), Modality.FINAL, Visibility.PUBLIC, VARIABLE_NAME, CallableMemberDescriptor.Kind.CREATED_BY_PLUGIN, false, false, false);
				newVariable.setType(napileType, Collections.<TypeParameterDescriptor>emptyList(), value.getImplicitReceiver());


				trace.record(BindingTraceKeys.CREATED_BY_PLUGIN, newVariable, key);
				value.getBuilder().addVariableDescriptor(newVariable);
			}
		}
	}
}
