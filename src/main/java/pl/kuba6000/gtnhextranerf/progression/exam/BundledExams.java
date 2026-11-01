package pl.kuba6000.gtnhextranerf.progression.exam;

import java.util.ArrayList;
import java.util.List;

import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;

/**
 * The exams that ship with the mod. Every text is a language key, so each client shows the exam in its own language;
 * the server sends the keys only. The answer key lives here, next to the structure of the exam.
 */
public final class BundledExams {

    private static final String LV_KEY = "BCADBCADCB";
    private static final String STEAM_KEY = "BDACBADCAB";
    private static final int REQUIRED_CORRECT = 8;
    private static final String[] LETTERS = { "a", "b", "c", "d" };

    private BundledExams() {}

    public static ExamDefinition of(ProgressionTier tier) {
        switch (tier) {
            case LV:
                return build(tier, LV_KEY);
            case STEAM:
                return build(tier, STEAM_KEY);
            default:
                throw new IllegalArgumentException(tier.id() + " has no exam");
        }
    }

    /** Every language key the exam of this tier refers to, in the order they appear in the exam. */
    public static List<String> keys(ProgressionTier tier) {
        List<String> keys = new ArrayList<>();
        keys.add(key(tier, "introduction"));
        keys.add(key(tier, "passed"));
        keys.add(key(tier, "failed"));
        for (int question = 1; question <= of(tier).questions()
            .size(); question++) {
            keys.add(key(tier, question(question) + ".prompt"));
            for (String letter : LETTERS) keys.add(key(tier, question(question) + "." + letter));
        }
        return keys;
    }

    private static ExamDefinition build(ProgressionTier tier, String answers) {
        List<ExamQuestion> questions = new ArrayList<>();
        int[] correct = new int[answers.length()];
        for (int i = 0; i < answers.length(); i++) {
            String number = question(i + 1);
            questions.add(
                new ExamQuestion(
                    tier.id() + "-" + number.substring(1),
                    key(tier, number + ".prompt"),
                    key(tier, number + "." + LETTERS[0]),
                    key(tier, number + "." + LETTERS[1]),
                    key(tier, number + "." + LETTERS[2]),
                    key(tier, number + "." + LETTERS[3])));
            correct[i] = "ABCD".indexOf(answers.charAt(i));
        }
        return new ExamDefinition(
            tier.id(),
            tier,
            questions,
            correct,
            REQUIRED_CORRECT,
            true,
            key(tier, "introduction"),
            key(tier, "passed"),
            key(tier, "failed"));
    }

    private static String question(int number) {
        return "q" + (number < 10 ? "0" : "") + number;
    }

    private static String key(ProgressionTier tier, String part) {
        return "gtnhextranerf.exam." + tier.id() + "." + part;
    }
}
