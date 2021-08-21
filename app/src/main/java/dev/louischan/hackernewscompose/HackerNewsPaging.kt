package dev.louischan.hackernewscompose

import android.util.Log
import androidx.paging.*
import kotlinx.coroutines.flow.*
import retrofit2.HttpException
import java.io.IOException
import kotlin.system.measureTimeMillis

/**
 * An implementation of [RemoteMediator] for Hacker News
 *
 * This loads a list of IDs from Hacker News' top stories endpoint via Retrofit, then uses this
 * list as a reference for loading pages of stories, of a defined page size.
 */
@ExperimentalPagingApi
class HNStoryRemoteMediator(appDatabase: AppDatabase, private val apiService: HNApiService) : RemoteMediator<Int, Story>() {
    private val topStoryIdDao = appDatabase.topStoryIdDao()
    private val storyDao = appDatabase.storyDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Story>
    ): MediatorResult {
        Log.d(javaClass.name, state.pages.toString())
        return try {
            val nextTopStoryIds = when (loadType) {
                LoadType.REFRESH -> {
                    Log.d(javaClass.name, "refreshing")
                    topStoryIdDao.insertNew(apiService.topStoryIds())
                    topStoryIdDao.getFirstPage(state.config.pageSize)
                }
                LoadType.APPEND -> {
                    Log.d(javaClass.name, "appending")
                    val lastItem = storyDao.lastItemOrNull()
                    if (lastItem == null) {
                        Log.d(javaClass.name, "last item was null, no more to append")
                        return MediatorResult.Success(endOfPaginationReached = true)
                    }
                    topStoryIdDao.getNextPage(lastItem.id, state.config.pageSize)
                }
                LoadType.PREPEND -> {
                    Log.d(javaClass.name, "no need to prepend, end paging")
                    return MediatorResult.Success(endOfPaginationReached = true)
                }
            }

            if (nextTopStoryIds.isEmpty()) {
                Log.d(javaClass.name, "next ids are null, end paging")
                return MediatorResult.Success(endOfPaginationReached = true)
            }

            val stories = nextTopStoryIds.asFlow().map { topStoryId ->
                val hnStory = apiService.story(topStoryId.id)
                Story.fromHNStory(hnStory, topStoryId.index)
            }
                .buffer()
                .toList()
            storyDao.insertStories(stories)

            Log.d(javaClass.name, "inserted ${stories.size} items into paging")

            MediatorResult.Success(endOfPaginationReached = false)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }
}

@ExperimentalPagingApi
class HNCommentRemoteMediator(appDatabase: AppDatabase) : RemoteMediator<Int, Comment>() {
    private val commentDao = appDatabase.commentDao()

    override suspend fun load(
        loadType: LoadType,
        state: PagingState<Int, Comment>
    ): MediatorResult {
        return try {

            MediatorResult.Success(endOfPaginationReached = false)
        } catch (e: IOException) {
            MediatorResult.Error(e)
        } catch (e: HttpException) {
            MediatorResult.Error(e)
        }
    }
}