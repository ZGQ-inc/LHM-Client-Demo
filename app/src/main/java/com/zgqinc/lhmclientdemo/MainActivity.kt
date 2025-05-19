package com.zgqinc.lhmclientdemo

import android.content.Context
import android.net.ConnectivityManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.gson.Gson
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import com.zgqinc.lhmclientdemo.ui.theme.LHMClientDemoTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!isNetworkAvailable()) {
            Toast.makeText(this, "当前无网络连接，请检查网络设置", Toast.LENGTH_LONG).show()
        }

        setContent {
            LHMClientDemoTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    MainScreen()
                }
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        return cm.activeNetworkInfo?.isConnected == true
    }
}

// 图标
val iconMap: Map<String, Int>
    get() = mapOf(
        "Voltage" to R.drawable.ic_voltage,
        "Power" to R.drawable.ic_power,
        "Clock" to R.drawable.ic_clock,
        "Temperature" to R.drawable.ic_temp,
        "Load" to R.drawable.ic_load,
        "Fan" to R.drawable.ic_fan,
        "Throughput" to R.drawable.ic_throughput,
        "Data" to R.drawable.ic_data,
        "Level" to R.drawable.ic_level,
        "Control" to R.drawable.ic_control,
        "default" to R.drawable.ic_sensor
    )

val typeColorMap = mapOf(
    "Voltage" to Color(0xFF6750A4),       // Purple (primary)
    "Power" to Color(0xFFB3261E),         // Red (error)
    "Clock" to Color(0xFF006494),         // Blue (secondary-like)
    "Temperature" to Color(0xFFEF6C00),   // Deep orange (heat)
    "Load" to Color(0xFF5D1049),          // Plum (intensity)
    "Fan" to Color(0xFF018786),           // Teal (cooling)
    "Throughput" to Color(0xFF7D5260),    // Muted pink-brown (data)
    "Data" to Color(0xFF4E6056)           // Cool gray-green (stable)
)


@Composable
fun MainScreen(viewModel: HardwareViewModel = viewModel()) {
    var serverAddress by remember { mutableStateOf("") }
    var showAllParameters by remember { mutableStateOf(false) }

    if (showAllParameters && viewModel.hardwareJson != null) {
        AllParametersScreen(viewModel.hardwareJson!!) {
            showAllParameters = false
        }
        return
    }

    Column(
        modifier = Modifier
            .padding(32.dp)
            .fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        OutlinedTextField(
            value = serverAddress,
            onValueChange = { serverAddress = it },
            label = { Text("输入IP/域名") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = {
                if (serverAddress.isNotBlank()) {
                    viewModel.connectToServer(serverAddress)
                }
            },
            modifier = Modifier.padding(vertical = 16.dp)
        ) {
            Text("连接")
        }

        when (val status = viewModel.connectionStatus) {
            is ConnectionStatus.Error -> ErrorMessage(status.message)
            is ConnectionStatus.Connecting -> LoadingIndicator()
            is ConnectionStatus.Connected -> {
                viewModel.hardwareData?.let {
                    HardwareInfoDisplay(it)
                    Button(onClick = { showAllParameters = true }) {
                        Text("查看全部参数")
                    }
                }
            }
            ConnectionStatus.Disconnected -> {}
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllParametersScreen(data: JsonRoot, onBack: () -> Unit) {
    val expansionState = remember { ExpansionState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("全部参数") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "返回")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            item { Spacer(modifier = Modifier.height(8.dp)) }
            items(data.Children) { node ->
                NodeItem(node, expansionState, 0)
            }
            item { Spacer(modifier = Modifier.height(8.dp)) }
        }
    }
}


@Composable
private fun NodeItem(
    node: JsonRoot,
    expansionState: ExpansionState,
    indentLevel: Int
) {
    val hasChildren = node.Children.isNotEmpty()
    val isExpanded = expansionState.isExpanded(node.id)

    Card(
        modifier = Modifier
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .fillMaxWidth()
    ) {
        Column {
            Row(
                modifier = Modifier
                    .padding(start = (indentLevel * 24).dp)
                    .padding(16.dp)
                    .fillMaxWidth()
                    .clickable {
                        if (hasChildren) {
                            expansionState.toggle(node.id)
                        }
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasChildren) {
                    Icon(
                        painter = painterResource(
                            id = if (isExpanded) R.drawable.ic_arrow_down
                            else R.drawable.ic_arrow_right
                        ),
                        contentDescription = if (isExpanded) "收起" else "展开",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable { expansionState.toggle(node.id) }
                    )
                } else {
                    Spacer(modifier = Modifier.size(24.dp))
                }

                Icon(
                    painter = painterResource(id = iconMap[node.Type] ?: R.drawable.ic_sensor),
                    contentDescription = node.Type,
                    tint = typeColorMap[node.Type] ?: MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(
                        text = node.Text,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    if (node.Value != null) {
                        val values = listOfNotNull(
//                            node.Min?.takeIf { it.isNotEmpty() }?.let { "最小: $it" },
//                            node.Value?.takeIf { it.isNotEmpty() }?.let { "当前: $it" },
//                            node.Max?.takeIf { it.isNotEmpty() }?.let { "最大: $it" }
                            node.Value?.takeIf { it.isNotEmpty() }?.let { "$it" }
                        )

                        values.forEach { value ->
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            if (hasChildren && isExpanded) {
                node.Children.forEach { child ->
                    NodeItem(child, expansionState, indentLevel + 1)
                }
            }
        }
    }
}



@Composable
fun HardwareInfoDisplay(data: HardwareData) {
    Column(modifier = Modifier.padding(4.dp)) {
        InfoCard("设备", data.deviceName)
        InfoCard("CPU使用率", "${data.cpuUsage}%")
        InfoCard("GPU使用率", "${data.gpuUsage}%")
        InfoCard("内存使用", "${data.memoryUsed}/${data.memoryTotal} GB")
        InfoCard("网络速度", "↑${data.uploadSpeed} ↓${data.downloadSpeed}")
    }
}

@Composable
fun InfoCard(title: String, value: String) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun ErrorMessage(message: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        modifier = Modifier
            .padding(8.dp)
            .fillMaxWidth()
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun LoadingIndicator() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

class HardwareViewModel : ViewModel() {
    var hardwareData by mutableStateOf<HardwareData?>(null)
    var hardwareJson by mutableStateOf<JsonRoot?>(null)
    var connectionStatus by mutableStateOf<ConnectionStatus>(ConnectionStatus.Disconnected)

    private val client = OkHttpClient()
    private val gson = Gson()
    private var fetchJob: Job? = null

    fun connectToServer(address: String) {
        fetchJob?.cancel()
        hardwareData = null
        hardwareJson = null
        connectionStatus = ConnectionStatus.Connecting

        fetchJob = viewModelScope.launch {
            try {
                while (isActive) {
                    withContext(Dispatchers.IO) { fetchData(address) }
                    connectionStatus = ConnectionStatus.Connected
                    delay(1000)
                }
            } catch (e: IOException) {
                connectionStatus = ConnectionStatus.Error(e.message ?: "网络错误")
            }
        }
    }

    private suspend fun fetchData(address: String) {
        val host = address.trim().trimEnd('/')
        val urls = listOf(
            { "https://$host/data.json" },
            { "http://$host/data.json" },
            { "$host/data.json" }
        )
        var lastEx: IOException? = null
        for (u in urls) {
            try {
                client.newCall(Request.Builder().url(u()).build()).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val json = resp.body?.string().orEmpty()
                        val parsed = gson.fromJson(json, JsonRoot::class.java)
                        hardwareJson = parsed
                        hardwareData = parsed.toHardwareData()
                        return
                    } else {
                        lastEx = IOException("HTTP ${resp.code}")
                    }
                }
            } catch (e: IOException) {
                lastEx = e
            }
        }
        throw lastEx ?: IOException("Unknown fetch error")
    }

    private fun JsonRoot.toHardwareData(): HardwareData {
        return HardwareData(
            deviceName = Children.firstOrNull()?.Text ?: "Unknown Device",
            cpuUsage = findCpuUsage(),
            gpuUsage = findGpuUsage(),
            memoryUsed = findMemoryUsed(),
            memoryTotal = findMemoryTotal(),
            uploadSpeed = findNetworkSpeed("Upload"),
            downloadSpeed = findNetworkSpeed("Download")
        )
    }
}

// 数据结构
data class HardwareData(
    val deviceName: String,
    val cpuUsage: Float,
    val gpuUsage: Float,
    val memoryUsed: Float,
    val memoryTotal: Float,
    val uploadSpeed: String,
    val downloadSpeed: String
)

data class JsonRoot(
    val id: Int,
    val Text: String,
    val Min: String? = null,
    val Value: String? = null,
    val Max: String? = null,
    val ImageURL: String? = null,
    val SensorId: String? = null,
    val Type: String? = null,
    val Children: List<JsonRoot> = emptyList()
)

sealed class ConnectionStatus {
    object Disconnected : ConnectionStatus()
    object Connecting : ConnectionStatus()
    object Connected : ConnectionStatus()
    data class Error(val message: String) : ConnectionStatus()
}

// JSON解析
fun JsonRoot.findNodeByText(predicate: (String) -> Boolean): JsonRoot? {
    if (predicate(this.Text)) return this
    for (child in Children) {
        child.findNodeByText(predicate)?.let { return it }
    }
    return null
}

fun JsonRoot.findAllNodes(predicate: (JsonRoot) -> Boolean): List<JsonRoot> {
    val results = mutableListOf<JsonRoot>()
    if (predicate(this)) results.add(this)
    for (child in Children) {
        results.addAll(child.findAllNodes(predicate))
    }
    return results
}

fun JsonRoot.flatten(): List<JsonRoot> =
    listOf(this) + Children.flatMap { it.flatten() }

fun JsonRoot.findCpuUsage(): Float {
    return findNodeByText { it.contains("CPU Total", ignoreCase = true) }
        ?.Value?.replace("%", "")?.toFloatOrNull() ?: 0f
}

fun JsonRoot.findGpuUsage(): Float {
    return findAllNodes { it.Text.equals("GPU Core", ignoreCase = true) && it.Type == "Load" }
        .firstOrNull()?.Value?.replace("%", "")?.toFloatOrNull() ?: 0f
}

fun JsonRoot.findMemoryUsed(): Float {
    return findNodeByText { it.equals("Memory Used", ignoreCase = true) }
        ?.Value?.replace("GB", "")?.toFloatOrNull() ?: 0f
}

fun JsonRoot.findMemoryTotal(): Float {
    val used = findMemoryUsed()
    val available = findNodeByText { it.equals("Memory Available", ignoreCase = true) }
        ?.Value?.replace("GB", "")?.toFloatOrNull() ?: 0f
    return String.format("%.0f", used + available).toFloat()
}

fun JsonRoot.findNetworkSpeed(type: String): String {
    fun parseSpeed(value: String?): Float {
        if (value.isNullOrBlank()) return 0f
        val v = value.trim()
        return when {
            v.endsWith("GB/s", true) -> v.removeSuffix("GB/s").trim().toFloatOrNull()
                ?.times(1024 * 1024) ?: 0f

            v.endsWith("MB/s", true) -> v.removeSuffix("MB/s").trim().toFloatOrNull()?.times(1024)
                ?: 0f

            v.endsWith("KB/s", true) -> v.removeSuffix("KB/s").trim().toFloatOrNull() ?: 0f
            else -> 0f
        }
    }

    val nodes = flatten()
    val matching = nodes.filter { node ->
        node.Type == "Throughput" &&
                node.Text.contains(type, true) &&
                nodes.any {
                    it.Children.contains(node) &&
                            (it.Text.contains("WLAN", true) || it.Text.contains("以太网", true))
                }
    }

    val totalKB = matching.sumOf { parseSpeed(it.Value).toDouble() }

    return when {
        totalKB >= 1024 * 1024 -> String.format("%.1f GB/s", totalKB / 1024f / 1024f)
        totalKB >= 1024 -> String.format("%.1f MB/s", totalKB / 1024f)
        else -> String.format("%.0f KB/s", totalKB)
    }
}

class ExpansionState {
    private val _expandedIds = mutableSetOf<Int>()
    fun isExpanded(id: Int): Boolean = _expandedIds.contains(id)
    fun toggle(id: Int) {
        if (_expandedIds.contains(id)) {
            _expandedIds.remove(id)
        } else {
            _expandedIds.add(id)
        }
    }
}
