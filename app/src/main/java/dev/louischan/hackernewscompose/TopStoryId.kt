package dev.louischan.hackernewscompose

import androidx.room.*

@Entity
data class TopStoryId(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "index") val index: Long = 0,
    @ColumnInfo(name = "id") val id: Long,
)

@Dao
interface TopStoryIdDao {
    @Query("SELECT * FROM topstoryid WHERE `index` = 1")
    suspend fun getFirst(): TopStoryId

    @Query("SELECT * FROM topstoryid WHERE `index` >= 1 and `index` < 1 + :pageSize")
    suspend fun getFirstPage(pageSize: Int): List<TopStoryId>

    @Query("SELECT * FROM topstoryid WHERE `index` = :index")
    suspend fun getByIndex(index: Long): TopStoryId

    @Query("SELECT * FROM topstoryid WHERE `index` >= :index and `index` < :index + :pageSize")
    suspend fun getPageByIndex(index: Long, pageSize: Int): List<TopStoryId>

    @Query("SELECT * FROM topstoryid WHERE id = :id")
    suspend fun getById(id: Long): TopStoryId

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(topStoryIds: List<TopStoryId>)

    @Query("DELETE FROM topstoryid")
    suspend fun deleteAll()

    @Transaction
    suspend fun insertNew(topStoryIds: List<Long>) {
        deleteAll()
        var index = 1L
        insertAll(topStoryIds.map { id ->
            val topStoryId = TopStoryId(index = index, id = id)
            index += 1
            topStoryId
        })
    }

    @Transaction
    suspend fun getNext(id: Long): TopStoryId {
        val currentItem = getById(id)
        return getByIndex(currentItem.index + 1)
    }

    @Transaction
    suspend fun getNextPage(id: Long, pageSize: Int): List<TopStoryId> {
        val currentItem = getById(id)
        return getPageByIndex(currentItem.index + 1, pageSize)
    }
}