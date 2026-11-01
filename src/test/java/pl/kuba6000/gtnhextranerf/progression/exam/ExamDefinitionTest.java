package pl.kuba6000.gtnhextranerf.progression.exam;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import pl.kuba6000.gtnhextranerf.LangFile;
import pl.kuba6000.gtnhextranerf.progression.ProgressionTier;

class ExamDefinitionTest {

    private static final String LV_ANSWERS = "BCADBCADCB";
    private static final String STEAM_ANSWERS = "BDACBADCAB";

    @Test
    void theServerChoosesWhichCertificateAnExamAwards() {
        assertSame(
            ProgressionTier.LV,
            BundledExams.of(ProgressionTier.LV)
                .tier());
        assertSame(
            ProgressionTier.STEAM,
            BundledExams.of(ProgressionTier.STEAM)
                .tier());
    }

    @Test
    void onlyTiersThatHaveAnExamHaveABundledExam() {
        for (ProgressionTier tier : ProgressionTier.values()) {
            if (tier.hasExam()) assertTrue(
                BundledExams.of(tier)
                    .enabled());
            else assertThrows(IllegalArgumentException.class, () -> BundledExams.of(tier));
        }
    }

    @Test
    void bothExamsHaveTenQuestionsAndNeedEightRight() {
        for (ProgressionTier tier : new ProgressionTier[] { ProgressionTier.LV, ProgressionTier.STEAM }) {
            ExamDefinition exam = BundledExams.of(tier);
            assertEquals(
                10,
                exam.questions()
                    .size());
            assertEquals(8, exam.requiredCorrect());
        }
    }

    @Test
    void theAgreedAnswerKeysGradeAPerfectPaperAndNothingElse() {
        assertEquals(
            10,
            BundledExams.of(ProgressionTier.LV)
                .score(paper(LV_ANSWERS)));
        assertEquals(
            10,
            BundledExams.of(ProgressionTier.STEAM)
                .score(paper(STEAM_ANSWERS)));
        assertEquals(
            0,
            BundledExams.of(ProgressionTier.LV)
                .score(paper("ADCBADCBAD")));
        assertEquals(
            9,
            BundledExams.of(ProgressionTier.LV)
                .score(paper("ACADBCADCB")));
    }

    @Test
    void theClientPaperCarriesLanguageKeysAndNoAnswers() {
        ExamPaper paper = BundledExams.of(ProgressionTier.LV)
            .paper();
        assertEquals("gtnhextranerf.exam.lv.introduction", paper.introduction());
        assertEquals(
            "gtnhextranerf.exam.lv.q01.prompt",
            paper.questions()
                .get(0)
                .prompt());
        assertEquals(
            Arrays.asList(
                "gtnhextranerf.exam.lv.q01.a",
                "gtnhextranerf.exam.lv.q01.b",
                "gtnhextranerf.exam.lv.q01.c",
                "gtnhextranerf.exam.lv.q01.d"),
            paper.questions()
                .get(0)
                .options());
        assertEquals(
            "lv-01",
            paper.questions()
                .get(0)
                .id());
    }

    @Test
    void everyTextOfBothExamsIsTranslatedInEnglish() throws Exception {
        Map<String, String> english = LangFile.english();
        Set<String> all = new HashSet<>();
        for (ProgressionTier tier : new ProgressionTier[] { ProgressionTier.LV, ProgressionTier.STEAM }) {
            List<String> keys = BundledExams.keys(tier);
            assertEquals(3 + 10 * 5, keys.size());
            for (String key : keys) {
                assertTrue(all.add(key), "Duplicate key " + key);
                String text = english.get(key);
                assertNotNull(text, "Missing translation " + key);
                assertFalse(
                    text.trim()
                        .isEmpty(),
                    "Empty translation " + key);
            }
        }
    }

    @Test
    void theKeysOfTheExamAreTheOnesItsPaperRefersTo() {
        for (ProgressionTier tier : new ProgressionTier[] { ProgressionTier.LV, ProgressionTier.STEAM }) {
            ExamPaper paper = BundledExams.of(tier)
                .paper();
            Set<String> used = new HashSet<>();
            used.add(paper.introduction());
            used.add(paper.passedMessage());
            used.add(paper.failedMessage());
            for (ExamQuestion question : paper.questions()) {
                used.add(question.prompt());
                used.addAll(question.options());
            }
            assertEquals(new HashSet<>(BundledExams.keys(tier)), used);
        }
    }

    private static int[] paper(String letters) {
        int[] choices = new int[letters.length()];
        for (int i = 0; i < choices.length; i++) choices[i] = 1 << "ABCD".indexOf(letters.charAt(i));
        return choices;
    }
}
