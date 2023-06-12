package me.gibsoncodes.spellingbee.log

import android.util.Log


inline fun<reified T>debug(crossinline action:()->String){
    Log.d(T::class.simpleName,action())

}
inline fun<reified T>info(crossinline action: () -> String){
    Log.i(T::class.qualifiedName,action())
}
inline fun<reified T>error(crossinline action: () -> String){
    Log.e(T::class.qualifiedName,action())
}
inline fun<reified T>warn(crossinline action: () -> String){
    Log.w(T::class.qualifiedName,action())
}