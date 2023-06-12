package me.gibsoncodes.spellingbee.log

import android.os.Build
import android.util.Log
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

// used in an inline function, has to be public for it to be accessible
val currentTimeInString:String
    get(){
       return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }else{
            val currentDate=Date()
            SimpleDateFormat("yyyy MM dd HH:mm", Locale.ENGLISH).format(currentDate)
        }
    }
inline fun<reified T>debug(crossinline action:()->String){
    Log.d(T::class.simpleName,"$currentTimeInString| ${action()}")

}
inline fun<reified T>info(crossinline action: () -> String){
    Log.i(T::class.simpleName,"$currentTimeInString| ${action()}")
}
inline fun<reified T>error(crossinline action: () -> String){
    Log.e(T::class.simpleName,"$currentTimeInString| ${action()}")
}
inline fun<reified T>warn(crossinline action: () -> String){
    Log.w(T::class.simpleName,"$currentTimeInString| ${action()}")
}