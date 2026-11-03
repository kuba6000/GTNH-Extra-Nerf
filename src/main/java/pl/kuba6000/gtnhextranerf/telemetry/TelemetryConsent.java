package pl.kuba6000.gtnhextranerf.telemetry;

import java.util.UUID;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;

/**
 * What the owners of one world decided about telemetry and team tracking. Nothing is sent until the world has been
 * asked: {@link #asked()} is the only thing that makes the other values count.
 */
public final class TelemetryConsent extends WorldSavedData {

    public static final String NAME = "gtnhextranerf_consent";

    /** The settings older versions kept in the configuration file, used once for a world that has no record yet. */
    public static final class Legacy {

        public final boolean telemetryOptedOut;
        public final boolean teamEnabled;
        public final String teamId;
        public final String leaderId;
        public final String leaderName;

        public Legacy(boolean telemetryOptedOut, boolean teamEnabled, String teamId, String leaderId,
            String leaderName) {
            this.telemetryOptedOut = telemetryOptedOut;
            this.teamEnabled = teamEnabled;
            this.teamId = teamId;
            this.leaderId = leaderId;
            this.leaderName = leaderName;
        }

        public static final Legacy NONE = new Legacy(false, false, "", "", "");

        boolean teamUsable() {
            return teamEnabled && isUuid(teamId)
                && isUuid(leaderId)
                && leaderName != null
                && !leaderName.trim()
                    .isEmpty();
        }
    }

    private boolean asked;
    private boolean telemetry;
    private boolean team;
    private String teamId = "";
    private String leaderId = "";
    private String leaderName = "";
    /** Not saved: true when this record was created from the configuration file during this load. */
    private boolean carriedTeam;

    public TelemetryConsent() {
        this(NAME);
    }

    public TelemetryConsent(String name) {
        super(name);
    }

    /**
     * A world without a record. A team that was listed from the configuration is moved into the world and a refusal is
     * kept; everything else, including the old default, has to be asked.
     */
    public static TelemetryConsent initial(Legacy legacy) {
        TelemetryConsent data = new TelemetryConsent();
        if (legacy.teamUsable()) {
            // Someone who listed their team wants to be counted too, unless they refused telemetry.
            data.telemetry = !legacy.telemetryOptedOut;
            data.asked = true;
            data.team = true;
            data.teamId = legacy.teamId;
            data.leaderId = legacy.leaderId;
            data.leaderName = legacy.leaderName;
            data.carriedTeam = true;
        } else if (legacy.telemetryOptedOut) {
            data.asked = true;
        }
        data.markDirty();
        return data;
    }

    /** The record of this world, created from {@code legacy} the first time the world is loaded. */
    public static TelemetryConsent load(WorldServer overworld, Legacy legacy) {
        TelemetryConsent data = (TelemetryConsent) overworld.mapStorage.loadData(TelemetryConsent.class, NAME);
        if (data != null) return data;
        data = initial(legacy);
        overworld.mapStorage.setData(NAME, data);
        return data;
    }

    public boolean asked() {
        return asked;
    }

    public boolean telemetry() {
        return telemetry;
    }

    public boolean team() {
        return team;
    }

    public String teamId() {
        return teamId;
    }

    public String leaderId() {
        return leaderId;
    }

    public String leaderName() {
        return leaderName;
    }

    /** True when a team that the configuration file had enabled was moved into this world. */
    public boolean carriedTeam() {
        return carriedTeam;
    }

    /** The answer to the question, or a telemetry command: asking is over, whichever way it went. */
    public void answerTelemetry(boolean enabled) {
        asked = true;
        telemetry = enabled;
        markDirty();
    }

    public void enableTeam(String newTeamId, String newLeaderId, String newLeaderName) {
        asked = true;
        team = true;
        teamId = newTeamId;
        leaderId = newLeaderId;
        leaderName = newLeaderName;
        markDirty();
    }

    /**
     * The team ID is kept, so that opting in again lists the same team and the data can still be asked to be deleted.
     */
    public void disableTeam() {
        asked = true;
        team = false;
        markDirty();
    }

    public void renameLeader(String newLeaderName) {
        leaderName = newLeaderName;
        markDirty();
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        asked = tag.getBoolean("asked");
        telemetry = tag.getBoolean("telemetry");
        team = tag.getBoolean("team");
        teamId = tag.getString("teamId");
        leaderId = tag.getString("leaderId");
        leaderName = tag.getString("leaderName");
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setBoolean("asked", asked);
        tag.setBoolean("telemetry", telemetry);
        tag.setBoolean("team", team);
        tag.setString("teamId", teamId);
        tag.setString("leaderId", leaderId);
        tag.setString("leaderName", leaderName);
    }

    private static boolean isUuid(String value) {
        if (value == null) return false;
        try {
            UUID.fromString(value);
            return true;
        } catch (IllegalArgumentException invalid) {
            return false;
        }
    }
}
