package me.gibsoncodes.spellingbee.di

import android.content.Context
import android.content.res.AssetManager
import android.database.sqlite.SQLiteDatabase
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import me.gibsoncodes.spellingbee.persistence.DatabaseHelper
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

interface AndroidComponent {
    val assets:AssetManager
    val handlerThread:HandlerThread
    val uiHandler:Handler
    fun getDatabaseInstance(inMemory: Boolean=false):SQLiteDatabase?
    fun getDatabaseHelper(inMemory: Boolean=false):DatabaseHelper
    fun getInMemoryDatabaseInstance():SQLiteDatabase?
}

class AndroidModule constructor(private val context:Context):AndroidComponent{
    private val handlerThreadName="HandlerThreadTag"
    private val databaseVersion=1
    private val databaseName= "Bee.db"
    companion object{
        const val DatabaseName ="SpellingBee.db"
    }

    override val assets: AssetManager
        get() = context.applicationContext.assets
    override val handlerThread: HandlerThread
        get() = HandlerThread(handlerThreadName).apply { start() }
    override val uiHandler: Handler
        get() = Handler(Looper.getMainLooper())

    override fun getDatabaseHelper(inMemory:Boolean): DatabaseHelper {
        return DatabaseHelper(context, version = databaseVersion,
            databaseName = if (inMemory) null else DatabaseName)
    }

    override fun getDatabaseInstance(inMemory: Boolean):SQLiteDatabase?{
        val countDownLatch = CountDownLatch(1)
        val databasesInstances = arrayOfNulls<SQLiteDatabase>(1)
        Handler(handlerThread.looper).post {
            databasesInstances[0] =  getDatabaseHelper(inMemory).writableDatabase
            countDownLatch.countDown()
        }

        try {
            countDownLatch.await()
        }catch (_:Exception){}

        return databasesInstances[0]
    }

    override fun getInMemoryDatabaseInstance(): SQLiteDatabase? {
        return getDatabaseInstance(true)
    }
}