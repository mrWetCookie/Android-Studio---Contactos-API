package com.example.contactos_app1.uii

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.ContactsContract
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.contactos_app1.viewmodel.ContactViewModel
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.clickable
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.LaunchedEffect
import com.example.contactos_app1.data.api.RetrofitClient
import com.example.contactos_app1.data.api.UserResponse
import androidx.compose.foundation.Image
import com.example.contactos_app1.R
import androidx.compose.ui.res.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    id: Int,
    navController: NavController,
    viewModel: ContactViewModel
) {
    val context = LocalContext.current
    val contact = viewModel.contacts.collectAsState().value.find { it.id == id }
    val primaryBlue = Color(0xFF0F172A)

    var userResponse by remember { mutableStateOf<UserResponse?>(null) }

    LaunchedEffect(id) {
        try {
            val response = RetrofitClient.api.getUser(id)
            if (response.isSuccessful) {
                userResponse = response.body()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }

    if (showBottomSheet && contact != null) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("¿Eliminar contacto?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(12.dp))
                Text("¿Estás seguro de que quieres eliminar a ${contact.name}?", color = Color.Gray)
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    onClick = {
                        viewModel.delete(contact)
                        showBottomSheet = false
                        navController.popBackStack()
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE53935)),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text("Eliminar Contacto", color = Color.White, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(12.dp))
                TextButton(onClick = { showBottomSheet = false }, modifier = Modifier.fillMaxWidth()) {
                    Text("Cancelar", color = Color.Gray)
                }
            }
        }
    }

    if (showQrDialog && contact != null) {
        Dialog(onDismissRequest = { showQrDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Escanea para guardar", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(16.dp))


                    val vCard = "BEGIN:VCARD\n" +
                                "VERSION:3.0\n" +
                                "FN:${contact.name}\n" +
                                "TEL;TYPE=CELL:${contact.phone}\n" +
                                "EMAIL:${contact.email}\n" +
                                "END:VCARD"

                    val qrBitmap = remember(vCard) { generateQRCode(vCard) }

                    qrBitmap?.let {
                        Image(
                            bitmap = it.asImageBitmap(),
                            contentDescription = "QR Code",
                            modifier = Modifier.size(250.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    Text(contact.name, fontWeight = FontWeight.Medium, color = primaryBlue)
                    Text(contact.phone, color = Color.Gray, fontSize = 14.sp)

                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { showQrDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Cerrar")
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White
    ) { paddingValues ->
        contact?.let {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(380.dp)
                    ) {

                        Image(
                            painter = painterResource(R.drawable.wallpaper_telegram),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )

                        // Capa oscura para mejorar la legibilidad
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.30f))
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .statusBarsPadding()
                        )
                        {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 0.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f),
                                    onClick = { navController.popBackStack() }
                                ) {
                                    Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.padding(8.dp))
                                }

                                Row {
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.2f),
                                        onClick = { viewModel.update(it.copy(isFavorite = !it.isFavorite)) }
                                    ) {
                                        Icon(
                                            if (it.isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                            null,
                                            tint = if (it.isFavorite) Color.Yellow else Color.White,
                                            modifier = Modifier.padding(10.dp).size(22.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.2f),
                                        onClick = { showQrDialog = true }
                                    ) {
                                        Icon(Icons.Default.QrCode, null, tint = Color.White, modifier = Modifier.padding(10.dp).size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.2f),
                                        onClick = { navController.navigate("form/${it.id}") }
                                    ) {
                                        Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.padding(10.dp).size(22.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(
                                        shape = CircleShape,
                                        color = Color.White.copy(alpha = 0.2f),
                                        onClick = { showBottomSheet = true }
                                    ) {
                                        Icon(Icons.Default.Delete, null, tint = Color.White, modifier = Modifier.padding(10.dp).size(22.dp))
                                    }
                                }
                            }
                        }


                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 60.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {val pagerState = rememberPagerState(
                            pageCount = {
                                userResponse?.images?.size ?: 1
                            }
                        )
                            Box(
                                modifier = Modifier
                                    .size(170.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (!userResponse?.images.isNullOrEmpty()) {

                                    HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize()
                                    ) { page ->

                                        AsyncImage(
                                            model = userResponse!!.images[page].url,
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )

                                    }

                                } else if (it.imageUri != null) {

                                    AsyncImage(
                                        model = it.imageUri,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                } else {

                                    Text(
                                        it.name.first().uppercase(),
                                        style = MaterialTheme.typography.displayLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )

                                }
                            }
                            if (!userResponse?.images.isNullOrEmpty()) {

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {

                                    repeat(userResponse!!.images.size) { index ->

                                        Box(
                                            modifier = Modifier
                                                .padding(horizontal = 4.dp)
                                                .size(
                                                    if (pagerState.currentPage == index) 10.dp else 8.dp
                                                )
                                                .clip(CircleShape)
                                                .background(
                                                    if (pagerState.currentPage == index)
                                                        Color.White
                                                    else
                                                        Color.White.copy(alpha = 0.35f)
                                                )
                                        )

                                    }

                                }

                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = it.name,
                                style = MaterialTheme.typography.headlineLarge,
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }


                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .offset(y = 51.dp)
                            .zIndex(1f),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        ActionDetailButton(
                            icon = Icons.Default.Call,
                            label = "Llamar",
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${it.phone}"))
                                context.startActivity(intent)
                            }
                        )
                        Spacer(modifier = Modifier.width(40.dp))
                        ActionDetailButton(
                            icon = Icons.Default.Email,
                            label = "Correo",
                            onClick = {
                                val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${it.email}"))
                                context.startActivity(intent)
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(50.dp))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F9FA)),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        DetailInfoRow(Icons.Default.Email, "Correo Electrónico", it.email.ifEmpty { "No disponible" })
                        HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray.copy(alpha = 0.4f))
                        DetailInfoRow(Icons.Default.Phone, "Teléfono", it.phone)
                    }
                }
            }
        }
    }
}

fun generateQRCode(content: String): Bitmap? {
    return try {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
        for (x in 0 until width) {
            for (y in 0 until height) {
                bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
        bitmap
    } catch (e: Exception) {
        null
    }
}

@Composable
fun ActionDetailButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            onClick = onClick,
            modifier = Modifier.size(56.dp),
            shape = CircleShape,
            color = Color(0xFFDDE1EE),
            shadowElevation = 4.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = Color(0xFF0F172A), modifier = Modifier.size(24.dp))
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun DetailInfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = Color(0xFF0F172A), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color.Gray)
            Text(value, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
    }
}
