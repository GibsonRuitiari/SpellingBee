package me.gibsoncodes.spellingbee.ioctest

import me.gibsoncodes.spellingbee.di.DefaultDependencyContainer
import org.junit.Before
import org.junit.Test
import kotlin.test.assertFails

class DependencyContainterTest {
     private lateinit var dependencyContainer:DefaultDependencyContainer
    @Before
    fun initDependencyContainerInstance() {
        dependencyContainer = DefaultDependencyContainer.constructNewDependencyContainerInstance()
    }
    @Test
    fun `registering a binding fails when the type exists`(){
        dependencyContainer.registerBinding(Bar::class,BarImpl::class,)
        assertFails{ dependencyContainer.registerBinding(Bar::class,BarImpl::class) }
    }
    @Test
    fun `resolve binding for registered types which have params in constructors`(){
        dependencyContainer.registerBinding(TypeWithInterfaceDependency::class)
        dependencyContainer.registerBinding(Foo::class,FooImpl::class)

        assert(TypeWithInternalDependency::class.isInstance(dependencyContainer.resolveBinding<TypeWithInternalDependency>()))
    }

    @Test
    fun `resolve binding for non registered type with parameterless constructor`(){
        val instance = dependencyContainer.resolveBinding<FooImpl>()
        assert(FooImpl::class.isInstance(instance))
    }
    @Test
    fun `resolve binding of Kotlin Object()` (){
        dependencyContainer.registerBinding(KotlinObject::class)
        val instance = dependencyContainer.resolveBinding<KotlinObject>()
        assert(instance::class.isInstance(KotlinObject))
    }
    @Test
    fun `resolve binding fails for an unknown instance`(){
        assertFails { dependencyContainer.resolveBinding<Foo>() }
    }
    @Test
    fun `resolve binding fails when there is no concrete implementation`() {
        dependencyContainer.registerBinding(Foo::class)
        assertFails { dependencyContainer.resolveBinding<Foo>() }
    }
    @Test
    fun `resolve fails for circular dependency`(){
        dependencyContainer.registerBinding(Egg::class)
        dependencyContainer.registerBinding(Chicken::class)
        assertFails{
            dependencyContainer.resolveBinding<Egg>()
        }
    }
}