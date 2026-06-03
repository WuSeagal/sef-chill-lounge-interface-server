package com.sef.cli.attendee.enums;

import java.util.regex.Pattern;

public enum PlatformEnum {
    FACEBOOK(Pattern.compile("^(www\\.|m\\.)?facebook\\.com$|^fb\\.com$", Pattern.CASE_INSENSITIVE)),
    STEAM(Pattern.compile("^(www\\.)?steamcommunity\\.com$", Pattern.CASE_INSENSITIVE)),
    PLURK(Pattern.compile("^(www\\.)?plurk\\.com$", Pattern.CASE_INSENSITIVE)),
    CAKERESUME(Pattern.compile("^(www\\.)?(cake\\.me|cakeresume\\.com)$", Pattern.CASE_INSENSITIVE)),
    LINKEDIN(Pattern.compile("^(www\\.)?linkedin\\.com$", Pattern.CASE_INSENSITIVE)),
    TWITCH(Pattern.compile("^(www\\.)?twitch\\.tv$", Pattern.CASE_INSENSITIVE)),
    THREADS(Pattern.compile("^(www\\.)?threads\\.(com|net)$", Pattern.CASE_INSENSITIVE)),
    INSTAGRAM(Pattern.compile("^(www\\.)?instagram\\.com$", Pattern.CASE_INSENSITIVE)),
    DISCORD(Pattern.compile("^(www\\.)?discord\\.(gg|com)$", Pattern.CASE_INSENSITIVE)),
    BLUESKY(Pattern.compile("^(www\\.)?bsky\\.app$", Pattern.CASE_INSENSITIVE)),
    X(Pattern.compile("^(www\\.)?(x\\.com|twitter\\.com)$", Pattern.CASE_INSENSITIVE)),
    GITHUB(Pattern.compile("^(www\\.)?github\\.com$", Pattern.CASE_INSENSITIVE)),
    PERSONAL(null),
    OTHER(null);

    private final Pattern urlHostPattern;

    PlatformEnum(Pattern urlHostPattern) {
        this.urlHostPattern = urlHostPattern;
    }

    public Pattern getUrlHostPattern() {
        return urlHostPattern;
    }

    public boolean hasHostPattern() {
        return urlHostPattern != null;
    }
}
