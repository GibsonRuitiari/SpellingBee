package me.gibsoncodes.spellingbee.ioctest

import me.gibsoncodes.spellingbee.di.ConstructorSelector
import org.junit.Test
import kotlin.reflect.full.primaryConstructor

class ConstructorSelectorTest{
    private val subject = ConstructorSelector()
    @Test
    fun `return constructor for type that has public constructor`(){
        val type = BarImpl::class
        val result = subject(type)
        val expected = BarImpl::class.constructors.first()
        assert(expected==result)
    }
    @Test
    fun `return primary constructor annotated with inject annotation`(){
        val sourceType = TypeWithInterfaceDependency::class
        val resulted=subject(sourceType)
        val expected = TypeWithInterfaceDependency::class.primaryConstructor
        assert(resulted==expected)
    }
    @Test
    fun `return secondary constructor annotated with inject annotation`(){
        val sourceType = TypeWithSecondaryConstructorAnnotatedWithInject::class
        val result = subject(sourceType)
        val expected= TypeWithSecondaryConstructorAnnotatedWithInject::class.constructors.toList()
        assert(result==expected[0])
    }

}