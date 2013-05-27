package org.napile.compiler.plugin;

import java.util.Collection;

import org.napile.compiler.lang.psi.NapileFile;
import org.napile.compiler.lang.resolve.BindingTrace;
import org.napile.compiler.lang.resolve.TopDownAnalysisContext;
import org.napile.compiler.lang.resolve.scopes.NapileScope;

/**
 * @author VISTALL
 * @since 21:56/15.05.13
 */
public interface CompilerPlugin
{
	void process(NapileScope scope, BindingTrace trace, TopDownAnalysisContext context, Collection<? extends NapileFile> declarations);
}
