package org.napile.compiler.injection.regexp;

import java.util.EnumSet;

import org.intellij.lang.regexp.RegExpCapability;
import org.intellij.lang.regexp.RegExpElementTypes;
import org.intellij.lang.regexp.RegExpLanguage;
import org.intellij.lang.regexp.RegExpLexer;
import org.intellij.lang.regexp.RegExpParser;
import org.intellij.lang.regexp.RegExpTT;
import org.intellij.lang.regexp.psi.impl.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.napile.asm.lib.NapileLangPackage;
import org.napile.compiler.injection.CodeInjection;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.scopes.JetScope;
import org.napile.compiler.lang.types.JetType;
import org.napile.compiler.lang.types.TypeUtils;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.TokenType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;

/**
 * @author VISTALL
 * @date 7:20/08.11.12
 */
public class RegexpCodeInjection extends CodeInjection
{
	private static final TokenSet COMMENT_TOKENS = TokenSet.create(RegExpTT.COMMENT);

	@NotNull
	@Override
	public String getName()
	{
		return "regexp";
	}

	@NotNull
	@Override
	public Language getLanguage()
	{
		return RegExpLanguage.INSTANCE;
	}

	@NotNull
	@Override
	public JetType getReturnType(@Nullable JetType expectType, @NotNull BindingTrace bindingTrace, @NotNull JetScope jetScope)
	{
		return TypeUtils.getTypeOfClassOrErrorType(jetScope, NapileLangPackage.ANY);
	}

	@NotNull
	@Override
	public Lexer createLexer(Project project)
	{
		return new RegExpLexer(EnumSet.of(RegExpCapability.NESTED_CHARACTER_CLASSES));
	}

	@Override
	public PsiParser createParser(Project project)
	{
		return new RegExpParser();
	}

	@NotNull
	@Override
	public TokenSet getWhitespaceTokens()
	{
		// trick to hide quote tokens from parser... should actually go into the lexer
		return TokenSet.create(RegExpTT.QUOTE_BEGIN, RegExpTT.QUOTE_END, TokenType.WHITE_SPACE);
	}

	@Override
	@NotNull
	public TokenSet getStringLiteralElements()
	{
		return TokenSet.EMPTY;
	}

	@Override
	@NotNull
	public TokenSet getCommentTokens()
	{
		return COMMENT_TOKENS;
	}

	@Override
	@NotNull
	public PsiElement createElement(ASTNode node)
	{
		final IElementType type = node.getElementType();
		if(type == RegExpElementTypes.PATTERN)
		{
			return new RegExpPatternImpl(node);
		}
		else if(type == RegExpElementTypes.BRANCH)
		{
			return new RegExpBranchImpl(node);
		}
		else if(type == RegExpElementTypes.SIMPLE_CLASS)
		{
			return new RegExpSimpleClassImpl(node);
		}
		else if(type == RegExpElementTypes.CLASS)
		{
			return new RegExpClassImpl(node);
		}
		else if(type == RegExpElementTypes.CHAR_RANGE)
		{
			return new RegExpCharRangeImpl(node);
		}
		else if(type == RegExpElementTypes.CHAR)
		{
			return new RegExpCharImpl(node);
		}
		else if(type == RegExpElementTypes.GROUP)
		{
			return new RegExpGroupImpl(node);
		}
		else if(type == RegExpElementTypes.PROPERTY)
		{
			return new RegExpPropertyImpl(node);
		}
		else if(type == RegExpElementTypes.SET_OPTIONS)
		{
			return new RegExpSetOptionsImpl(node);
		}
		else if(type == RegExpElementTypes.OPTIONS)
		{
			return new RegExpOptionsImpl(node);
		}
		else if(type == RegExpElementTypes.BACKREF)
		{
			return new RegExpBackrefImpl(node);
		}
		else if(type == RegExpElementTypes.CLOSURE)
		{
			return new RegExpClosureImpl(node);
		}
		else if(type == RegExpElementTypes.QUANTIFIER)
		{
			return new RegExpQuantifierImpl(node);
		}
		else if(type == RegExpElementTypes.BOUNDARY)
		{
			return new RegExpBoundaryImpl(node);
		}
		else if(type == RegExpElementTypes.INTERSECTION)
		{
			return new RegExpIntersectionImpl(node);
		}
		else if(type == RegExpElementTypes.PY_NAMED_GROUP_REF)
		{
			return new RegExpPyNamedGroupRefImpl(node);
		}
		else if(type == RegExpElementTypes.PY_COND_REF)
		{
			return new RegExpPyCondRefImpl(node);
		}

		return new ASTWrapperPsiElement(node);
	}
}
