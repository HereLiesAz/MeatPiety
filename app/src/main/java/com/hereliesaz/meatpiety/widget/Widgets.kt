package com.hereliesaz.meatpiety.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.hereliesaz.meatpiety.MainActivity
import com.hereliesaz.meatpiety.WidgetSnapshot
import com.hereliesaz.meatpiety.compact
import com.hereliesaz.meatpiety.widgetSnapshot

/*
 * Five home-screen widgets, one idea each. Dark earth palette from the app
 * (Theme.kt); every text colour clears WCAG AA on its background, numbers are
 * large, and each widget carries a full-sentence content description for
 * screen readers. Tapping opens the app. Values come from widgetSnapshot(),
 * the same model as the app, refreshed every few hours and when the app closes.
 */

private object Ink {
    val Soil = Color(0xFF0D120E)
    val Loam = Color(0xFF141B16)
    val Moss = Color(0xFF3E5A3A)
    val Sage = Color(0xFFA8C69A)
    val Sprout = Color(0xFFD6E8B8)
    val Oat = Color(0xFFEDE6D6)
    val Lichen = Color(0xFFB4BEAF) // lightened from the app's Lichen for small text contrast
    val Clay = Color(0xFFD9895F)
    val Gold = Color(0xFFC89B53)
}

private fun c(color: Color) = ColorProvider(color)

private val Eyebrow = TextStyle(color = c(Ink.Lichen), fontSize = 11.sp, fontWeight = FontWeight.Medium)
private val Body = TextStyle(color = c(Ink.Lichen), fontSize = 13.sp)

@Composable
private fun Card(description: String, content: @Composable ColumnScope.() -> Unit) {
    Column(
        GlanceModifier
            .fillMaxSize()
            .background(Ink.Soil)
            .cornerRadius(24.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>())
            .semantics { contentDescription = description },
        content = content,
    )
}

@Composable
private fun Empty() = Card("Meat Piety: open the app to pick the day you started.") {
    Text("MEAT PIETY", style = Eyebrow)
    Spacer(GlanceModifier.height(8.dp))
    Text("Pick the day you started.", style = TextStyle(color = c(Ink.Oat), fontSize = 16.sp, fontWeight = FontWeight.Bold))
    Text("Tap to open.", style = Body)
}

private fun whole(n: Double) = if (n < 1) "none yet" else compact(kotlin.math.floor(n))

abstract class LedgerWidgetBase : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(120.dp, 120.dp), DpSize(250.dp, 120.dp), DpSize(250.dp, 250.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = widgetSnapshot(context)
        provideContent { if (snapshot == null) Empty() else Draw(snapshot) }
    }

    @Composable
    abstract fun Draw(s: WidgetSnapshot)
}

/** 1. The headline: fewer animals slaughtered, and the day count. */
class LedgerWidget : LedgerWidgetBase() {
    @Composable
    override fun Draw(s: WidgetSnapshot) {
        val n = whole(s.fewerSlaughtered)
        Card("Meat Piety: $n fewer animals slaughtered after ${s.days} days ${if (s.vegan) "vegan" else "vegetarian"}.") {
            Text("FEWER SLAUGHTERED", style = Eyebrow)
            Spacer(GlanceModifier.defaultWeight())
            Text(n, style = TextStyle(color = c(Ink.Sprout), fontSize = 44.sp, fontWeight = FontWeight.Bold))
            Text("day ${s.days} · ${if (s.vegan) "vegan" else "vegetarian"}", style = Body)
        }
    }
}

/** 2. Never born: everyone who wasn't, and who they were. */
class NeverBornWidget : LedgerWidgetBase() {
    @Composable
    override fun Draw(s: WidgetSnapshot) {
        val top = s.bySpecies.take(3)
        Card(
            "Meat Piety: ${whole(s.neverBorn)} animals never born, including ${whole(s.litters)} whole litters and " +
                "${whole(s.malesNeverBorn)} male chicks and calves. " + top.joinToString { "${it.first}: ${whole(it.second)}" },
        ) {
            Text("NEVER BORN", style = Eyebrow)
            Text(whole(s.neverBorn), style = TextStyle(color = c(Ink.Oat), fontSize = 36.sp, fontWeight = FontWeight.Bold))
            Text("${whole(s.litters)} litters · ${whole(s.malesNeverBorn)} males", style = TextStyle(color = c(Ink.Clay), fontSize = 13.sp))
            Spacer(GlanceModifier.height(8.dp))
            val max = top.firstOrNull()?.second?.coerceAtLeast(1.0) ?: 1.0
            top.forEach { (name, n) ->
                Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(name, style = Body, modifier = GlanceModifier.width(96.dp))
                    LinearProgressIndicator(
                        progress = (n / max).toFloat(),
                        modifier = GlanceModifier.defaultWeight().height(6.dp),
                        color = c(Ink.Sage),
                        backgroundColor = c(Ink.Moss),
                    )
                    Text(" ${whole(n)}", style = TextStyle(color = c(Ink.Oat), fontSize = 13.sp, fontWeight = FontWeight.Bold))
                }
                Spacer(GlanceModifier.height(4.dp))
            }
        }
    }
}

/** 3. The next whole animal: a progress bar you can watch fill. */
class NextAnimalWidget : LedgerWidgetBase() {
    @Composable
    override fun Draw(s: WidgetSnapshot) {
        val next = s.nextAnimal
        if (next == null) {
            Card("Meat Piety: your first animal is still on its way.") {
                Text("NEXT ANIMAL", style = Eyebrow)
                Text("On its way.", style = TextStyle(color = c(Ink.Oat), fontSize = 20.sp, fontWeight = FontWeight.Bold))
            }
            return
        }
        val pct = (next.progress * 100).toInt()
        val wait = if (next.daysToNext <= 1) "any day now" else "in ${next.daysToNext} days"
        Card("Meat Piety: next ${next.singular} never bred is $pct percent of the way, $wait. ${next.count} so far.") {
            Text("NEXT ${next.singular.uppercase()}", style = Eyebrow)
            Spacer(GlanceModifier.defaultWeight())
            Text("$pct%", style = TextStyle(color = c(Ink.Sprout), fontSize = 36.sp, fontWeight = FontWeight.Bold))
            Spacer(GlanceModifier.height(6.dp))
            LinearProgressIndicator(
                progress = next.progress.toFloat(),
                modifier = GlanceModifier.fillMaxWidth().height(10.dp),
                color = c(Ink.Sprout),
                backgroundColor = c(Ink.Moss),
            )
            Spacer(GlanceModifier.height(6.dp))
            Text("$wait · ${next.count} so far", style = Body)
        }
    }
}

/** 4. The planet: emissions, land and water, net of the food eaten instead. */
class FootprintWidget : LedgerWidgetBase() {
    @Composable
    override fun Draw(s: WidgetSnapshot) {
        val co2 = if (s.co2eKg >= 1000) "${compact(s.co2eKg / 1000)} t" else "${compact(s.co2eKg.coerceAtLeast(0.0))} kg"
        val land = if (s.landM2 >= 10_000) "${compact(s.landM2 / 10_000)} ha" else "${compact(s.landM2.coerceAtLeast(0.0))} m²"
        val water = if (s.waterL >= 1e6) "${compact(s.waterL / 1000)} m³" else "${compact(s.waterL.coerceAtLeast(0.0))} L"
        Card("Meat Piety: spared $co2 of greenhouse gas, $land of land and $water of water, after the food eaten instead.") {
            Text("SPARED, NET", style = Eyebrow)
            Spacer(GlanceModifier.defaultWeight())
            Row(GlanceModifier.fillMaxWidth()) {
                Stat("CO₂e", co2, Ink.Sprout, GlanceModifier.defaultWeight())
                Stat("LAND", land, Ink.Sage, GlanceModifier.defaultWeight())
                Stat("WATER", water, Ink.Oat, GlanceModifier.defaultWeight())
            }
        }
    }

    @Composable
    private fun Stat(label: String, value: String, color: Color, modifier: GlanceModifier) {
        Column(modifier) {
            Text(value, style = TextStyle(color = c(color), fontSize = 20.sp, fontWeight = FontWeight.Bold))
            Text(label, style = Eyebrow)
        }
    }
}

/** 5. Outlived: how many farm lifetimes your span already covers. */
class OutlivedWidget : LedgerWidgetBase() {
    @Composable
    override fun Draw(s: WidgetSnapshot) {
        val rows = s.lifetimesOutlived.take(3)
        val lead = rows.firstOrNull()
        Card(
            "Meat Piety: in ${s.days} days you've outlived " +
                (rows.joinToString { "${it.second} ${it.first} lifetimes" }.ifEmpty { "no farm lifetime yet" }) + ".",
        ) {
            Text("YOU'VE OUTLIVED", style = Eyebrow)
            Spacer(GlanceModifier.defaultWeight())
            if (lead == null) {
                Text("Not even a chicken. Yet.", style = TextStyle(color = c(Ink.Oat), fontSize = 18.sp, fontWeight = FontWeight.Bold))
            } else {
                Text("${lead.second}", style = TextStyle(color = c(Ink.Gold), fontSize = 40.sp, fontWeight = FontWeight.Bold))
                Text("${lead.first} lifetimes", style = TextStyle(color = c(Ink.Oat), fontSize = 14.sp))
                rows.drop(1).forEach { (name, n) -> Text("$n × $name", style = Body) }
            }
        }
    }
}

class LedgerWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = LedgerWidget() }
class NeverBornWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = NeverBornWidget() }
class NextAnimalWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = NextAnimalWidget() }
class FootprintWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = FootprintWidget() }
class OutlivedWidgetReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget = OutlivedWidget() }

/** Refreshes every widget; called when the app goes to the background. */
suspend fun updateAllWidgets(context: Context) {
    LedgerWidget().updateAll(context)
    NeverBornWidget().updateAll(context)
    NextAnimalWidget().updateAll(context)
    FootprintWidget().updateAll(context)
    OutlivedWidget().updateAll(context)
}
