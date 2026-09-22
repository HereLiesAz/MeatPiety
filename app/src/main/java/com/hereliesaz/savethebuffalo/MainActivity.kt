package com.hereliesaz.savethebuffalo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SaveTheBuffaloApp() }
    }
}

private val BuffaloDark = darkColorScheme(
    primary = Color(0xFFE0E0E0),
    onPrimary = Color(0xFF000000),
    background = Color(0xFF000000),
    onBackground = Color(0xFFE0E0E0),
    surface = Color(0xFF0A0A0A),
    onSurface = Color(0xFFE0E0E0),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFAAAAAA),
    outline = Color(0xFF444444),
)

@Composable
private fun SaveTheBuffaloApp() {
    MaterialTheme(colorScheme = BuffaloDark) {
        Surface(modifier = Modifier.fillMaxSize()) {
            LedgerScreen()
        }
    }
}

@Composable
private fun LedgerScreen() {
    var daysText by remember { mutableStateOf("365") }
    var diet by remember { mutableStateOf(Diet.VEGAN) }
    val days = daysText.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val results = remember(days, diet) { animalsSpared(days, diet) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "Save The Buffalo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
        )
        Text(
            text = "A ledger of the dead who stayed that way, on your account.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            Diet.entries.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = diet == option,
                    onClick = { diet = option },
                    shape = SegmentedButtonDefaults.itemShape(index, Diet.entries.size),
                ) {
                    Text(if (option == Diet.VEGAN) "Vegan" else "Vegetarian")
                }
            }
        }

        OutlinedTextField(
            value = daysText,
            onValueChange = { daysText = it.filter(Char::isDigit) },
            label = { Text("Days lived this way") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline)

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            contentPadding = PaddingValues(vertical = 8.dp),
        ) {
            items(results) { animal ->
                LedgerRow(animal)
            }
        }
    }
}

@Composable
private fun LedgerRow(animal: SparedAnimal) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = animal.name,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = "%.1f spared".format(animal.count),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
        )
    }
}
