package me.gibsoncodes.spellingbee.di

import kotlin.reflect.KClass

interface DependencyContainer:Disposable{
    fun registerBinding(source:KClass<*>,
                        target:KClass<*>?=null,
                        vararg finderParams:Any?)
    fun resolveBinding(sourceType:KClass<*>):Any
    fun onStart()
}