package com.example.contactos_app1.uii

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.contactos_app1.data.Contact
import com.example.contactos_app1.viewmodel.ContactViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContactListScreen(
    navController: NavController,
    viewModel: ContactViewModel
) {
    val contacts by viewModel.contacts.collectAsState()
    var searchText by remember { mutableStateOf("") }
    val primaryBlue = Color(0xFF425A92)
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    val filteredContacts = contacts.filter {
        it.name.contains(searchText, true) ||
                it.phone.contains(searchText, true) ||
                it.email.contains(searchText, true)
    }

    val favoriteContacts = filteredContacts.filter { it.isFavorite }.sortedBy { it.name }
    val regularContacts = filteredContacts.filter { !it.isFavorite }
    val groupedContacts = regularContacts.groupBy { it.name.first().uppercaseChar() }.toSortedMap()

    val shortcutItems = remember(favoriteContacts, groupedContacts) {
        val list = mutableListOf<String>()
        if (favoriteContacts.isNotEmpty()) list.add("★")
        list.addAll(groupedContacts.keys.map { it.toString() })
        list
    }

    val itemToIndex = remember(favoriteContacts, groupedContacts) {
        val map = mutableMapOf<String, Int>()
        var currentIndex = 0
        if (favoriteContacts.isNotEmpty()) {
            map["★"] = 0
            currentIndex += 1 + favoriteContacts.size
        }
        groupedContacts.forEach { (letter, contactsInGroup) ->
            map[letter.toString()] = currentIndex
            currentIndex += 1 + contactsInGroup.size
        }
        map
    }

    Scaffold(
        containerColor = Color(0xFFF5F6FA),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("form/0") },
                containerColor = primaryBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, null)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Buscar contactos") },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = primaryBlue) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = primaryBlue,
                    unfocusedBorderColor = Color.Transparent,
                    focusedContainerColor = Color.White,
                    unfocusedContainerColor = Color.White,
                )
            )

            if (filteredContacts.isEmpty()) {
                EmptyState(
                    icon = if (searchText.isEmpty()) Icons.Default.PersonOff else Icons.Default.SearchOff,
                    message = if (searchText.isEmpty()) "Aún no tienes contactos guardados" else "No se encontraron resultados para \"$searchText\""
                )
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        if (favoriteContacts.isNotEmpty()) {
                            stickyHeader {
                                SectionHeader("Favoritos")
                            }
                            items(favoriteContacts, key = { "fav_${it.id}" }) { contact ->
                                ContactItem(contact, Modifier.animateItem()) {
                                    navController.navigate("detail/${contact.id}")
                                }
                            }
                        }

                        groupedContacts.forEach { (letter, contactsByLetter) ->
                            stickyHeader {
                                SectionHeader(letter.toString())
                            }
                            items(contactsByLetter, key = { it.id }) { contact ->
                                ContactItem(contact, Modifier.animateItem()) {
                                    navController.navigate("detail/${contact.id}")
                                }
                            }
                        }
                    }

                    if (searchText.isEmpty() && shortcutItems.isNotEmpty()) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .padding(end = 8.dp)
                                .width(32.dp)
                                .wrapContentHeight(),
                            color = Color.White.copy(alpha = 0.8f),
                            shape = RoundedCornerShape(16.dp),
                            shadowElevation = 4.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                shortcutItems.forEach { item ->
                                    Text(
                                        text = item,
                                        modifier = Modifier
                                            .clickable {
                                                coroutineScope.launch {
                                                    itemToIndex[item]?.let { listState.animateScrollToItem(it) }
                                                }
                                            }
                                            .padding(vertical = 4.dp),
                                        fontSize = if (item == "★") 16.sp else 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (item == "★") Color(0xFFFBC02D) else primaryBlue,
                                        textAlign = TextAlign.Center
                                    )
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
fun SectionHeader(text: String) {
    val primaryBlue = Color(0xFF425A92)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFFF5F6FA).copy(alpha = 0.95f))
            .padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFFE8EAF6))
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = text,
                color = primaryBlue,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun EmptyState(icon: androidx.compose.ui.graphics.vector.ImageVector, message: String) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(icon, null, modifier = Modifier.size(80.dp), tint = Color.LightGray)
        Spacer(modifier = Modifier.height(16.dp))
        Text(message, textAlign = TextAlign.Center, color = Color.Gray, fontSize = 16.sp)
    }
}

@Composable
fun ContactItem(contact: Contact, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val primaryBlue = Color(0xFF425A92)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(12.dp)) {
            Box(modifier = Modifier.size(56.dp).clip(CircleShape).background(Color(0xFFE8EAF6)), contentAlignment = Alignment.Center) {
                if (contact.imageUri != null) {
                    AsyncImage(model = contact.imageUri, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    Text(contact.name.first().uppercase(), color = primaryBlue, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(contact.name, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.Black)
                Text(contact.phone.ifEmpty { contact.email }, color = Color.Gray, fontSize = 13.sp)
            }
            if (contact.isFavorite) {
                Icon(Icons.Default.Star, null, tint = Color(0xFFFBC02D), modifier = Modifier.size(20.dp).padding(end = 4.dp))
            }
        }
    }
}
