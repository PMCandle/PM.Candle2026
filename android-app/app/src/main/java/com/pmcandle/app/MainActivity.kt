package com.pmcandle.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.net.HttpURLConnection
import java.net.URL

private val Cream = Color(0xFFF8F1EA)
private val Brown = Color(0xFF38271F)
private val BrownLight = Color(0xFF76533F)
private val Gold = Color(0xFFE8B445)
private val WhatsApp = "201277604439"
private const val CATALOG_URL = "https://pmcandle.github.io/PM.Candle2026/"

data class Product(val id: Int, val name: String, val category: String, val price: Int, val image: android.graphics.Bitmap?)
data class CartLine(val product: Product, val quantity: Int)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(56,39,31)
        window.navigationBarColor = android.graphics.Color.rgb(56,39,31)
        setContent { PMRoot() }
    }
}

private suspend fun loadCatalog(): List<Product> = withContext(Dispatchers.IO) {
    val connection = (URL(CATALOG_URL).openConnection() as HttpURLConnection).apply {
        connectTimeout = 25000
        readTimeout = 45000
        requestMethod = "GET"
        setRequestProperty("User-Agent", "PM-Candle-Android/1.0")
    }
    try {
        val html = connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        val match = Regex("var PRODUCTS=(\\[.*?\\]);", RegexOption.DOT_MATCHES_ALL).find(html)
            ?: error("تعذر العثور على كتالوج المنتجات في الموقع")
        val array = JSONArray(match.groupValues[1])
        buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                val variants = item.optJSONArray("v")
                val price = variants?.optJSONObject(0)?.optInt("p", 0) ?: 0
                val imageData = item.optString("img")
                val bitmap = if (imageData.startsWith("data:image")) {
                    runCatching {
                        val bytes = Base64.decode(imageData.substringAfter(","), Base64.DEFAULT)
                        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }.getOrNull()
                } else null
                add(Product(item.optInt("id", i + 1), item.optString("name", "منتج PM Candle"), item.optString("cat", ""), price, bitmap))
            }
        }
    } finally {
        connection.disconnect()
    }
}

@Composable
private fun PMRoot() {
    var showSplash by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) { delay(3000); showSplash = false }
    AnimatedContent(targetState = showSplash, label = "splash") { showing ->
        if (showing) PMSplash() else PMApp()
    }
}

@Composable
private fun PMSplash() {
    val anim = rememberInfiniteTransition(label = "logo")
    val pulse by anim.animateFloat(0.96f, 1.04f, infiniteRepeatable(tween(1600), RepeatMode.Reverse), label = "pulse")
    val sweep by anim.animateFloat(-260f, 260f, infiniteRepeatable(tween(2200, easing = LinearEasing)), label = "sweep")
    Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF211713), Color(0xFF513629), Color(0xFF211713)))), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(Modifier.size(270.dp).graphicsLayer { scaleX = pulse; scaleY = pulse }.shadow(26.dp, RoundedCornerShape(140.dp)).clip(RoundedCornerShape(140.dp)).background(Color.White.copy(alpha = 0.14f)).padding(12.dp), contentAlignment = Alignment.Center) {
                Box(Modifier.fillMaxSize().clip(RoundedCornerShape(130.dp)).background(Brush.linearGradient(listOf(Color(0xFFFFF8E9), Color(0xFFEAD9C8), Color(0xFFFFFDF7)))), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🕯", fontSize = 42.sp)
                        Text("P.M", color = Color(0xFF17100E), fontSize = 58.sp, fontWeight = FontWeight.Black)
                        Text("CANDLE", color = Color(0xFF51283B), fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 2.sp)
                        Text("🕯", fontSize = 30.sp)
                    }
                }
                Box(Modifier.fillMaxWidth().height(55.dp).align(Alignment.TopCenter).graphicsLayer { translationX = sweep; rotationZ = -24f }.background(Brush.horizontalGradient(listOf(Color.Transparent, Color.White.copy(alpha = 0.8f), Color.Transparent))))
            }
            Spacer(Modifier.height(28.dp))
            Text("PM Candle", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(7.dp))
            Text("خلي مناسبتك متتنسيش", color = Color(0xFFF0C66D), fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(24.dp))
            Text("MADE WITH LOVE", color = Color(0xFFE8D7C9), fontSize = 10.sp, letterSpacing = 3.sp)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PMApp() {
    val context = LocalContext.current
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf("all") }
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Product?>(null) }
    var cart by remember { mutableStateOf<Map<Int, CartLine>>(emptyMap()) }
    var showCart by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("home") }

    LaunchedEffect(Unit) {
        runCatching { loadCatalog() }
            .onSuccess { products = it; loading = false }
            .onFailure { loadError = it.message ?: "حصل خطأ أثناء تحميل المنتجات"; loading = false }
    }

    val categoryNames = listOf(
        "all" to "الكل", "animals" to "بيبي وحيوانات", "bears" to "دباديب",
        "flowers" to "ورود", "shapes" to "أشكال", "jars" to "جارات", "gifts" to "هدايا"
    )
    val visible = products.filter {
        (category == "all" || it.category == category) && it.name.contains(search, true)
    }
    val total = cart.values.sumOf { it.product.price * it.quantity }
    val count = cart.values.sumOf { it.quantity }

    MaterialTheme(colorScheme = lightColorScheme(primary = Brown, secondary = Gold, background = Cream, surface = Color.White)) {
        Scaffold(
            containerColor = Cream,
            topBar = {
                Column(Modifier.fillMaxWidth().background(Brown).padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Text("PM CANDLE", color = Color.White, fontSize = 23.sp, fontWeight = FontWeight.Black, letterSpacing = 2.sp)
                    Text("تفاصيل صغيرة، بتعمل ذكرى كبيرة", color = Color(0xFFEAD9CC), fontSize = 12.sp)
                }
            },
            bottomBar = {
                NavigationBar(containerColor = Color.White) {
                    NavigationBarItem(selected = selectedTab == "home", onClick = { selectedTab = "home"; showCart = false; selected = null }, icon = { Text("⌂", fontSize = 21.sp) }, label = { Text("الرئيسية") })
                    NavigationBarItem(selected = selectedTab == "products", onClick = { selectedTab = "products"; showCart = false; selected = null }, icon = { Text("✦", fontSize = 19.sp) }, label = { Text("المنتجات") })
                    NavigationBarItem(selected = selectedTab == "gallery", onClick = { selectedTab = "gallery"; showCart = false; selected = null }, icon = { Text("▦", fontSize = 20.sp) }, label = { Text("شغلنا") })
                    NavigationBarItem(selected = selectedTab == "contact", onClick = { selectedTab = "contact"; showCart = false; selected = null }, icon = { Text("♡", fontSize = 20.sp) }, label = { Text("تواصل") })
                    NavigationBarItem(selected = selectedTab == "cart", onClick = { selectedTab = "cart"; showCart = true; selected = null }, icon = { Text("🛍", fontSize = 17.sp) }, label = { Text("السلة ($count)") })
                }
            }
        ) { padding ->
            when {
                showCart -> CartView(cart.values.toList(), total,
                    onPlus = { p -> val old = cart[p.id]; cart = cart + (p.id to CartLine(p, (old?.quantity ?: 0) + 1)) },
                    onMinus = { p -> val old = cart[p.id]; if (old != null) cart = if (old.quantity <= 1) cart - p.id else cart + (p.id to old.copy(quantity = old.quantity - 1)) },
                    onCheckout = {
                        val lines = cart.values.joinToString("%0A") { "${it.product.name} × ${it.quantity} = ${it.product.price * it.quantity} جنيه" }
                        val message = "طلب جديد من تطبيق PM Candle%0A$lines%0Aالإجمالي: $total جنيه"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$WhatsApp?text=" + Uri.encode(message)))
                        context.startActivity(intent)
                    }, modifier = Modifier.padding(padding))
                selectedTab == "gallery" && !showCart -> GalleryScreen(Modifier.padding(padding))
                selectedTab == "contact" && !showCart -> ContactScreen(Modifier.padding(padding))
                selected != null -> ProductDetails(selected!!, onBack = { selected = null }, onAdd = {
                    val p = selected!!
                    val old = cart[p.id]
                    cart = cart + (p.id to CartLine(p, (old?.quantity ?: 0) + 1))
                    selected = null
                }, modifier = Modifier.padding(padding))
                else -> Column(Modifier.fillMaxSize().padding(padding)) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text("أهلاً بيكي في PM Candle", color = Brown, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("شموع وتوزيعات وهدايا معمولة بحب", color = BrownLight, fontSize = 13.sp)
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(value = search, onValueChange = { search = it }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("دوري على المنتج اللي بتحبيه") }, shape = RoundedCornerShape(14.dp))
                        Spacer(Modifier.height(10.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            categoryNames.forEach { (id, name) ->
                                FilterChip(selected = category == id, onClick = { category = id }, label = { Text(name, fontSize = 11.sp) })
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("منتجاتنا", color = Brown, fontWeight = FontWeight.Bold, fontSize = 19.sp, modifier = Modifier.weight(1f))
                            Text(if (loading) "بيتم تحميل المنتجات..." else "${visible.size} منتج", color = BrownLight, fontSize = 12.sp)
                        }
                    }
                    when {
                        loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator(color = Brown) }
                        loadError != null -> Column(Modifier.fillMaxSize().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text("مش قادرين نحمل الكتالوج دلوقتي", color = Brown, fontWeight = FontWeight.Bold)
                            Text(loadError!!, color = BrownLight, fontSize = 12.sp)
                            Spacer(Modifier.height(12.dp))
                            Button(onClick = { loading = true; loadError = null }) { Text("إعادة المحاولة") }
                            LaunchedEffect(loading) {
                                if (loading) runCatching { loadCatalog() }.onSuccess { products = it; loading = false }.onFailure { loadError = it.message; loading = false }
                            }
                        }
                        else -> LazyVerticalGrid(columns = GridCells.Fixed(2), contentPadding = PaddingValues(start = 12.dp, end = 12.dp, bottom = 18.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(visible, key = { it.id }) { product ->
                                Card(Modifier.fillMaxWidth().clickable { selected = product }, shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                                    Column {
                                        if (product.image != null) Image(product.image.asImageBitmap(), product.name, Modifier.fillMaxWidth().height(145.dp), contentScale = ContentScale.Crop)
                                        else Box(Modifier.fillMaxWidth().height(145.dp).background(Color(0xFFEDE2D8)), contentAlignment = Alignment.Center) { Text("PM Candle", color = BrownLight) }
                                        Column(Modifier.padding(10.dp)) {
                                            Text(product.name, color = Brown, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, maxLines = 2, minLines = 2, overflow = TextOverflow.Ellipsis)
                                            Text("${product.price} جنيه", color = BrownLight, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                                            Spacer(Modifier.height(6.dp))
                                            Button(onClick = {
                                                val old = cart[product.id]
                                                cart = cart + (product.id to CartLine(product, (old?.quantity ?: 0) + 1))
                                            }, modifier = Modifier.fillMaxWidth(), contentPadding = PaddingValues(vertical = 7.dp)) { Text("أضيفي للسلة", fontSize = 12.sp) }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryScreen(modifier: Modifier = Modifier) {
    val urls = (1..18).map { "https://raw.githubusercontent.com/PMCandle/PM.Candle2026/main/assets/real-work/work-%02d.jpg".format(it) }
    Column(modifier.fillMaxSize().padding(14.dp)) {
        Text("من شغلنا على الطبيعة", color=Brown, fontSize=22.sp, fontWeight=FontWeight.Bold)
        Text("تفاصيل معمولة بحب علشان مناسبتك تبقى مميزة", color=BrownLight, fontSize=13.sp)
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(columns=GridCells.Fixed(2), contentPadding=PaddingValues(bottom=20.dp), horizontalArrangement=Arrangement.spacedBy(10.dp), verticalArrangement=Arrangement.spacedBy(10.dp)) {
            items(urls) { url ->
                AsyncImage(model=url, contentDescription="PM Candle real work", modifier=Modifier.fillMaxWidth().height(170.dp).clip(RoundedCornerShape(14.dp)), contentScale=ContentScale.Crop)
            }
        }
    }
}

@Composable
private fun ContactScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(modifier.fillMaxSize().padding(22.dp), horizontalAlignment=Alignment.CenterHorizontally, verticalArrangement=Arrangement.Center) {
        Text("خلي مناسبتك متتنسيش", color=Brown, fontSize=24.sp, fontWeight=FontWeight.Bold)
        Text("شموع وتوزيعات وهدايا معمولة بحب", color=BrownLight, fontSize=14.sp)
        Spacer(Modifier.height(22.dp))
        Button(onClick={ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/201277604439"))) }, modifier=Modifier.fillMaxWidth()) { Text("اطلبي عبر واتساب") }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick={ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.instagram.com/pm.candle2026/"))) }, modifier=Modifier.fillMaxWidth()) { Text("تابعينا على Instagram") }
        OutlinedButton(onClick={ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://pmcandle.github.io/PM.Candle2026/"))) }, modifier=Modifier.fillMaxWidth()) { Text("زيارة الموقع") }
    }
}

@Composable
private fun ProductDetails(product: Product, onBack: () -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← رجوع") }
        if (product.image != null) Image(product.image.asImageBitmap(), product.name, Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Fit)
        Text(product.name, color = Brown, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("${product.price} جنيه", color = BrownLight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("أضيفي للسلة") }
    }
}

@Composable
private fun CartView(lines: List<CartLine>, total: Int, onPlus: (Product) -> Unit, onMinus: (Product) -> Unit, onCheckout: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Text("سلة مشترياتك", color = Brown, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        if (lines.isEmpty()) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("السلة لسه فاضية 🛍️", color = BrownLight) }
        else {
            androidx.compose.foundation.lazy.LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                items(lines.size) { index ->
                    val line = lines[index]
                    Card(colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Row(Modifier.fillMaxWidth().padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(line.product.name, color = Brown, fontWeight = FontWeight.Bold)
                                Text("${line.product.price} جنيه", color = BrownLight)
                            }
                            TextButton(onClick = { onMinus(line.product) }) { Text("−") }
                            Text(line.quantity.toString(), color = Brown, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { onPlus(line.product) }) { Text("+") }
                        }
                    }
                }
            }
            HorizontalDivider()
            Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("الإجمالي", color = Brown, fontWeight = FontWeight.Bold)
                Text("$total جنيه", color = Brown, fontWeight = FontWeight.ExtraBold)
            }
            Button(onClick = onCheckout, modifier = Modifier.fillMaxWidth()) { Text("إتمام الطلب عبر واتساب") }
        }
    }
}
