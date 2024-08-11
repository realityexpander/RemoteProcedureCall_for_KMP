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
import io.ktor.client.plugins.timeout
import io.ktor.http.*
import kotlinx.coroutines.delay
import kotlinx.rpc.client.withService
import kotlinx.rpc.serialization.json
import kotlinx.rpc.streamScoped
import kotlinx.rpc.transport.ktor.client.installRPC
import kotlinx.rpc.transport.ktor.client.rpc
import kotlinx.rpc.transport.ktor.client.rpcConfig

const val DEV_SERVER_HOST: String = "192.168.1.69"

val client by lazy {
	HttpClient {
		installRPC()
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
			json {
				ignoreUnknownKeys = true
				isLenient = true
			}
		}
		timeout {
			requestTimeoutMillis = 3000
			socketTimeoutMillis = 3000
			connectTimeoutMillis = 3000
		}

		waitForServices = true
	}
}.withService()


@Composable
fun App() {
	var service: UserService? by remember { mutableStateOf(null) }
	var refresh by remember { mutableStateOf(false) }
	var connected by remember { mutableStateOf(false) }
	var errorState by remember { mutableStateOf<String?>(null) }

	var greeting by remember { mutableStateOf<String?>(null) }
	val news = remember { mutableStateListOf<String>() }

	// Connect & Ping the RPC server
	LaunchedEffect(Unit) {
		while (true) {

			// Attempt to (re)connect to the RPC server.
			while (!connected) {
				try {
					service = setupRPC()
					connected = true
				} catch (e: Exception) {
					errorState = e.message
					connected = false
					println(e.message)
				}

				delay(2000) // Wait 2 seconds before trying to reconnect.
			}

			// Ping the RPC server every second.
			service?.let {
				errorState = null
				try {m
					var count = 0
					while (true) {
						println(service?.ping())
//						println(service?.ping(count++)) // test broken API

						delay(1000)
					}
				} catch (e: Exception) {
					connected = false
					errorState = e.message + ", trying to reconnect..."
					println(e.message)
				}
			}
		}
	}

	service?.also { serviceNotNull ->

		// Simulate a server call with a simple return value.
		LaunchedEffect(Unit) {
			greeting = serviceNotNull.hello(
				"${getPlatform().name} platform",
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
			// Display the greeting if there is one.
			greeting?.let {
				Text(it)
			} ?: run {
				Text("Establishing server connection...")
			}

			// Display the error state if there is one.
			errorState?.let {
				Text(
					"Error: $it",
					color = Color.White,
					modifier = Modifier.background(Color.Red)
				)
			}

			// Display & Load more news articles.
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
