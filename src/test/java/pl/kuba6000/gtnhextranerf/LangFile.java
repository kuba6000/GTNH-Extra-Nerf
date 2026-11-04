package pl.kuba6000.gtnhextranerf;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.minecraft.util.ChatComponentText;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.IChatComponent;

/** The English language file, read from the classpath, and the text a player sees for a chat message. */
public final class LangFile {

    private static final Pattern PLACEHOLDER = Pattern.compile("%(\\d+\\$)?s");
    private static Map<String, String> english;

    private LangFile() {}

    public static synchronized Map<String, String> english() throws Exception {
        if (english != null) return english;
        Map<String, String> lines = new HashMap<>();
        try (InputStream stream = LangFile.class.getResourceAsStream("/assets/gtnhextranerf/lang/en_US.lang");
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null;) {
                int separator = line.indexOf('=');
                if (line.startsWith("#") || separator < 0) continue;
                lines.put(line.substring(0, separator), line.substring(separator + 1));
            }
        }
        english = lines;
        return lines;
    }

    /** The English text of a translation key; fails when the key is missing or its placeholders do not fit. */
    public static String text(String key, Object... arguments) throws Exception {
        String pattern = english().get(key);
        assertNotNull(pattern, "Missing translation " + key);
        Matcher matcher = PLACEHOLDER.matcher(pattern);
        int placeholders = 0;
        while (matcher.find()) placeholders++;
        assertEquals(placeholders, arguments.length, "Placeholders of " + key);
        Object[] shown = new Object[arguments.length];
        for (int i = 0; i < shown.length; i++)
            shown[i] = arguments[i] instanceof IChatComponent ? render((IChatComponent) arguments[i]) : arguments[i];
        return String.format(pattern, shown);
    }

    /** What a player reading English would see, for a message made of translation keys and arguments. */
    public static String render(IChatComponent message) throws Exception {
        if (message instanceof ChatComponentText) return message.getUnformattedText();
        assertTrue(message instanceof ChatComponentTranslation, "Not a translation: " + message);
        ChatComponentTranslation translation = (ChatComponentTranslation) message;
        return text(translation.getKey(), translation.getFormatArgs());
    }
}
