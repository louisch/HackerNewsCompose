package dev.louischan.hackernewscompose

import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.paging.*
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import dev.louischan.hackernewscompose.ui.theme.HackerNewsComposeTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @ExperimentalMaterialApi
    @ExperimentalPagingApi
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val model: MainViewModel by viewModels()
        val storyDao = model.db.storyDao()
        val commentDao = model.db.commentDao()
        val apiService = HNApiService.buildService()
        val pagerConfig = PagingConfig(pageSize = 30)
        val storyPager = Pager(config = pagerConfig,
                               remoteMediator = HNStoryRemoteMediator(model.db, apiService)) {
            storyDao.pagingSource()
        }
        val commentPager = Pager(config = pagerConfig, remoteMediator = HNCommentRemoteMediator(model.db)) {
            commentDao.pagingSource()
        }

        setContent {
            HackerNewsComposeTheme {
                NewsRoot(storyPager = storyPager, commentPager = commentPager)
            }
        }
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val db = AppDatabase.getInstance(application)
}

enum class NavigationState {
    NEWS,
    COMMENTS
}

@ExperimentalMaterialApi
@Composable
fun NewsRoot(storyPager: Pager<Int, Story>, commentPager: Pager<Int, Comment>) {
    val scaffoldState = rememberScaffoldState()
    val navigationState = NavigationState.NEWS
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch { scaffoldState.drawerState.open() }
                        }
                    ) {
                        Icon(Icons.Filled.Menu, contentDescription = stringResource(R.string.topAppBarOpenMenuContentDescription))
                    }
                },
                actions = {
                    IconButton(onClick = {}) {
                        Icon(Icons.Filled.Person, contentDescription = stringResource(R.string.topAppBarAccountContentDescription))
                    }
                }
            )
        },
        drawerContent = {
            Text("Drawer content")
        },
        content = { innerPadding ->
            when (navigationState) {
                NavigationState.NEWS -> {
                    NewsList(storyPager, innerPadding)
                }
                NavigationState.COMMENTS -> {
                    NewsComments(commentPager, innerPadding)
                }
            }
        }
    )
}

@ExperimentalMaterialApi
@Composable
fun NewsList(pager: Pager<Int, Story>, innerPadding: PaddingValues) {
    val lazyPagingItems = pager.flow.collectAsLazyPagingItems()

    LazyColumn(contentPadding = innerPadding) {
        lazyPagingItems.apply {
            when {
                loadState.refresh == LoadState.Loading -> {
                    // TODO("add UI for refreshing items")
                }
                loadState.refresh is LoadState.Error -> {
                    val message = (loadState.refresh as LoadState.Error).error.localizedMessage ?: "null error message"
                    item { Text(message, color = Color.Red) }
                }
                loadState.append == LoadState.Loading -> {
                    // TODO("add UI for appending items")
                }
                loadState.append is LoadState.Error -> {
                    val message = (loadState.append as LoadState.Error).error.localizedMessage ?: "null error message"
                    item { Text(message, color = Color.Red) }
                }
            }
        }

        items(lazyPagingItems) { item ->
            if (item != null) {
                NewsRow(item)
            } else {
                NewsPlaceholder()
            }
        }
    }
}

@Composable
fun NewsPlaceholder() {
    Text(text = "Loading item, please wait...")
}

@ExperimentalMaterialApi
@Composable
fun NewsRow(story: Story) {
    Column {
        ListItem(
            text = { Text(text = story.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            secondaryText = {
                Column {
                    Row {
                        Text(text = story.timePosted)
                    }
                }
            },
            icon = { IconButton(onClick = { }) { Text(text = story.score.toString()) } },
        )
        Row(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
            Button(onClick = {}, colors = ButtonDefaults.outlinedButtonColors()) {
                Text("Comments")
            }
        }
    }
    Divider()
}

@Composable
fun NewsComments(pager: Pager<Int, Comment>, innerPadding: PaddingValues) {
    LazyColumn(contentPadding = innerPadding) {

    }
}