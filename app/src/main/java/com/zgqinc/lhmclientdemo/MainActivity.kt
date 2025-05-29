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
//import androidx.lifecycle.ViewModel
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
//import androidx.compose.ui.platform.LocalDensity
import androidx.lifecycle.AndroidViewModel
//import java.util.Collections
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
//import androidx.lifecycle.LifecycleOwner
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.derivedStateOf
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalUriHandler
//import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
//import androidx.compose.ui.unit.TextUnit
//import androidx.compose.ui.unit.TextUnitType
//import java.util.concurrent.ConcurrentHashMap


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
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
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
fun MainScreen() {
    val viewModel: HardwareViewModel = viewModel()
    var serverAddress by remember { mutableStateOf("") }
    var showAllParameters by remember { mutableStateOf(false) }
    var menuExpanded by remember { mutableStateOf(false) }
    val deviceHistory = remember { viewModel.getDeviceHistory() }
    var showAboutDialog by remember { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                coroutineScope.launch {
                    viewModel.reconnectIfNeeded()
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

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
                    towards = AnimatedContentTransitionScope.SlideDirection.Left,
                    animationSpec = tween(300)
                ) + fadeIn() with
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Left,
                            animationSpec = tween(300)
                        ) + fadeOut()
            } else {
                slideIntoContainer(
                    towards = AnimatedContentTransitionScope.SlideDirection.Right,
                    animationSpec = tween(300)
                ) + fadeIn() with
                        slideOutOfContainer(
                            towards = AnimatedContentTransitionScope.SlideDirection.Right,
                            animationSpec = tween(300)
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

//                    Spacer(modifier = Modifier.height(32.dp))

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
        val uriHandler = LocalUriHandler.current
        val annotatedText = buildAnnotatedString {
            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                append("个人主页：")
            }

            pushStringAnnotation(tag = "URL", annotation = "https://domain.zgqinc.gq")
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("domain.zgqinc.gq")
            }
            pop()

            append("\n\n")

            withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onSurface)) {
                append("项目地址：")
            }

            pushStringAnnotation(tag = "GITHUB", annotation = "https://github.com/ZGQ-inc/LHM-Client-Demo")
            withStyle(
                style = SpanStyle(
                    color = MaterialTheme.colorScheme.primary,
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append("ZGQ-inc/LHM-Client-Demo")
            }
            pop()
        }

        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            title = { Text("关于") },
            text = {
                ClickableText(
                    text = annotatedText,
                    style = MaterialTheme.typography.bodyMedium,
                    onClick = { offset ->
                        annotatedText.getStringAnnotations(tag = "URL", start = offset, end = offset)
                            .firstOrNull()?.let { uriHandler.openUri(it.item) }

                        annotatedText.getStringAnnotations(tag = "GITHUB", start = offset, end = offset)
                            .firstOrNull()?.let { uriHandler.openUri(it.item) }
                    }
                )
            },
            confirmButton = {
                TextButton(onClick = { showAboutDialog = false }) {
                    Text("关闭")
                }
            }
        )
    }

}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AllParametersScreen(data: JsonRoot, onBack: () -> Unit) {
    val listState = rememberLazyListState()

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
            state = listState,
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

@Composable
private fun NodeItem(
    node: JsonRoot,
    expansionState: ExpansionState,
    indentLevel: Int
) {
    val hasChildren = node.Children.isNotEmpty()
    val isExpanded = expansionState.isExpanded(node.id)
    val colorScheme = MaterialTheme.colorScheme
    val containerColor = remember(node.Type, colorScheme) {
        typeColorMap[node.Type] ?: colorScheme.surfaceVariant
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .fillMaxWidth()
                .clickable(enabled = hasChildren) { expansionState.toggle(node.id)
                                                  },
//            colors = CardDefaults.cardColors(containerColor = containerColor),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(
                        start = (indentLevel * 16).dp,
                        top = 12.dp,
                        end = 16.dp,
                        bottom = 12.dp
                    )
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasChildren) {
                    Icon(
                        painter = painterResource(
                            id = if (isExpanded) R.drawable.ic_arrow_down else R.drawable.ic_arrow_right
                        ),
                        contentDescription = if (isExpanded) "收起" else "展开",
                        modifier = Modifier.size(20.dp))
                } else {
                    Spacer(modifier = Modifier.size(20.dp))
                }

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    painter = painterResource(id = iconMap[node.Type] ?: R.drawable.ic_sensor),
                    contentDescription = node.Type,
                    tint = typeColorMap[node.Type] ?: colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = node.Text,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    node.Value?.takeIf { it.isNotEmpty() }?.let {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp,
                            color = colorScheme.onSurface.copy(alpha = 0.7f)
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
//            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .background(Color.Transparent)
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
//        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
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
    private var currentAddress: String? = null

    @RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
    fun reconnectIfNeeded() {
        currentAddress?.let { address ->
            if (connectionStatus == ConnectionStatus.Disconnected ||
                connectionStatus is ConnectionStatus.Error) {
                connectToServer(address)
            }
        }
    }

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
        currentAddress = address

        fetchJob = viewModelScope.launch {
            try {
                while (isActive) {
                    try {
                        withContext(Dispatchers.IO) { fetchData(address) }
                        connectionStatus = ConnectionStatus.Connected
                        saveDeviceHistory(address)
                        saveLastDevice(address)
                        delay(1000)
                    } catch (e: CancellationException) {
                    } catch (e: Exception) {
                        connectionStatus = ConnectionStatus.Error(e.message ?: "获取数据失败")
                        cancel()
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

// TODO 修复网络速度计算
fun JsonRoot.findNetworkSpeed(type: String): String {
    fun parseSpeed(value: String?): Float {
        if (value.isNullOrBlank()) return 0f
        val v = value.trim()
        return when {
            v.endsWith("GB/s", true) -> v.removeSuffix("GB/s").trim().toFloatOrNull()?.times(1024 * 1024) ?: 0f
            v.endsWith("MB/s", true) -> v.removeSuffix("MB/s").trim().toFloatOrNull()?.times(1024) ?: 0f
            v.endsWith("KB/s", true) -> v.removeSuffix("KB/s").trim().toFloatOrNull() ?: 0f
            v.endsWith("B/s", true) -> v.removeSuffix("B/s").trim().toFloatOrNull()?.div(1024) ?: 0f
            else -> 0f
        }
    }

    val nodes = flatten()
    val matching = nodes.filter { node ->
        node.Type == "Throughput" &&
                node.Text.contains(type, true) &&
                nodes.any { parent ->
                    parent.Children.contains(node) &&
                            (parent.Text.contains("WLAN", true) ||
                                    parent.Text.contains("以太网", true) ||
                                    parent.Text.contains("Bluetooth", true) ||
                                    parent.Text.contains("蓝牙", true))
                }
    }

    val totalKB = matching.sumOf { parseSpeed(it.Value).toDouble() }

    return when {
        totalKB >= 1024 * 1024 -> String.format("%.1f GB/s", totalKB / 1024 / 1024)
        totalKB >= 1024 -> String.format("%.1f MB/s", totalKB / 1024)
        totalKB > 0 -> String.format("%.0f KB/s", totalKB)
        else -> "0 KB/s"
    }
}

class ExpansionState {
    private val _expandedIds = mutableSetOf<Int>()
    fun isExpanded(id: Int): Boolean = _expandedIds.contains(id)
    fun toggle(id: Int) {
        if (_expandedIds.contains(id)) _expandedIds.remove(id) else _expandedIds.add(id)
    }
}