package org.napile.idea.plugin.refactoring.introduceVariable;

import org.napile.compiler.lang.psi.NapilePsiFactory;
import org.napile.compiler.lang.psi.NapileVariable;
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

	public static void declareValueOrVariable(Project project, boolean isVariable, NapileVariable property)
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
		property.getVarOrValNode().getPsi().replace(node.getPsi());
	}
}
