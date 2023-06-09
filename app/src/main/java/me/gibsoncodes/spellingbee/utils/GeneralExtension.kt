package me.gibsoncodes.spellingbee.utils

import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.Handler
import android.os.Looper
import me.gibsoncodes.spellingbee.BuildConfig
import java.util.concurrent.CountDownLatch
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

fun CharArray.getCharacterBitVector():Int{
    var result=0
    for(character in this){
        //get the ordinal of this character from char 'a'[96] so b will be 97
        val characterOrdinal = character-'a'
        // perform a right inverse thus 1 will represent the character 0 will represent an empty space
        result=result or 1.shl(characterOrdinal)
    }
    return result
}

fun String.getScoreOfWord():Int{
    val vectorBit= toCharArray().getCharacterBitVector()
    return if (Integer.bitCount(vectorBit)>= 7) length.plus(7)
    else if (length == 4)  1
    else length
}


inline fun ifDebugDo(crossinline action:()->Unit){
    if (BuildConfig.DEBUG){
        action()
    }
}
fun String.shuffle():String{
    val bucket = toCharArray().toMutableList()
    val shuffledOutput = StringBuilder(length)
    while (bucket.size!=0){
        val picker= Math.random().times(bucket.size)
        shuffledOutput.append(bucket.removeAt(picker.toInt()))
    }
    return shuffledOutput.toString()
}
@OptIn(ExperimentalContracts::class)
inline fun<T> java.util.concurrent.Semaphore.withLock(action:()->T):T{
    // invoke the action() block only once, and don't re-invoke once it is
    // complete
    contract { callsInPlace(action, InvocationKind.EXACTLY_ONCE) }
    acquire()
    try {
        return action()
    }finally {
        release()
    }
}
fun SQLiteOpenHelper.getDatabaseInstance(handlerThreadLooper: Looper): SQLiteDatabase?{
    val countdownLatch = CountDownLatch(1)
    val databaseInstances = arrayOfNulls<SQLiteDatabase>(1)
    Handler(handlerThreadLooper).post {
        databaseInstances[0] = writableDatabase
        countdownLatch.countDown()
    }
    try {
        countdownLatch.await()
    }catch (_:Exception){}
    return databaseInstances[0]
}
