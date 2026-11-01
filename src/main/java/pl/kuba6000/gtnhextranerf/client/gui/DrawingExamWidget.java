package pl.kuba6000.gtnhextranerf.client.gui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.util.StatCollector;

import com.cleanroommc.modularui.api.widget.Interactable;
import com.cleanroommc.modularui.drawable.GuiDraw;
import com.cleanroommc.modularui.drawable.UITexture;
import com.cleanroommc.modularui.screen.viewport.ModularGuiContext;
import com.cleanroommc.modularui.theme.WidgetThemeEntry;
import com.cleanroommc.modularui.utils.GlStateManager;
import com.cleanroommc.modularui.widget.ScrollWidget;
import com.cleanroommc.modularui.widget.Widget;
import com.cleanroommc.modularui.widget.scroll.ScrollArea;

import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import pl.kuba6000.gtnhextranerf.GTNHExtraNerf;
import pl.kuba6000.gtnhextranerf.progression.exam.AnswerSheet;
import pl.kuba6000.gtnhextranerf.progression.exam.ExamAttempt;
import pl.kuba6000.gtnhextranerf.progression.exam.ExamGui;
import pl.kuba6000.gtnhextranerf.progression.exam.ExamPaper;
import pl.kuba6000.gtnhextranerf.progression.exam.ExamQuestion;
import pl.kuba6000.gtnhextranerf.progression.exam.ExamSyncHandler;

/** Minecraft-font paper and pixel pencil. Every raster and page selection stays on the client. */
@SideOnly(Side.CLIENT)
public final class DrawingExamWidget extends Widget<DrawingExamWidget> implements Interactable {

    private static final int WIDTH = 270;
    private static final int INK = 0xFF282522;
    private static final int TEXT = 0xFF40362A;
    private static final UITexture PENCIL = UITexture.fullImage(GTNHExtraNerf.MODID, "gui/exam_pencil");
    private static final UITexture ERASER = UITexture.fullImage(GTNHExtraNerf.MODID, "gui/exam_eraser");

    private final ExamSyncHandler exam;
    private final Map<Integer, Page> pages = new HashMap<>();
    private ExamPaper loadedPaper;
    private Page active;
    private int activeIndex = -2;
    private int windowHeight = -1;
    private int dragButton = -1;
    private int lastX;
    private int lastY;
    private boolean lastInside;
    private String paymentText;

    public DrawingExamWidget(ExamSyncHandler exam) {
        this.exam = exam;
        size(WIDTH, 138);
    }

    @Override
    public void onUpdate() {
        super.onUpdate();
        ExamPaper paper = exam.paper();
        if (loadedPaper != paper) {
            loadedPaper = paper;
            pages.clear();
            activeIndex = -2;
        }
        int index = !exam.admitted() || paper == null ? -3 : exam.finished() ? Integer.MAX_VALUE : exam.page();
        String payment = index == -3 ? exam.paymentText() : null;
        if (activeIndex != index || !java.util.Objects.equals(paymentText, payment)) {
            activeIndex = index;
            paymentText = payment;
            if (index == -3) active = new Page(payment);
            else if (exam.finished()) {
                boolean passed = exam.result()
                    .status() == ExamAttempt.Status.PASSED;
                String message = passed ? paper.passedMessage() : paper.failedMessage();
                active = new Page(display(message));
            } else if (index < 0) {
                String introduction = paper.introduction()
                    .isEmpty()
                        ? StatCollector.translateToLocalFormatted(
                            "gtnhextranerf.exam.default_instructions",
                            paper.requiredCorrect(),
                            paper.questions()
                                .size())
                        : display(paper.introduction());
                active = new Page(introduction + "\n\n" + StatCollector.translateToLocal("gtnhextranerf.exam.begin"));
            } else active = pages.computeIfAbsent(
                index,
                question -> new Page(
                    paper.questions()
                        .get(question)));
            height(active.height);
            if (getParent() instanceof ScrollWidget) {
                ScrollArea area = ((ScrollWidget<?>) getParent()).getScrollArea();
                area.getScrollY()
                    .setScrollSize(active.height);
                area.getScrollY()
                    .scrollTo(area, 0);
                getParent().scheduleResize();
            }
            dragButton = -1;
            lastInside = false;
        }
        // The GUI context already uses scaled screen units. Keep eight units clear above and below.
        int availableHeight = Math.max(ExamGui.CONTENT_VERTICAL_PADDING + 1, getContext().getScreenArea().height - 16);
        int desiredHeight = Math
            .min(availableHeight, Math.max(ExamGui.HEIGHT, active.height + ExamGui.CONTENT_VERTICAL_PADDING));
        if (windowHeight != desiredHeight) {
            windowHeight = desiredHeight;
            getPanel().height(desiredHeight);
        }
    }

    @Override
    public void draw(ModularGuiContext context, WidgetThemeEntry<?> theme) {
        if (active == null) return;
        // The scroll viewport's stencil can leave texturing disabled before the first glyph.
        GlStateManager.enableTexture2D();
        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        for (int i = 0; i < active.prompt.size(); i++) font.drawString(active.prompt.get(i), 6, 6 + i * 10, TEXT);
        for (int option = 0; option < active.options.length; option++) {
            int y = active.optionY[option];
            GuiDraw.drawBorder(8, y + 2, 16, 16, TEXT, 1);
            font.drawString("ABCD".substring(option, option + 1) + ".", 30, y + 4, TEXT);
            List<String> lines = active.options[option];
            for (int i = 0; i < lines.size(); i++) font.drawString(lines.get(i), 46, y + 4 + i * 10, TEXT);
        }
        if (active.sheet != null) active.sheet.drawInk((x, y, length) -> GuiDraw.drawRect(x, y, length, 1, INK));
    }

    @Override
    public void drawForeground(ModularGuiContext context) {
        if (active == null || active.sheet == null || !isBelowMouse()) return;
        // MUI2's foreground pass does not apply the widget's OpenGL transform.
        (dragButton == 1 ? ERASER : PENCIL).draw(context.getAbsMouseX() - 1, context.getAbsMouseY() - 13, 16, 16);
    }

    @Override
    public Result onMousePressed(int button) {
        if (active == null || active.sheet == null || button < 0 || button > 1) return Result.IGNORE;
        dragButton = button;
        lastX = getContext().getMouseX();
        lastY = getContext().getMouseY();
        lastInside = true;
        active.sheet.line(lastX, lastY, lastX, lastY, button == 1);
        exam.choose(activeIndex, active.sheet.selectedMask());
        return Result.SUCCESS;
    }

    @Override
    public void onMouseDrag(int button, long timeSinceClick) {
        if (button != dragButton || active == null) return;
        int x = getContext().getMouseX(), y = getContext().getMouseY();
        boolean inside = isBelowMouse();
        if (inside) {
            active.sheet.line(lastInside ? lastX : x, lastInside ? lastY : y, x, y, button == 1);
            exam.choose(activeIndex, active.sheet.selectedMask());
        }
        lastX = x;
        lastY = y;
        lastInside = inside;
    }

    @Override
    public boolean onMouseRelease(int button) {
        if (button != dragButton) return false;
        dragButton = -1;
        lastInside = false;
        return true;
    }

    /**
     * A language key shows in the player's language; any other text shows as it is. Lang files cannot hold a real line
     * break, so a backslash followed by "n" in a translation stands for one.
     */
    private static String display(String text) {
        String shown = StatCollector.canTranslate(text) ? StatCollector.translateToLocal(text) : text;
        return shown.replace("\\n", "\n");
    }

    private static final class Page {

        private final List<String> prompt;
        private final List<String>[] options;
        private final int[] optionY = new int[4];
        private final AnswerSheet sheet;
        private final int height;

        @SuppressWarnings("unchecked")
        private Page(String text) {
            prompt = Minecraft.getMinecraft().fontRenderer.listFormattedStringToWidth(text, WIDTH - 12);
            options = new List[0];
            sheet = null;
            height = Math.max(138, 12 + prompt.size() * 10);
        }

        @SuppressWarnings("unchecked")
        private Page(ExamQuestion question) {
            FontRenderer font = Minecraft.getMinecraft().fontRenderer;
            prompt = font.listFormattedStringToWidth(display(question.prompt()), WIDTH - 12);
            options = new List[4];
            int y = 6 + prompt.size() * 10 + 8;
            int[][] boxes = new int[4][];
            for (int option = 0; option < 4; option++) {
                options[option] = font.listFormattedStringToWidth(
                    display(
                        question.options()
                            .get(option)),
                    WIDTH - 52);
                optionY[option] = y;
                boxes[option] = new int[] { 9, y + 3, 14, 14 };
                y += Math.max(18, options[option].size() * 10 + 6) + 6;
            }
            sheet = new AnswerSheet(WIDTH, Math.max(138, y + 6), boxes);
            height = sheet.height();
        }
    }
}
