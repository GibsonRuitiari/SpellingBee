@file:Suppress("UNCHECKED_CAST")

package me.gibsoncodes.spellingbee.di

import android.util.ArrayMap
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import kotlin.properties.ReadOnlyProperty


interface Factory<T> {
    val instancesCache:InstancesCache
    fun createInstance(key:Int,
                       creationExtras:CreationExtras,
                       modelClass:Class<out T>):T
    fun get(key: Int,modelClass: Class<out T>,
            creationExtras: MutableCreationExtras):T?
    // to be called when the scope associated with the
    // instance been created by this factory is being destroyed
    fun clear()
}

abstract class DefaultFactory<T>:Factory<T> where T:Any{
    private var internalId:Int? = null
    override fun get(key: Int,
                     modelClass: Class<out T>,
                     creationExtras:MutableCreationExtras): T {
        internalId = key
        var cachedInstance = instancesCache[key] as? T
        return if (modelClass.isInstance(cachedInstance)){
           cachedInstance as T
        }else {
            cachedInstance = createInstance(key, creationExtras, modelClass).also {
                instancesCache.set(key,it)?.also {purgedValue->
                    purgedValue.closeQuietlyIfAutoCloseable()
                }
            }
            cachedInstance
        }
    }
    override fun clear() {
        internalId?.let {
            val removedValue=instancesCache.remove(it)
            removedValue?.closeQuietlyIfAutoCloseable()
        }
    }

    private inline fun<reified T:Any>T.closeQuietlyIfAutoCloseable(){
        try {
            if (this::class.java.isInstance(AutoCloseable::class.java)){
                (this as AutoCloseable).close()
            }
        }catch (ex:Exception){
            //ignore
        }
    }
}

object FactoryManager {
    private lateinit var _instanceCache: InstancesCache

    val instanceCache:InstancesCache
        get() = _instanceCache
    private val dependenciesCreators = ArrayMap<Int, Factory<*>>()
    fun onActivityCreate(recentNonConfigurationInstance:InstancesCache?){
        _instanceCache = when (recentNonConfigurationInstance) {
            null -> {
                // create a new cache
                InstancesCache
            }
            else -> {
                recentNonConfigurationInstance
            }
        }
    }
    fun<T:Any> createDependency(
        key: Int,
        defaultCreationExtras:MutableCreationExtras,
        modelClass: Class<out T>,
        factoryProducer: ()->Factory<T>
    ):T{
        var factory=dependenciesCreators[key]
        if (factory==null){
            factory=factoryProducer()
            dependenciesCreators[key] = factory
        }
        return (factory as Factory<T>).get(key, modelClass = modelClass, defaultCreationExtras)!!
    }
    fun onActivityDestroy(){
        dependenciesCreators.forEach { (_, u) ->u.clear()  }
        dependenciesCreators.clear()
    }
}

inline fun<reified T : Any> FactoryManager.create(key: Int,creationExtras:MutableCreationExtras=MutableCreationExtras(),
                                                  modelClass: Class<out T>,
                                                  noinline createObject:(key:Int,modelClass:Class<out T>,
                                                                         creationExtras:CreationExtras)->T):ReadOnlyProperty<Any?,T?>{
    val localInstanceCache = this.instanceCache
    return ReadOnlyProperty { _, _ ->
        createDependency(key,
            defaultCreationExtras=creationExtras,
            modelClass = modelClass,
            factoryProducer = {
                object:DefaultFactory<T>(){
                    override fun createInstance(key: Int,
                                                creationExtras:CreationExtras,
                                                modelClass: Class<out T>): T {
                        return createObject(key, modelClass,creationExtras)
                    }
                    override val instancesCache: InstancesCache
                        get() = localInstanceCache
                }})
    }

}