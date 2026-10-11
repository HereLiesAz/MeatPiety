package com.hereliesaz.meatpiety

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
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
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.abs
import kotlin.math.log10
import kotlinx.coroutines.delay

/*
 * Layout: scrollytelling chapters closing on a bento recap.
 *   Hero (parallax field, interactive variable title) → inputs →
 *   accounts → chapters (yours, your reach [only with a linked account], the
 *   world), each a counting headline over log-scaled bars → buffalo eulogy →
 *   market → the animal (breeding) → precedent timeline → bento recap.
 *   Milestones are notified only while the user is away (Notifier).
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

@Composable
private fun LedgerScreen() {
    var startDay by remember { mutableStateOf(StartDate.epochDay() ?: (todayEpochDay() - 365)) }
    var picking by remember { mutableStateOf(false) }
    var diet by remember { mutableStateOf(Storage.get("diet")?.let { d -> Diet.entries.firstOrNull { it.name == d } } ?: Diet.VEGAN) }
    val handles = remember { mutableStateMapOf<Platform, String>() }
    val lookups = remember { mutableStateMapOf<Platform, FollowerLookup>() }
    var prices by remember { mutableStateOf(emptyMap<String, Double>()) }
    var money by remember { mutableStateOf(Money.USD) }
    var growth by remember { mutableStateOf(DEFAULT_GROWTH) }

    LaunchedEffect(Unit) {
        prices = LiveData.prices()
        LiveData.money()?.let { money = it }
    }

    val days = (todayEpochDay() - startDay).toInt().coerceAtLeast(0)
    val accounts = handles.filterValues { it.isNotBlank() }.map { (p, h) ->
        LinkedAccount(p, h, lookups[p] ?: FollowerLookup.Pending)
    }
    val funnel = influenceFunnel(accounts, days / 365.0)

    val personal = remember(days, diet, prices) { personalLayer(days, diet, prices) }
    val personalBand = remember(days, diet, prices) { personalRange(days, diet, prices) }
    val influence = remember(funnel, days, diet, prices) { funnel?.let { influenceLayer(it.stayed, days, diet, prices) } }
    val world = remember(days, prices) { worldLayer(days, prices) }
    val notices = remember(days, personal, influence, world, money) { milestones(days, personal, influence, world, money) }
    LaunchedEffect(notices) { Notifier.foreground(LedgerSnapshot(days, diet, funnel?.stayed, money), notices) }

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
                                onClick = { diet = option; Storage.set("diet", option.name) },
                                shape = SegmentedButtonDefaults.itemShape(index, Diet.entries.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = Piety.Moss, activeContentColor = Piety.Oat,
                                    inactiveContainerColor = Color.Transparent, inactiveContentColor = Piety.Lichen,
                                    activeBorderColor = Piety.Fern, inactiveBorderColor = Piety.Rule,
                                ),
                            ) { Text(if (option == Diet.VEGAN) "Vegan" else "Vegetarian") }
                        }
                    }
                    StartDateField(startDay, days) { picking = true }
                    if (picking) {
                        StartDatePicker(
                            initial = startDay,
                            onPick = { startDay = it; StartDate.setEpochDay(it); picking = false },
                            onDismiss = { picking = false },
                        )
                    }
                }
            }
            item(key = "accounts") {
                Column(
                    Modifier.column().padding(top = 32.dp).reveal(),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Eyebrow("Your reach · optional")
                    Text(
                        "Add your handles and we'll read your follower counts. Leave them blank and the " +
                            "influence chapter stays out of the ledger.",
                        style = MaterialTheme.typography.bodyMedium, color = Piety.Lichen,
                    )
                    Platform.entries.forEach { platform ->
                        HandleField(
                            platform = platform,
                            value = handles[platform].orEmpty(),
                            lookup = lookups[platform],
                            onChange = {
                                handles[platform] = it
                                lookups.remove(platform)
                            },
                            onLookup = { lookups[platform] = it },
                        )
                    }
                }
            }

            chapter(
                key = "yours", number = "01", title = "Your ledger",
                caption = "Measured against a standard U.S. diet. Gross is what you didn't buy; net is what the " +
                    "market actually stopped raising once cheaper prices tempted someone else.",
                layer = personal, accent = Piety.Sage, money = money, days = days, band = personalBand,
            ) {
                Pictogram(
                    count = personal.netSpared, cap = 200, color = Piety.Sage,
                    modifier = Modifier.fillMaxWidth().aspectRatio(2f).padding(vertical = 8.dp),
                )
                Text("ONE MARK PER ANIMAL NEVER RAISED, UP TO 200", style = MaterialTheme.typography.labelSmall, color = Piety.Lichen)
            }

            if (funnel != null && influence != null) {
                chapter(
                    key = "circle", number = "02", title = "If your conscience were contagious",
                    caption = "${format(funnel.followers.toDouble())} followers → ${format(funnel.reached)} who " +
                        "regularly see you → ${format(funnel.persuaded)} persuaded → ${format(funnel.stayed)} " +
                        "still at it (84% quit). Each counted at the world-average diet for half your span. " +
                        "Persuasion rate is a guess, labelled as one.",
                    layer = influence, accent = Piety.Fern, money = money, days = days,
                ) {
                    Ripples(
                        rings = 1 + log10(1.0 + funnel.followers).toInt(), color = Piety.Fern,
                        modifier = Modifier.fillMaxWidth().height(160.dp),
                    )
                }
            }

            chapter(
                key = "world", number = if (funnel != null) "03" else "02", title = "Meanwhile, out there",
                caption = "An estimated ${format(VEGANS_WORLDWIDE.toDouble())} vegans and " +
                    "${format(VEGETARIANS_WORLDWIDE.toDouble())} vegetarians, at the world-average diet, for " +
                    "the same $days days. At this scale the price falls far enough to matter.",
                layer = world, accent = Piety.Sprout, money = money, days = days,
            )

            item(key = "future-head") {
                ChapterHead(
                    "→", "If you keep going",
                    "Your ledger at each horizon if you stay ${if (diet == Diet.VEGAN) "vegan" else "vegetarian"}, and the world's " +
                        "under the abstainer-growth scenario below. No one measures a global trend; Gallup finds the U.S. share flat.",
                )
            }
            item(key = "growth-pick") {
                Row(Modifier.column(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GROWTH_SCENARIOS.forEach { g ->
                        val on = g == growth
                        Text(
                            if (g == 0.0) "Flat" else "+${(g * 100).toInt()}% a year",
                            style = MaterialTheme.typography.labelLarge,
                            color = if (on) Piety.Soil else Piety.Lichen,
                            modifier = Modifier
                                .background(if (on) Piety.Sage else Piety.Loam, RoundedCornerShape(12.dp))
                                .clickable { growth = g }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                        )
                    }
                }
            }
            itemsIndexed(projections(days, diet, prices, growth), key = { _, it -> "future-${it.yearsAhead}" }) { i, p ->
                ProjectionRow(p, money, Modifier.column().padding(vertical = 10.dp).reveal(delayMillis = (i % 4) * 60))
            }

            item(key = "extinct-head") {
                ChapterHead(
                    "†", "How long they last",
                    "Meat demand grows ~1.1% a year with population and income (OECD-FAO); abstainers take a growing share under " +
                        "the scenario above. The herd follows. A market never prices a species to zero, so this gives the herd's " +
                        "path, not an extinction date — and what would outlast farming if people stopped keeping them.",
                )
            }
            itemsIndexed(herdOutlook(growth), key = { _, it -> "ext-${it.species.name}" }) { i, o ->
                OutlookRow(o, Modifier.column().padding(vertical = 10.dp).reveal(delayMillis = (i % 4) * 60))
            }

            item(key = "soy") { SoyChapter(personal, world, Modifier.column().padding(top = 96.dp)) }

            item(key = "eulogy") { Eulogy(Modifier.column().padding(vertical = 72.dp)) }

            item(key = "market-head") {
                ChapterHead(
                    if (funnel != null) "04" else "03", "What happens to the price",
                    "World abstention as a demand shock on one global market per species, modelled from " +
                        "published elasticities. U.S. retail prices${if (prices.isEmpty()) " (built-in)" else " (live, BLS)"}" +
                        ", converted to ${money.code}.",
                )
            }
            itemsIndexed(world.impacts, key = { _, it -> "market-${it.species.name}" }) { i, impact ->
                MarketRow(impact, personal.impacts.firstOrNull { it.species == impact.species }, money,
                    Modifier.column().padding(vertical = 10.dp).reveal(delayMillis = (i % 4) * 60))
            }

            item(key = "animal-head") {
                ChapterHead(
                    if (funnel != null) "05" else "04", "What happens to the animal",
                    "Fewer sold means fewer bred. Breeding animals no longer kept, births no longer needed, and how long a " +
                        "breeding decision takes to reach the shelf.",
                )
            }
            itemsIndexed(world.impacts.filter { it.species.biology != null }, key = { _, it -> "bio-${it.species.name}" }) { i, impact ->
                BiologyRow(impact, personal.impacts.firstOrNull { it.species == impact.species },
                    Modifier.column().padding(vertical = 10.dp).reveal(delayMillis = (i % 4) * 60))
            }

            item(key = "history-head") {
                ChapterHead(
                    if (funnel != null) "06" else "05", "This has happened before",
                    "Demand didn't just spare buffalo. It's the reason several species exist at all — " +
                        "and the reason at least one doesn't.",
                )
            }
            itemsIndexed(EXTINCTION_PRECEDENTS, key = { _, it -> "p-${it.species}" }) { _, p ->
                PrecedentRow(p, state, "p-${p.species}", parallax = !reduced)
            }

            item(key = "caveats") {
                Column(Modifier.column().padding(top = 72.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Eyebrow("What this doesn't count")
                    Text(
                        "Trade and regional prices (one world market stands in), subsidies and price supports, herd cycles " +
                            "and cold storage (one lag stands in), wild-catch bycatch and discards, animals killed in crop " +
                            "farming, what spared land does for wild animals, and how much each animal can suffer: a shrimp " +
                            "and a cow each count as one. Every figure is a central estimate from the sources in the README.",
                        style = MaterialTheme.typography.bodySmall, color = Piety.Lichen,
                    )
                }
            }

            item(key = "recap") { Recap(personal, influence, world, money, Modifier.column().padding(top = 96.dp)) }
        }
    }
}

/** Handle entry; looks the account up once typing pauses. */
@Composable
private fun HandleField(
    platform: Platform,
    value: String,
    lookup: FollowerLookup?,
    onChange: (String) -> Unit,
    onLookup: (FollowerLookup) -> Unit,
) {
    LaunchedEffect(value) {
        if (value.isBlank()) return@LaunchedEffect
        delay(900)
        onLookup(FollowerLookup.Pending)
        onLookup(LiveData.followers(platform, value.trim()))
    }
    val status = when (lookup) {
        is FollowerLookup.Found -> "${format(lookup.followers.toDouble())} followers"
        is FollowerLookup.Failed -> "Couldn't read @${value.trim().removePrefix("@")}: ${lookup.reason}"
        FollowerLookup.Pending -> "Looking up…"
        null -> null
    }
    OutlinedTextField(
        value = value,
        onValueChange = { onChange(it.take(120)) },
        label = { Text(platform.label) },
        placeholder = { Text(platform.hint) },
        supportingText = status?.let { { Text(it, color = if (lookup is FollowerLookup.Failed) Piety.Clay else Piety.Lichen) } },
        singleLine = true,
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = Piety.Oat),
        shape = RoundedCornerShape(16.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Piety.Sage, unfocusedBorderColor = Piety.Rule,
            focusedLabelColor = Piety.Sage, unfocusedLabelColor = Piety.Lichen, cursorColor = Piety.Sage,
            focusedContainerColor = Piety.Loam, unfocusedContainerColor = Piety.Loam,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun Hero(height: Dp, scroll: () -> Float) {
    Box(Modifier.fillMaxWidth().height(height.coerceIn(520.dp, 900.dp))) {
        FieldBackdrop(scroll, Modifier.fillMaxSize())
        Box(Modifier.matchParentSize().inkPatina())
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

private const val DAY_MS = 86_400_000L

@Composable
private fun StartDateField(startDay: Long, days: Int, onClick: () -> Unit) {
    val shape = RoundedCornerShape(16.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(Piety.Loam, shape)
            .border(1.dp, Piety.Rule, shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Text("Began on", style = MaterialTheme.typography.labelSmall, color = Piety.Lichen)
        Text(StartDate.label(startDay), style = MaterialTheme.typography.displayMedium.copy(fontSize = 30.sp), color = Piety.Oat)
        Text("$days days so far · counts up daily", style = MaterialTheme.typography.bodySmall, color = Piety.Lichen)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StartDatePicker(initial: Long, onPick: (Long) -> Unit, onDismiss: () -> Unit) {
    val today = todayEpochDay()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial * DAY_MS,
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) = utcTimeMillis / DAY_MS <= today
        },
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = { state.selectedDateMillis?.let { onPick(it / DAY_MS) } ?: onDismiss() }) { Text("Set") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = state, title = { Text("When did you start?", Modifier.padding(start = 24.dp, top = 16.dp)) })
    }
}

@Composable
private fun ChapterHead(number: String, title: String, caption: String) {
    Column(
        Modifier.column().padding(top = 96.dp, bottom = 16.dp).reveal(),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(number, style = MaterialTheme.typography.labelLarge, color = Piety.Gold)
            Canvas(Modifier.height(8.dp).width(48.dp)) {
                val y = size.height / 2f
                drawLine(Piety.Gold.copy(alpha = 0.56f), Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                drawCircle(Piety.Gold, radius = 2.dp.toPx(), center = Offset(size.width, y))
            }
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
    layer: Layer,
    accent: Color,
    money: Money,
    days: Int,
    band: Pair<Double, Double>? = null,
    figure: (@Composable () -> Unit)? = null,
) {
    item(key = "$key-head") { ChapterHead(number, title, caption) }
    item(key = "$key-total") {
        Column(Modifier.column().padding(bottom = 24.dp).reveal(120)) {
            Counter(layer.netSpared, MaterialTheme.typography.displayLarge, accent) { format(it) }
            Text("fewer animals slaughtered", style = MaterialTheme.typography.titleMedium, color = Piety.Lichen)
            if (band != null) Text(
                "Plausibly ${format(band.first)}–${format(band.second)}, as farm supply responds less or more than the central estimate.",
                style = MaterialTheme.typography.bodySmall, color = Piety.Lichen,
            )
            Text(
                "${format(layer.neverBorn)} never born, counting replacement breeders and the male chicks and calves bred alongside them" +
                    (if (layer.dairyBeefOffset > 0.005) " · ${format(layer.dairyBeefOffset)} more beef cattle raised to replace dairy beef, already subtracted" else ""),
                style = MaterialTheme.typography.bodyMedium, color = Piety.Lichen,
            )
            Text(
                "${format(layer.impacts.filterNot { it.species.existsBecauseOfDemand }.sumOf { it.fewerInBarns ?: 0.0 })} fewer in barns right now · " +
                    "${format(layer.impacts.sumOf { it.animalDaysNotLived ?: 0.0 })} days of farm life never lived",
                style = MaterialTheme.typography.titleMedium, color = accent,
            )
            if (layer.shiftedToFactory > 0) Text(
                "The cost: ${format(layer.shiftedToFactory)} animals still bred now live in factory farms, not smaller ones — lower prices close the small farms first.",
                style = MaterialTheme.typography.bodyMedium, color = Piety.Clay,
            )
            Text(
                "Net of the tofu eaten instead: ${layer.environment.describe()} not emitted, cleared or drawn " +
                    "(${format(layer.grossEnvironment.co2eKg)} kg CO₂e before the replacement).",
                style = MaterialTheme.typography.bodyMedium, color = Piety.Sprout,
            )
            Text(
                "${format(layer.grossSpared)} not bought · ${money.format(layer.impacts.sumOf { it.grossUnits * it.usdPrice })} not spent",
                style = MaterialTheme.typography.bodyMedium, color = Piety.Lichen,
            )
            if (figure != null) {
                Spacer(Modifier.height(16.dp))
                figure()
            }
        }
    }
    val max = layer.impacts.maxOfOrNull { abs(it.grossAnimals) } ?: 0.0
    itemsIndexed(layer.impacts, key = { _, it -> "$key-${it.species.name}" }) { i, impact ->
        LedgerRow(impact, max, accent, days, Modifier.column().padding(vertical = 10.dp).reveal(delayMillis = (i % 4) * 60))
    }
}

@Composable
private fun LedgerRow(impact: Impact, max: Double, accent: Color, days: Int, modifier: Modifier) {
    val backward = impact.species.existsBecauseOfDemand
    val colour = if (backward) Piety.Clay else accent
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(impact.species.name, style = MaterialTheme.typography.titleMedium, color = Piety.Oat, modifier = Modifier.weight(1f))
            Counter(impact.totalNeverBorn, MaterialTheme.typography.bodyLarge, colour) {
                "${wholeAnimals(it, days)} never bred"
            }
        }
        LogBar(impact.grossAnimals, max, colour)
        // The chain, each step feeding the next.
        val i = impact
        val bio = i.species.biology
        Text(
            "You skipped ${format(i.grossUnits)} ${i.species.unit}, ${format(i.grossAnimals)} animals' worth. " +
                "${format(i.bredAnyway)} were already bred → the surplus dropped the price ${formatTiny(i.shortRunPriceChange * 100)}% " +
                "→ others bought ${format(i.reboundAnimals)} of them cheap → farms cut breeding" +
                (bio?.let {
                    " — ${format(i.breedersNotKept ?: 0.0)} fewer breeding animals, so ${format(i.birthingsNotNeeded ?: 0.0)} whole " +
                        "${it.birthing}s of ${format(it.perBirthing)} never happen, ${format(i.replacementsNeverBred ?: 0.0)} replacement " +
                        "breeders are never born, and ${format(i.offspringOfUnbornBreeders ?: 0.0)} of the never-bred would have been " +
                        "their offspring"
                } ?: "") +
                " → ${format(i.netAnimals)} fewer slaughtered so far" +
                (i.cullWave?.takeIf { it > 0.005 }?.let { " (after ${format(it)} breeding animals culled early, which the cut hasn't yet paid back)" } ?: "") +
                (i.fewerInBarns?.let { ", ${format(it)} fewer in barns right now" } ?: "") +
                (i.malesNeverBorn?.takeIf { it > 0 }?.let { ", ${format(it)} ${if (i.species.name == "Egg-Laying Hens") "male chicks never hatched to be killed" else "male calves never born for veal or beef"}" } ?: "") +
                (if (i.forageFishNotCaught > 0) ", ${format(i.forageFishNotCaught)} wild feed fish left in the sea" else "") +
                ". Price settles ${formatTiny(i.priceChange * 100)}%" +
                (if (i.shiftedToFactory > 0) " → thinner margins push small farms out: ${format(i.shiftedToFactory)} of the animals still bred are raised in factory farms instead" else "") +
                ". " +
                "Spared: ${i.environment.describe()}.",
            style = MaterialTheme.typography.bodySmall, color = Piety.Lichen,
        )
    }
}

@Composable
private fun ProjectionRow(p: Projection, money: Money, modifier: Modifier) {
    val chicken = p.world.impacts.first { it.species.name == "Chickens" }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text("In ${p.yearsAhead} year${if (p.yearsAhead == 1) "" else "s"}", style = MaterialTheme.typography.titleMedium, color = Piety.Oat, modifier = Modifier.weight(1f))
            Text("${format(p.personal.netSpared)} fewer slaughtered", style = MaterialTheme.typography.bodyLarge, color = Piety.Sage)
        }
        Text(
            "You: ${money.format(p.personal.impacts.sumOf { it.grossUnits * it.usdPrice })} not spent. " +
                "World: ${format(p.abstainers)} abstainers, ${format(p.world.netSpared)} fewer slaughtered that year, " +
                "chicken ${formatPercent(chicken.priceChange)}.",
            style = MaterialTheme.typography.bodySmall, color = Piety.Lichen,
        )
    }
}

@Composable
private fun OutlookRow(o: HerdOutlook, modifier: Modifier) {
    val year = StartDate.label(todayEpochDay()).substringAfterLast(' ').toInt()
    val pct = { v: Double -> "${(v / o.herdNow * 100).toInt()}%" }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(o.species.name, style = MaterialTheme.typography.titleMedium, color = Piety.Oat, modifier = Modifier.weight(1f))
            Text(
                o.yearsToHalf?.let { "half gone by ~${year + it}" } ?: "${pct(o.herdIn100)} in 100 years",
                style = MaterialTheme.typography.bodyLarge, color = if (o.species.existsBecauseOfDemand) Piety.Clay else Piety.Sprout,
            )
        }
        Text(
            "${format(o.herdNow)} in farms today → ${pct(o.herdIn25)} in 25 years, ${pct(o.herdIn50)} in 50, ${pct(o.herdIn100)} in 100" +
                (o.factoryShareNow?.let { now -> " · factory-farmed ${(now * 100).toInt()}% today" + (o.factoryShareIn100?.let { ", ${(it * 100).toInt()}% in 100 years" } ?: "") } ?: "") +
                ". If farming stopped, it would live on in ${o.reservoir}" +
                when (o.reservoirViable) {
                    true -> " (~${format(o.reservoirSize!!)}): enough to persist."
                    false -> " (~${format(o.reservoirSize!!)}): too few to persist."
                    null -> ": nobody has counted, so survival can't be called."
                },
            style = MaterialTheme.typography.bodySmall, color = Piety.Lichen,
        )
    }
}

@Composable
private fun SoyChapter(personal: Layer, world: Layer, modifier: Modifier) {
    val you = personal.soy
    val all = world.soy
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Eyebrow("Soy and tofu")
        Text("The feed was the soy", style = MaterialTheme.typography.displayMedium, color = Piety.Oat)
        Text(
            "About ${(SOY_FED_TO_ANIMALS * 100).toInt()}% of the world's soy is grown to feed animals; tofu, tempeh and soy milk " +
                "are a sliver. The animals never bred on your account would have eaten ${format(you.soyFreedKg)} kg of soy — " +
                "${format(you.tofuKg)} kg of tofu, if it went to people instead.",
            style = MaterialTheme.typography.bodyMedium, color = Piety.Lichen,
        )
        Text(
            "Everyone abstaining frees ${format(all.soyFreedKg / 1e6)} thousand tonnes over the same span: " +
                "${format(all.shareOfWorldTofu * 100)}% of a year's world tofu (≈${format(GLOBAL_TOFU_KG / 1e9)} Mt, a disputed estimate).",
            style = MaterialTheme.typography.bodyMedium, color = Piety.Lichen,
        )
        Text(
            "Tofu on the shelf barely moves meat prices: grocery-scanner studies find plant proteins are weak substitutes " +
                "for beef and, at best, modest ones for chicken. Its market role runs the other way — the less meat is raised, " +
                "the more soy is left for tofu.",
            style = MaterialTheme.typography.bodySmall, color = Piety.Lichen,
        )
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
            .clip(shape)
            .inkPatina()
            .border(1.dp, Piety.Clay.copy(alpha = 0.4f), shape)
            .padding(28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Eyebrow("The line that runs backward", Piety.Clay)
        Box(Modifier.fillMaxWidth().height(1.dp).background(Piety.Gold.copy(alpha = 0.36f)))
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
private fun MarketRow(world: Impact, personal: Impact?, money: Money, modifier: Modifier) {
    val unit = world.species.unit
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(world.species.name, style = MaterialTheme.typography.titleMedium, color = Piety.Oat, modifier = Modifier.weight(1f))
            Text(formatPercent(world.priceChange), style = MaterialTheme.typography.bodyLarge, color = Piety.Sage)
        }
        Text(
            "Without the world's abstainers: ${money.format(world.counterfactualUsdPrice)} per $unit; today ${money.format(world.usdPrice)} · " +
                "demand ${formatPercent(world.demandShift).removePrefix("+")} lower · " +
                "${money.format(world.retailSalesLostUsd)} in retail sales gone over the span",
            style = MaterialTheme.typography.bodyMedium, color = Piety.Lichen,
        )
        // Linear scale here: price changes all sit within one order of magnitude.
        LinearBar(abs(world.priceChange) / 0.4, Piety.Fern)
        if (personal != null) {
            Text(
                "Your part: ${formatTiny(personal.priceChange * 100)}% on price, " +
                    "${money.format(personal.retailSalesLostUsd)} of retail sales.",
                style = MaterialTheme.typography.bodySmall, color = Piety.Lichen,
            )
        }
    }
}

@Composable
private fun BiologyRow(world: Impact, personal: Impact?, modifier: Modifier) {
    val bio = world.species.biology ?: return
    val backward = world.species.existsBecauseOfDemand
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(world.species.name, style = MaterialTheme.typography.titleMedium, color = Piety.Oat, modifier = Modifier.weight(1f))
            Text(
                "${format(world.breedersNotKept ?: 0.0)} fewer breeding animals",
                style = MaterialTheme.typography.bodyLarge, color = if (backward) Piety.Clay else Piety.Sprout,
            )
        }
        Text(
            "${format(bio.perBirthing)} per ${bio.birthing}, ${format(bio.offspringPerBreederYear)} a year per breeder · " +
                "${bio.gestationDays} days to birth + ${bio.slaughterAgeDays} to slaughter = " +
                "${bio.supplyLagDays} days before a cut reaches the shelf",
            style = MaterialTheme.typography.bodyMedium, color = Piety.Lichen,
        )
        Text(
            "World: ${format(world.birthingsNotNeeded ?: 0.0)} ${bio.birthing}s not needed." +
                (personal?.birthingsNotNeeded?.let { " Yours: ${format(it)}." } ?: ""),
            style = MaterialTheme.typography.bodySmall, color = Piety.Lichen,
        )
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
private fun Recap(personal: Layer, influence: Layer?, world: Layer, money: Money, modifier: Modifier) {
    val top = personal.impacts.filterNot { it.species.existsBecauseOfDemand }.maxByOrNull { it.netAnimals }
    val steepest = world.impacts.minByOrNull { it.priceChange }
    Column(modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Eyebrow("In sum")
        Text("The whole ledger", style = MaterialTheme.typography.displayMedium, color = Piety.Oat)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            maxItemsInEachRow = 2,
        ) {
            Tile("You", format(personal.netSpared), "fewer slaughtered", Piety.Sage, Modifier.weight(1f))
            if (influence != null) Tile("Your reach", format(influence.netSpared), "fewer slaughtered", Piety.Fern, Modifier.weight(1f))
            Tile("Your footprint", "${format(personal.environment.co2eKg)} kg", "CO₂e not emitted", Piety.Sprout, Modifier.fillMaxWidth())
            Tile("Everyone abstaining", format(world.netSpared), "fewer slaughtered, same span", Piety.Sprout, Modifier.fillMaxWidth())
            if (top != null) Tile("Most spared", top.species.name, "${format(top.netAnimals)} of them", Piety.Sage, Modifier.weight(1f))
            if (steepest != null) {
                Tile("Steepest price drop", steepest.species.name, "${formatPercent(steepest.priceChange)} · ${money.format(steepest.newUsdPrice)}", Piety.Clay, Modifier.weight(1f))
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
            .clip(shape)
            .inkPatina()
            .border(1.dp, Piety.Rule, shape)
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Eyebrow(label, Piety.Lichen)
        Text(value, style = MaterialTheme.typography.headlineMedium, color = accent)
        Text(note, style = MaterialTheme.typography.bodySmall, color = Piety.Lichen)
    }
}
