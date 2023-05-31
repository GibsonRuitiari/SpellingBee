package me.gibsoncodes.spellingbee.utils

inline fun <reified T:Any> checkNotNull(vararg values:T?) = checkValues(values = values, predicate = {it!=null})
inline fun<T:Any?> checkValues(vararg values:T, predicate: (T) -> Boolean, lazyMessage:String?=null) = checkValues(values=values,
    predicate=predicate, lazyMessage = {lazyMessage ?: it})
inline fun<T:Any?> checkValues(vararg values:T, predicate:(T)->Boolean, lazyMessage:(errorMessage:String)->Unit):Array<out T>{
    val unwantedItemsIndices = mutableListOf<Int>()
    values.forEachIndexed { index, t ->
        if (predicate(t).not()){
            unwantedItemsIndices += index
        }
    }
    return if (unwantedItemsIndices.isNotEmpty()){
        val exceptionMessage = "The following indices did not satisfy the precondition ${unwantedItemsIndices.joinToString(",")}"
        throw IllegalStateException(lazyMessage(exceptionMessage).toString())
    }else values
}