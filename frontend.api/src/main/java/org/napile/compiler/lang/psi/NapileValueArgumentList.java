package org.napile.compiler.lang.psi;

import java.util.List;

/**
 * @author VISTALL
 * @since 19:57/20.02.13
 */
public interface NapileValueArgumentList extends NapileElement
{
	List<NapileValueArgument> getArguments();
}
