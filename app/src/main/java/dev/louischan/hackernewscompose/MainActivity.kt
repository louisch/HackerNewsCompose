package dev.louischan.hackernewscompose

import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.AndroidViewModel
import androidx.paging.*
import androidx.paging.compose.collectAsLazyPagingItems
import androidx.paging.compose.items
import dev.louischan.hackernewscompose.ui.theme.HackerNewsComposeTheme
import kotlinx.coroutines.launch
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.safety.Safelist
import org.jsoup.select.NodeVisitor

@ExperimentalMaterialApi
@ExperimentalPagingApi
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val model: MainViewModel by viewModels()

        setContent {
            HackerNewsComposeTheme {
                NewsRoot(model = model)
            }
        }
    }
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    val db = AppDatabase.getInstance(application)
}

@ExperimentalPagingApi
@ExperimentalMaterialApi
@Composable
fun NewsRoot(model: MainViewModel) {
    val scaffoldState = rememberScaffoldState()
    val coroutineScope = rememberCoroutineScope()

    val storyDao = model.db.storyDao()
    val commentDao = model.db.commentDao()

    val apiService = HNApiService.buildService()
    val pagerConfig = PagingConfig(pageSize = 30)
    val storyPager = Pager(config = pagerConfig,
        remoteMediator = HNStoryRemoteMediator(model.db, apiService)) {
        storyDao.pagingSource()
    }

    val (storyForComments, setStoryForComments) = remember { mutableStateOf<Story?>(null) }
    val (commentPager, setCommentPager) = remember { mutableStateOf<Pager<Int, Comment>?>(null) }
    LaunchedEffect(storyForComments) {
        coroutineScope.launch {
            if (storyForComments == null) {
                setCommentPager(null)
                return@launch
            }

            val storyWithComments = storyDao.getStoryWithCommentsById(storyForComments.id)
            if (storyWithComments == null) {
                setCommentPager(null)
                return@launch
            }

            setCommentPager(
                Pager(
                    config = pagerConfig,
                    remoteMediator = HNCommentRemoteMediator(
                        storyWithComments,
                        model.db,
                        apiService
                    )
                ) {
                    commentDao.pagingSource()
                }
            )
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(
                        onClick = {
                            if (commentPager == null) {
                                coroutineScope.launch { scaffoldState.drawerState.open() }
                            } else {
                                setStoryForComments(null)
                            }
                        }
                    ) {
                        if (commentPager == null) {
                            Icon(
                                Icons.Filled.Menu,
                                contentDescription = stringResource(R.string.topAppBarOpenMenuContentDescription)
                            )
                        } else {
                            Icon(Icons.Filled.ArrowBack, contentDescription = stringResource(R.string.prevMenuContentDescription))
                        }
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
            Text("Line two")
        },
        content = { innerPadding ->
            if (commentPager == null) {
                NewsList(storyPager, innerPadding, setStoryForComments)
            } else {
                NewsComments(commentPager, innerPadding)
            }
        }
    )
}

@ExperimentalMaterialApi
@Composable
fun NewsList(pager: Pager<Int, Story>, innerPadding: PaddingValues, onSelectComments: (Story?) -> Unit) {
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
                NewsRow(item, onSelectComments)
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
fun NewsRow(story: Story, onSelectComments: (Story?) -> Unit) {
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
            Button(onClick = { onSelectComments(story) }, colors = ButtonDefaults.outlinedButtonColors()) {
                Text("Comments")
            }
        }
    }
    Divider()
}

@ExperimentalMaterialApi
@Composable
fun NewsComments(pager: Pager<Int, Comment>, innerPadding: PaddingValues) {
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
                CommentRow(item)
            } else {
                CommentPlaceholder()
            }
        }
    }
}

@Composable
fun CommentPlaceholder() {
    Text("Loading comment, please wait...")
}

@ExperimentalMaterialApi
@Composable
fun CommentRow(item: Comment) {
    if (!item.isDeleted) {
        ListItem(
            modifier = Modifier.padding(bottom = 16.dp),
            text = { CommentText(item.text) },
            overlineText = { Text(item.author) },
            secondaryText = { Text(item.timePosted.toString()) }
        )
    } else {
        ListItem(text = { Text("<deleted comment>") })
    }
    Divider()
}

fun printParent(node: Node): String {
    val parent = node.parent()
    return if (parent != null) "parent: ${parent.nodeName()}" else "no parent"
}

fun printCommentNode(node: Node): String {
    return when (node) {
        is TextNode -> {
            "#text $node"
        }
        is Element -> {
            val nodeName = node.nodeName()
            "element $nodeName with ${node.childNodeSize()} children"
        }
        else -> {
            "other ${node.nodeName()}"
        }
    }
}

@Composable
fun CommentText(textWithHtml: String) {
    val cleanTextWithHtml = Jsoup.clean(textWithHtml, Safelist.basic())
    val textAsNode = Jsoup.parseBodyFragment(cleanTextWithHtml).body()

    Log.d("DEBUG NODE VISITOR", "comment text: $cleanTextWithHtml")

    val visitor = object : NodeVisitor {
        val visitedNodes = mutableSetOf<NodeToCompose>()
        val stackOfParents = mutableListOf<Node>()

        override fun head(node: Node, depth: Int) {
//            if (stackOfParents.isNotEmpty()) {
//                Log.d("DEBUG NODE VISITOR", "head for ${node.hashCode()} matching: ${stackOfParents.lastOrNull() == node.parentNode()} p: ${node.parentNode().hashCode()} sp: ${stackOfParents.lastOrNull().hashCode()} details: ${printCommentNode(node)} depth: $depth")
//            } else {
//                Log.d("DEBUG NODE VISITOR", "head for ${node.hashCode()} details: ${printCommentNode(node)} depth: $depth")
//            }

            if (node is Element) {
                stackOfParents.add(node)
                visitedNodes.add(ElementHeadToCompose(node))
            } else if (node is TextNode) {
                visitedNodes.add(TextNodeToCompose(node.wholeText))
            }
        }

        override fun tail(node: Node, depth: Int) {
//            Log.d("DEBUG NODE VISITOR", "tail for ${node.hashCode()}")
            if (stackOfParents.lastOrNull() == node) {
                stackOfParents.removeLast()
            }

            if (node is Element) {
                visitedNodes.add(ElementTailToCompose(node))
            }
        }
    }
    textAsNode.traverse(visitor)

    // HN Comment formatting notes:
    // It seems that HN Comments use only a limited number of HTML tags for formatting.
    // - <p> tags are used primarily just to create paragraphs (i.e. newlines). The first line of a
    //   comment is always a bare text node and not wrapped in any tag, but every line proceeding is
    //   wrapped in a <p> tag (with some exceptions noted below)
    // - <pre> tags wrapping a <code> tag are used for verbatim/quoted/code text. It seems that the
    //   line following a <pre> tag is not wrapped in a <p> tag but is instead a bare text node.
    //   Additionally, an empty <p></p> element is always inserted before a <pre> tag. (To create a
    //   newline in the comment presumably)
    // - <i> tags are used to create inline italicized text.
    // - <a> tags are used to create inline URLs.
    val annotatedText = buildAnnotatedString {
        val currentStyleContext = mutableListOf<String>()
        for ((index, visitedNode) in visitor.visitedNodes.withIndex()) {
            when (visitedNode) {
                is ElementHeadToCompose -> {
                    val tagName = visitedNode.element.tagName()

                    val annotation = if (tagName == "a") {
                        visitedNode.element.attr("href")
                    } else {
                        "other"
                    }
                    pushStringAnnotation(tag = tagName, annotation = annotation)

                    if (tagName in styleContexts) {
                        currentStyleContext.add(tagName)
                    }
                }
                is ElementTailToCompose -> {
                    val tagName = visitedNode.element.tagName()

                    if (tagName == "pre" && index < visitor.visitedNodes.size - 2) {
                        append("\n")
                    }

                    pop()

                    if (tagName in styleContexts) {
                        currentStyleContext.removeLast()
                    }
                }
                is TextNodeToCompose -> {
                    val spanStyle = (when {
                        currentStyleContext.lastOrNull() == "i" -> {
                            SpanStyle(fontStyle = FontStyle.Italic)
                        }
                        currentStyleContext.lastOrNull() == "code" -> {
                            SpanStyle(fontFamily = FontFamily.Monospace)
                        }
                        else -> {
                            SpanStyle()
                        }
                    }).merge(
                        if (currentStyleContext.contains("a")) {
                            SpanStyle(color = Color.Blue)
                        } else {
                            SpanStyle()
                        }
                    )

                    withStyle(spanStyle) {
                        append(visitedNode.text)
                    }
                }
            }
        }
    }
    ClickableText(
        text = annotatedText,
        onClick = { offset ->
            annotatedText.getStringAnnotations(tag = "a", start = offset, end = offset).firstOrNull()?.let { annotation ->
                Log.d("Clicked URL", annotation.item)
            }
        }
    )
}

val styleContexts = setOf("a", "i", "code")

interface NodeToCompose

data class ElementHeadToCompose(val element: Element) : NodeToCompose

data class ElementTailToCompose(val element: Element) : NodeToCompose

data class TextNodeToCompose(val text: String) : NodeToCompose