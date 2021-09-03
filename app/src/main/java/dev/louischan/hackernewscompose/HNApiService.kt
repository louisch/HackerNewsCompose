package dev.louischan.hackernewscompose

import android.util.Log
import androidx.compose.ui.Modifier
import com.google.gson.*
import com.google.gson.annotations.SerializedName
import okhttp3.RequestBody
import okhttp3.ResponseBody
import org.jsoup.Jsoup
import org.jsoup.nodes.Element
import org.jsoup.nodes.Node
import org.jsoup.nodes.TextNode
import org.jsoup.select.NodeVisitor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.lang.reflect.Type
import java.sql.Timestamp

interface HNApiService {
    companion object {
        fun buildService(): HNApiService {
            val gson = GsonBuilder()
                .registerTypeAdapter(Timestamp::class.java, TimestampDeserializer())
                .create()
            return Retrofit.Builder()
                .baseUrl("https://hacker-news.firebaseio.com")
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(HNApiService::class.java)
        }
    }

    @GET("v0/topstories.json")
    suspend fun topStoryIds(): List<Long>

    @GET("v0/item/{id}.json")
    suspend fun story(@Path("id") id: Long): HNStory

    @GET("v0/item/{id}.json")
    suspend fun comment(@Path("id") id: Long): HNComment
}

interface HNWebService {
    companion object {
        fun buildService(): HNWebService {
            return Retrofit.Builder()
                .baseUrl("https://news.ycombinator.com")
                .addConverterFactory(ScalarsConverterFactory.create())
                .build()
                .create(HNWebService::class.java)
        }
    }

    @GET("news")
    suspend fun news(@Query("p") page: Int = 1): String
}

data class HNStory (
    val id: Long,
    val title: String,
    val time: Timestamp,
    @SerializedName("by") val author: String,
    val content: String?,
    val deleted: Boolean,
    val dead: Boolean,
    val url: String?,
    val score: Int,
    val descendants: Int,
    val poll: Int?,
    val kids: List<Long>?,
)

data class HNComment (
    val id: Long,
    val parent: Long,
    @SerializedName("by") val author: String?,
    val text: String?,
    @SerializedName("deleted") val isDeleted: Boolean?,
    val kids: List<Long>?,
    val time: Timestamp,
)

class TimestampDeserializer : JsonDeserializer<Timestamp> {
    override fun deserialize(json: JsonElement?, typeOfT: Type?, context: JsonDeserializationContext?): Timestamp {
        if (json == null) {
            throw JsonParseException("Cannot deserialize null into HNType!")
        }
        val seconds = json.asLong
        return Timestamp(seconds * 1000)
    }
}

open class ComposeHTMLNode

data class ComposeHTMLElement(
    val modifier: Modifier,
    val children: List<ComposeHTMLNode>,
) : ComposeHTMLNode()

data class ComposeHTMLTextNode(
    val text: String,
) : ComposeHTMLNode()

class ComposeHTMLNothing : ComposeHTMLNode()

class JsoupNodeConverter : Converter.Factory() {
    override fun requestBodyConverter(
        type: Type,
        parameterAnnotations: Array<out Annotation>,
        methodAnnotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, RequestBody>? {
        return super.requestBodyConverter(type, parameterAnnotations, methodAnnotations, retrofit)
    }

    override fun responseBodyConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<ResponseBody, ComposeHTMLNode> {
        return Converter<ResponseBody, ComposeHTMLNode> { body ->
            val jsoup = Jsoup.parse(body.string())
            var rootNode: ComposeHTMLNode? = null

            jsoup.traverse(object : NodeVisitor {
                override fun head(node: Node, depth: Int) {
                    val asComposeNode = when (node) {
                        is TextNode -> {
                            ComposeHTMLTextNode(node.text())
                        }
                        is Element -> {
                            ComposeHTMLElement(modifier = Modifier, children = listOf())
                        }
                        else -> {
                            ComposeHTMLNothing()
                        }
                    }
                    if (rootNode == null) {
                        rootNode = asComposeNode
                    }

                    Log.d(javaClass.name, "head: $node   depth: $depth")
                }

                override fun tail(node: Node, depth: Int) {
                    Log.d(javaClass.name, "tail: $node   depth: $depth")
                }
            })
            rootNode
        }
    }

    override fun stringConverter(
        type: Type,
        annotations: Array<out Annotation>,
        retrofit: Retrofit
    ): Converter<*, String>? {
        return super.stringConverter(type, annotations, retrofit)
    }
}