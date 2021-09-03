package dev.louischan.hackernewscompose

import androidx.paging.PagingSource
import androidx.room.*

@Entity
data class Story (
    @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "id") val id: Long = 0,
    @ColumnInfo(name = "index") val index: Long,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "time_posted") val timePosted: String,
    @ColumnInfo(name = "author") val author: String?,
    @ColumnInfo(name = "content") val content: String?,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean,
    @ColumnInfo(name = "is_dead") val isDead: Boolean,
    @ColumnInfo(name = "url") val url: String?,
    @ColumnInfo(name = "score") val score: Int,
    @ColumnInfo(name = "comment_count") val commentCount: Int,
    @ColumnInfo(name = "poll_id") val pollId: Int?,
) {
    companion object {
        fun fromHNStory(hnStory: HNStory, index: Long): Story {
            return Story(
                id = hnStory.id,
                index = index,
                title = hnStory.title,
                timePosted = hnStory.time.toString(),
                author = hnStory.author,
                content = hnStory.content,
                isDeleted = hnStory.deleted,
                isDead = hnStory.dead,
                url = hnStory.url,
                score = hnStory.score,
                commentCount = hnStory.descendants,
                pollId = hnStory.poll,
            )
        }
    }
}

data class StoryWithComments(
    @Embedded val story: Story,
    @Relation(
        parentColumn = "id",
        entityColumn = "story_id",
    )
    val commentIds: List<CommentId>
)

@Dao
interface StoryDao {
    @Query("SELECT * FROM story ORDER BY `index`")
    fun pagingSource(): PagingSource<Int, Story>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStory(story: Story)

    @Query("DELETE FROM story")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStories(stories: List<Story>)

    @Query("SELECT * FROM story ORDER BY `index` DESC LIMIT 1")
    suspend fun lastItemOrNull(): Story?

    @Transaction
    @Query("SELECT * FROM story WHERE id = :id")
    suspend fun getStoryWithCommentsById(id: Long): StoryWithComments?
}