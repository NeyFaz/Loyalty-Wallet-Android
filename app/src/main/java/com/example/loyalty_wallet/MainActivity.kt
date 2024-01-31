package com.example.loyalty_wallet

import android.graphics.Bitmap
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.Image
import androidx.compose.material3.Text
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.loyalty_wallet.ui.theme.Loyalty_WalletTheme
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import java.text.SimpleDateFormat
import java.util.*

data class LoyaltyCard(
    val cardName: String,
    val creationDate: Date,
    val lastUsedDate: Date,
    val encodedInformation: String,
    val barcodeType: String
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Loyalty_WalletTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun CardDetailsView(card: LoyaltyCard, navController: NavController, viewModel: LoyaltyCardViewModel) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    var cardName by remember { mutableStateOf(card.cardName) } // To hold the editable name
    val barcodeBitmap: Bitmap? = generateBarcode(card.encodedInformation, card.barcodeType)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextField(
            value = cardName,
            onValueChange = {cardName = it},
            label = { Text("Store Name") },
            readOnly = false
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(card.creationDate),
            onValueChange = {},
            label = { Text("Creation Date") },
            readOnly = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(card.lastUsedDate),
            onValueChange = {},
            label = { Text("Last Used Date") },
            readOnly = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = card.encodedInformation,
            onValueChange = {},
            label = { Text("Encoded Information") },
            readOnly = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        barcodeBitmap?.let {
            Image(
                bitmap = it.asImageBitmap(),
                contentDescription = "Barcode",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp) // Adjust the height as needed
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Delete Card")
        }

        if (showDeleteDialog) {
            // Show confirmation dialog for deletion
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Delete Card") },
                text = { Text("Are you sure you want to delete this card?") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteCard(card)
                            showDeleteDialog = false
                            navController.popBackStack()
                        }
                    ) {
                        Text("Confirm")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDeleteDialog = false }) {
                        Text("Cancel")
                    }
                },
                properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
            )
        }

        Spacer(modifier = Modifier.weight(1f)) // Pushes the button to the bottom

        // Validate button at the bottom right
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomEnd) {
            Button(
                onClick = {
                    // Implement the validation logic for validating the card
                    // Update the card name if needed
                    viewModel.updateCardName(card, cardName)
                    navController.popBackStack()

                },
                modifier = Modifier
                    .padding(16.dp)
                    .align(Alignment.BottomEnd)
            ) {
                Text("Validate")
            }
        }
    }
}

fun generateBarcode(data: String, type: String): Bitmap? {
    val size = 512 // Adjust the size for your needs
    val format = when (type) {
        "QR_CODE" -> BarcodeFormat.QR_CODE
        "EAN_13" -> BarcodeFormat.EAN_13
        // Add other formats as needed
        else -> return null
    }

    try {
        val bitMatrix: BitMatrix = MultiFormatWriter().encode(data, format, size, size)
        val width = bitMatrix.width
        val height = bitMatrix.height
        val pixels = IntArray(width * height)
        for (y in 0 until height) {
            val offset = y * width
            for (x in 0 until width) {
                pixels[offset + x] = if (bitMatrix[x, y]) Color.Black.toArgb() else Color.White.toArgb()
            }
        }
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height)
        return bitmap
    } catch (e: Exception) {
        return null
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    // Create an instance of LoyaltyCardViewModel and remember it across recompositions
    val viewModel: LoyaltyCardViewModel = remember { LoyaltyCardViewModel() }

    NavHost(navController = navController, startDestination = "cardList") {
        composable("cardList") {
            // Access the cards list directly from the viewModel
            LoyaltyCardGrid(cards = viewModel.cards, navController = navController)
        }
        composable("cardDetails/{cardName}") { backStackEntry ->
            // Extract the cardName from the arguments and find the corresponding card
            backStackEntry.arguments?.getString("cardName")?.let { cardName ->
                viewModel.cards.firstOrNull { it.cardName == cardName }?.let { card ->
                    // Pass the card and navController to the CardDetailsView composable
                    CardDetailsView(card = card, navController = navController, viewModel = viewModel)
                }
            }
        }
    }
}

class LoyaltyCardViewModel {
    var cards = mutableStateListOf(
        LoyaltyCard("Bookshop", Date(), Date(), "EncodedInfo1", "QR_CODE"),
        // Ensure the EAN_13 barcode has valid data (13 numeric digits)
        LoyaltyCard("Cafeteria", Date(), Date(), "1234567890128", "EAN_13")
        // Add more sample cards here
    )

    fun updateCardName(card: LoyaltyCard, newName: String) {
        cards[cards.indexOf(card)] = card.copy(cardName = newName)
    }

    fun deleteCard(card: LoyaltyCard) {
        cards.remove(card)
    }
}

@Composable
fun LoyaltyCardGrid(cards: List<LoyaltyCard>, navController: NavController, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        modifier = modifier
    ) {
        items(cards) { card ->
            LoyaltyCardItem(card = card, onClick = {
                navController.navigate("cardDetails/${card.cardName}")
            })
        }
    }
}

@Composable
fun LoyaltyCardItem(card: LoyaltyCard, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .padding(8.dp)
            .aspectRatio(1f)
            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = card.cardName,
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = Color.White
        )
    }
}

object SampleData {
    val cards = listOf(
        LoyaltyCard("Bookshop", Date(), Date(), "EncodedInfo1", "QR_CODE"),
        LoyaltyCard("Cafeteria", Date(), Date(), "EncodedInfo2", "EAN_13")
        // Add more sample cards here
    )
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Loyalty_WalletTheme {
        LoyaltyCardGrid(cards = SampleData.cards, navController = rememberNavController())
    }
}
