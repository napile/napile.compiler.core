package org.napile.idea.plugin.refactoring.introduceVariable;

import org.napile.compiler.lang.psi.NapileProperty;
import org.napile.compiler.lang.psi.NapilePsiFactory;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.Project;

/**
 * User: Alefas
 * Date: 14.02.12
 */
public class JetChangePropertyActions
{
	private JetChangePropertyActions()
	{
	}

	public static void declareValueOrVariable(Project project, boolean isVariable, NapileProperty property)
	{
		ASTNode node;
		if(isVariable)
		{
			node = NapilePsiFactory.createVarNode(project);
		}
		else
		{
			node = NapilePsiFactory.createValNode(project);
		}
		property.getVarNode().getPsi().replace(node.getPsi());
	}
}
