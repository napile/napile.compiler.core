package org.napile.compiler.lang.psi.util;

import java.util.Comparator;

import org.napile.compiler.lang.psi.NapileElement;
import com.intellij.openapi.util.text.StringUtil;

/**
 * @author VISTALL
 * @since 11:06/10.03.13
 */
public class NapileNameComparator implements Comparator<NapileElement>
{
	public static Comparator<NapileElement> INSTANCE = new NapileNameComparator();

	@Override
	public int compare(NapileElement o1, NapileElement o2)
	{
		final String s1 = StringUtil.notNullize(o1.getName());
		final String s2 = StringUtil.notNullize(o2.getName());
		return s1.compareTo(s2);
	}
}
