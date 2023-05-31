package me.gibsoncodes.spellingbee.di

import me.gibsoncodes.spellingbee.utils.CircularDependencyException
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation

/**
 * An IOC container that behind the scene uses dependency binding to construct your dependencies graph
 * Few things to note: a target-type refers to a concrete implementation of an abstract/interface class
 * a source-type refers to the abstract class/interface
 * Binding as used in this context refers to binding/mapping an interface/abstract class to its concrete implementation
 * or actual object type. When we bind, the actual/concrete implementation is passed to the dependent class that takes in
 * the interface/abstract class as its parameter (basically the source type)
 * This class will automatically resolve the bindings so the client does not have to worry about it
 * Regarding the scope/lifetime of the objects: they exist in three instances(either as a singleton or scoped to the
 * calling scope of the dependency)
 */
class DependencyContainer private constructor(val constructorSelector: ConstructorSelector) {
    init {
        instance=this
    }
    // the value which is the target type can be null at first instance eg during initialization/registration of the bindings to allow for lazy registration
    // of concrete types. Mostly in handy during testing ?
    private val dependencyBindings = mutableMapOf<KClass<*>,KClass<*>?>()
    private val singletonInstances = mutableMapOf<KClass<*>,Any>()
    /**
     * Register a new binding to the container
     * @param sourceType represents the type to be resolved. A source can be an interface/abstract class
     * @param targetType represents the type to be mapped to the source. This ideally is (should/must) a concrete implementation
     * of an interface or an abstract class. The target type is nullable to allow registering of source types without having to instantly register
     * their concrete types
     */
    fun registerBinding(sourceType:KClass<*>,targetType:KClass<*>?=null){
        check(dependencyBindings.contains(sourceType)){"The source type ${sourceType.qualifiedName} is already registered/present in the container!"}
        dependencyBindings[sourceType] = targetType
    }
    /**
     * An integration function that returns an instance of a specific source type.
     * The type ideally is passed in as a generic type arg
     */
    inline fun<reified T:Any> resolveBinding() = resolveBinding(T::class) as T
    /**
     * Returns an instance of a specific source type
     * This functions either constructs the target type or just returns it from our stored bindings if present
     * @param sourceType to be resolved
     */
    fun resolveBinding(sourceType: KClass<*>):Any{
        // if target type is empty/non-existence in the already registered bindings, then take the
        // source type to be the target type
       val targetType = dependencyBindings[sourceType] ?: sourceType
        // target type cannot be an interface, so if that's the case,
        // there's no target-type(concrete implementation) stored for that interface
        check(!targetType.java.isInterface){"No binding found for interface: ${targetType.qualifiedName}"}

        // source type can be an object declaration so check it
        if (sourceType.objectInstance !=null){
           return sourceType.objectInstance as Any
        }
        // if the source is a singleton then return it
        if (singletonInstances.contains(sourceType)){
            return singletonInstances[targetType] as Any
        }

        val constructor = constructorSelector(targetType)
        return if (constructor.isFunctionParameterless){
            // just call the constructor
            constructor.call().also { possiblySaveSingletonInstance(targetType,it) }
        }else{
            try {
                /* constructor has parameters so get an ordered list of these parameters, then get their respective
                * Kotlin-class classifier as in if a parameter is of type [String] its classifier would be KClass instance for [String]
                * ,then resolve their bindings by ideally constructing concrete instances of these parameters
                * Being recursive function will throw a stack overflow error if two classes exhibit circular dependency- recursively calls each other*/
                val parameterInstances = constructor.parameters.map { it.type.classifier as KClass<*> }.map { resolveBinding(it) }
                constructor.call(*parameterInstances.toTypedArray()).also { possiblySaveSingletonInstance(targetType,it) }
            }catch (stackOverFlowError:StackOverflowError){
                throw CircularDependencyException("Type ${sourceType.qualifiedName}")
            }
        }
    }
    private fun possiblySaveSingletonInstance(targetType:KClass<*>,instance:Any){
        if (targetType.hasAnnotation<Singleton>()){
            // persist it in the cache
            singletonInstances[targetType] = instance
        }
    }
    companion object{
        private lateinit var instance:DependencyContainer
        fun getInstance():DependencyContainer{
            return if (::instance.isInitialized) instance
            else constructNewDependencyContainer(ConstructorSelector())
        }
        internal fun constructNewDependencyContainer(constructorSelector: ConstructorSelector) =DependencyContainer(constructorSelector)
    }
}