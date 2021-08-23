package dev.louischan.hackernewscompose

import androidx.paging.PagingSource
import androidx.room.*
import java.sql.Timestamp

@Entity
data class Comment(
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "parent") val parent: Long,
    @ColumnInfo(name = "author") val author: String,
    @ColumnInfo(name = "text") val text: String,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean,
    @ColumnInfo(name = "time_posted") val timePosted: Timestamp,
) {
    companion object {
        fun fromHNComment(hnComment: HNComment): Comment {
            return Comment(
                id = hnComment.id,
                parent = hnComment.parent,
                author = hnComment.author ?: "",
                text = hnComment.text ?: "",
                isDeleted = hnComment.isDeleted ?: false,
                timePosted = hnComment.time,
            )
        }
    }
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comment ORDER BY id")
    fun pagingSource(): PagingSource<Int, Comment>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(comment: Comment)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(comments: List<Comment>)

    @Query("DELETE FROM comment")
    suspend fun deleteAll()

    @Query("SELECT count(id) FROM comment")
    suspend fun count(): Long

    @Transaction
    suspend fun insertNew(comments: List<Comment>) {
        deleteAll()
        insertAll(comments)
    }
}

@Entity
data class CommentId(
    @PrimaryKey @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "story_id") val storyId: Long,
)

@Dao
interface CommentIdDao {
    @Insert
    suspend fun insertAll(ids: List<CommentId>)

    suspend fun insertAllLongs(parentId: Long, ids: List<Long>) {
        insertAll(ids.map { id -> CommentId(id, parentId) })
    }

    @Query("SELECT count(id) FROM comment")
    suspend fun count(): Long
}