package pl.kuba6000.gtnhextranerf.mixins.support;

/** Shared multiplier for steam consumed while machines process recipes. */
public final class SteamConsumption {

    public static final int MULTIPLIER = 5;
    public static final int SINGLE_STEAM_PER_EU = 2 * MULTIPLIER;

    private SteamConsumption() {}
}
