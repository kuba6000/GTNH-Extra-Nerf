package pl.kuba6000.gtnhextranerf.progression.exam;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;

/** Immutable server-owned exam: question texts as language keys, and the answer key. */
public final class ExamDefinition {

    public static final int MAX_QUESTIONS = 32;

    private final String id;
    private final ProgressionTier tier;
    private final List<ExamQuestion> questions;
    private final int[] correctOptions;
    private final int requiredCorrect;
    private final boolean enabled;
    private final ExamPaper paper;

    public ExamDefinition(String id, ProgressionTier tier, List<ExamQuestion> questions, int[] correctOptions,
        int requiredCorrect, boolean enabled) {
        this(id, tier, questions, correctOptions, requiredCorrect, enabled, "", "", "");
    }

    public ExamDefinition(String id, ProgressionTier tier, List<ExamQuestion> questions, int[] correctOptions,
        int requiredCorrect, boolean enabled, String introduction, String passedMessage, String failedMessage) {
        this.id = Objects.requireNonNull(id);
        this.tier = Objects.requireNonNull(tier);
        if (id.isEmpty() || id.length() > 64
            || questions.isEmpty()
            || questions.size() > MAX_QUESTIONS
            || correctOptions.length != questions.size()
            || requiredCorrect < 1
            || requiredCorrect > questions.size()) throw new IllegalArgumentException("Invalid exam definition: " + id);
        Set<String> ids = new HashSet<>();
        for (ExamQuestion question : questions) if (!ids.add(question.id()))
            throw new IllegalArgumentException("Duplicate exam question: " + question.id());
        for (int option : correctOptions) if (option < (enabled ? 0 : -1) || option > 3)
            throw new IllegalArgumentException("Correct exam options must be A-D");
        this.questions = Collections.unmodifiableList(new ArrayList<>(questions));
        this.correctOptions = correctOptions.clone();
        this.requiredCorrect = requiredCorrect;
        this.enabled = enabled;
        this.paper = new ExamPaper(
            id,
            this.questions,
            requiredCorrect,
            enabled,
            introduction,
            passedMessage,
            failedMessage);
    }

    public String id() {
        return id;
    }

    public ProgressionTier tier() {
        return tier;
    }

    public List<ExamQuestion> questions() {
        return questions;
    }

    public int requiredCorrect() {
        return requiredCorrect;
    }

    public boolean enabled() {
        return enabled;
    }

    public ExamPaper paper() {
        return paper;
    }

    int score(int[] choices) {
        int correct = 0;
        for (int i = 0; i < choices.length; i++) if (choices[i] == (1 << correctOptions[i])) correct++;
        return correct;
    }
}
