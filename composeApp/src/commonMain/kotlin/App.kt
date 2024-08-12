import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
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

suspend fun setupRPC(): NewsService = client.rpc {
	url {
		host = DEV_SERVER_HOST
		port = 8080
		encodedPath = "/api"
	}

	headers {
		append(HttpHeaders.Authorization, "Basic YWRtaW46YWRtaW4=")
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
	var service: NewsService? by remember { mutableStateOf(null) }
	var shouldRefreshFeed by remember { mutableStateOf(false) }
	var connected by remember { mutableStateOf(false) }
	var errorState by remember { mutableStateOf<String?>(null) }

	var greeting by remember { mutableStateOf<String?>(null) }
	val articles = remember { mutableStateListOf<String>() }
	var topic by remember { mutableStateOf("Science") }

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
				try {
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
		// Create a server call with a simple return value.
		LaunchedEffect(Unit) {
			greeting = serviceNotNull.hello(
				"${getPlatform().name} platform",
				UserData("Austin", "Athanas")
			)
		}

		// Refresh the article stream.
		LaunchedEffect(Unit) {
			shouldRefreshFeed = true
		}

		// Create a server-sent stream of articles.
		LaunchedEffect(shouldRefreshFeed) {
			streamScoped {
				if(topic.isBlank()) {
					serviceNotNull.subscribeToNews().collect { article ->
						articles.add(article)
					}
				} else {
					articles.clear()
					serviceNotNull.subscribeToTopic(topic).collect { article ->
						articles.add(article)
					}
				}
			}
		}
	}

	// User Interface
	MaterialTheme(
		colors = MaterialTheme.colors.copy(
			background = Color.Black,
		),
		typography = MaterialTheme.typography.copy(
			body1 = MaterialTheme.typography.body1.copy(color = Color.White)
		)
	) {
		Column(
			Modifier.fillMaxSize()
				.background(Color.Black)
				.verticalScroll(rememberScrollState())
			,
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

			Row(
				verticalAlignment = Alignment.CenterVertically
			) {
				// Text entry for a topic to subscribe to.
				TextField(
					value = topic,
					onValueChange = { topic = it },
					label = { Text("Topic") }
				)
				Spacer(modifier = Modifier.width(8.dp))

				// Display & Load more news articles.
				Button(onClick = {
					shouldRefreshFeed = !shouldRefreshFeed
				}) {
					if (topic.isBlank()) {
						Text("Get More News")
					} else {
						Text("Search for $topic")
					}
				}
			}

			Divider(modifier = Modifier.background(Color.Gray))

			// Display the articles.
			articles.forEach { article ->
				Text(article)
			}
		}
	}
}
