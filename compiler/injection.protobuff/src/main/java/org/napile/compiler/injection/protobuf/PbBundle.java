package org.napile.compiler.injection.protobuf;

import java.util.ResourceBundle;

import org.jetbrains.annotations.PropertyKey;
import com.intellij.CommonBundle;

/**
 * @author Nikolay Matveev
 *         Date: Mar 12, 2010
 */
public class PbBundle
{
	private static final String BUNDLE_NAME = "messages.PbBundle";
	private static final ResourceBundle BUNDLE = ResourceBundle.getBundle(BUNDLE_NAME);

	public static String message(@PropertyKey(resourceBundle = BUNDLE_NAME) String key, Object... params)
	{
		return CommonBundle.message(BUNDLE, key, params);
	}
}
