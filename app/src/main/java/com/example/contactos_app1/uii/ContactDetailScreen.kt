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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import kotlin.math.abs
import kotlinx.coroutines.flow.debounce

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactDetailScreen(
    id: Int,
    navController: NavController,
    viewModel: ContactViewModel
) {
    val context = LocalContext.current
    val primaryBlue = Color(0xFF0F172A)

    var userResponse by remember { mutableStateOf<UserResponse?>(null) }

    // ✅ Obtener el contacto correctamente
    val contacts by viewModel.contacts.collectAsState()
    val contact = contacts.find { it.id == id }

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

    // ✅ Recuperar la página guardada
    val savedPage = viewModel.getCurrentPage(id)

    val pagerState = rememberPagerState(
        initialPage = savedPage,
        pageCount = {
            userResponse?.images?.size ?: 1
        }
    )

    // ✅ Actualizar el contacto cuando cambia la página
    LaunchedEffect(pagerState.currentPage) {
        snapshotFlow { pagerState.currentPage }
            .debounce(300)
            .collect { currentPage ->
                val images = userResponse?.images ?: emptyList()
                if (images.isNotEmpty() && currentPage < images.size) {
                    val currentImageUrl = images[currentPage].url
                    val currentContact = contacts.find { it.id == id }
                    if (currentContact != null && currentContact.imageUri != currentImageUrl) {
                        viewModel.update(
                            currentContact.copy(
                                imageUri = currentImageUrl
                            )
                        )
                    }
                    viewModel.saveCurrentPage(id, currentPage)
                }
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

                    val qrBitmap = remember(vCard) {
                        try {
                            val writer = QRCodeWriter()
                            val bitMatrix = writer.encode(vCard, BarcodeFormat.QR_CODE, 512, 512)
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
                        if (!userResponse?.images.isNullOrEmpty()) {
                            HorizontalPager(
                                state = pagerState,
                                modifier = Modifier.fillMaxSize()
                            ) { page ->
                                AsyncImage(
                                    model = userResponse!!.images[page].url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        } else if (it.imageUri != null) {
                            AsyncImage(
                                model = it.imageUri,
                                contentDescription = null,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        }

                        // Gradiente
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color.Black.copy(alpha = 0.15f),
                                            Color.Transparent,
                                            Color.Black.copy(alpha = 0.55f)
                                        )
                                    )
                                )
                        )

                        // Botones superiores
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .statusBarsPadding()
                                .padding(horizontal = 16.dp, vertical = 10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = {
                                        navController.popBackStack()
                                    }
                                ) {
                                    Icon(
                                        Icons.Default.ArrowBack,
                                        null,
                                        tint = Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }

                                Row {
                                    IconButton(
                                        onClick = {
                                            viewModel.update(
                                                it.copy(
                                                    isFavorite = !it.isFavorite
                                                )
                                            )
                                        }
                                    ) {
                                        Icon(
                                            if (it.isFavorite)
                                                Icons.Default.Star
                                            else
                                                Icons.Outlined.StarBorder,
                                            null,
                                            tint = Color.White
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            showQrDialog = true
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.QrCode,
                                            null,
                                            tint = Color.White
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            navController.navigate("form/${it.id}")
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            null,
                                            tint = Color.White
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            showBottomSheet = true
                                        }
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            null,
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }

                        // Indicadores de página
                        if (!userResponse?.images.isNullOrEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                repeat(userResponse?.images?.size ?: 1) { index ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(50.dp))
                                            .background(
                                                if (pagerState.currentPage == index)
                                                    Color.White
                                                else
                                                    Color.White.copy(alpha = .35f)
                                            )
                                    )
                                }
                            }
                        }

                        // Nombre y teléfono
                        Column(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(20.dp)
                        ) {
                            Text(
                                text = it.name,
                                color = Color.White,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(4.dp))

                            Text(
                                text = it.phone,
                                color = Color.White.copy(alpha = .85f),
                                fontSize = 16.sp
                            )
                        }
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
                        DetailInfoRow(
                            icon = Icons.Default.Email,
                            label = "Correo Electrónico",
                            value = it.email.ifEmpty { "No disponible" }
                        )
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 16.dp),
                            color = Color.LightGray.copy(alpha = 0.4f)
                        )
                        DetailInfoRow(
                            icon = Icons.Default.Phone,
                            label = "Teléfono",
                            value = it.phone
                        )
                    }
                }
            }
        }
    }
}

// ✅ Función generateQRCode fuera del Composable
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

// ✅ Composable DetailInfoRow fuera de ContactDetailScreen
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

// ✅ Composable ActionDetailButton fuera de ContactDetailScreen
@Composable
fun ActionDetailButton(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(62.dp),
            shape = CircleShape,
            color = Color(0xFF3390EC),
            onClick = onClick
        ) {
            Box(
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            label,
            fontSize = 12.sp,
            color = Color.Gray
        )
    }
}