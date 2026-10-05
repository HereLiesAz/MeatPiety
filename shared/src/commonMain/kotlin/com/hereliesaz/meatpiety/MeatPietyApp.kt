package com.hereliesaz.meatpiety

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.log10

/*
 * Layout: scrollytelling chapters closing on a bento recap.
 *   Hero (parallax field, interactive variable title) → inputs →
 *   chapters (yours, your circle, the world), each a counting headline over
 *   log-scaled bars → buffalo eulogy (kinetic quote) → market → precedent
 *   timeline with parallax years → bento recap.
 * All motion honours LocalReducedMotion.
 */

@Composable
fun MeatPietyApp() {
    CompositionLocalProvider(LocalReducedMotion provides rememberReducedMotion()) {
        MaterialTheme(colorScheme = PietyColors, typography = pietyTypography()) {
            Surface(modifier = Modifier.fillMaxSize(), color = Piety.Soil) {
                LedgerScreen()
            }
        }
    }
}

private val Gutter = 24.dp
private val MaxColumn = 720.dp

private fun Modifier.column() = widthIn(max = MaxColumn).fillMaxWidth().padding(horizontal = Gutter)

private fun List<SparedAnimal>.sparedTotal() = filterNot { it.existsBecauseOfDemand }.sumOf { it.count }

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
    val market = remember { marketEffects() }

    val state = rememberLazyListState()
    val reduced = LocalReducedMotion.current

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val heroHeight = maxHeight
        LazyColumn(
            state = state,
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            contentPadding = PaddingValues(bottom = 96.dp),
        ) {
            item(key = "hero") {
                Hero(heroHeight) {
                    if (state.firstVisibleItemIndex == 0) state.firstVisibleItemScrollOffset.toFloat() else 1e5f
                }
            }
            item(key = "inputs") {
                Column(
                    Modifier.column().windowInsetsPadding(WindowInsets.safeDrawing).padding(top = 32.dp).reveal(),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Eyebrow("Your terms")
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        Diet.entries.forEachIndexed { index, option ->
                            SegmentedButton(
                                selected = diet == option,
                                onClick = { diet = option },
                                shape = SegmentedButtonDefaults.itemShape(index, Diet.entries.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = Piety.Moss, activeContentColor = Piety.Oat,
                                    inactiveContainerColor = Color.Transparent, inactiveContentColor = Piety.Lichen,
                                    activeBorderColor = Piety.Fern, inactiveBorderColor = Piety.Rule,
                                ),
                            ) { Text(if (option == Diet.VEGAN) "Vegan" else "Vegetarian") }
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        NumberField(daysText, { daysText = it }, "Days lived this way", Modifier.weight(1f))
                        NumberField(friendsText, { friendsText = it }, "Facebook friends", Modifier.weight(1f))
                    }
                }
            }

            chapter(
                key = "yours", number = "01", title = "Your ledger",
                caption = "Every one of these is an animal a standard U.S. diet would have put through the system on your behalf.",
                animals = personal, accent = Piety.Sage,
            ) {
                Pictogram(
                    count = personal.sparedTotal(), cap = 200, color = Piety.Sage,
                    modifier = Modifier.fillMaxWidth().aspectRatio(2f).padding(vertical = 8.dp),
                )
                Text("ONE MARK PER ANIMAL, UP TO 200", style = MaterialTheme.typography.labelSmall, color = Piety.Lichen)
            }

            chapter(
                key = "circle", number = "02", title = "If your conscience were contagious",
                caption = "As if all $friends of your friends had done exactly what you did, for exactly as long.",
                animals = sphere, accent = Piety.Fern,
            ) {
                Ripples(
                    rings = 1 + log10(1.0 + friends).toInt() * 2, color = Piety.Fern,
                    modifier = Modifier.fillMaxWidth().height(160.dp),
                )
            }

            chapter(
                key = "world", number = "03", title = "Meanwhile, out there",
                caption = "An estimated ${format(world.veganPopulation.toDouble())} vegans and " +
                    "${format(world.vegetarianPopulation.toDouble())} vegetarians worldwide, keeping this up " +
                    "for the same $days days — each counted as if they'd otherwise eaten a U.S. standard diet, " +
                    "which most of them never would have. Population figures are estimates, not a census.",
                animals = world.animals, accent = Piety.Sprout,
            )

            item(key = "eulogy") { Eulogy(Modifier.column().padding(vertical = 72.dp)) }

            item(key = "market-head") {
                ChapterHead(
                    "04", "What happens to the price",
                    "Today's abstainers are already a standing demand shock. Modeled from published " +
                        "demand and supply elasticities, not observed — nobody's run this experiment at scale.",
                )
            }
            itemsIndexed(market, key = { _, it -> "market-${it.name}" }) { i, effect ->
                MarketRow(effect, Modifier.column().padding(vertical = 10.dp).reveal(delayMillis = (i % 4) * 60))
            }

            item(key = "history-head") {
                ChapterHead(
                    "05", "This has happened before",
                    "Demand didn't just spare buffalo. It's the reason several species exist at all — " +
                        "and the reason at least one doesn't.",
                )
            }
            itemsIndexed(EXTINCTION_PRECEDENTS, key = { _, it -> "p-${it.species}" }) { _, p ->
                PrecedentRow(p, state, "p-${p.species}", parallax = !reduced)
            }

            item(key = "recap") { Recap(personal, sphere, world, market, Modifier.column().padding(top = 96.dp)) }
        }
    }
}

@Composable
private fun Hero(height: Dp, scroll: () -> Float) {
    Box(Modifier.fillMaxWidth().height(height.coerceIn(520.dp, 900.dp))) {
        FieldBackdrop(scroll, Modifier.fillMaxSize())
        Column(
            Modifier
                .align(Alignment.Center)
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .column()
                .graphicsLayer {
                    // Title drifts up and fades faster than the page scrolls.
                    val s = scroll().coerceAtMost(size.height * 2)
                    translationY = -s * 0.35f
                    alpha = (1f - s / (size.height * 1.4f)).coerceIn(0f, 1f)
                },
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Eyebrow("A calculator for the abstaining")
            KineticTitle("Meat", 92.sp, Piety.Oat)
            KineticTitle("Piety", 92.sp, Piety.Sage)
            Text(
                "A ledger of the dead who stayed that way, on your account.",
                style = MaterialTheme.typography.headlineMedium.copy(fontSize = 20.sp),
                color = Piety.Lichen,
            )
            Spacer(Modifier.height(8.dp))
            Eyebrow("Drag across the title · Scroll to begin")
        }
    }
}

@Composable
private fun Eyebrow(text: String, color: Color = Piety.Fern) {
    Text(text.uppercase(), style = MaterialTheme.typography.labelSmall, color = color)
}

@Composable
private fun NumberField(value: String, onChange: (String) -> Unit, label: String, modifier: Modifier) {
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.filter(Char::isDigit).take(MAX_INPUT_DIGITS)) },
        label = { Text(label) },
        singleLine = true,
        textStyle = MaterialTheme.typography.displayMedium.copy(fontSize = 30.sp, color = Piety.Oat),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Piety.Sage, unfocusedBorderColor = Piety.Rule,
            focusedLabelColor = Piety.Sage, unfocusedLabelColor = Piety.Lichen, cursorColor = Piety.Sage,
            focusedContainerColor = Piety.Loam, unfocusedContainerColor = Piety.Loam,
        ),
        modifier = modifier,
    )
}

@Composable
private fun ChapterHead(number: String, title: String, caption: String) {
    Column(
        Modifier.column().padding(top = 96.dp, bottom = 16.dp).reveal(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(number, style = MaterialTheme.typography.labelLarge, color = Piety.Fern)
            Box(Modifier.height(1.dp).width(48.dp).background(Piety.Moss))
        }
        Text(title, style = MaterialTheme.typography.displayMedium, color = Piety.Oat)
        Text(caption, style = MaterialTheme.typography.bodyMedium, color = Piety.Lichen)
    }
}

private fun LazyListScope.chapter(
    key: String,
    number: String,
    title: String,
    caption: String,
    animals: List<SparedAnimal>,
    accent: Color,
    figure: (@Composable () -> Unit)? = null,
) {
    item(key = "$key-head") { ChapterHead(number, title, caption) }
    item(key = "$key-total") {
        Column(Modifier.column().padding(bottom = 24.dp).reveal(120)) {
            Counter(animals.sparedTotal(), MaterialTheme.typography.displayLarge, accent) { format(it) }
            Text("animals spared", style = MaterialTheme.typography.titleMedium, color = Piety.Lichen)
            if (figure != null) {
                Spacer(Modifier.height(16.dp))
                figure()
            }
        }
    }
    val max = animals.maxOfOrNull { abs(it.count) } ?: 0.0
    itemsIndexed(animals, key = { _, it -> "$key-${it.name}" }) { i, animal ->
        LedgerRow(animal, max, accent, Modifier.column().padding(vertical = 10.dp).reveal(delayMillis = (i % 4) * 60))
    }
}

@Composable
private fun LedgerRow(animal: SparedAnimal, max: Double, accent: Color, modifier: Modifier) {
    val backward = animal.existsBecauseOfDemand
    val colour = if (backward) Piety.Clay else accent
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(animal.name, style = MaterialTheme.typography.titleMedium, color = Piety.Oat, modifier = Modifier.weight(1f))
            Counter(animal.count, MaterialTheme.typography.bodyLarge, colour) {
                if (backward) "${format(it)} fewer will ever be born" else "${format(it)} spared"
            }
        }
        LogBar(animal.count, max, colour)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Eulogy(modifier: Modifier) {
    val words = (
        "Buffalo don't run on this ledger like the rest. Ranching is most of why they still " +
            "exist at all — fewer eaten means fewer bred, not fewer killed. Every other line here is " +
            "a rescue. That one's a eulogy."
        ).split(" ")
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier
            .reveal()
            .background(Piety.Loam, shape)
            .border(1.dp, Piety.Clay.copy(alpha = 0.4f), shape)
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Eyebrow("The line that runs backward", Piety.Clay)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            val light = MaterialTheme.typography.headlineMedium.copy(fontFamily = azrienoch(Axes(wght = 300)))
            val heavy = MaterialTheme.typography.headlineMedium.copy(fontFamily = azrienoch(Axes(wght = 820, serf = 100)))
            words.forEachIndexed { i, w ->
                // Words arrive one by one; the last sentence lands in heavy slab.
                val last = i >= words.size - 3
                Text(
                    w,
                    style = if (last) heavy else light,
                    color = if (last) Piety.Clay else Piety.Oat,
                    modifier = Modifier.reveal(delayMillis = i * 35),
                )
            }
        }
    }
}

@Composable
private fun MarketRow(effect: MarketEffect, modifier: Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(effect.name, style = MaterialTheme.typography.titleMedium, color = Piety.Oat, modifier = Modifier.weight(1f))
            Text(formatPercent(effect.percentPriceChange), style = MaterialTheme.typography.bodyLarge, color = Piety.Sage)
        }
        Text(
            "${formatPrice(effect.currentPrice)} → ${formatPrice(effect.newPrice)} ${effect.unit}",
            style = MaterialTheme.typography.bodyMedium, color = Piety.Lichen,
        )
        // Linear scale here: price changes all sit within one order of magnitude.
        LinearBar(abs(effect.percentPriceChange) / 0.4, if (effect.belowBreakeven) Piety.Clay else Piety.Fern)
        if (effect.belowBreakeven) {
            Text(
                "Past an illustrative producer-margin line — herds would get thinned, not just margins.",
                style = MaterialTheme.typography.bodySmall, color = Piety.Clay,
            )
        }
    }
}

@Composable
private fun PrecedentRow(p: ExtinctionPrecedent, state: LazyListState, key: String, parallax: Boolean) {
    Box(Modifier.column().padding(vertical = 20.dp)) {
        // Oversized hairline year sits behind the text and drifts against the scroll.
        Text(
            p.year.toString(),
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 120.sp, fontFamily = azrienoch(Axes(wght = 100, wdth = 75))),
            color = Piety.Moss.copy(alpha = 0.35f),
            modifier = Modifier.align(Alignment.TopEnd).parallax(state, key, rate = -0.25f, enabled = parallax),
        )
        Row(Modifier.reveal()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(end = 16.dp, top = 4.dp)) {
                Box(Modifier.size(12.dp).background(Piety.Sage, RoundedCornerShape(6.dp)))
                Box(Modifier.width(1.dp).height(120.dp).background(Piety.Moss))
            }
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Eyebrow("${p.year} · ${p.species}")
                Text(p.event, style = MaterialTheme.typography.titleLarge, color = Piety.Oat)
                Text(p.detail, style = MaterialTheme.typography.bodyMedium, color = Piety.Lichen)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Recap(
    personal: List<SparedAnimal>,
    sphere: List<SparedAnimal>,
    world: WorldTally,
    market: List<MarketEffect>,
    modifier: Modifier,
) {
    val top = personal.filterNot { it.existsBecauseOfDemand }.maxByOrNull { it.count }
    val steepest = market.minByOrNull { it.percentPriceChange }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Eyebrow("In sum")
        Text("The whole ledger", style = MaterialTheme.typography.displayMedium, color = Piety.Oat)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 2,
        ) {
            Tile("You", format(personal.sparedTotal()), "animals spared", Piety.Sage, Modifier.weight(1f))
            Tile("Your circle", format(sphere.sparedTotal()), "if they followed", Piety.Fern, Modifier.weight(1f))
            Tile("Everyone abstaining", format(world.animals.sparedTotal()), "over the same span", Piety.Sprout, Modifier.fillMaxWidth())
            if (top != null) Tile("Most spared", top.name, "${format(top.count)} of them", Piety.Sage, Modifier.weight(1f))
            if (steepest != null) {
                Tile("Steepest price drop", steepest.name, formatPercent(steepest.percentPriceChange), Piety.Clay, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Tile(label: String, value: String, note: String, accent: Color, modifier: Modifier) {
    val shape = RoundedCornerShape(24.dp)
    Column(
        modifier
            .reveal()
            .background(Piety.Loam, shape)
            .border(1.dp, Piety.Rule, shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Eyebrow(label, Piety.Lichen)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = accent)
        Text(note, style = MaterialTheme.typography.bodySmall, color = Piety.Lichen)
    }
}
