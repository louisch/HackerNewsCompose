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
    @ColumnInfo(name = "time_posted") val timePosted: Timestamp,
) {
    companion object {
        fun fromHNComment(hnComment: HNComment): Comment {
            return Comment(
                id = hnComment.id,
                parent = hnComment.parent,
                author = hnComment.author,
                text = hnComment.text,
                timePosted = hnComment.time,
            )
        }
    }
}

@Dao
interface CommentDao {
    @Query("SELECT * FROM comment ORDER BY id")
    fun pagingSource(): PagingSource<Int, Comment>
}