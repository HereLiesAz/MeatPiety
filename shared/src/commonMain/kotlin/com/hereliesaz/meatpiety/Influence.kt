package com.hereliesaz.meatpiety

/**
 * Social accounts the user links by handle. Follower counts are fetched by the
 * MeatPiety worker (`worker/`), never typed in. X is deliberately absent.
 */
enum class Platform(
    val label: String,
    val hint: String,
    // Share of followers who regularly see what you post (organic reach).
    // Industry benchmarks (Hootsuite, Rival IQ, Socialinsider); illustrative.
    val reach: Double,
) {
    FACEBOOK("Facebook", "username or profile URL", 0.05),
    INSTAGRAM("Instagram", "@username", 0.10),
    TIKTOK("TikTok", "@username", 0.10),
    YOUTUBE("YouTube", "@handle", 0.05),
    BLUESKY("Bluesky", "name.bsky.social", 0.15),
    MASTODON("Mastodon", "@name@instance", 0.15),
}

sealed interface FollowerLookup {
    data object Pending : FollowerLookup
    data class Found(val followers: Long) : FollowerLookup
    data class Failed(val reason: String) : FollowerLookup
}

data class LinkedAccount(val platform: Platform, val handle: String, val lookup: FollowerLookup)

// Of the people who regularly see you live this way, the share who go vegan or
// vegetarian because of it over the span. A guess with a straight face; the
// literature measures leaflets and ads, not friends.
internal const val PERSUASION_RATE = 0.002

// Faunalytics (2014): 84% of people who go vegetarian or vegan quit.
internal const val RETENTION_RATE = 0.16

// Audiences overlap across platforms; everyone past your biggest audience
// counts at this weight so the same friend isn't converted three times.
internal const val OVERLAP_DISCOUNT = 0.5

/** The influence funnel, shown step by step in the UI. */
data class InfluenceFunnel(
    val followers: Long,
    val reached: Double,
    val persuaded: Double,
    val stayed: Double,
)

/** Null when no account has a follower count, so the influence chapter is omitted. */
fun influenceFunnel(accounts: List<LinkedAccount>): InfluenceFunnel? {
    val found = accounts.mapNotNull { a -> (a.lookup as? FollowerLookup.Found)?.let { a.platform to it.followers } }
    if (found.isEmpty()) return null
    val reachedEach = found.map { (p, n) -> n * p.reach }.sortedDescending()
    val reached = reachedEach.first() + reachedEach.drop(1).sum() * OVERLAP_DISCOUNT
    val persuaded = reached * PERSUASION_RATE
    return InfluenceFunnel(
        followers = found.sumOf { it.second },
        reached = reached,
        persuaded = persuaded,
        stayed = persuaded * RETENTION_RATE,
    )
}
