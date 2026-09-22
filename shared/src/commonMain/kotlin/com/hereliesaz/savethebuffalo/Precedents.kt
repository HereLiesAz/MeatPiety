package com.hereliesaz.savethebuffalo

data class ExtinctionPrecedent(
    val species: String,
    val year: Int,
    val event: String,
    val detail: String,
)

// Real historical cases of the same forces this app models: a species pushed
// to the edge, and either commercial or captive demand deciding what happened
// next. Sources: aurochs (well-documented last death, Jaktorów Forest,
// Poland); American bison (William Hornaday's 1889 Smithsonian census); wild
// turkey (USFWS/NWTF restoration history); European bison (European Bison
// Pedigree Book); Père David's deer and Przewalski's horse (Woburn Abbey and
// zoo studbook histories); scimitar-horned oryx (IUCN status, Sahara
// Conservation Fund reintroduction to Chad).
val EXTINCTION_PRECEDENTS = listOf(
    ExtinctionPrecedent(
        species = "Aurochs",
        year = 1627,
        event = "Went extinct — the wild ancestor of every cow alive today.",
        detail = "Hunted out of existence in a Polish royal forest while its already-domesticated descendants multiplied across three continents.",
    ),
    ExtinctionPrecedent(
        species = "American Bison",
        year = 1889,
        event = "Fell to roughly 1,000 animals continent-wide, down from tens of millions.",
        detail = "Ranching demand for meat and hides is most of why the species didn't finish the job. Today's ~500,000 are overwhelmingly livestock.",
    ),
    ExtinctionPrecedent(
        species = "Wild Turkey",
        year = 1930,
        event = "Bottomed out around 30,000 birds in the U.S.",
        detail = "Regulated hunting, funded by a federal tax on guns and ammunition, rebuilt the population past 7 million — commerce again, just aimed the other way.",
    ),
    ExtinctionPrecedent(
        species = "European Bison (Wisent)",
        year = 1927,
        event = "Went extinct in the wild; 54 animals left, all in zoos.",
        detail = "Every wisent alive today descends from 12 of those zoo animals. The population has since passed 12,000, about 9,800 free-ranging.",
    ),
    ExtinctionPrecedent(
        species = "Père David's Deer",
        year = 1900,
        event = "Vanished from the wild in China.",
        detail = "Survived only because an English duke had already collected a captive herd at Woburn Abbey. Every deer alive today descends from it.",
    ),
    ExtinctionPrecedent(
        species = "Przewalski's Horse",
        year = 1969,
        event = "Last wild sighting, in Mongolia — the only true wild horse, gone from nature.",
        detail = "Rebuilt from as few as a dozen captive animals; hundreds have since been reintroduced to Mongolia and Kazakhstan.",
    ),
    ExtinctionPrecedent(
        species = "Scimitar-Horned Oryx",
        year = 2000,
        event = "Declared extinct in the wild by the IUCN.",
        detail = "Survived on private Texas hunting ranches, which bred them by the thousand for sport. That captive stock reintroduced over 600 to Chad.",
    ),
)
