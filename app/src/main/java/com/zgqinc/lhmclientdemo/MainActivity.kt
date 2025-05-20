@file:OptIn(ExperimentalMaterial3Api::class)

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
import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.with
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.AndroidViewModel
import java.util.Collections
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.ui.unit.sp


class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
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
    "Voltage" to Color(0xFF6750A4),       // Primary (明亮紫)
    "Power" to Color(0xFFDC362E),         // Error (明亮红)
    "Clock" to Color(0xFF4285F4),         // Secondary (Google蓝)
    "Temperature" to Color(0xFFFFB74D),   // Tertiary/橙黄（暖热）
    "Load" to Color(0xFF9C27B0),          // 强度感的紫色
    "Fan" to Color(0xFF00ACC1),           // 冷却感的青色
    "Throughput" to Color(0xFF8E24AA),    // 数据传输感的洋红
    "Data" to Color(0xFF607D8B)           // 稳定、沉着的灰蓝
)



private const val AnimationDurationMillis = 300

@OptIn(ExperimentalAnimationApi::class)
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
@Composable
fun MainScreen(viewModel: HardwareViewModel = viewModel()) {
    var serverAddress by remember { mutableStateOf("") }
    var showAllParameters by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val deviceHistory = remember { viewModel.getDeviceHistory() }
    var showAboutDialog by remember { mutableStateOf(false) }

    val AnimationDurationMillis = 300

    LaunchedEffect(Unit) {
        viewModel.getLastDevice()?.let {
            serverAddress = it
            viewModel.connectToServer(it)
        }
    }

    AnimatedContent(
        targetState = showAllParameters,
        transitionSpec = {
            if (targetState) {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(AnimationDurationMillis)
                ) + fadeIn() with
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(AnimationDurationMillis)
                        ) + fadeOut()
            } else {
                slideIntoContainer(
                    AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(AnimationDurationMillis)
                ) + fadeIn() with
                        slideOutOfContainer(
                            AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(AnimationDurationMillis)
                        ) + fadeOut()
            }
        },
        modifier = Modifier.fillMaxSize()
    ) { showAll ->
        if (showAll && viewModel.hardwareJson != null) {
            AllParametersScreen(viewModel.hardwareJson!!) {
                showAllParameters = false
            }
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp)
                        .padding(bottom = 80.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ExposedDropdownMenuBox(
                        expanded = menuExpanded,
                        onExpandedChange = { menuExpanded = it },
                        modifier = Modifier.animateContentSize()
                    ) {
                        OutlinedTextField(
                            value = serverAddress,
                            onValueChange = { serverAddress = it },
                            label = { Text("设备IP/域名") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                                .clickable { menuExpanded = true },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = menuExpanded)
                            }
                        )

                        ExposedDropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            deviceHistory.forEach { device ->
                                DropdownMenuItem(
                                    text = { Text(device) },
                                    onClick = {
                                        serverAddress = device
                                        menuExpanded = false
                                        viewModel.connectToServer(device)
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    when (val status = viewModel.connectionStatus) {
                        is ConnectionStatus.Error -> ErrorMessage(status.message)
                        is ConnectionStatus.Connecting -> LoadingIndicator()
                        is ConnectionStatus.Connected -> {
                            viewModel.hardwareData?.let {
                                HardwareInfoDisplay(it)
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { showAllParameters = true }) {
                                    Text("查看全部参数")
                                }
                            }
                        }
                        ConnectionStatus.Disconnected -> {}
                    }
                }

                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 32.dp)
                ) {
                    Button(
                        onClick = { showAboutDialog = true },
                        modifier = Modifier.height(36.dp)
                    ) {
                        Text("关于", fontSize = 14.sp)
                    }
                }
            }
        }
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("关于") },
            text = { Text("没想好写什么。") },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllParametersScreen(data: JsonRoot, onBack: () -> Unit) {
    val density = LocalDensity.current
    var visible by remember { mutableStateOf(true) }
    var backClicked by remember { mutableStateOf(false) }
    var backDisabled by remember { mutableStateOf(false) }

    if (backClicked) {
        LaunchedEffect(Unit) {

            delay(AnimationDurationMillis.toLong())
            onBack()
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally { with(density) { 300.dp.roundToPx() } } + fadeIn(),
        exit = slideOutHorizontally { with(density) { -300.dp.roundToPx() } } + fadeOut()
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("全部参数") },
                    navigationIcon = {
                        IconButton(
                            onClick = {
                                if (!backDisabled) {
                                    backDisabled = true
                                    visible = false
                                    backClicked = true
                                }
                            },
                            enabled = !backDisabled
                        ) {
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
                    NodeItem(node, remember { ExpansionState() }, 0)
                }
                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
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

    Column(
        modifier = Modifier
            .animateContentSize() // 对整块做动画
            .fillMaxWidth()
    ) {
        Card(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .fillMaxWidth()
                .clickable(enabled = hasChildren) { expansionState.toggle(node.id) }
                .animateContentSize(), // 点击卡片动画过渡
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(start = (indentLevel * 24).dp)
                    .padding(16.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasChildren) {
                    Icon(
                        painter = painterResource(
                            id = if (isExpanded) R.drawable.ic_arrow_down else R.drawable.ic_arrow_right
                        ),
                        contentDescription = if (isExpanded) "收起" else "展开",
                        modifier = Modifier.size(24.dp)
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
                    Text(node.Text, style = MaterialTheme.typography.titleMedium)
                    node.Value?.takeIf { it.isNotEmpty() }?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium)
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




@Composable
fun HardwareInfoDisplay(
    data: HardwareData,
//    modifier: Modifier = Modifier
) {
    Column(modifier = Modifier.padding(4.dp)) {
        InfoCard("设备", data.deviceName)
        InfoCard("CPU使用率", "${data.cpuUsage}%")
        InfoCard("GPU使用率", "${data.gpuUsage}%")
        InfoCard("内存使用", "${data.memoryUsed}/${data.memoryTotal} GB")
        InfoCard("网络速度", "↑${data.uploadSpeed} ↓${data.downloadSpeed}")
    }
}

@Composable
fun InfoCard(
    title: String,
    value: String,
    visible: Boolean = true
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it / 2 }) + fadeIn(),
        exit = slideOutHorizontally(targetOffsetX = { it }) + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .padding(8.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
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
                    style = MaterialTheme.typography.bodyLarge
                )
            }
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
        Text(message, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(16.dp))
    }
}

@Composable
fun LoadingIndicator() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator()
    }
}

class HardwareViewModel(application: Application) : AndroidViewModel(application) {
    var hardwareData by mutableStateOf<HardwareData?>(null)
    var hardwareJson by mutableStateOf<JsonRoot?>(null)
    var connectionStatus by mutableStateOf<ConnectionStatus>(ConnectionStatus.Disconnected)

    private val client = OkHttpClient()
    private val gson = Gson()
    private var fetchJob: Job? = null
    private val prefs = application.getSharedPreferences("device_prefs", Context.MODE_PRIVATE)

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun saveDeviceHistory(address: String) {
        val history = getDeviceHistory().toMutableList().apply {
            remove(address)
            add(0, address)
            if (size > 10) removeLast()
        }
        prefs.edit().putString("history", gson.toJson(history)).apply()
    }

    fun getDeviceHistory(): List<String> {
        return gson.fromJson(prefs.getString("history", null), Array<String>::class.java)?.toList() ?: emptyList()
    }

    fun saveLastDevice(address: String) {
        prefs.edit().putString("last_device", address).apply()
    }

    fun getLastDevice(): String? {
        return prefs.getString("last_device", null)
    }

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun connectToServer(address: String) {
        fetchJob?.cancel()
        hardwareData = null
        hardwareJson = null
        connectionStatus = ConnectionStatus.Connecting

        fetchJob = viewModelScope.launch {
            try {
                while (isActive) {
                    try {
                        withContext(Dispatchers.IO) { fetchData(address) }
                        connectionStatus = ConnectionStatus.Connected
                        saveDeviceHistory(address)
                        saveLastDevice(address)
                        delay(1000)
                    } catch (e: Exception) {
                        connectionStatus = ConnectionStatus.Error(e.message ?: "获取数据失败")
                        cancel() // 停止循环
                    }
                    delay(1000)
                }
            } catch (e: IOException) {
                connectionStatus = ConnectionStatus.Error(e.message ?: "网络错误")
            }
        }
    }

    private fun fetchData(address: String) {
        val host = address.trim().trimEnd('/')
        val urls = listOf(
            "https://$host/data.json",
            "http://$host/data.json",
            "$host/data.json"
        )
        var lastEx: IOException? = null
        for (u in urls) {
            try {
                client.newCall(Request.Builder().url(u).build()).execute().use { resp ->
                    if (resp.isSuccessful) {
                        val json = resp.body?.string().orEmpty()
                        val parsed = try {
                            gson.fromJson(json, JsonRoot::class.java)
                        } catch (e: Exception) {
                            throw IOException("JSON解析错误")
                        }
                        if (parsed == null) throw IOException("解析结果为空")
                        hardwareJson = parsed
                        hardwareData = try {
                            parsed.toHardwareData()
                        } catch (e: Exception) {
                            null
                        }
                        return
                    } else {
                        lastEx = IOException("HTTP ${resp.code}")
                    }
                }
            } catch (e: IOException) {
                lastEx = e
            }
        }
        throw lastEx ?: IOException("未知获取错误")
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

fun JsonRoot.flatten(): List<JsonRoot> = listOf(this) + Children.flatMap { it.flatten() }

fun JsonRoot.findCpuUsage(): Float {
    return findNodeByText { it.contains("CPU Total", ignoreCase = true) }
        ?.Value?.replace("%", "")?.toFloatOrNull() ?: 0f
}

fun JsonRoot.findGpuUsage(): Float {
    return findAllNodes { it.Text.equals("GPU Core", true) && it.Type == "Load" }
        .firstOrNull()?.Value?.replace("%", "")?.toFloatOrNull() ?: 0f
}

fun JsonRoot.findMemoryUsed(): Float {
    return findNodeByText { it.equals("Memory Used", true) }
        ?.Value?.replace("GB", "")?.toFloatOrNull() ?: 0f
}

fun JsonRoot.findMemoryTotal(): Float {
    val used = findMemoryUsed()
    val available = findNodeByText { it.equals("Memory Available", true) }
        ?.Value?.replace("GB", "")?.toFloatOrNull() ?: 0f
    return used + available
}

fun JsonRoot.findNetworkSpeed(type: String): String {
    fun parseSpeed(value: String?): Float {
        if (value.isNullOrBlank()) return 0f
        val v = value.trim()
        return when {
            v.endsWith("GB/s", true) -> v.removeSuffix("GB/s").trim().toFloatOrNull()?.times(1024 * 1024) ?: 0f
            v.endsWith("MB/s", true) -> v.removeSuffix("MB/s").trim().toFloatOrNull()?.times(1024) ?: 0f
            v.endsWith("KB/s", true) -> v.removeSuffix("KB/s").trim().toFloatOrNull() ?: 0f
            else -> 0f
        }
    }

    val nodes = flatten()
    val matching = nodes.filter { node ->
        node.Type == "Throughput" &&
                node.Text.contains(type, true) &&
                nodes.any { it.Children.contains(node) &&
                        (it.Text.contains("WLAN", true) || it.Text.contains("以太网", true)) }
    }

    val totalKB = matching.sumOf { parseSpeed(it.Value).toDouble() }

    return when {
        totalKB >= 1024 * 1024 -> String.format("%.1f GB/s", totalKB / 1024 / 1024)
        totalKB >= 1024 -> String.format("%.1f MB/s", totalKB / 1024)
        else -> String.format("%.0f KB/s", totalKB)
    }
}

class ExpansionState {
    private val _expandedIds = mutableSetOf<Int>()
    fun isExpanded(id: Int): Boolean = _expandedIds.contains(id)
    fun toggle(id: Int) {
        if (_expandedIds.contains(id)) _expandedIds.remove(id) else _expandedIds.add(id)
    }
}
