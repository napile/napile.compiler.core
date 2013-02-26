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

package org.napile.compiler.lang.diagnostics;


import java.util.Collections;
import java.util.List;

import org.jetbrains.annotations.NotNull;
import org.napile.compiler.lang.psi.*;
import org.napile.compiler.lang.lexer.NapileKeywordToken;
import org.napile.compiler.lang.lexer.NapileTokens;
import org.napile.compiler.lang.psi.NapileClass;
import org.napile.compiler.lang.psi.NapileDeclaration;
import org.napile.compiler.lang.psi.NapileElement;
import org.napile.compiler.lang.psi.NapileExpression;
import org.napile.compiler.lang.psi.NapileMethod;
import org.napile.compiler.lang.psi.NapileModifierList;
import org.napile.compiler.lang.psi.NapileModifierListOwner;
import org.napile.compiler.lang.psi.NapileNamedMethodOrMacro;
import org.napile.compiler.lang.psi.NapileVariable;
import org.napile.compiler.lang.psi.NapileTypeReference;
import com.google.common.collect.Lists;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiNameIdentifierOwner;

/**
 * @author svtk
 */
public class PositioningStrategies
{

	public static final PositioningStrategy<PsiElement> DEFAULT = new PositioningStrategy<PsiElement>();

	public static final PositioningStrategy<PsiElement> POSITION_REDECLARATION = new PositioningStrategy<PsiElement>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull PsiElement element)
		{
			if(element instanceof NapileNamedDeclaration)
			{
				PsiElement nameIdentifier = ((NapileNamedDeclaration) element).getNameIdentifier();
				if(nameIdentifier != null)
				{
					return markElement(nameIdentifier);
				}
			}
			else if(element instanceof NapileFile)
			{
				NapileFile file = (NapileFile) element;
				PsiElement nameIdentifier = file.getPackage().getLastPartExpression();
				if(nameIdentifier != null)
				{
					return markElement(nameIdentifier);
				}
			}
			return markElement(element);
		}
	};

	public static final PositioningStrategy<NapileReferenceExpression> ELEMENT_RANGE_OR_BRACKETS = new PositioningStrategy<NapileReferenceExpression>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull NapileReferenceExpression element)
		{
			if(element instanceof NapileArrayAccessExpressionImpl)
			{
				List<TextRange> ranges = ((NapileArrayAccessExpressionImpl) element).getBracketRanges();
				if(!ranges.isEmpty())
				{
					return ranges;
				}
			}
			return Collections.singletonList(element.getTextRange());
		}
	};

	public static final PositioningStrategy<NapileInjectionExpression> INJECTION_NAME = new PositioningStrategy<NapileInjectionExpression>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull NapileInjectionExpression declaration)
		{
			PsiElement first = declaration.getFirstChild();
			String text = first.getText();

			return Collections.singletonList(new TextRange(first.getTextOffset() + 1, first.getTextOffset() + text.lastIndexOf("/")));
		}
	};

	public static final PositioningStrategy<NapileDeclaration> DECLARATION_RETURN_TYPE = new PositioningStrategy<NapileDeclaration>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull NapileDeclaration declaration)
		{
			NapileTypeReference returnTypeRef = null;
			ASTNode nameNode = null;
			if(declaration instanceof NapileNamedMethodOrMacro)
			{
				NapileMethod function = (NapileNamedMethodOrMacro) declaration;
				returnTypeRef = function.getReturnTypeRef();
				nameNode = getNameNode(function);
			}
			else if(declaration instanceof NapileVariable)
			{
				NapileVariable property = (NapileVariable) declaration;
				returnTypeRef = property.getType();
				nameNode = getNameNode(property);
			}

			if(returnTypeRef != null)
				return markElement(returnTypeRef);
			if(nameNode != null)
				return markNode(nameNode);
			return markElement(declaration);
		}

		private ASTNode getNameNode(NapileNamedDeclaration function)
		{
			PsiElement nameIdentifier = function.getNameIdentifier();
			return nameIdentifier == null ? null : nameIdentifier.getNode();
		}
	};

	public static final PositioningStrategy<PsiNameIdentifierOwner> NAME_IDENTIFIER = new PositioningStrategy<PsiNameIdentifierOwner>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull PsiNameIdentifierOwner element)
		{
			PsiElement nameIdentifier = element.getNameIdentifier();
			if(nameIdentifier != null)
			{
				return markElement(nameIdentifier);
			}
			return markElement(element);
		}
	};

	public static final PositioningStrategy<PsiNameIdentifierOwner> NAMED_ELEMENT = new PositioningStrategy<PsiNameIdentifierOwner>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull PsiNameIdentifierOwner element)
		{
			if(element instanceof NapileNamedMacro)
			{
				NapileNamedMacro function = (NapileNamedMacro) element;
				PsiElement endOfSignatureElement;
				NapileCallParameterList valueParameterList = function.getCallParameterList();
				NapileElement returnTypeRef = function.getReturnTypeRef();
				PsiElement nameIdentifier = function.getNameIdentifier();
				if(returnTypeRef != null)
				{
					endOfSignatureElement = returnTypeRef;
				}
				else if(valueParameterList != null)
				{
					endOfSignatureElement = valueParameterList;
				}
				else if(nameIdentifier != null)
				{
					endOfSignatureElement = nameIdentifier;
				}
				else
				{
					endOfSignatureElement = function;
				}
				return markRange(new TextRange(function.getTextRange().getStartOffset(), endOfSignatureElement.getTextRange().getEndOffset()));
			}
			else if(element instanceof NapileNamedMethod)
			{
				NapileNamedMethod function = (NapileNamedMethod) element;
				PsiElement endOfSignatureElement;
				NapileCallParameterList valueParameterList = function.getCallParameterList();
				NapileElement returnTypeRef = function.getReturnTypeRef();
				PsiElement nameIdentifier = function.getNameIdentifier();

				if(returnTypeRef != null)
					endOfSignatureElement = returnTypeRef;
				else if(valueParameterList != null)
					endOfSignatureElement = valueParameterList;
				else if(nameIdentifier != null)
					endOfSignatureElement = nameIdentifier;
				else
					endOfSignatureElement = function;
				return markRange(new TextRange(function.getTextRange().getStartOffset(), endOfSignatureElement.getTextRange().getEndOffset()));
			}
			else if(element instanceof NapileVariable)
			{
				NapileVariable property = (NapileVariable) element;
				PsiElement endOfSignatureElement;
				NapileTypeReference propertyTypeRef = property.getType();
				PsiElement nameIdentifier = property.getNameIdentifier();
				if(propertyTypeRef != null)
				{
					endOfSignatureElement = propertyTypeRef;
				}
				else if(nameIdentifier != null)
				{
					endOfSignatureElement = nameIdentifier;
				}
				else
				{
					endOfSignatureElement = property;
				}
				return markRange(new TextRange(property.getTextRange().getStartOffset(), endOfSignatureElement.getTextRange().getEndOffset()));
			}
			else if(element instanceof NapileClass)
			{
				// primary constructor
				NapileClass klass = (NapileClass) element;
				PsiElement nameAsDeclaration = klass.getNameIdentifier();
				if(nameAsDeclaration == null)
				{
					return markElement(klass);
				}
				return markRange(nameAsDeclaration.getTextRange());
			}
			return super.mark(element);
		}

		@Override
		public boolean isValid(@NotNull PsiNameIdentifierOwner element)
		{
			return true;
		}
	};

	public static final PositioningStrategy<NapileDeclaration> DECLARATION = new PositioningStrategy<NapileDeclaration>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull NapileDeclaration element)
		{
			if(element instanceof PsiNameIdentifierOwner)
			{
				return NAMED_ELEMENT.mark((PsiNameIdentifierOwner) element);
			}
			return super.mark(element);
		}

		@Override
		public boolean isValid(@NotNull NapileDeclaration element)
		{
			if(element instanceof PsiNameIdentifierOwner)
			{
				return NAMED_ELEMENT.isValid((PsiNameIdentifierOwner) element);
			}
			return super.isValid(element);
		}
	};

	public static final PositioningStrategy<NapileModifierListOwner> ABSTRACT_MODIFIER = positionModifier(NapileTokens.ABSTRACT_KEYWORD);

	public static final PositioningStrategy<NapileModifierListOwner> OVERRIDE_MODIFIER = positionModifier(NapileTokens.OVERRIDE_KEYWORD);

	public static PositioningStrategy<NapileModifierListOwner> positionModifier(final NapileKeywordToken token)
	{
		return new PositioningStrategy<NapileModifierListOwner>()
		{
			@NotNull
			@Override
			public List<TextRange> mark(@NotNull NapileModifierListOwner modifierListOwner)
			{
				assert modifierListOwner.hasModifier(token);
				NapileModifierList modifierList = modifierListOwner.getModifierList();
				assert modifierList != null;
				ASTNode node = modifierList.getModifierNode(token);
				assert node != null;
				return markNode(node);
			}
		};
	}

	public static PositioningStrategy<NapileArrayAccessExpressionImpl> ARRAY_ACCESS = new PositioningStrategy<NapileArrayAccessExpressionImpl>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull NapileArrayAccessExpressionImpl element)
		{
			return markElement(element.getIndicesNode());
		}
	};

	public static PositioningStrategy<NapileModifierListOwner> VISIBILITY_MODIFIER = new PositioningStrategy<NapileModifierListOwner>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull NapileModifierListOwner element)
		{
			List<NapileKeywordToken> visibilityTokens = Lists.newArrayList(NapileTokens.LOCAL_KEYWORD, NapileTokens.COVERED_KEYWORD, NapileTokens.HERITABLE_KEYWORD);
			List<TextRange> result = Lists.newArrayList();
			for(NapileKeywordToken token : visibilityTokens)
			{
				if(element.hasModifier(token))
				{
					result.add(element.getModifierList().getModifierNode(token).getTextRange());
				}
			}
			if(result.isEmpty())
			{
				if(element.hasModifier(NapileTokens.OVERRIDE_KEYWORD))
				{
					result.add(element.getModifierList().getModifierNode(NapileTokens.OVERRIDE_KEYWORD).getTextRange());
				}
			}
			return result;
		}
	};

	public static PositioningStrategy<NapileCallParameterAsVariable> PARAMETER_DEFAULT_VALUE = new PositioningStrategy<NapileCallParameterAsVariable>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull NapileCallParameterAsVariable element)
		{
			return markNode(element.getDefaultValue().getNode());
		}
	};

	public static PositioningStrategy<PsiElement> CALL_ELEMENT = new PositioningStrategy<PsiElement>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull PsiElement callElement)
		{
			if(callElement instanceof NapileCallElement)
			{
				NapileExpression calleeExpression = ((NapileCallElement) callElement).getCalleeExpression();
				if(calleeExpression != null)
				{
					return markElement(calleeExpression);
				}
			}
			return markElement(callElement);
		}
	};

	public static PositioningStrategy<NapileDeclarationWithBody> DECLARATION_WITH_BODY = new PositioningStrategy<NapileDeclarationWithBody>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull NapileDeclarationWithBody element)
		{
			NapileExpression bodyExpression = element.getBodyExpression();
			if((bodyExpression instanceof NapileBlockExpression))
			{
				TextRange lastBracketRange = ((NapileBlockExpression) bodyExpression).getLastBracketRange();
				if(lastBracketRange != null)
				{
					return markRange(lastBracketRange);
				}
			}
			return markElement(element);
		}

		@Override
		public boolean isValid(@NotNull NapileDeclarationWithBody element)
		{
			NapileExpression bodyExpression = element.getBodyExpression();
			if(!(bodyExpression instanceof NapileBlockExpression))
				return false;
			if(((NapileBlockExpression) bodyExpression).getLastBracketRange() == null)
				return false;
			return true;
		}
	};

	public static PositioningStrategy<NapileWhenEntry> ELSE_ENTRY = new PositioningStrategy<NapileWhenEntry>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull NapileWhenEntry entry)
		{
			PsiElement elseKeywordElement = entry.getElseKeywordElement();
			assert elseKeywordElement != null;
			return markElement(elseKeywordElement);
		}
	};

	public static PositioningStrategy<NapileWhenExpression> WHEN_EXPRESSION = new PositioningStrategy<NapileWhenExpression>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull NapileWhenExpression element)
		{
			return markElement(element.getWhenKeywordElement());
		}
	};

	public static PositioningStrategy<NapileWhenConditionInRange> WHEN_CONDITION_IN_RANGE = new PositioningStrategy<NapileWhenConditionInRange>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull NapileWhenConditionInRange condition)
		{
			return markElement(condition.getOperationReference());
		}
	};

	public static PositioningStrategy<NapileNullableType> NULLABLE_TYPE = new PositioningStrategy<NapileNullableType>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull NapileNullableType element)
		{
			return markNode(element.getQuestionMarkNode());
		}
	};

	public static PositioningStrategy<NapileExpression> CALL_EXPRESSION = new PositioningStrategy<NapileExpression>()
	{
		@NotNull
		@Override
		public List<TextRange> mark(@NotNull NapileExpression element)
		{
			if(element instanceof NapileCallExpression)
			{
				NapileCallExpression callExpression = (NapileCallExpression) element;
				PsiElement endElement;
				NapileTypeArgumentList typeArgumentList = callExpression.getTypeArgumentList();
				NapileExpression calleeExpression = callExpression.getCalleeExpression();
				if(typeArgumentList != null)
				{
					endElement = typeArgumentList;
				}
				else if(calleeExpression != null)
				{
					endElement = calleeExpression;
				}
				else
				{
					endElement = element;
				}
				return markRange(new TextRange(element.getTextRange().getStartOffset(), endElement.getTextRange().getEndOffset()));
			}
			return super.mark(element);
		}
	};
}