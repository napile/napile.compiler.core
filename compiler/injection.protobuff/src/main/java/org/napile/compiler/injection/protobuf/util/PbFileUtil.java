package org.napile.compiler.injection.protobuf.util;

/**
 * @author Nikolay Matveev
 */

public class PbFileUtil
{
	public static boolean isPathToDirectory(String filePath)
	{
		return filePath.endsWith("/") || filePath.endsWith("\\");
	}
}
