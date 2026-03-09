package com.example.contactos_app1.uii

import android.content.Intent
import android.net.Uri
import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.contactos_app1.data.Contact
import com.example.contactos_app1.viewmodel.ContactViewModel
import io.michaelrocks.libphonenumber.android.PhoneNumberUtil
import java.util.Locale

data class CountryCode(val name: String, val code: String, val flag: String)

@Composable
fun rememberCountryCodes(): List<CountryCode> {
    val context = LocalContext.current
    return remember {
        val phoneUtil = PhoneNumberUtil.createInstance(context)
        Locale.getISOCountries().mapNotNull { isoCode ->
            val countryName = Locale("", isoCode).getDisplayCountry(Locale("es"))
            val dialCode = phoneUtil.getCountryCodeForRegion(isoCode)
            if (dialCode != 0) {
                CountryCode(
                    name = countryName,
                    code = "+$dialCode",
                    flag = isoCodeToEmojiFlag(isoCode)
                )
            } else null
        }.distinctBy { it.name + it.code }.sortedBy { it.name }
    }
}

fun isoCodeToEmojiFlag(isoCode: String): String {
    if (isoCode.length != 2) return "🌐"
    val firstChar = Character.codePointAt(isoCode, 0) - 0x41 + 0x1F1E6
    val secondChar = Character.codePointAt(isoCode, 1) - 0x41 + 0x1F1E6
    return String(Character.toChars(firstChar)) + String(Character.toChars(secondChar))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContactFormScreen(
    id: Int,
    navController: NavController,
    viewModel: ContactViewModel
) {
    val context = LocalContext.current
    val contacts by viewModel.contacts.collectAsState()
    val existingContact = contacts.find { it.id == id }
    val allCountries = rememberCountryCodes()

    var name by remember { mutableStateOf(existingContact?.name ?: "") }
    
    val initialFullPhone = existingContact?.phone ?: ""
    var lada by remember { 
        mutableStateOf(
            if (initialFullPhone.startsWith("+")) {
                allCountries.sortedByDescending { it.code.length }
                    .find { initialFullPhone.startsWith(it.code) }?.code ?: "+52"
            } else "+52"
        ) 
    }
    var phone by remember { 
        mutableStateOf(
            if (initialFullPhone.startsWith("+")) {
                initialFullPhone.substringAfter(lada).trim()
            } else initialFullPhone
        ) 
    }
    
    var email by remember { mutableStateOf(existingContact?.email ?: "") }
    var isFavorite by remember { mutableStateOf(existingContact?.isFavorite ?: false) }
    var imageUri by remember {
        mutableStateOf(existingContact?.imageUri?.let { Uri.parse(it) })
    }
    var bannerUri by remember {
        mutableStateOf(existingContact?.bannerUri?.let { Uri.parse(it) })
    }

    var nameError by remember { mutableStateOf(false) }
    var phoneError by remember { mutableStateOf(false) }
    var ladaError by remember { mutableStateOf(false) }
    var phoneExistsError by remember { mutableStateOf(false) }
    var emailError by remember { mutableStateOf(false) }
    var emailExistsError by remember { mutableStateOf(false) }
    var showCountryDialog by remember { mutableStateOf(false) }
    var countrySearchQuery by remember { mutableStateOf("") }

    val primaryBlue = Color(0xFF425A92)

    val imageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            imageUri = it
        }
    }

    val bannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            bannerUri = it
        }
    }

    if (showCountryDialog) {
        Dialog(onDismissRequest = { showCountryDialog = false }) {
            Card(
                modifier = Modifier.fillMaxWidth().height(550.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1C1B1F))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Seleccionar país", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                    
                    OutlinedTextField(
                        value = countrySearchQuery,
                        onValueChange = { countrySearchQuery = it },
                        placeholder = { Text("Buscar país...", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = Color.DarkGray
                        ),
                        singleLine = true
                    )

                    val filteredCountries = allCountries.filter { 
                        it.name.contains(countrySearchQuery, ignoreCase = true) || it.code.contains(countrySearchQuery) 
                    }

                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(filteredCountries) { country ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { 
                                        lada = country.code
                                        ladaError = false
                                        showCountryDialog = false 
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(country.flag, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(country.name, color = Color.White, modifier = Modifier.weight(1f))
                                Text(country.code, color = Color.Gray)
                            }
                            HorizontalDivider(color = Color.DarkGray)
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.White
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .background(primaryBlue)
            ) {
                if (bannerUri != null) {
                    AsyncImage(
                        model = bannerUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .clickable { bannerLauncher.launch(arrayOf("image/*")) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 0.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.2f),
                            onClick = { navController.popBackStack() }
                        ) {
                            Icon(Icons.Default.ArrowBack, null, tint = Color.White, modifier = Modifier.padding(8.dp))
                        }
                        
                        Text(
                            text = if (id == 0) "Nuevo Contacto" else "Editar Contacto",
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Row {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                onClick = { isFavorite = !isFavorite }
                            ) {
                                Icon(
                                    if (isFavorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                                    null,
                                    tint = if (isFavorite) Color.Yellow else Color.White,
                                    modifier = Modifier.padding(10.dp).size(22.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.2f),
                                onClick = { 
                                    if (bannerUri != null) bannerUri = null 
                                    else bannerLauncher.launch(arrayOf("image/*"))
                                }
                            ) {
                                Icon(
                                    if (bannerUri != null) Icons.Default.Delete else Icons.Default.PhotoCamera,
                                    null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(10.dp).size(22.dp)
                                )
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
                ) {
                    Box(
                        modifier = Modifier
                            .size(170.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.3f))
                            .clickable { imageLauncher.launch(arrayOf("image/*")) },
                        contentAlignment = Alignment.Center
                    ) {
                        if (imageUri != null) {
                            AsyncImage(
                                model = imageUri,
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(Icons.Default.Person, null, modifier = Modifier.size(90.dp), tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = name.ifEmpty { if (id == 0) "Nombre" else "" },
                        style = MaterialTheme.typography.headlineLarge,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                OutlinedTextField(
                    value = name,
                    onValueChange = { if (it.length <= 20) { name = it; nameError = false } },
                    label = { Text("Nombre") },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = primaryBlue) },
                    isError = nameError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                if (nameError) ErrorText("Mínimo 3 caracteres")

                Spacer(modifier = Modifier.height(16.dp))


                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        emailError = email.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(email).matches()

                        emailExistsError = email.isNotEmpty() && contacts.any { it.email.equals(email, ignoreCase = true) && it.id != id }
                    },
                    label = { Text("Correo") },
                    leadingIcon = { Icon(Icons.Default.Email, null, tint = primaryBlue) },
                    isError = emailError || emailExistsError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                )
                if (emailError) ErrorText("Correo electrónico inválido")
                if (emailExistsError) ErrorText("Este correo ya está registrado")

                Spacer(modifier = Modifier.height(16.dp))


                OutlinedTextField(
                    value = phone,
                    onValueChange = { input ->
                        if (input.startsWith("+")) {
                            val matchedCountry = allCountries.sortedByDescending { it.code.length }
                                .find { input.startsWith(it.code) }
                            
                            if (matchedCountry != null) {
                                lada = matchedCountry.code
                                ladaError = false
                                phone = input.removePrefix(matchedCountry.code).trim().filter { it.isDigit() }.take(15)
                            } else {
                                phone = input.take(16)
                            }
                        } else {
                            val filtered = input.filter { it.isDigit() }
                            if (filtered.length <= 15) {
                                phone = filtered
                                phoneExistsError = contacts.any { "${lada} ${phone}" == it.phone && it.id != id }
                                phoneError = false
                            }
                        }
                    },
                    label = { Text("Teléfono") },
                    leadingIcon = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(start = 12.dp, end = 8.dp)
                                .clickable { showCountryDialog = true }
                        ) {
                            Text(allCountries.find { it.code == lada }?.flag ?: "🌐", fontSize = 20.sp)
                            Icon(Icons.Default.ArrowDropDown, null, tint = Color.Gray)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(lada, fontWeight = FontWeight.Bold, color = primaryBlue)
                            Spacer(modifier = Modifier.width(8.dp))
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color.LightGray))
                        }
                    },
                    isError = phoneError || phoneExistsError || ladaError,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                )
                
                if (ladaError) ErrorText("Lada no válida")
                if (phoneError) ErrorText("Número inválido (7-15 dígitos)")
                if (phoneExistsError) ErrorText("Este número ya existe")

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = {
                        nameError = name.length < 3
                        phoneError = phone.length < 7
                        ladaError = lada.isEmpty()
                        
                        if (!nameError && !phoneError && !ladaError && !phoneExistsError && !emailError && !emailExistsError) {
                            val contact = Contact(id = id, name = name, phone = "${lada} ${phone}", email = email, imageUri = imageUri?.toString(), bannerUri = bannerUri?.toString(), isFavorite = isFavorite)
                            if (id == 0) viewModel.insert(contact) else viewModel.update(contact)
                            navController.popBackStack()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = primaryBlue)
                ) {
                    Text(if (id == 0) "Guardar" else "Actualizar", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
fun ErrorText(text: String) {
    Text(text = text, color = Color.Red, style = MaterialTheme.typography.bodySmall, modifier = Modifier.fillMaxWidth().padding(start = 8.dp, top = 4.dp))
}
