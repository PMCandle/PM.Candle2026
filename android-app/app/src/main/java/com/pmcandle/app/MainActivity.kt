package com.pmcandle.app

import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.content.Context
import android.util.Base64
import android.view.HapticFeedbackConstants
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
import androidx.compose.ui.platform.LocalView
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

private const val INSTAGRAM_URL = "https://www.instagram.com/pm.candle2026/"
private const val FACEBOOK_URL = "https://www.facebook.com/profile.php?id=61577439134472"
private const val TIKTOK_URL = "https://www.tiktok.com/@pm.candle"

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
    val hapticView = LocalView.current
    val tapVibration = { hapticView.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var category by remember { mutableStateOf("all") }
    var search by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Product?>(null) }
    var cart by remember { mutableStateOf<Map<Int, CartLine>>(emptyMap()) }
    var showCart by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf("home") }
    val ratingPrefs = remember { context.getSharedPreferences("pm_ratings", Context.MODE_PRIVATE) }
    var ratings by remember { mutableStateOf<Map<Int, Int>>(emptyMap()) }
    LaunchedEffect(products) { ratings = products.associate { it.id to ratingPrefs.getInt(it.id.toString(), 0) }.filterValues { it > 0 } }
    fun setRating(id: Int, value: Int) { ratingPrefs.edit().putInt(id.toString(), value).apply(); ratings = ratings + (id to value) }

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
    val discountRate = when { count >= 100 -> 0.20; count >= 30 -> 0.10; else -> 0.0 }
    val discount = if (discountRate == 0.20) minOf((total * discountRate).toInt(), 500) else if (discountRate == 0.10) minOf((total * discountRate).toInt(), 300) else 0
    val finalTotal = total - discount
    val topRated = products.filter { (ratings[it.id] ?: 0) > 0 }.sortedWith(compareByDescending<Product> { ratings[it.id] ?: 0 }.thenBy { it.name }).take(5)

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
                showCart -> CartView(cart.values.toList(), total, count, discount, finalTotal,
                    onPlus = { p -> tapVibration(); val old = cart[p.id]; cart = cart + (p.id to CartLine(p, (old?.quantity ?: 0) + 10)) },
                    onMinus = { p -> tapVibration(); val old = cart[p.id]; if (old != null) cart = if (old.quantity > 10) cart + (p.id to old.copy(quantity = old.quantity - 1)) else cart },
                    onCheckout = {
                        val lines = cart.values.joinToString("%0A") { "${it.product.name} × ${it.quantity} = ${it.product.price * it.quantity} جنيه" }
                        val message = "طلب جديد من تطبيق PM Candle%0A$lines%0Aالإجمالي قبل الخصم: $total جنيه%0Aالخصم: $discount جنيه%0Aالإجمالي النهائي: $finalTotal جنيه"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/$WhatsApp?text=" + Uri.encode(message)))
                        context.startActivity(intent)
                    }, modifier = Modifier.padding(padding))
                selectedTab == "gallery" && !showCart -> GalleryScreen(Modifier.padding(padding))
                selectedTab == "contact" && !showCart -> ContactScreen(Modifier.padding(padding))
                selected != null -> ProductDetails(selected!!, rating = ratings[selected!!.id] ?: 0, onRate = { setRating(selected!!.id, it) }, onBack = { selected = null }, onAdd = {
                    tapVibration()
                    val p = selected!!
                    val old = cart[p.id]
                    cart = cart + (p.id to CartLine(p, (old?.quantity ?: 0) + 10))
                    selected = null
                }, modifier = Modifier.padding(padding))
                else -> Column(Modifier.fillMaxSize().padding(padding)) {
                    Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                        Text("أهلاً بيكي في PM Candle", color = Brown, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("شموع وتوزيعات وهدايا معمولة بحب", color = BrownLight, fontSize = 13.sp)
                        Text("خصم يصل إلى 20% على الكميات", color = Color(0xFF9B633D), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        if (topRated.isNotEmpty()) { Spacer(Modifier.height(8.dp)); Text("الأعلى تقييماً ⭐", color = Brown, fontWeight = FontWeight.Bold); Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { topRated.forEach { p -> AssistChip(onClick = { selected = p }, label = { Text("${p.name.take(12)} ★${ratings[p.id] ?: 0}", fontSize = 10.sp) }) } } }
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
                                            RatingStars(rating = ratings[product.id] ?: 0, onRate = { setRating(product.id, it) })
                                            Spacer(Modifier.height(6.dp))
                                            Button(onClick = {
                                                tapVibration()
                                                val old = cart[product.id]
                                                cart = cart + (product.id to CartLine(product, (old?.quantity ?: 0) + 10))
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
        OutlinedButton(onClick={ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(INSTAGRAM_URL))) }, modifier=Modifier.fillMaxWidth()) { Text("تابعينا على Instagram") }
        OutlinedButton(onClick={ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(FACEBOOK_URL))) }, modifier=Modifier.fillMaxWidth()) { Text("تابعينا على Facebook") }
        OutlinedButton(onClick={ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(TIKTOK_URL))) }, modifier=Modifier.fillMaxWidth()) { Text("تابعينا على TikTok") }
        OutlinedButton(onClick={ context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://pmcandle.github.io/PM.Candle2026/"))) }, modifier=Modifier.fillMaxWidth()) { Text("زيارة الموقع") }
    }
}

@Composable
private fun ProductDetails(product: Product, rating: Int, onRate: (Int) -> Unit, onBack: () -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().padding(16.dp)) {
        TextButton(onClick = onBack) { Text("← رجوع") }
        if (product.image != null) Image(product.image.asImageBitmap(), product.name, Modifier.fillMaxWidth().weight(1f).clip(RoundedCornerShape(18.dp)), contentScale = ContentScale.Fit)
        Text(product.name, color = Brown, fontSize = 23.sp, fontWeight = FontWeight.Bold)
        Text("${product.price} جنيه", color = BrownLight, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        RatingStars(rating = rating, onRate = onRate)
        Spacer(Modifier.height(14.dp))
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) { Text("أضيفي للسلة") }
    }
}

@Composable
private fun CartView(lines: List<CartLine>, total: Int, count: Int, discount: Int, finalTotal: Int, onPlus: (Product) -> Unit, onMinus: (Product) -> Unit, onCheckout: () -> Unit, modifier: Modifier = Modifier) {
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
                            TextButton(onClick = { onMinus(line.product) }, enabled = line.quantity > 10) { Text("−1") }
                            Text(line.quantity.toString(), color = Brown, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { onPlus(line.product) }) { Text("+10") }
                        }
                    }
                }
            }
            HorizontalDivider()
            Text("عدد القطع: $count", color = BrownLight, modifier = Modifier.padding(top = 8.dp))
            Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("الإجمالي قبل الخصم", color = Brown); Text("$total جنيه", color = Brown, fontWeight = FontWeight.Bold) }
            if (discount > 0) Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("الخصم", color = Color(0xFF27834A)); Text("-$discount جنيه", color = Color(0xFF27834A), fontWeight = FontWeight.Bold) }
            Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) { Text("الإجمالي النهائي", color = Brown, fontWeight = FontWeight.Bold); Text("$finalTotal جنيه", color = Brown, fontWeight = FontWeight.ExtraBold) }
            if (count < 30) Text("الحد الأدنى لإتمام الطلب 30 قطعة. باقي ${30-count} قطعة.", color = Color(0xFFB05A35), fontSize = 13.sp, modifier = Modifier.padding(bottom = 8.dp))
            Button(onClick = onCheckout, enabled = count >= 30, modifier = Modifier.fillMaxWidth()) { Text("إتمام الطلب عبر واتساب") }
        }
    }
}


@Composable
private fun RatingStars(rating: Int, onRate: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        (1..5).forEach { star ->
            Text(text = if (star <= rating) "★" else "☆", color = Color(0xFFE0A52E), fontSize = 20.sp,
                modifier = Modifier.clickable { onRate(star) }.padding(end = 2.dp))
        }
        if (rating > 0) Text("($rating/5)", color = BrownLight, fontSize = 10.sp)
    }
}
