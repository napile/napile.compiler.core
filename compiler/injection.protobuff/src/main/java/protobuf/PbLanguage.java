package protobuf;

import com.intellij.lang.Language;

/**
 * @author Nikolay Matveev
 *         Date: Mar 5, 2010
 */
public class PbLanguage extends Language
{
	public static final Language INSTANCE = new PbLanguage();

	public PbLanguage()
	{
		super("NAPILE-INJECTION-LANGUAGE");
	}
}
