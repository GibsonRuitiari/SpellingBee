package me.gibsoncodes.spellingbee.ioctest

import javax.inject.Inject
import javax.inject.Singleton

object KotlinObject
@Singleton
class SingletonClass

class TypeWithInterfaceDependency @Inject constructor(private val bar: Bar)

class TypeWithInternalDependency internal constructor()

class TypeWithSecondaryConstructorAnnotatedWithInject constructor(param1:Int){
    @Inject
    constructor(param1: Int, param2:String) : this(param1)
}
// circular dependency
class Chicken (val egg: Egg)
class Egg(val chicken: Chicken)

interface Bar
class BarImpl: Bar

interface Foo
class FooImpl:Foo{
    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        return javaClass == other?.javaClass
    }
}
