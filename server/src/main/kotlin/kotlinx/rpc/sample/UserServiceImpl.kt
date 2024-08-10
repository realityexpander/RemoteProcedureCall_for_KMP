
package kotlinx.rpc.sample

import UserData
import UserService
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlin.coroutines.CoroutineContext

class UserServiceImpl(override val coroutineContext: CoroutineContext) : UserService {
    override suspend fun hello(user: String, userData: UserData): String {
        return "Nice to meet you $user, how is it in ${userData.address}?"
    }

    override suspend fun subscribeToNews(): Flow<String> {
        return flow {
            repeat(10) {
                delay(300)
                emit("Article number $it")
            }
        }
    }
}
