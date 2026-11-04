package pl.kuba6000.gtnhextranerf.telemetry;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.UUID;

import net.minecraft.nbt.CompressedStreamTools;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.MinecraftException;
import net.minecraft.world.WorldSavedData;
import net.minecraft.world.WorldServer;
import net.minecraft.world.storage.ISaveHandler;
import net.minecraft.world.storage.MapStorage;

/**
 * What the owners of one world decided about telemetry and team tracking. Neither kind of report is sent until the
 * world has been asked: {@link #asked()} is the only thing that makes the other values count.
 */
public final class TelemetryConsent extends WorldSavedData {

    public static final String NAME = "gtnhextranerf_consent";
    public static final String TEAM_ID_FILE = "gtnhextranerf-team-id.txt";

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
    private ISaveHandler saveHandler;

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
        if (!legacy.teamEnabled && isUuid(legacy.teamId)) {
            // Keep the old ID even when reporting was switched off, for re-enabling and deletion requests.
            data.teamId = legacy.teamId;
            data.leaderId = legacy.leaderId == null ? "" : legacy.leaderId;
            data.leaderName = legacy.leaderName == null ? "" : legacy.leaderName;
            data.carriedTeam = true;
        }
        if (legacy.teamUsable()) {
            // Team tracking replaces, rather than implicitly enabling, minimal telemetry.
            data.telemetry = false;
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
        return load(overworld.getSaveHandler(), overworld.mapStorage, legacy);
    }

    /** Loads and durably saves the decision without waiting for the next world autosave. */
    public static TelemetryConsent load(ISaveHandler saveHandler, MapStorage storage, Legacy legacy) {
        validateStoredDecision(saveHandler);
        TelemetryConsent data = (TelemetryConsent) storage.loadData(TelemetryConsent.class, NAME);
        if (data == null) {
            data = initial(legacy);
            storage.setData(NAME, data);
        }
        data.saveHandler = saveHandler;
        data.persist();
        return data;
    }

    /** MapStorage silently returns an empty record after a read failure, so check the existing file first. */
    private static void validateStoredDecision(ISaveHandler saveHandler) {
        File file = saveHandler.getMapFileFromName(NAME);
        if (file == null) throw new UncheckedIOException(new IOException("The world has no consent storage file"));
        Path path = file.toPath();
        if (Files.notExists(path)) return;
        try (InputStream input = Files.newInputStream(path)) {
            NBTTagCompound root = CompressedStreamTools.readCompressed(input);
            if (root == null || !root.hasKey("data", 10)) throw new IOException("Missing consent data");
            NBTTagCompound data = root.getCompoundTag("data");
            if (!data.hasKey("asked", 1) || !data.hasKey("telemetry", 1)
                || !data.hasKey("team", 1)
                || !data.hasKey("teamId", 8)
                || !data.hasKey("leaderId", 8)
                || !data.hasKey("leaderName", 8)) throw new IOException("Incomplete consent data");
        } catch (IOException failure) {
            throw new UncheckedIOException("Could not read the world's telemetry decision", failure);
        }
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
        if (enabled) team = false;
        changed();
    }

    public void enableTeam(String newTeamId, String newLeaderId, String newLeaderName) {
        asked = true;
        team = true;
        telemetry = false;
        teamId = newTeamId;
        leaderId = newLeaderId;
        leaderName = newLeaderName;
        changed();
    }

    /**
     * The team ID is kept, so that opting in again lists the same team and the data can still be asked to be deleted.
     */
    public void disableTeam() {
        asked = true;
        team = false;
        changed();
    }

    public void renameLeader(String newLeaderName) {
        leaderName = newLeaderName;
        changed();
    }

    private void changed() {
        markDirty();
        if (saveHandler != null) persist();
    }

    /** Use the vanilla compressed map-data format, with a forced temporary file and atomic replacement. */
    private void persist() {
        File file = saveHandler.getMapFileFromName(NAME);
        if (file == null) throw new UncheckedIOException(new IOException("The world has no consent storage file"));
        Path destination = file.toPath()
            .toAbsolutePath();
        Path temporary = null;
        try {
            try {
                saveHandler.checkSessionLock();
            } catch (MinecraftException failure) {
                throw new IOException("The world save is in use elsewhere", failure);
            }
            Files.createDirectories(destination.getParent());
            temporary = Files.createTempFile(destination.getParent(), ".extranerf-consent-", ".tmp");
            NBTTagCompound data = new NBTTagCompound();
            writeToNBT(data);
            NBTTagCompound root = new NBTTagCompound();
            root.setTag("data", data);
            ByteBuffer bytes = ByteBuffer.wrap(CompressedStreamTools.compress(root));
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            Files.move(temporary, destination, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            if (isUuid(teamId)) writeReadableTeamId();
            // Where supported, persist the directory entry too; Windows does not allow opening directories this way.
            try (FileChannel directory = FileChannel.open(destination.getParent(), StandardOpenOption.READ)) {
                directory.force(true);
            } catch (IOException | UnsupportedOperationException ignored) {
                // The file itself has already been forced and atomically replaced.
            }
            setDirty(false);
        } catch (IOException failure) {
            throw new UncheckedIOException("Could not save the world's telemetry decision", failure);
        } finally {
            if (temporary != null) try {
                Files.deleteIfExists(temporary);
            } catch (IOException ignored) {
                // The decision was either committed or the original error is reported to the operator.
            }
        }
    }

    /** Keep an easy-to-find copy for owners requesting deletion of their team's data. */
    private void writeReadableTeamId() throws IOException {
        File worldDirectory = saveHandler.getWorldDirectory();
        if (worldDirectory == null) throw new IOException("The world has no directory for the team ID");
        Path directory = worldDirectory.toPath()
            .toAbsolutePath();
        Path temporary = Files.createTempFile(directory, ".extranerf-team-id-", ".tmp");
        try {
            ByteBuffer bytes = ByteBuffer.wrap((teamId + "\n").getBytes(StandardCharsets.UTF_8));
            try (FileChannel channel = FileChannel.open(temporary, StandardOpenOption.WRITE)) {
                while (bytes.hasRemaining()) channel.write(bytes);
                channel.force(true);
            }
            Files.move(
                temporary,
                directory.resolve(TEAM_ID_FILE),
                StandardCopyOption.ATOMIC_MOVE,
                StandardCopyOption.REPLACE_EXISTING);
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        asked = tag.getBoolean("asked");
        team = tag.getBoolean("team");
        telemetry = !team && tag.getBoolean("telemetry");
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
