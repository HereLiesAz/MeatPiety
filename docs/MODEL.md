# The model

How Meat Piety turns "days lived without meat" into animals, prices, land
and lives. Every figure on screen comes from the functions named here, in
`shared/src/commonMain/kotlin/com/hereliesaz/meatpiety/`. Sources for every
constant are in [SOURCES.md](SOURCES.md).

## Layers

The same calculation runs three times, each on one global market per species:

| Layer | Who | Diet counted | Baseline |
|---|---|---|---|
| `personalLayer` | You | Standard U.S. diet, U.S. yields | Today's market (marginal) |
| `influenceLayer` | People you persuaded | World-average diet, world yields, half your span | Today's market (marginal) |
| `worldLayer` | Every vegan and vegetarian | World-average diet, world yields | A world without them (counterfactual) |

Today's prices already include today's abstainers. So the world layer is
measured against the market they would have bought into: its price change
says how much cheaper today is than a world without them
(`Impact.counterfactualUsdPrice`). Marginal layers shift today's market.

## Units

`Species.unit` is the retail unit (lb, dozen eggs, gallon milk).
`unitsPerAnimal` turns units into whole animals: a carcass for meat, a
productive lifetime of output for hens (~1 laying year) and dairy cows
(~2.9 milking years). Counts are always whole animals, never animal-years.
World-scope layers use `worldUnitsPerAnimal` where yields differ sharply
(dairy, eggs).

## The market

One constant-elasticity market per species. Removing a share `s` of demand:

~~~
long run   ln P' = ln(1 − s) / (es + ed)     ln Q' = es · ln P'
short run  ln P' = ln(1 − s) / ed            (supply fixed)
~~~

`ed` is the own-price demand elasticity, and `es` the long-run supply
elasticity. `es` is backed out of Norwood & Lusk's cumulative elasticity
factor (CEF, the share of a demand cut that becomes a production cut):

~~~
es = CEF · ed / (1 − CEF)        so   es / (es + ed) = CEF
~~~

Seafood: wild catch is quota-bound, so only the farmed share responds:
`es_effective = es · farmedShare`.

**Market size** is `worldPopulation(year) × usPerCapita × worldToUsRatio`,
scaled by `marketFactor` for population and per-capita growth (UN WPP 2024
medium variant; +0.2%/yr intake, OECD-FAO). Ledgers average the factor over
their span (`pastMarketFactor`). See `Population.kt`.

## The chain in time (`impact`)

1. **Bred anyway.** Your skipped purchases during the supply lag were
   animals already bred: `bredAnyway`. They clear at the short-run price,
   and other buyers take them (rebound).
2. **Cull wave.** The lower price shrinks the breeding herd at once:
   `breedersNotKept = cutPerDay × 365 / offspringPerBreederYear`. Culling
   them early is extra slaughter now. It unwinds as their scheduled
   retirements never come: `cullWave = breeders × max(0, 1 − replacementRate · years)`.
3. **Births fall** after one gestation:
   `neverBred = cutPerDay × (days − gestation)⁺`. Whole litters and clutches:
   `birthingsNotNeeded = neverBred / perBirthing`.
4. **Slaughter falls** after the supply lag (gestation + slaughter age; ~90
   days for egg and milk producers, who cut by culling):
   `netAnimals = cutPerDay × (days − lag)⁺ − cullWave`. This can be negative
   in the first weeks.
5. **Price settles** at the long-run equilibrium.

`cutPerDay = −ΔQ · market / 365 / unitsPerAnimal` is the long-run cut in
animals a day.

### Also derived

- **Replacement breeders never born:** `breeders × replacementRate × (days − gestation)⁺ / 365`.
- **Offspring of unborn breeders:** the share of `neverBred` that the unborn
  replacements would have had once they started breeding at
  `firstBreedingDays`. It is integrated in closed form, and it is a
  breakdown of `neverBred`, not an addition.
- **Male chicks and calves:** `malesNeverBorn = neverBred × malesPerAnimal`.
  That is about one male chick killed at hatch per pullet, and about 1.3
  male calves per dairy cow lifetime.
- **Fewer in barns now:** `cutPerDay × min((days − gestation)⁺, slaughterAge)`.
  These are animals that would be standing in barns today but were never born.
- **Animal-days not lived:** the time integral of fewer-in-barns.
- **Feed fish:** `netUnits × feedFishPerUnit`. That is wild forage fish not
  caught for aquaculture (fish-in:fish-out 0.3 kg/kg ÷ ~20 g a fish).
- **Retail sales lost:** this is retail spending, not farm income.
- **Total never born:** `neverBred + replacementsNeverBred + malesNeverBorn`.

### Dairy–beef link (`layerOf`)

About a fifth of beef comes from the dairy herd. A dairy cut shifts beef
supply left by `0.2 × dairy cut`, which raises the beef price. The beef herd
then grows by `es · Δln P`. Those extra beef cattle (`dairyBeefOffset`) are
subtracted from the layer's total.

### Range

`personalRange` reruns the personal layer with each CEF moved by ±0.1. That
gives the "plausibly X–Y" band on the headline.

## Consolidation (`Consolidation.kt`)

Lower prices close smaller, higher-cost farms first. The non-factory share
shrinks with the price drop:

~~~
n = n0 · max(0, 1 − drop / smallMargin)      smallMargin = breakevenDrop / 2
~~~

`shiftedToFactory` counts the animals still bred that are raised in factory
farms instead. U.S. shares are scaled by 0.74 / 0.99 for world-scope layers.
Bison have no factory system.

## Footprint (`Footprint.kt`)

Production avoided × Poore & Nemecek per-kg CO₂e, land and water. Beef
blends 80% beef-herd and 20% dairy-herd figures. The displayed figure is
**net** of the replacement food: protein-matched tofu for everything not
bought (`Impact.replacement`). Early on, before production responds, net
CO₂e can be negative.

## Soy and tofu (`Outlook.kt`)

Soy freed is `netUnits × kg per unit × soy per kg` (WWF/Profundo), applied
to farmed fish only. Tofu is 2 kg per kg of beans. The share of world tofu
is per year, against a 3.5 Mt midpoint (no authoritative figure exists).

## Herd outlook (`herdOutlook`)

Relative to today, for a chosen abstainer growth scenario (flat, 1% or 3% a
year):

~~~
herd(t) / herd(0) = [ marketFactor(t) · (1 − s(t)) / (1 − s0) ] ^ (es / (es + ed))
s(t) = abstainers(t) / worldPopulation(t)
~~~

A constant-elasticity market never reaches zero, so the outlook gives a
path (half-life, 25/50/100-year herd), not an extinction date. Survival
outside farming is judged against the Traill et al. (2007) minimum viable
population (4,169), and only where a wild or sanctuary population has been
counted.

## Projections (`projections`)

- **Personal:** your ledger 1, 5, 10 and 25 years on.
- **World:** one year of abstention at that horizon's headcount, in that
  year's market.

## Influence (`Influence.kt`)

~~~
followers → reached   (organic reach per platform; extra platforms × 0.5 overlap)
reached   → persuaded (1 − e^(−0.002 · years))
persuaded → stayed    (× 0.16; Faunalytics lapse rate)
~~~

Nothing shows until a linked account returns a follower count.

## Milestones and notifications

`milestones()` lists thresholds crossed: whole animals per species, pounds,
money, litters, rebound, bison, outliving a broiler, pig or cow, followers
out-sparing you, world price drops and fewer breeders.

Notifications fire only while the user is away. In the foreground the app
marks every crossed milestone as known.
- **Android:** a daily WorkManager job recomputes the ledger at the new day
  count and notifies the new milestones.
- **Web:** the page hands its service worker a 60-day schedule, and a daily
  Periodic Background Sync shows the due ones. This needs an installed PWA
  in a Chromium browser.

## Not counted

These are shown in the app as not counted:
- trade and regional prices;
- subsidies and price supports;
- herd cycles and cold storage;
- wild-catch bycatch and discards;
- animals killed in crop farming;
- what spared land does for wild animals;
- differences in capacity to suffer.
