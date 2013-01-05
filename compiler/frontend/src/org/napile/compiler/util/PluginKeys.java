package org.napile.compiler.util;

import org.napile.compiler.lang.descriptors.DeclarationDescriptor;
import com.intellij.openapi.util.Key;

/**
 * @author VISTALL
 * @date 20:43/05.01.13
 */
public interface PluginKeys
{
	Key<DeclarationDescriptor> DESCRIPTOR_KEY = Key.create("napile-class");
}
