package me.gibsoncodes.spellingbee.di

import android.util.ArrayMap

object InstancesCache {
    // a sparse array can also suffice here
    private val dependencies = ArrayMap<Int, Any>(1)
    operator fun get(index:Int) = dependencies[index]
    fun remove(index: Int) = dependencies.remove(index)
    operator fun set(index: Int, component:Any?) = dependencies.put(index,component)


}