# Sources

Every input the model uses: its value, its source, and whether it can be
trusted. Constants live in the files named; change them there and here
together.

## Species (`Species.kt`)

| Input | Values | Source | Confidence |
|---|---|---|---|
| U.S. consumption per person | chicken 100 lb, turkey 15, pork 50, beef 59, lamb 1.1, duck 0.33, bison 0.096, fish 12.5, shellfish 8, eggs 23.3 dozen, milk 76 gal | USDA ERS food availability; NOAA Fisheries; USDA egg/dairy supply and use | Medium |
| Retail units per animal | chicken 4.31 lb, turkey 21.4, pig 140, cow 490, lamb 47, duck 3.3, bison 400, fish 0.81, shellfish 0.061; hen 25 dozen (one laying year); dairy cow 2,790 gal/yr × 2.9 yrs | USDA AMS / extension carcass yields, calibrated to Counting Animals per-capita counts | Medium; seafood rough |
| World yields | eggs 17.9 dozen/hen-yr; milk 655 gal/cow-yr | FAOSTAT world output ÷ laying hens / dairy cows | Medium |
| World ÷ U.S. per-capita supply | chicken 0.29, pork 0.52, beef 0.24, lamb 4.0, duck 6.0, eggs 0.64, milk 0.43, fish 0.9 | FAO food balance sheets via Our World in Data | Medium |
| Demand elasticity | beef 0.80, pork 0.67, poultry 0.71, eggs 0.82, dairy 0.78 | USDA ERS meta-analysis, Chen & Abler (2012) | Medium (slide deck) |
| Supply response (CEF) | beef 0.68, pork 0.74, chicken 0.76, eggs 0.91, milk 0.56 | Norwood & Lusk, *Compassion by the Pound* (2011), fig. 8.2 | Medium-high |
| Fish and shellfish elasticities | ed 0.5 / 0.6, es 0.3 | — | **Unsourced** |
| Farmed share of seafood | 51% | FAO SOFIA 2024 | High |
| Feed fish | 0.3 kg wild fish per kg farmed; ~20 g a fish | IFFO / FAO fish-in:fish-out | Medium |
| Producer margin (`breakevenDrop`) | 15–25% | — | **Unsourced**, illustrative |
| Reproduction | litter/clutch size, offspring per breeder, gestation, slaughter age, replacement rate, first breeding age | USDA and extension herd-management figures | Medium |
| Retail prices | live: BLS CPI average prices; fallback built-in | U.S. Bureau of Labor Statistics | High (U.S.) |

## World (`Species.kt`, `Population.kt`)

| Input | Value | Source | Confidence |
|---|---|---|---|
| World population | 1990–2100 curve, peak ~10.3 bn mid-2080s | UN World Population Prospects 2024, medium variant | High |
| Per-capita intake growth | 0.2%/yr | OECD-FAO Agricultural Outlook 2023–2032 | Medium |
| Vegans / vegetarians | 100 M / 800 M | Ipsos 2018 (28 countries); national surveys | **Low**, order of magnitude |
| Abstainer growth | flat, 1%, 3% scenarios | Gallup shows the U.S. share flat since 1999 | Scenario, not data |

## Herds and reservoirs (`Outlook.kt`)

| Input | Value | Source | Confidence |
|---|---|---|---|
| Farmed animals (2024) | chickens 27.68 bn (incl. 8.65 bn laying hens), ducks 1.27 bn, turkeys 244 M, pigs 962 M, cattle 1.58 bn (incl. 295 M dairy cows), sheep 1.36 bn | FAOSTAT QCL, World | High |
| Commercial bison | ~500,000 | IUCN 2017 (~97% commercial); Canada 2021 census 149,539 | Medium |
| Conservation bison | ~31,000 in 68 herds | IUCN Red List 2017 | High |
| Wild turkeys | 6.0 M | NWTF (2016) | Medium, dated |
| Mallards | 6.6 M (survey area) | USFWS Waterfowl Population Status 2025 | High |
| Wild boar / feral swine | 19.5 M (Europe 13.5 M + U.S. 6 M) | ENETWILD 2025; USDA APHIS | Medium |
| Red junglefowl, mouflon, heritage cattle | uncounted | BirdLife / IUCN | — |
| Minimum viable population | 4,169 | Traill, Bradshaw & Brook (2007) | High (contested as universal) |

## Farms (`Consolidation.kt`)

| Input | Value | Source | Confidence |
|---|---|---|---|
| U.S. factory-farmed share | broilers 99.9%, layers 98.3%, turkeys 99.8%, pigs 98.6%, cattle 74.9% | Sentience Institute, US Factory Farming Estimates (2024) | High |
| Global factory-farmed share | 74% of land animals | Sentience Institute (2019) | Medium |
| Small-farm margin | half the producer margin | — | **Unsourced**, illustrative |

## Environment (`Footprint.kt`)

| Input | Source | Confidence |
|---|---|---|
| CO₂e, land, water per kg (beef herd, dairy beef, lamb, pork, poultry, eggs, milk, farmed fish, farmed crustaceans, tofu) | Poore & Nemecek (2018), *Science* 360:987, via Our World in Data | High |
| Dairy share of beef | ~20% | USDA ERS | Medium |
| Protein shares | meat and fish 20%, eggs 12.5%, milk 3.4%, tofu 12% | Standard food composition | Medium |

## Soy (`Outlook.kt`)

| Input | Value | Source | Confidence |
|---|---|---|---|
| Soy per kg | broiler 0.956, pork 0.415, beef 0.206, eggs ~0.58, farmed fish 0.951 | WWF-NL / Profundo, *European soy supply* (2021), EU data | High for EU |
| Soy fed to animals | 77% | Ritchie & Roser, Our World in Data (2021) | Medium |
| Tofu yield | 2.0 kg per kg beans (1.9–2.4) | Food-science trials | Medium |
| World tofu output | 3.5 Mt midpoint of 2–5 Mt | Industry aggregators | **Low**, no authoritative figure |

## Influence (`Influence.kt`)

| Input | Value | Source | Confidence |
|---|---|---|---|
| Organic reach | Facebook 5%, Instagram 10%, TikTok 10%, YouTube 5%, Bluesky 15%, Mastodon 15% | Industry benchmarks | Low |
| Persuasion | 0.2% per year of exposure | — | **Unsourced** |
| Retention | 16% | Faunalytics (2014) | Medium |

## Known gaps

- **Trade:** trade and regional markets are not modelled.
- **Subsidies:** subsidies and price supports are absent.
- **Herd cycles:** herd cycles and cold storage are not modelled.
- **Bycatch:** wild-catch bycatch is not counted.
- **Crop deaths:** animals killed in crop farming are not counted.
- **Wild animals:** wild-animal effects of land use are not modelled.
- **Welfare weights:** welfare weights by species or sentience are absent.
- **U.S. bison:** the 2022 U.S. Census of Agriculture bison inventory has not been retrieved.
- **Fresh milk soy:** no soy-per-kg figure for fresh milk, lamb or shellfish.
