
import kotlinx.coroutines.flow.Flow
import kotlinx.rpc.RPC
import kotlinx.serialization.Serializable

@Serializable
data class UserData(
    val address: String,
    val lastName: String,
)

interface UserService : RPC {
    suspend fun hello(platform: String, userData: UserData): String

    suspend fun subscribeToNews(): Flow<String>

    suspend fun ping() : String
}
