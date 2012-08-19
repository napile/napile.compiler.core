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

package org.napile.compiler.lang.psi;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.compiler.lang.resolve.ImportPath;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.napile.compiler.lang.types.lang.JetStandardClasses;
import org.napile.compiler.lexer.JetTokens;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.CheckUtil;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.psi.util.PsiTreeUtil;

/**
 * @author abreslav
 */
public class NapilePsiUtil
{

	public static final Name NO_NAME_PROVIDED = Name.special("<no name provided>");
	public static final Name ROOT_NAMESPACE_NAME = Name.special("<root namespace>");

	private NapilePsiUtil()
	{
	}

	@Nullable
	public static NapileExpression deparenthesize(@NotNull NapileExpression expression)
	{
		if(expression instanceof NapileBinaryExpressionWithTypeRHS)
		{
			NapileSimpleNameExpression operationSign = ((NapileBinaryExpressionWithTypeRHS) expression).getOperationSign();
			if(JetTokens.COLON.equals(operationSign.getReferencedNameElementType()))
			{
				expression = ((NapileBinaryExpressionWithTypeRHS) expression).getLeft();
			}
		}
		else if(expression instanceof NapilePrefixExpression)
		{
			if(JetTokens.LABELS.contains(((NapilePrefixExpression) expression).getOperationReference().getReferencedNameElementType()))
			{
				NapileExpression baseExpression = ((NapilePrefixExpression) expression).getBaseExpression();
				if(baseExpression != null)
				{
					expression = baseExpression;
				}
			}
		}
		if(expression instanceof NapileParenthesizedExpression)
		{
			NapileExpression innerExpression = ((NapileParenthesizedExpression) expression).getExpression();
			return innerExpression != null ? deparenthesize(innerExpression) : null;
		}
		return expression;
	}

	@NotNull
	public static Name safeName(@Nullable String name)
	{
		return name == null ? NO_NAME_PROVIDED : Name.identifier(name);
	}

	@NotNull
	public static Set<NapileElement> findRootExpressions(@NotNull Collection<NapileElement> unreachableElements)
	{
		Set<NapileElement> rootElements = new HashSet<NapileElement>();
		final Set<NapileElement> shadowedElements = new HashSet<NapileElement>();
		NapileVisitorVoid shadowAllChildren = new NapileVisitorVoid()
		{
			@Override
			public void visitJetElement(NapileElement element)
			{
				if(shadowedElements.add(element))
				{
					element.acceptChildren(this);
				}
			}
		};

		for(NapileElement element : unreachableElements)
		{
			if(shadowedElements.contains(element))
				continue;
			element.acceptChildren(shadowAllChildren);

			rootElements.removeAll(shadowedElements);
			rootElements.add(element);
		}
		return rootElements;
	}

	@Nullable
	public static NapileNamedFunction getSurroundingFunction(@Nullable PsiElement element)
	{
		while(element != null)
		{
			if(element instanceof NapileNamedFunction)
				return (NapileNamedFunction) element;
			if(element instanceof NapileClassOrObject || element instanceof NapileFile)
				return null;
			element = element.getParent();
		}
		return null;
	}

	@NotNull
	public static String unquoteIdentifier(@NotNull String quoted)
	{
		if(quoted.indexOf('`') < 0)
		{
			return quoted;
		}

		if(quoted.startsWith("`") && quoted.endsWith("`") && quoted.length() >= 2)
		{
			return quoted.substring(1, quoted.length() - 1);
		}
		else
		{
			return quoted;
		}
	}

	@NotNull
	public static String unquoteIdentifierOrFieldReference(@NotNull String quoted)
	{
		if(quoted.indexOf('`') < 0)
		{
			return quoted;
		}

		if(quoted.startsWith("$"))
		{
			return "$" + unquoteIdentifier(quoted.substring(1));
		}
		else
		{
			return unquoteIdentifier(quoted);
		}
	}

	private static FqName getFQName(NapileNamespaceHeader header)
	{
		StringBuilder builder = new StringBuilder();
		for(NapileSimpleNameExpression nameExpression : header.getParentNamespaceNames())
		{
			builder.append(nameExpression.getReferencedName());
			builder.append(".");
		}
		builder.append(header.getName());
		return new FqName(builder.toString());
	}

	public static FqName getFQName(NapileFile file)
	{
		return getFQName(file.getNamespaceHeader());
	}

	@Nullable
	public static FqName getFQName(NapileNamedDeclaration namedDeclaration)
	{
		if(namedDeclaration instanceof NapileObjectDeclarationName)
		{
			NapileObjectDeclaration objectDeclaration = PsiTreeUtil.getParentOfType(namedDeclaration, NapileObjectDeclaration.class);
			if(objectDeclaration == null)
			{
				return null;
			}

			return getFQName(objectDeclaration);
		}

		Name name = namedDeclaration.getNameAsName();
		if(name == null)
		{
			return null;
		}

		PsiElement parent = namedDeclaration.getParent();
		if(parent instanceof NapileClassBody)
		{
			// One nesting to NapileClassBody doesn't affect to qualified name
			parent = parent.getParent();
		}

		FqName firstPart = null;
		if(parent instanceof NapileFile)
		{
			firstPart = getFQName((NapileFile) parent);
		}
		else if(parent instanceof NapileNamedFunction || parent instanceof NapileClass || parent instanceof NapileObjectDeclaration)
		{
			firstPart = getFQName((NapileNamedDeclaration) parent);
		}

		if(firstPart == null)
		{
			return null;
		}

		return firstPart.child(name);
	}

	@Nullable
	@IfNotParsed
	public static ImportPath getImportPath(NapileImportDirective importDirective)
	{
		final NapileExpression importedReference = importDirective.getImportedReference();
		if(importedReference == null)
		{
			return null;
		}

		final String text = importedReference.getText();
		return new ImportPath(text.replaceAll(" ", "") + (importDirective.isAllUnder() ? ".*" : ""));
	}

	@NotNull
	private static FqName makeFQName(@NotNull FqName prefix, @NotNull NapileClassOrObject jetClass)
	{
		return prefix.child(Name.identifier(jetClass.getName()));
	}

	public static boolean isIrrefutable(NapileWhenEntry entry)
	{
		if(entry.isElse())
			return true;
		for(NapileWhenCondition condition : entry.getConditions())
		{
			if(condition instanceof NapileWhenConditionIsPattern)
			{
				NapilePattern pattern = ((NapileWhenConditionIsPattern) condition).getPattern();
				if(pattern instanceof NapileWildcardPattern)
				{
					return true;
				}
				if(pattern instanceof NapileBindingPattern)
				{
					NapileBindingPattern bindingPattern = (NapileBindingPattern) pattern;
					if(bindingPattern.getVariableDeclaration().getPropertyTypeRef() == null && bindingPattern.getCondition() == null)
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	@Nullable
	public static <T extends PsiElement> T getDirectParentOfTypeForBlock(@NotNull NapileBlockExpression block, @NotNull Class<T> aClass)
	{
		T parent = PsiTreeUtil.getParentOfType(block, aClass);
		if(parent instanceof NapileIfExpression)
		{
			NapileIfExpression ifExpression = (NapileIfExpression) parent;
			if(ifExpression.getElse() == block || ifExpression.getThen() == block)
			{
				return parent;
			}
		}
		if(parent instanceof NapileWhenExpression)
		{
			NapileWhenExpression whenExpression = (NapileWhenExpression) parent;
			for(NapileWhenEntry whenEntry : whenExpression.getEntries())
			{
				if(whenEntry.getExpression() == block)
				{
					return parent;
				}
			}
		}
		if(parent instanceof NapileFunctionLiteral)
		{
			NapileFunctionLiteral functionLiteral = (NapileFunctionLiteral) parent;
			if(functionLiteral.getBodyExpression() == block)
			{
				return parent;
			}
		}
		if(parent instanceof NapileTryExpression)
		{
			NapileTryExpression tryExpression = (NapileTryExpression) parent;
			if(tryExpression.getTryBlock() == block)
			{
				return parent;
			}
			for(NapileCatchClause clause : tryExpression.getCatchClauses())
			{
				if(clause.getCatchBody() == block)
				{
					return parent;
				}
			}
		}
		return null;
	}

	public static boolean isImplicitlyUsed(@NotNull NapileElement element)
	{
		PsiElement parent = element.getParent();
		if(!(parent instanceof NapileBlockExpression))
			return true;
		NapileBlockExpression block = (NapileBlockExpression) parent;
		List<NapileElement> statements = block.getStatements();
		if(statements.get(statements.size() - 1) == element)
		{
			NapileExpression expression = getDirectParentOfTypeForBlock(block, NapileIfExpression.class);
			if(expression == null)
			{
				expression = getDirectParentOfTypeForBlock(block, NapileWhenExpression.class);
			}
			if(expression == null)
			{
				expression = getDirectParentOfTypeForBlock(block, NapileFunctionLiteral.class);
			}
			if(expression == null)
			{
				expression = getDirectParentOfTypeForBlock(block, NapileTryExpression.class);
			}
			if(expression != null)
			{
				return isImplicitlyUsed(expression);
			}
		}
		return false;
	}

	public static void deleteClass(@NotNull NapileClassOrObject clazz)
	{
		CheckUtil.checkWritable(clazz);
		NapileFile file = (NapileFile) clazz.getContainingFile();
		List<NapileClass> declarations = file.getDeclarations();
		if(declarations.size() == 1)
		{
			file.delete();
		}
		else
		{
			PsiElement parent = clazz.getParent();
			CodeEditUtil.removeChild(parent.getNode(), clazz.getNode());
		}
	}

	@Nullable
	public static Name getAliasName(@NotNull NapileImportDirective importDirective)
	{
		String aliasName = importDirective.getAliasName();
		NapileExpression importedReference = importDirective.getImportedReference();
		if(importedReference == null)
		{
			return null;
		}
		NapileSimpleNameExpression referenceExpression = getLastReference(importedReference);
		if(aliasName == null)
		{
			aliasName = referenceExpression != null ? referenceExpression.getReferencedName() : null;
		}

		//noinspection ConstantConditions
		return StringUtil.isNotEmpty(aliasName) ? Name.identifier(aliasName) : null;
	}

	@Nullable
	public static NapileSimpleNameExpression getLastReference(@NotNull NapileExpression importedReference)
	{
		if(importedReference instanceof NapileDotQualifiedExpression)
		{
			NapileExpression selectorExpression = ((NapileDotQualifiedExpression) importedReference).getSelectorExpression();
			return (selectorExpression instanceof NapileSimpleNameExpression) ? (NapileSimpleNameExpression) selectorExpression : null;
		}
		if(importedReference instanceof NapileSimpleNameExpression)
		{
			return (NapileSimpleNameExpression) importedReference;
		}
		return null;
	}

	public static boolean isVoidType(@Nullable NapileTypeReference typeReference)
	{
		if(typeReference == null)
		{
			return false;
		}

		return JetStandardClasses.UNIT_ALIAS.getName().equals(typeReference.getText());
	}

	public static boolean isSafeCall(@NotNull Call call)
	{
		ASTNode callOperationNode = call.getCallOperationNode();
		return callOperationNode != null && callOperationNode.getElementType() == JetTokens.SAFE_ACCESS;
	}

	public static boolean isFunctionLiteralWithoutDeclaredParameterTypes(NapileExpression expression)
	{
		if(!(expression instanceof NapileFunctionLiteralExpression))
			return false;
		NapileFunctionLiteralExpression functionLiteral = (NapileFunctionLiteralExpression) expression;
		for(NapileParameter parameter : functionLiteral.getValueParameters())
		{
			if(parameter.getTypeReference() != null)
			{
				return false;
			}
		}
		return true;
	}
}
