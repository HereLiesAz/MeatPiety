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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlin.math.abs

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { MeatPietyApp() }
    }
}

private val PietyDark = darkColorScheme(
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
private fun MeatPietyApp() {
    MaterialTheme(colorScheme = PietyDark) {
        Surface(modifier = Modifier.fillMaxSize()) {
            LedgerScreen()
        }
    }
}

@Composable
private fun LedgerScreen() {
    var daysText by remember { mutableStateOf("365") }
    var friendsText by remember { mutableStateOf("0") }
    var diet by remember { mutableStateOf(Diet.VEGAN) }

    val days = daysText.toIntOrNull()?.coerceAtLeast(0) ?: 0
    val friends = friendsText.toIntOrNull()?.coerceAtLeast(0) ?: 0

    val personal = remember(days, diet) { animalsSpared(days, diet) }
    val sphere = remember(personal, friends) { sphereOfInfluence(personal, friends) }
    val world = remember(days) { worldTally(days) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(vertical = 8.dp),
    ) {
        item {
            Text(
                text = "Meat Piety",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
        }
        item {
            Text(
                text = "A ledger of the dead who stayed that way, on your account.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        item {
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
        }
        item {
            OutlinedTextField(
                value = daysText,
                onValueChange = { daysText = it.filter(Char::isDigit) },
                label = { Text("Days lived this way") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        item {
            OutlinedTextField(
                value = friendsText,
                onValueChange = { friendsText = it.filter(Char::isDigit) },
                label = { Text("Facebook friends") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
        }

        item { HorizontalDivider(color = MaterialTheme.colorScheme.outline) }
        item { SectionHeader("Your ledger") }
        items(personal) { LedgerRow(it) }

        item { HorizontalDivider(color = MaterialTheme.colorScheme.outline) }
        item {
            SectionHeader(
                title = "If your conscience were contagious",
                caption = "As if all $friends of your friends had done exactly what you did, for exactly as long.",
            )
        }
        items(sphere) { LedgerRow(it) }

        item { HorizontalDivider(color = MaterialTheme.colorScheme.outline) }
        item {
            SectionHeader(
                title = "Meanwhile, out there",
                caption = "An estimated ${format(world.veganPopulation.toDouble())} vegans and " +
                    "${format(world.vegetarianPopulation.toDouble())} vegetarians worldwide, keeping this up " +
                    "for the same $days days. Nobody is actually counting; the arithmetic is the point.",
            )
        }
        items(world.animals) { LedgerRow(it) }

        item {
            Text(
                text = "Buffalo don't run on this ledger like the rest. Ranching is most of why they still " +
                    "exist at all — fewer eaten means fewer bred, not fewer killed. Every other line here is " +
                    "a rescue. That one's a eulogy.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontStyle = FontStyle.Italic,
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, caption: String? = null) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        if (caption != null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun LedgerRow(animal: SparedAnimal) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(text = animal.name, style = MaterialTheme.typography.titleMedium)
        Text(
            text = if (animal.existsBecauseOfDemand) {
                "${format(animal.count)} fewer will ever be born"
            } else {
                "${format(animal.count)} spared"
            },
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            fontStyle = if (animal.existsBecauseOfDemand) FontStyle.Italic else FontStyle.Normal,
        )
    }
}

private fun format(count: Double): String {
    val n = abs(count)
    return if (n >= 1000) "%,.0f".format(n) else "%.1f".format(n)
}
