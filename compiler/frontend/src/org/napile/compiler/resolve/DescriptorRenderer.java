/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.napile.compiler.resolve;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.descriptors.*;
import org.napile.compiler.lang.diagnostics.rendering.Renderer;
import org.napile.compiler.lang.resolve.DescriptorUtils;
import org.napile.compiler.lang.resolve.name.FqName;
import org.napile.compiler.lang.resolve.name.FqNameUnsafe;
import org.napile.compiler.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.napile.compiler.lang.types.ErrorUtils;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.lang.JetStandardClasses;
import org.napile.compiler.lexer.JetTokens;
import org.napile.compiler.lexer.NapileKeywordToken;

/**
 * @author abreslav
 */
public class DescriptorRenderer implements Renderer<DeclarationDescriptor>
{

	public static final DescriptorRenderer COMPACT_WITH_MODIFIERS = new DescriptorRenderer()
	{
		@Override
		protected boolean shouldRenderDefinedIn()
		{
			return false;
		}
	};

	public static final DescriptorRenderer COMPACT = new DescriptorRenderer()
	{
		@Override
		protected boolean shouldRenderDefinedIn()
		{
			return false;
		}

		@Override
		protected boolean shouldRenderModifiers()
		{
			return false;
		}
	};

	public static final DescriptorRenderer TEXT = new DescriptorRenderer();

	public static final DescriptorRenderer DEBUG_TEXT = new DescriptorRenderer()
	{
		@Override
		protected boolean hasDefaultValue(ParameterDescriptor descriptor)
		{
			// hasDefaultValue() has effects
			return descriptor.declaresDefaultValue();
		}
	};

	public static final DescriptorRenderer HTML = new HtmlDescriptorRenderer();

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final RenderDeclarationDescriptorVisitor rootVisitor = new RenderDeclarationDescriptorVisitor();

	protected final DeclarationDescriptorVisitor<Void, StringBuilder> subVisitor = new RenderDeclarationDescriptorVisitor()
	{
		@Override
		public Void visitTypeParameterDescriptor(TypeParameterDescriptor descriptor, StringBuilder builder)
		{
			renderTypeParameter(descriptor, builder, false);
			return null;
		}

		@Override
		public Void visitPropertyParameterDescriptor(ParameterDescriptor descriptor, StringBuilder builder)
		{
			super.visitVariableDescriptor(descriptor, builder, true);
			if(hasDefaultValue(descriptor))
			{
				builder.append(" = ...");
			}
			return null;
		}
	};

	protected boolean hasDefaultValue(ParameterDescriptor descriptor)
	{
		return descriptor.hasDefaultValue();
	}

	protected String renderKeyword(NapileKeywordToken keyword)
	{
		return keyword.getValue();
	}

	public String renderType(JetType type)
	{
		return renderType(type, false);
	}

	public String renderTypeWithShortNames(JetType type)
	{
		return renderType(type, true);
	}

	private String renderType(JetType type, boolean shortNamesOnly)
	{
		if(type == null)
		{
			return escape("[NULL]");
		}
		else if(ErrorUtils.isErrorType(type))
		{
			return escape(type.toString());
		}
		else if(JetStandardClasses.isUnit(type))
		{
			return escape(JetStandardClasses.UNIT_ALIAS + (type.isNullable() ? "?" : ""));
		}
		else if(JetStandardClasses.isFunctionType(type))
		{
			return escape(renderFunctionType(type, shortNamesOnly));
		}
		else
		{
			return escape(renderDefaultType(type, shortNamesOnly));
		}
	}

	private static List<String> getOuterClassesNames(ClassDescriptor cd)
	{
		ArrayList<String> result = new ArrayList<String>();
		while(cd.getContainingDeclaration() instanceof ClassifierDescriptor)
		{
			result.add(cd.getName().getName());
			cd = (ClassDescriptor) cd.getContainingDeclaration();
		}
		return result;
	}

	private String renderDefaultType(JetType type, boolean shortNamesOnly)
	{
		StringBuilder sb = new StringBuilder();
		ClassifierDescriptor cd = type.getConstructor().getDeclarationDescriptor();

		Object typeNameObject;

		if(cd == null || cd instanceof TypeParameterDescriptor)
		{
			typeNameObject = type.getConstructor();
		}
		else
		{
			if(shortNamesOnly)
			{
				// for nested classes qualified name should be used
				typeNameObject = cd.getName();
				DeclarationDescriptor parent = cd.getContainingDeclaration();
				while(parent instanceof ClassDescriptor)
				{
					typeNameObject = parent.getName() + "." + typeNameObject;
					parent = parent.getContainingDeclaration();
				}
			}
			else
			{
				typeNameObject = DescriptorUtils.getFQName(cd);
			}
		}

		sb.append(typeNameObject);
		if(!type.getArguments().isEmpty())
		{
			sb.append("<");
			appendTypes(sb, type.getArguments(), shortNamesOnly);
			sb.append(">");
		}
		if(type.isNullable())
		{
			sb.append("?");
		}
		return sb.toString();
	}

	private void appendTypes(StringBuilder result, List<JetType> types, boolean shortNamesOnly)
	{
		for(Iterator<JetType> iterator = types.iterator(); iterator.hasNext(); )
		{
			result.append(renderType(iterator.next(), shortNamesOnly));
			if(iterator.hasNext())
			{
				result.append(", ");
			}
		}
	}

	private String renderFunctionType(JetType type, boolean shortNamesOnly)
	{
		StringBuilder sb = new StringBuilder();

		JetType receiverType = JetStandardClasses.getReceiverType(type);
		if(receiverType != null)
		{
			sb.append(renderType(receiverType, shortNamesOnly));
			sb.append(".");
		}

		sb.append("(");
		appendTypes(sb, JetStandardClasses.getParameterTypeProjectionsFromFunctionType(type), shortNamesOnly);
		sb.append(") -> ");
		sb.append(renderType(JetStandardClasses.getReturnTypeFromFunctionType(type), shortNamesOnly));

		if(type.isNullable())
		{
			return "(" + sb + ")?";
		}
		return sb.toString();
	}

	protected String escape(String s)
	{
		return s;
	}

	private String lt()
	{
		return escape("<");
	}

	@NotNull
	@Override
	public String render(@NotNull DeclarationDescriptor declarationDescriptor)
	{
		StringBuilder stringBuilder = new StringBuilder();
		declarationDescriptor.accept(rootVisitor, stringBuilder);

		if(shouldRenderDefinedIn())
		{
			appendDefinedIn(declarationDescriptor, stringBuilder);
		}
		return stringBuilder.toString();
	}

	public String renderFunctionParameters(@NotNull MethodDescriptor methodDescriptor)
	{
		StringBuilder stringBuilder = new StringBuilder();
		renderValueParameters(methodDescriptor, stringBuilder);
		return stringBuilder.toString();
	}

	protected boolean shouldRenderDefinedIn()
	{
		return true;
	}

	protected boolean shouldRenderModifiers()
	{
		return true;
	}

	private void appendDefinedIn(DeclarationDescriptor declarationDescriptor, StringBuilder stringBuilder)
	{
		if(declarationDescriptor instanceof ModuleDescriptor)
		{
			stringBuilder.append(" is a module");
			return;
		}
		stringBuilder.append(" ").append(renderMessage("defined in")).append(" ");

		final DeclarationDescriptor containingDeclaration = declarationDescriptor.getContainingDeclaration();
		if(containingDeclaration != null)
		{
			FqNameUnsafe fqName = DescriptorUtils.getFQName(containingDeclaration);
			stringBuilder.append(FqName.ROOT.equalsTo(fqName) ? "root package" : escape(fqName.getFqName()));
		}
	}

	public String renderMessage(String s)
	{
		return s;
	}

	protected void renderValueParameters(MethodDescriptor descriptor, StringBuilder builder)
	{
		if(descriptor.getValueParameters().isEmpty())
		{
			renderEmptyValueParameters(builder);
		}
		for(Iterator<ParameterDescriptor> iterator = descriptor.getValueParameters().iterator(); iterator.hasNext(); )
		{
			renderValueParameter(iterator.next(), !iterator.hasNext(), builder);
		}
	}

	protected void renderEmptyValueParameters(StringBuilder builder)
	{
		builder.append("()");
	}

	protected void renderValueParameter(ParameterDescriptor parameterDescriptor, boolean isLast, StringBuilder builder)
	{
		if(parameterDescriptor.getIndex() == 0)
		{
			builder.append("(");
		}
		parameterDescriptor.accept(subVisitor, builder);
		if(!isLast)
		{
			builder.append(", ");
		}
		else
		{
			builder.append(")");
		}
	}

	private class RenderDeclarationDescriptorVisitor implements DeclarationDescriptorVisitor<Void, StringBuilder>
	{
		@Override
		public Void visitPropertyParameterDescriptor(ParameterDescriptor descriptor, StringBuilder builder)
		{
			return visitVariableDescriptor(descriptor, builder);
		}

		@Override
		public Void visitReferenceParameterDescriptor(ReferenceParameterDescriptor descriptor, StringBuilder builder)
		{
			renderName(descriptor, builder);
			builder.append(" : ");
			builder.append(escape(renderType(descriptor.getType())));
			return null;
		}

		@Override
		public Void visitVariableDescriptor(VariableDescriptor descriptor, StringBuilder builder)
		{
			return visitVariableDescriptor(descriptor, builder, false);
		}

		@Override
		public Void visitLocalVariableDescriptor(LocalVariableDescriptor descriptor, StringBuilder builder)
		{
			return visitVariableDescriptor(descriptor, builder);
		}

		protected Void visitVariableDescriptor(VariableDescriptor descriptor, StringBuilder builder, boolean skipValVar)
		{
			JetType type = descriptor.getType();
			if(descriptor instanceof ParameterDescriptor)
			{
				JetType varargElementType = ((ParameterDescriptor) descriptor).getVarargElementType();
				if(varargElementType != null)
				{
					builder.append(renderKeyword(JetTokens.VARARG_KEYWORD)).append(" ");
					type = varargElementType;
				}
			}
			String typeString = renderPropertyPrefixAndComputeTypeString(builder, skipValVar ? null : descriptor.getPropertyKind(), Collections.<TypeParameterDescriptor>emptyList(), ReceiverDescriptor.NO_RECEIVER, type);
			renderName(descriptor, builder);
			builder.append(" : ").append(escape(typeString));
			return null;
		}

		private String renderPropertyPrefixAndComputeTypeString(@NotNull StringBuilder builder, @Nullable PropertyKind propertyKind, @NotNull List<TypeParameterDescriptor> typeParameters, @NotNull ReceiverDescriptor receiver, @Nullable JetType outType)
		{
			String typeString = lt() + "no type>";
			if(outType != null)
			{
				if(propertyKind != null)
				{
					switch(propertyKind)
					{
						case VAR:
							builder.append(renderKeyword(JetTokens.VAR_KEYWORD));
							break;
						default:
							break;
					}
					builder.append(" ");
				}
				typeString = renderType(outType);
			}

			renderTypeParameters(typeParameters, builder);

			if(receiver.exists())
			{
				builder.append(escape(renderType(receiver.getType()))).append(".");
			}

			return typeString;
		}

		@Override
		public Void visitPropertyDescriptor(PropertyDescriptor descriptor, StringBuilder builder)
		{
			renderVisibility(descriptor, builder);
			renderModality(descriptor.getModality(), builder);
			String typeString = renderPropertyPrefixAndComputeTypeString(builder, descriptor.getPropertyKind(), descriptor.getTypeParameters(), descriptor.getReceiverParameter(), descriptor.getType());
			renderName(descriptor, builder);
			builder.append(" : ").append(escape(typeString));
			return null;
		}

		private void renderVisibility(DeclarationDescriptorWithVisibility visibility, StringBuilder builder)
		{
			if(!shouldRenderModifiers())
				return;

			if(visibility.getVisibility() != Visibility.PUBLIC)
				builder.append(renderKeyword(visibility.getVisibility().getKeyword())).append(" ");
			if(visibility.isStatic())
				builder.append(renderKeyword(JetTokens.STATIC_KEYWORD)).append(" ");
		}

		private void renderModality(Modality modality, StringBuilder builder)
		{
			if(!shouldRenderModifiers())
				return;
			NapileKeywordToken keyword = null;
			switch(modality)
			{
				case FINAL:
					keyword = JetTokens.FINAL_KEYWORD;
					break;
				case ABSTRACT:
					keyword = JetTokens.ABSTRACT_KEYWORD;
					break;
				default:
					break;
			}
			if(keyword != null)
				builder.append(renderKeyword(keyword)).append(" ");
		}

		@Override
		public Void visitFunctionDescriptor(MethodDescriptor descriptor, StringBuilder builder)
		{
			renderVisibility(descriptor, builder);
			renderModality(descriptor.getModality(), builder);
			builder.append(renderKeyword(JetTokens.METH_KEYWORD)).append(" ");
			if(renderTypeParameters(descriptor.getTypeParameters(), builder))
			{
				builder.append(" ");
			}

			ReceiverDescriptor receiver = descriptor.getReceiverParameter();
			if(receiver.exists())
			{
				builder.append(escape(renderType(receiver.getType()))).append(".");
			}

			renderName(descriptor, builder);
			renderValueParameters(descriptor, builder);
			builder.append(" : ").append(escape(renderType(descriptor.getReturnType())));
			renderWhereSuffix(descriptor, builder);
			return null;
		}

		@Override
		public Void visitPropertyGetterDescriptor(PropertyGetterDescriptor descriptor, StringBuilder data)
		{
			return visitFunctionDescriptor(descriptor, data);
		}

		@Override
		public Void visitPropertySetterDescriptor(PropertySetterDescriptor descriptor, StringBuilder data)
		{
			return visitFunctionDescriptor(descriptor, data);
		}

		private void renderWhereSuffix(@NotNull CallableMemberDescriptor callable, @NotNull StringBuilder builder)
		{
			boolean first = true;
			for(TypeParameterDescriptor typeParameter : callable.getTypeParameters())
			{
				if(typeParameter.getUpperBounds().size() > 1)
				{
					for(JetType upperBound : typeParameter.getUpperBounds())
					{
						if(first)
						{
							builder.append(" ");
							builder.append(renderKeyword(JetTokens.WHERE_KEYWORD));
							builder.append(" ");
						}
						else
						{
							builder.append(", ");
						}
						builder.append(typeParameter.getName());
						builder.append(" : ");
						builder.append(escape(renderType(upperBound)));
						first = false;
					}
				}
			}
		}

		@Override
		public Void visitConstructorDescriptor(ConstructorDescriptor constructorDescriptor, StringBuilder builder)
		{
			renderVisibility(constructorDescriptor, builder);

			builder.append(renderKeyword(JetTokens.THIS_KEYWORD));

			ClassDescriptor classDescriptor = constructorDescriptor.getContainingDeclaration();
			renderTypeParameters(classDescriptor.getTypeConstructor().getParameters(), builder);
			renderValueParameters(constructorDescriptor, builder);
			return null;
		}

		private boolean renderTypeParameters(List<TypeParameterDescriptor> typeParameters, StringBuilder builder)
		{
			if(!typeParameters.isEmpty())
			{
				builder.append(lt());
				for(Iterator<TypeParameterDescriptor> iterator = typeParameters.iterator(); iterator.hasNext(); )
				{
					TypeParameterDescriptor typeParameterDescriptor = iterator.next();
					typeParameterDescriptor.accept(subVisitor, builder);
					if(iterator.hasNext())
					{
						builder.append(", ");
					}
				}
				builder.append(">");
				return true;
			}
			return false;
		}

		@Override
		public Void visitTypeParameterDescriptor(TypeParameterDescriptor descriptor, StringBuilder builder)
		{
			builder.append(lt());
			renderTypeParameter(descriptor, builder, true);
			builder.append(">");
			return null;
		}

		@Override
		public Void visitNamespaceDescriptor(NamespaceDescriptor namespaceDescriptor, StringBuilder builder)
		{
			builder.append(renderKeyword(JetTokens.PACKAGE_KEYWORD)).append(" ");
			renderName(namespaceDescriptor, builder);
			return null;
		}

		@Override
		public Void visitModuleDeclaration(ModuleDescriptor descriptor, StringBuilder builder)
		{
			renderName(descriptor, builder);
			return null;
		}


		@Override
		public Void visitClassDescriptor(ClassDescriptor descriptor, StringBuilder builder)
		{
			NapileKeywordToken keyword;
			switch(descriptor.getKind())
			{
				case ENUM_CLASS:
					keyword = JetTokens.ENUM_KEYWORD;
					break;
				case RETELL:
					keyword = JetTokens.RETELL_KEYWORD;
					break;
				default:
					keyword = JetTokens.CLASS_KEYWORD;
			}
			renderClassDescriptor(descriptor, builder, keyword);
			return null;
		}


		public void renderClassDescriptor(ClassDescriptor descriptor, StringBuilder builder, NapileKeywordToken keyword)
		{
			renderVisibility(descriptor, builder);

			if(descriptor.getKind() != ClassKind.ANONYM_CLASS)
			{
				renderModality(descriptor.getModality(), builder);
			}
			builder.append(renderKeyword(keyword));
			if(descriptor.getKind() != ClassKind.ANONYM_CLASS)
			{
				builder.append(" ");
				renderName(descriptor, builder);
				renderTypeParameters(descriptor.getTypeConstructor().getParameters(), builder);
			}
			if(!descriptor.equals(JetStandardClasses.getNothing()))
			{
				Collection<? extends JetType> supertypes = descriptor.getTypeConstructor().getSupertypes();
				if(supertypes.isEmpty() || supertypes.size() == 1 && JetStandardClasses.isAny(supertypes.iterator().next()))
				{
				}
				else
				{
					builder.append(" : ");
					for(Iterator<? extends JetType> iterator = supertypes.iterator(); iterator.hasNext(); )
					{
						JetType supertype = iterator.next();
						builder.append(renderType(supertype));
						if(iterator.hasNext())
						{
							builder.append(", ");
						}
					}
				}
			}
		}

		protected void renderName(DeclarationDescriptor descriptor, StringBuilder stringBuilder)
		{
			stringBuilder.append(escape(descriptor.getName().getName()));
		}

		protected void renderTypeParameter(TypeParameterDescriptor descriptor, StringBuilder builder, boolean topLevel)
		{
			if(descriptor.isReified())
			{
				builder.append(renderKeyword(JetTokens.REIFIED_KEYWORD)).append(" ");
			}
			renderName(descriptor, builder);
			if(descriptor.getUpperBounds().size() == 1)
			{
				JetType upperBound = descriptor.getUpperBounds().iterator().next();
				if(upperBound != JetStandardClasses.getDefaultBound())
				{
					builder.append(" : ").append(renderType(upperBound));
				}
			}
			else if(topLevel)
			{
				boolean first = true;
				for(JetType upperBound : descriptor.getUpperBounds())
				{
					if(upperBound.equals(JetStandardClasses.getDefaultBound()))
					{
						continue;
					}
					if(first)
					{
						builder.append(" : ");
					}
					else
					{
						builder.append(" & ");
					}
					builder.append(renderType(upperBound));
					first = false;
				}
			}
			else
			{
				// rendered with "where"
			}
		}
	}

	public static class HtmlDescriptorRenderer extends DescriptorRenderer
	{

		@Override
		protected String escape(String s)
		{
			return s.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
		}

		@Override
		public String renderKeyword(NapileKeywordToken keyword)
		{
			return "<b>" + keyword.getValue() + "</b>";
		}

		@Override
		public String renderMessage(String s)
		{
			return "<i>" + s + "</i>";
		}
	}
}
