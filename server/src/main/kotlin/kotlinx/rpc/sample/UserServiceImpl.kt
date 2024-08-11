
package kotlinx.rpc.sample

import UserData
import UserService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.CoroutineContext
import kotlin.random.Random

class UserServiceImpl(override val coroutineContext: CoroutineContext) : UserService {

    override suspend fun hello(platform: String, userData: UserData): String {
        return "$platform: Nice to meet you ${userData.lastName}, " +
                "how is it in ${userData.address}?"
    }

    override suspend fun subscribeToNews(): Flow<String> {
        return flow {
            repeat(10) { count ->
                delay(300)
                emit("${count+1}. " +
                        articleTitles[Random.nextInt(articleTitles.size)]
                )
            }
        }
    }

    var count = 0
    override suspend fun ping() : String {
        return "pong ${count++}"
    }

    // Simulate a database of articles
    companion object {
        val articleTitles = listOf(
            "The Day the Earth Stood Still",
            "The Birth of a New Species",
            "The Origin Story of Our World",
            "The Power of Positive Thinking",
            "The Science of Love",
            "The Magic of Sleep",
            "The Art of Reading",
            "The Future of Work",
            "The Science of Happiness",
            "The Mystery of the Old Man",
            "The Power of Positive Energy",
            "The Science of Time",
            "The Art of Music",
            "The Magic of Art",
            "The Science of Space",
        )
    }
}
