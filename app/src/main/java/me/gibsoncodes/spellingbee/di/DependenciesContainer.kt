package me.gibsoncodes.spellingbee.di

import android.content.Context
import android.content.res.AssetManager
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.os.HandlerThread
import android.os.Looper
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import me.gibsoncodes.spellingbee.BuildConfig
import me.gibsoncodes.spellingbee.SpellingBeeApplication
import me.gibsoncodes.spellingbee.persistence.*
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGenerator
import me.gibsoncodes.spellingbee.puzzlegenerator.PuzzleGeneratorDelegate

object DependenciesContainer {
    val factoryManager by lazy { FactoryManager }
    private val localDefaultCreationExtras = MutableCreationExtras()
    private object ContextKeyImpl: CreationExtras.Key<Context>
    private object DatabaseNameKeyImpl: CreationExtras.Key<String>
    private object DatabaseVersionKeyImpl: CreationExtras.Key<Int>
    private object HandlerThreadKeyImpl: CreationExtras.Key<HandlerThread>
    private object DatabaseKeyImpl: CreationExtras.Key<SQLiteDatabase>
    private object LooperKeyImpl: CreationExtras.Key<Looper>
    private object AssetsKeyImpl: CreationExtras.Key<AssetManager>
    private object PuzzleDaoKeyImpl: CreationExtras.Key<PuzzleDao>
    private val PuzzleDaoKey: CreationExtras.Key<PuzzleDao> =PuzzleDaoKeyImpl
    private val DatabaseKey: CreationExtras.Key<SQLiteDatabase> = DatabaseKeyImpl
    private val HandlerThreadKey: CreationExtras.Key<HandlerThread> =HandlerThreadKeyImpl
    private val LooperKey: CreationExtras.Key<Looper> = LooperKeyImpl
    private val AssetsKey: CreationExtras.Key<AssetManager> = AssetsKeyImpl
    private val SqliteOpenHelperVersionKey: CreationExtras.Key<Int> = DatabaseVersionKeyImpl
    private val SqliteOpenHelperContextKey: CreationExtras.Key<Context> = ContextKeyImpl
    private val SqliteOpenHelperDbNameKey: CreationExtras.Key<String> = DatabaseNameKeyImpl

    private const val PuzzleDaoFactoryKey =0x001
    private const val PuzzleRepositoryFactoryKey =0x002
    private const val PuzzleGeneratorFactoryKey=0x003
    private const val SqliteOpenHelperFactoryKey=0x004
    val Context.inMemoryOpenHelper:SQLiteOpenHelper
        get() {
            val openHelper by factoryManager.create(
                SqliteOpenHelperFactoryKey,
                creationExtras = localDefaultCreationExtras.apply {
                    this[SqliteOpenHelperContextKey] = applicationContext
                    this[SqliteOpenHelperDbNameKey]= ""
                    this[SqliteOpenHelperVersionKey] = BuildConfig.DatabaseVersion
                }, modelClass = SQLiteOpenHelper::class.java,
                createObject = {key, _, creationExtras ->

                    when(key){
                        SqliteOpenHelperFactoryKey->{
                            val databaseName=creationExtras[SqliteOpenHelperDbNameKey]
                            val databaseVersion=creationExtras[SqliteOpenHelperVersionKey]
                            val appContext=creationExtras[SqliteOpenHelperContextKey]
                            requireNotNull(appContext)
                            requireNotNull(databaseName)
                            requireNotNull(databaseVersion)
                            DatabaseHelper(appContext,databaseName, version = databaseVersion)
                        }
                        else->throw IllegalArgumentException("Key provided is un-recognizable to openHelper factory")
                    }
                })
            return openHelper!!
        }
    val Context.defaultOpenHelper:SQLiteOpenHelper
        get() {
           val openHelper by factoryManager.create(
                   SqliteOpenHelperFactoryKey,
            creationExtras = localDefaultCreationExtras.apply {
                this[SqliteOpenHelperContextKey] = applicationContext
                this[SqliteOpenHelperDbNameKey]= BuildConfig.DatabaseName
                this[SqliteOpenHelperVersionKey] = BuildConfig.DatabaseVersion
            }, modelClass = SQLiteOpenHelper::class.java,
            createObject = {key, _, creationExtras ->

                when(key){
                    SqliteOpenHelperFactoryKey->{
                        val databaseName=creationExtras[SqliteOpenHelperDbNameKey]
                        val databaseVersion=creationExtras[SqliteOpenHelperVersionKey]
                        val appContext=creationExtras[SqliteOpenHelperContextKey]
                        requireNotNull(appContext)
                        requireNotNull(databaseName)
                        requireNotNull(databaseVersion)
                        DatabaseHelper(appContext,databaseName, version = databaseVersion)
                    }
                    else->throw IllegalArgumentException("Key provided is un-recognizable to openHelper factory")
                }
            })
           return openHelper!!
        }
    val Context.puzzleGenerator:PuzzleGenerator
        get() {
            val localPuzzleGenerator by factoryManager.create(PuzzleGeneratorFactoryKey,
                creationExtras = localDefaultCreationExtras.apply {
                    this[AssetsKey] = assets
                    this[LooperKey] = (applicationContext as SpellingBeeApplication).handlerThread.looper
                }, modelClass = PuzzleGenerator::class.java, createObject = { key, _, creationExtras->
                    when(key){
                       PuzzleGeneratorFactoryKey->{
                            val handlerThreadLooper = creationExtras[LooperKey]
                            val assetManager = creationExtras[AssetsKey]
                            requireNotNull(assetManager)
                            requireNotNull(handlerThreadLooper)
                            PuzzleGeneratorDelegate(handlerThreadLooper,assetManager)
                        }
                        else->throw IllegalArgumentException("An unknown key $key was passed to puzzle generator factory")
                    }
                })
           return localPuzzleGenerator!!
        }

    fun Context.getPuzzleDao(databaseInstance:SQLiteDatabase):PuzzleDao{
        val puzzleDao by factoryManager.create(
            PuzzleDaoFactoryKey,
            creationExtras =localDefaultCreationExtras.apply {
                this[DatabaseKey] = databaseInstance
                this[HandlerThreadKey] = (applicationContext as SpellingBeeApplication).handlerThread
            },modelClass = PuzzleDao::class.java,
            createObject = {key, _, creationExtras ->
                return@create when (key) {
                    PuzzleDaoFactoryKey -> {
                        val handlerThread = creationExtras[HandlerThreadKey]
                        val db = creationExtras[DatabaseKey]
                        requireNotNull(handlerThread) { throw IllegalArgumentException("Must provide a handler thread!") }
                        requireNotNull(db) { throw IllegalArgumentException("Must provide a database instance!!") }
                        PuzzleDaoDelegate(db,handlerThread)
                    }
                    else -> throw IllegalArgumentException("Key provided is not recognized!!")
                }
            })
       return puzzleDao!!
    }
    val PuzzleDao.puzzleRepository:PuzzleRepository
        get() {
            val localPuzzleRepository by factoryManager.create(
               PuzzleRepositoryFactoryKey,
                creationExtras = localDefaultCreationExtras.apply {
                    this[PuzzleDaoKey] =this@puzzleRepository
                }, modelClass = PuzzleRepository::class.java, createObject = { key, _, creationExtras ->
                    when(key){
                       PuzzleRepositoryFactoryKey->{
                            val dao =creationExtras[PuzzleDaoKey]
                            requireNotNull(dao)
                            PuzzleRepositoryDelegate(dao)
                        }
                        else->throw IllegalArgumentException("key provided is not recognizable!")
                    }
                })
           return localPuzzleRepository!!
        }


}