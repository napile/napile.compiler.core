package org.napile.compiler.plugin;

import java.util.ArrayList;
import java.util.List;

import org.napile.compiler.plugin.impl.VariableChangeListenerCompilerPlugin;

/**
 * @author VISTALL
 * @since 21:56/15.05.13
 */
public class CompilerPluginManager
{
	public static final CompilerPluginManager INSTANCE = new CompilerPluginManager();

	private final List<CompilerPlugin> plugins = new ArrayList<CompilerPlugin>();

	private CompilerPluginManager()
	{
		plugins.add(new VariableChangeListenerCompilerPlugin());
	}

	public List<CompilerPlugin> getPlugins()
	{
		return plugins;
	}
}
