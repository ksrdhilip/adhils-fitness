package com.adhils.fitness

import android.app.Application
import android.util.AtomicFile
import androidx.room.*
import com.adhils.fitness.core.*
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
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
class FitnessRepository(private val dao:SnapshotDao,private val file:File?=null) {
    private val mutex=Mutex()
    private val atomic get()=file?.let { AtomicFile(it) }
    suspend fun load():ProfileStore=mutex.withLock {
        withContext(Dispatchers.IO) {
            val target=atomic
            if(target!=null && file?.exists()==true) {
                val json=target.openRead().use { it.readBytes().toString(Charsets.UTF_8) }
                return@withContext AppJson.decodeFromString<ProfileStore>(json).validated()
            }
            val legacy=dao.get()?.let { AppJson.decodeFromString<ProfileStore>(it.json).validated() }
            if(legacy!=null) {
                writeAtomic(legacy)
                return@withContext legacy
            }
            ProfileStore().initialized()
        }
    }
    suspend fun save(store:ProfileStore)=mutex.withLock {
        withContext(Dispatchers.IO) {
            val validated=store.validated()
            val encoded=AppJson.encodeToString(validated)
            writeAtomic(validated,encoded)
            // Keep Room updated while below the 2 MB CursorWindow threshold.
            if(encoded.length<1_000_000) dao.save(Snapshot(json=encoded))
        }
    }
    private fun writeAtomic(store:ProfileStore,encoded:String=AppJson.encodeToString(store)) {
        val target=atomic ?: return
        val bytes=encoded.toByteArray(Charsets.UTF_8)
        val stream=target.startWrite()
        try { stream.write(bytes); target.finishWrite(stream) }
        catch(e:Throwable) { target.failWrite(stream); throw e }
    }
}
