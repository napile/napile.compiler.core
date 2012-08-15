class Test : String, MyInterface
{
covered this() : String(), MyInterface()
this(a : Int) : this()
heritable this(a : String?) : String(a), MyInterface()
}