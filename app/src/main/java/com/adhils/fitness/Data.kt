package com.adhils.fitness

import android.app.Application
import androidx.room.*
import com.adhils.fitness.core.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString

@Entity(tableName="app_snapshot")
data class Snapshot(@PrimaryKey val id:Int=1,val json:String)
@Dao interface SnapshotDao {
    @Query("SELECT * FROM app_snapshot WHERE id=1") suspend fun get():Snapshot?
    @Insert(onConflict=OnConflictStrategy.REPLACE) suspend fun save(snapshot:Snapshot)
}
@Database(entities=[Snapshot::class],version=1,exportSchema=false)
abstract class FitnessDatabase:RoomDatabase() { abstract fun snapshot():SnapshotDao }
class FitnessApplication:Application() {
    val database by lazy { Room.databaseBuilder(this,FitnessDatabase::class.java,"adhils-fitness.db").build() }
}
class FitnessRepository(private val dao:SnapshotDao) {
    private val mutex=Mutex()
    suspend fun load():ProfileStore=mutex.withLock {
        dao.get()?.let { AppJson.decodeFromString<ProfileStore>(it.json).validated() } ?: ProfileStore().initialized()
    }
    suspend fun save(store:ProfileStore)=mutex.withLock {
        dao.save(Snapshot(json=AppJson.encodeToString(store.validated())))
    }
}
