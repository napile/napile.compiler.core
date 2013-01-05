/**
 * Copyright
 */
package main;

/**
 * Class JavaDoc
 * @author VISTALL
 */
public class Initial extends Parent
{

	/**
	 * Variable JavaDoc
	 */
	public final int myVar = 1;

	/**
	 * Constructor JavaDoc
	 */
	private Initial()
	{
		this(1);
	}

	/**
	 * Constructor JavaDoc
	 */
	private Initial(final int a, int... b)
	{
		super(1);
	}

	/**
	 * Method JavaDoc
	 * @return
	 */
	public static int method(final int a, int b)
	{
		return 1;
	}
}