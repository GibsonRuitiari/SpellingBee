package me.gibsoncodes.spellingbee.di

import kotlin.reflect.KClass

interface DependencyContainer:Disposable{
    fun registerBinding(source:KClass<*>,
                        target:KClass<*>?=null,
                        vararg finderParams:Any?)
    fun resolveBinding(sourceType:KClass<*>):Any
    fun onStart()
}

fun DependencyContainer.installBindings(vararg acceptBindings:Pair<KClass<*>,KClass<*>?>){
    acceptBindings.forEach {
        val source= it.first
        val target= it.second
        registerBinding(source,target)
    }
}
fun startContainer():DependencyContainer=
    DefaultDependencyContainer.getInstance()