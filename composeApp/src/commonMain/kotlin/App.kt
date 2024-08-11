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
import kotlinx.coroutines.delay
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
	var service: UserService? by remember { mutableStateOf(null) }
	var refresh by remember { mutableStateOf(false) }
	var connected by remember { mutableStateOf(false) }
	var errorState by remember { mutableStateOf<String?>(null) }

	var greeting by remember { mutableStateOf<String?>(null) }
	val news = remember { mutableStateListOf<String>() }

	// Connect & Ping the server
	LaunchedEffect(Unit) {
		while (true) {

			// Attempt to (re)connect to the server.
			while (!connected) {
				delay(1000)

				try {
					service = setupRPC()
					connected = true
					errorState = null
				} catch (e: Exception) {
					errorState = e.message
					connected = false
				}
			}

			// Ping the server every second.
			service?.let {
				try {
					while (true) {
						println(service?.ping())
						delay(1000)
					}
				} catch (e: Exception) {
					connected = false
					errorState = e.message + ", trying to reconnect..."
				}
			}
		}
	}

	service?.also { serviceNotNull ->

		// Simulate a server call with a simple return value.
		LaunchedEffect(serviceNotNull) {
			greeting = serviceNotNull.hello(
				"using ${getPlatform().name} platform",
				UserData("Austin", "Athanas")
			)
		}

		// Refresh the news stream.
		LaunchedEffect(Unit) {
			refresh = true
		}

		// Simulate a server-sent stream.
		LaunchedEffect(refresh) {
			streamScoped {
				serviceNotNull.subscribeToNews().collect { article ->
					news.add(article)
				}
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
			horizontalAlignment = Alignment.Start
		) {
			greeting?.let {
				Text(it)
			} ?: run {
				Text("Establishing server connection...")
			}

			errorState?.let {
				Text(
					"Error: $it",
					color = Color.White,
					modifier = Modifier.background(Color.Red)
				)
			}

			Button(onClick = {
				refresh = !refresh
			}) {
				Text("Get More articles")
			}

			news.forEach {
				Text("Article: $it")
			}
		}
	}
}

suspend fun setupRPC(): UserService = client.rpc {
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
