package me.gibsoncodes.spellingbee.persistence

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.provider.BaseColumns
import me.gibsoncodes.spellingbee.persistence.PuzzleContract.PuzzleEntry.PuzzleTableCenterLetterOuterLetterIndexColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleContract.PuzzleEntry.PuzzleTableGeneratedTimeColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleContract.PuzzleEntry.PuzzleTableInnerLetterColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleContract.PuzzleEntry.PuzzleTableOutLetterColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleContract.PuzzleEntry.PuzzleTablePangramColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleContract.PuzzleEntry.PuzzleTableSolutionsColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleContract.PuzzleEntry.PuzzleTableTotalScoreColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleGameStateContract.PuzzleGameStateCurrentWordColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleGameStateContract.PuzzleGameStateIdColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleGameStateContract.PuzzleGameStateOuterLettersColumnName
import me.gibsoncodes.spellingbee.persistence.PuzzleGameStateContract.PuzzleGameStateSolutionColumnName

class DatabaseHelper constructor(context:Context, version:Int,databaseName:String?):SQLiteOpenHelper(context,
    databaseName,null,version) {
  companion object{
      const val PuzzleTableName="puzzle"
      const val PuzzleGameStateTableName="game_state"

      val puzzleTableSqlStatement = """
       CREATE TABLE IF NOT EXISTS `$PuzzleTableName` (`${BaseColumns._ID}` INTEGER PRIMARY KEY AUTOINCREMENT ,
        `$PuzzleTableOutLetterColumnName` TEXT NOT NULL,
        `$PuzzleTableInnerLetterColumnName` INTEGER NOT NULL,
        `$PuzzleTablePangramColumnName` TEXT NOT NULL,
        `$PuzzleTableSolutionsColumnName` TEXT NOT NULL,
        `$PuzzleTableTotalScoreColumnName` INTEGER NOT NULL,
         `$PuzzleTableGeneratedTimeColumnName` TEXT NOT NULL);
    """.trimIndent()

      val puzzleGameStateTableSqlStatement="""
          CREATE TABLE IF NOT EXISTS `$PuzzleGameStateTableName` (`${PuzzleGameStateIdColumnName}` INTEGER PRIMARY KEY  ,
          `$PuzzleGameStateSolutionColumnName` TEXT NOT NULL,
           `$PuzzleGameStateCurrentWordColumnName` TEXT NOT NULL,
           `$PuzzleGameStateOuterLettersColumnName` TEXT NOT NULL);
          
      """.trimIndent()

      val uniquePuzzleIndexStatement = """
       CREATE UNIQUE INDEX IF NOT EXISTS `$PuzzleTableCenterLetterOuterLetterIndexColumnName` ON `$PuzzleTableName` (`$PuzzleTableInnerLetterColumnName`,
        `$PuzzleTableOutLetterColumnName`);
    """.trimIndent()

      val PuzzleBoardStateEntitySqlStatement="""
            SELECT `${BaseColumns._ID}`,
            `${PuzzleTableInnerLetterColumnName}`,
            `${PuzzleTableOutLetterColumnName}`,
            `${PuzzleTableSolutionsColumnName}`,
            `${PuzzleTableTotalScoreColumnName}`,
            `${PuzzleTableGeneratedTimeColumnName}`,
            `${PuzzleTablePangramColumnName}`,
            
            `${PuzzleGameStateIdColumnName}`,
            `${PuzzleGameStateSolutionColumnName}`,
            `${PuzzleGameStateOuterLettersColumnName}`,
            `${PuzzleGameStateCurrentWordColumnName}`
            
            FROM `${PuzzleTableName}`
            LEFT JOIN `${PuzzleGameStateTableName}` ON 
            `${BaseColumns._ID}` = `${PuzzleGameStateIdColumnName}`;
        
        """.trim()

      /* Sql statements used during database upgrades */
      val puzzleTableDropSqlStatement ="DROP TABLE IF EXISTS $PuzzleTableName;"
      val puzzleTableGameStateDropSql ="DROP TABLE IF EXISTS $PuzzleGameStateTableName;"


  }
    override fun onCreate(db: SQLiteDatabase?) {
        db?.let {database->
            database.execSQL(puzzleTableSqlStatement)
            database.execSQL(uniquePuzzleIndexStatement)
            database.execSQL(puzzleGameStateTableSqlStatement)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase?, oldVersion: Int, newVersion: Int) {
        db?.let {
         it.execSQL(puzzleTableDropSqlStatement)
         it.execSQL(puzzleTableGameStateDropSql)
         onCreate(db)
        }
    }

    override fun onOpen(db: SQLiteDatabase?) {
        db?.enableWriteAheadLogging()
    }
}