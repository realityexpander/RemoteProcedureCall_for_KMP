
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import io.ktor.client.*
import io.ktor.http.*
import kotlinx.rpc.client.withService
import kotlinx.rpc.serialization.json
import kotlinx.rpc.streamScoped
import kotlinx.rpc.transport.ktor.client.installRPC
import kotlinx.rpc.transport.ktor.client.rpc
import kotlinx.rpc.transport.ktor.client.rpcConfig

expect val DEV_SERVER_HOST: String

val client by lazy {
    HttpClient {
        installRPC()
    }
}

@Composable
fun App() {
    var serviceOrNull: UserService? by remember { mutableStateOf(null) }
    var refresh by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        serviceOrNull = client.rpc {
            url {
                host = DEV_SERVER_HOST
                port = 8080
                encodedPath = "/api"
            }

            rpcConfig {
                serialization {
                    json()
                }
            }
        }.withService()
    }

    val service = serviceOrNull // for smart casting

    if (service != null) {
        var greeting by remember { mutableStateOf<String?>(null) }
        val news = remember { mutableStateListOf<String>() }

        LaunchedEffect(service) {
            greeting = service.hello(
                "User using ${getPlatform().name} platform",
                UserData("Austin", "Athanas")
            )
        }

        LaunchedEffect(service, refresh) {
            streamScoped {
                service.subscribeToNews().collect { article ->
                    news.add(article)
                }
            }
        }

        MaterialTheme(
            colors = MaterialTheme.colors.copy(
                background = Color.Black,
            ),
            typography = MaterialTheme.typography.copy(body1 = MaterialTheme.typography.body1.copy(color = Color.White))
        ) {
            var showIcon by remember { mutableStateOf(false) }

            Column(
                Modifier.fillMaxSize()
                    .background(Color.Black),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                greeting?.let {
                    Text(it)
                } ?: run {
                    Text("Establishing server connection...")
                }

                Button(onClick = {
                    news.clear()
                    refresh = !refresh
                }) {
                    Text("Refresh articles")
                }

                news.forEach {
                    Text("Article: $it")
                }
            }
        }
    }
}
