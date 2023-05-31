package me.gibsoncodes.spellingbee.di

import me.gibsoncodes.spellingbee.utils.checkValues
import javax.inject.Inject
import kotlin.reflect.KClass
import kotlin.reflect.KFunction
import kotlin.reflect.KVisibility
import kotlin.reflect.full.hasAnnotation

class ConstructorSelector {
    operator fun invoke(targetType:KClass<*>):KFunction<Any>{
        checkValues(targetType.isInterface,targetType.isAbstract, predicate = {!it}, lazyMessage ="The provided target type ${targetType.qualifiedName} is either an interface  or an abstract class" )
        checkValues(targetType.doesClassHasAtLeastOneAccessibleConstructor,targetType.doesClassHasAtMost1InjectAnnotatedConstructor, predicate = {true},
            lazyMessage = "Class must have at least 1 public constructor or if it has multiple constructors one of them must be injected with inject annotation ${targetType.qualifiedName}")
    val accessibleConstructors = targetType.constructors.filter { it.visibility == KVisibility.PUBLIC  || it.visibility==KVisibility.INTERNAL}
    return when(accessibleConstructors.count()){
        1-> accessibleConstructors.first()
        else->{
            /*
             select the first constructor annotated with Inject or the first that has no parameters
             Note:Class's constructors are callable so we are treating them as functions [KFunction> hence, we check if they have params,
             */
            val selectedConstructor = accessibleConstructors.firstOrNull { it.hasAnnotation<Inject>() } ?: accessibleConstructors.firstOrNull { it.isFunctionParameterless }
            checkNotNull(selectedConstructor){"Unable to select a constructor from the provided type ${targetType.qualifiedName}"}
            selectedConstructor
        } }
    }
}
val KFunction<Any>.isFunctionParameterless
    get() = parameters.isEmpty()
val KClass<*>.doesClassHasAtMost1InjectAnnotatedConstructor:Boolean
    get() {return constructors.flatMap { it.annotations }.count { it.annotationClass == Inject::class } <= 1}
private val KClass<*>.isInterface
    get() = java.isInterface
private val KClass<*>.doesClassHasAtLeastOneAccessibleConstructor
    get() = constructors.any{it.visibility == KVisibility.INTERNAL || it.visibility == KVisibility.PUBLIC}