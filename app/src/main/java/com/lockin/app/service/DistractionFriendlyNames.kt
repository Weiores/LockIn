package com.lockin.app.service

/**
 * Package name → what a human calls the thing. Ordered most-specific first,
 * because e.g. `com.instagram.android` also contains "insta".
 */
internal object DistractionFriendlyNames {

    private val orderedNames = listOf(
        "com.instagram.android" to "Instagram",
        "com.instagram.barcelona" to "Threads",
        "com.zhiliaoapp.musically" to "TikTok",
        "com.ss.android.ugc.trill" to "TikTok",
        "com.twitter.android" to "X (Twitter)",
        "com.android.chrome" to "Chrome",
        "com.reddit.frontpage" to "Reddit",
        "com.snapchat.android" to "Snapchat",
        "com.facebook.katana" to "Facebook",
        "com.facebook.lite" to "Facebook Lite",
        "com.google.android.youtube" to "YouTube",
        "com.whatsapp" to "WhatsApp",
        "com.zhiliaoapp.musically.go" to "TikTok Lite",
        "com.pinterest" to "Pinterest",
        "com.linkedin.android" to "LinkedIn",
        "com.discord" to "Discord",
        "com.twitch.android" to "Twitch",
    )

    fun nameFor(packageName: String): String {
        val exact = orderedNames.firstOrNull { it.first == packageName }
        if (exact != null) return exact.second

        val fragment = fragmentNameFor(packageName)
        return fragment ?: "social media"
    }

    private fun fragmentNameFor(packageName: String): String? {
        val hits = listOf(
            "instagram" to "Instagram",
            "musically" to "TikTok",
            "tiktok" to "TikTok",
            "twitter" to "X (Twitter)",
            ".x." to "X (Twitter)",
            "facebook" to "Facebook",
            "reddit" to "Reddit",
            "youtube" to "YouTube",
            "snapchat" to "Snapchat",
            "whatsapp" to "WhatsApp",
            "discord" to "Discord",
            "twitch" to "Twitch",
            "linkedin" to "LinkedIn",
            "pinterest" to "Pinterest",
            "tumblr" to "Tumblr",
            "telegram" to "Telegram",
            "bilibili" to "Bilibili",
            "weibo" to "Weibo",
            "douyin" to "Douyin",
            "kuaishou" to "Kuaishou",
        )
        return hits.firstOrNull { (needle, _) ->
            packageName.contains(needle, ignoreCase = true)
        }?.second
    }
}
