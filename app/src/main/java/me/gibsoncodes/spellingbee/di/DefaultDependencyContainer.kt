package me.gibsoncodes.spellingbee.di

import me.gibsoncodes.spellingbee.log.info
import me.gibsoncodes.spellingbee.utils.CircularDependencyException
import me.gibsoncodes.spellingbee.utils.ifDebugDo
import javax.inject.Singleton
import kotlin.reflect.KClass
import kotlin.reflect.full.hasAnnotation
import kotlin.reflect.full.isSubclassOf

class DefaultDependencyContainer :DependencyContainer{
    init {
        instance =this
    }

    private lateinit var dependencyBindings: MutableMap<KClass<*>, KClass<*>?>
    private lateinit var dependencyBindingsParams: MutableMap<KClass<*>, List<Any?>>
    private lateinit var singletonInstances: MutableMap<KClass<*>, Any>
    private lateinit var constructorSelector:ConstructorSelector

    override fun onStart() {
        ifDebugDo { info<DefaultDependencyContainer> {"onStart is called, initializing variables needed by the DependencyContainer" } }
        dependencyBindingsParams = mutableMapOf()
        singletonInstances = mutableMapOf()
        dependencyBindings = mutableMapOf()
        constructorSelector= ConstructorSelector()
    }


    /**
     * Register a new binding to the container
     * @param source represents the type to be resolved. A source can be an interface/abstract class
     * @param target represents the type to be mapped to the source. This ideally is (should/must) a concrete implementation
     * of an interface or an abstract class. The target type is nullable to allow registering of source types without having to instantly register
     * their concrete types
     * @param finderParams are parameters that are to be provided to the [target]'s constructor call. Example are params
     * that cannot be resolved by the container such as primitives, third party libraries etc.
     * Note: the finderParams ought to be provided in the order they are called/required by the [target]'s constructor or else
     * an error will be thrown. Example
     * ```
     * class Foo(val index:Int, val defaultExecutor:Executor)
     * val defaultExecutor = // some executor
     * container.registerBinding(Foo::class,1,defaultExecutor)
     * ```
     */
    override fun registerBinding(source:KClass<*>,target:KClass<*>?,vararg finderParams:Any?){
        check(!dependencyBindings.contains(source)){"The source type ${source
            .qualifiedName} is already registered/present in the container!"}
        dependencyBindings[source] = target
        if (finderParams.isNotEmpty()){
            ifDebugDo { info<DependencyContainer> { "finder params is not empty" } }
            /*
             Point to note: target type must not be null since, the finder params only apply to the target type
             */
            checkNotNull(target)
            dependencyBindingsParams[target]= finderParams.toList()
        }
    }

    /**
     * An integration function that returns an instance of a specific source type.
     * The type ideally is passed in as a generic type arg
     */

    inline fun<reified T:Any> resolveBinding()
            = resolveBinding(T::class) as T

    /**
     * Returns an instance of a specific source type
     * This functions either constructs the target type or just returns it from our stored bindings if present
     * @param sourceType to be resolved
     */
    override fun resolveBinding(sourceType:KClass<*>):Any{
        val targetType = dependencyBindings[sourceType] ?: sourceType

        val localFinderParams = dependencyBindingsParams[targetType]

        // target type cannot be an interface, if that's the case,
        // there's no target-type(concrete implementation) stored for that interface
        check(!targetType.java.isInterface){"No binding found for interface: ${targetType.qualifiedName}"}
        // if source is an object declaration return it
        if (sourceType.objectInstance!=null){
            return sourceType.objectInstance as Any
        }

        // if the source is a singleton, return the target type
        if (singletonInstances.containsKey(targetType)){
            return singletonInstances[targetType] as Any
        }

        val constructor = constructorSelector(targetType)

        return if (constructor.parameters.isEmpty()) {
            //parameterless constructor
            ifDebugDo { info<DefaultDependencyContainer> {"constructor is parameterless"} }
            constructor.call().also { possiblySaveSingletonInstance(targetType, it) }
        } else {
            try {
                /* constructor has parameters so get an ordered list of these parameters, then get their respective
                 * Kotlin-class classifier as in if a parameter is of type [String] its classifier would be KClass instance for [String]
                 * ,then resolve their bindings by ideally constructing concrete instances of these parameters
                 * Being recursive function will throw a stack overflow error if two classes exhibit circular dependency- recursively calls each other*/
                val constructorParams=Array(constructor.parameters.size){ index->
                    val kClass = constructor.parameters[index].type.classifier as KClass<*>
                    return@Array when(localFinderParams.isNullOrEmpty()){
                        false-> localFinderParams.firstOrNull { it!!::class.isSubclassOf(kClass) } ?: resolveBinding(kClass)
                        else->resolveBinding(kClass)
                    }
                }
                constructor.call(*constructorParams).also { possiblySaveSingletonInstance(targetType, it) }
            } catch (stackOverFlowError: StackOverflowError) {
                me.gibsoncodes.spellingbee.log.error<DefaultDependencyContainer> { "circular dependency exception occurred while resolving bindings for ${sourceType.qualifiedName}" }
                throw CircularDependencyException("Type ${sourceType.qualifiedName}")
            }
        }

    }

    private fun possiblySaveSingletonInstance(targetType:KClass<*>,instance:Any){
        if (targetType.hasAnnotation<Singleton>()){
            singletonInstances[targetType] = instance
        }
    }

    override fun dispose() {
        dependencyBindings.forEach { (t, u) ->
            if (t.isInstance(AutoCloseable::class)){
                (u as AutoCloseable).close()
            }
        }
        dependencyBindingsParams.forEach { (t, u) ->
            if (t.isInstance(AutoCloseable::class)){
                (u as AutoCloseable).close()
            }
        }
        dependencyBindingsParams.clear()
        dependencyBindings.clear()
        singletonInstances.clear()
        ifDebugDo { info<DefaultDependencyContainer> { "bindings, bindings-params and cached singleton-instances have been disposed" } }
    }

    companion object{
        private lateinit var instance: DefaultDependencyContainer
        // no need of double locking
        fun getInstance(): DefaultDependencyContainer {
            return  if (Companion::instance.isInitialized) return instance
            else constructNewDependencyContainerInstance()
        }
        // for testing purposes
        internal fun constructNewDependencyContainerInstance() =
            DefaultDependencyContainer().apply { onStart() }
    }
}
