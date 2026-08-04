package com.hospital.vaccination.service;

import com.hospital.vaccination.config.AppConfig;
import com.hospital.vaccination.model.Child;
import com.hospital.vaccination.model.VaccinationRecord;
import com.hospital.vaccination.util.BSDateConverter;
import com.hospital.vaccination.util.BSDateConverter.BSDate;
import com.hospital.vaccination.util.DateUtil;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts.FontName;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

/**
 * Generates the printable "Vaccination Reference Card" PDF that
 * the nurse hands to the parent after registration.
 *
 * <p>Layout is A4 portrait, one page: hospital header, child+parent
 * info block, full schedule table (17 rows), footer instructions.
 *
 * <p>Uses only PDFBox Standard 14 fonts (Helvetica) so we don't have
 * to ship any TTF files. That means BS month names are written in
 * romanized English (e.g. "Baishakh"), not Devanagari — which is
 * fine and readable for parents.
 */
public class PdfCardService {

    // ---- layout constants ---------------------------------------------------
    private static final float MARGIN = 40f;
    private static final float LINE   = 14f;      // default text line height

    // Fonts (PDFBox 3 API — Standard14Fonts.FontName, no ttf needed)
    private static final PDType1Font BOLD    = new PDType1Font(FontName.HELVETICA_BOLD);
    private static final PDType1Font REGULAR = new PDType1Font(FontName.HELVETICA);
    private static final PDType1Font ITALIC  = new PDType1Font(FontName.HELVETICA_OBLIQUE);

    // ---- public API ---------------------------------------------------------

    /**
     * Render the card for the given child + schedule to the given file.
     *
     * @param outFile   destination .pdf (will be created/overwritten)
     * @param child     the registered child
     * @param records   the full generated schedule (17 doses)
     * @throws IOException if PDFBox cannot write the file
     */
    public void generate(File outFile, Child child, List<VaccinationRecord> records)
            throws IOException {

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = page.getMediaBox().getHeight() - MARGIN;

                y = drawHeader(cs, page, y);
                y -= 10;
                y = drawChildInfo(cs, page, y, child);
                y -= 10;
                y = drawScheduleTable(cs, page, y, records);
                y -= 15;
                drawFooter(cs, page, y);
            }

            doc.save(outFile);
        }
    }

    // ---- sections -----------------------------------------------------------

    private float drawHeader(PDPageContentStream cs, PDPage page, float y) throws IOException {
        float pageWidth = page.getMediaBox().getWidth();

        // Hospital name (centered, largest)
        String hospital = AppConfig.hospitalName();
        float hw = stringWidth(BOLD, 20, hospital);
        drawText(cs, BOLD, 20, (pageWidth - hw) / 2f, y, hospital);
        y -= 22;

        // Card title (centered, smaller)
        String title = "Child Vaccination Card";
        float titleWidth = stringWidth(BOLD, 13, title);
        drawText(cs, BOLD, 13, (pageWidth - titleWidth) / 2f, y, title);
        y -= 15;

        // Subtitle (centered, gray, italic)
        String sub = AppConfig.hospitalTagline();
        float subWidth = stringWidth(ITALIC, 11, sub);
        drawText(cs, ITALIC, 11, (pageWidth - subWidth) / 2f, y, sub, new Color(90, 90, 90));
        y -= 8;

        // Divider line
        cs.setStrokingColor(new Color(0, 102, 178));
        cs.setLineWidth(1.2f);
        cs.moveTo(MARGIN, y);
        cs.lineTo(pageWidth - MARGIN, y);
        cs.stroke();

        return y - 5;
    }

    private float drawChildInfo(PDPageContentStream cs, PDPage page, float y, Child child) throws IOException {
        // Section header
        drawText(cs, BOLD, 12, MARGIN, y, "Child & parent details");
        y -= LINE + 2;

        BSDate dobBs = BSDateConverter.toBS(child.getDateOfBirth());
        String dobStr = DateUtil.display(child.getDateOfBirth()) +
                "   (" + dobBs.display() + " BS)";
        String age = formatAge(child.getDateOfBirth(), LocalDate.now());

        // Two-column info block
        float col1 = MARGIN;
        float col2 = MARGIN + 260;

        y = drawKV(cs, col1, y, "Name",   child.getName());
        drawKV  (cs, col2, y + LINE, "Gender", child.getGender().name());

        y = drawKV(cs, col1, y, "DOB",    dobStr);
        drawKV  (cs, col2, y + LINE, "Age (today)", age);

        y = drawKV(cs, col1, y, "Parent", child.getParentName());
        drawKV  (cs, col2, y + LINE, "Phone",  child.getParentPhone());

        if (child.getAddress() != null && !child.getAddress().isBlank()) {
            y = drawKV(cs, col1, y, "Address", child.getAddress());
        }

        return y;
    }

    /** One "Label: value" pair. Returns the new y position. */
    private float drawKV(PDPageContentStream cs, float x, float y, String key, String value)
            throws IOException {
        drawText(cs, BOLD,    10, x,        y, key + ":");
        drawText(cs, REGULAR, 10, x + 55,   y, value == null ? "" : value);
        return y - LINE;
    }

    private float drawScheduleTable(PDPageContentStream cs, PDPage page, float y,
                                    List<VaccinationRecord> records) throws IOException {

        drawText(cs, BOLD, 12, MARGIN, y, "Vaccination schedule");
        y -= LINE + 2;

        float pageWidth = page.getMediaBox().getWidth();
        float tableLeft  = MARGIN;
        float tableRight = pageWidth - MARGIN;
        float rowH       = 18f;

        // column widths (must sum to tableRight - tableLeft = ~515pt)
        float[] cw = { 22, 90, 90, 100, 100, 113 };
        String[] headers = { "#", "Vaccine", "Age due", "Due (AD)", "Due (BS)", "Given on" };

        // ----- header row -----
        cs.setNonStrokingColor(new Color(0, 102, 178));
        cs.addRect(tableLeft, y - rowH, tableRight - tableLeft, rowH);
        cs.fill();
        cs.setNonStrokingColor(Color.WHITE);

        drawRow(cs, tableLeft, y - rowH + 5, cw, headers, BOLD, 10);

        y -= rowH;

        // ----- data rows -----
        cs.setNonStrokingColor(Color.BLACK);
        int i = 1;
        boolean zebra = false;
        for (VaccinationRecord r : records) {
            if (zebra) {
                cs.setNonStrokingColor(new Color(240, 246, 252));
                cs.addRect(tableLeft, y - rowH, tableRight - tableLeft, rowH);
                cs.fill();
                cs.setNonStrokingColor(Color.BLACK);
            }
            zebra = !zebra;

            String ageDue = ageAt(r.getDueDate(), records.get(0).getDueDate() /* birth-anchor */);
            BSDate bs = BSDateConverter.toBS(r.getDueDate());
            String[] row = {
                    String.valueOf(i++),
                    r.getVaccineName(),
                    ageDue,
                    DateUtil.display(r.getDueDate()),
                    bs.format(),
                    ""   // blank line for handwriting
            };
            drawRow(cs, tableLeft, y - rowH + 5, cw, row, REGULAR, 9);
            y -= rowH;
        }

        // ----- outer + row borders -----
        cs.setStrokingColor(new Color(180, 180, 180));
        cs.setLineWidth(0.5f);
        float tableTop = y + records.size() * rowH + rowH;   // top of header
        float tableBottom = y;
        // horizontal lines
        for (int k = 0; k <= records.size() + 1; k++) {
            float ly = tableTop - k * rowH;
            cs.moveTo(tableLeft, ly);
            cs.lineTo(tableRight, ly);
            cs.stroke();
        }
        // vertical lines
        float x = tableLeft;
        for (int k = 0; k <= cw.length; k++) {
            cs.moveTo(x, tableTop);
            cs.lineTo(x, tableBottom);
            cs.stroke();
            if (k < cw.length) x += cw[k];
        }

        return y;
    }

    private void drawRow(PDPageContentStream cs, float xStart, float y,
                         float[] cw, String[] cells, PDType1Font font, float size)
            throws IOException {
        float x = xStart + 4;   // small left padding inside cell
        for (int i = 0; i < cells.length; i++) {
            drawText(cs, font, size, x, y, cells[i]);
            x += cw[i];
        }
    }

    private void drawFooter(PDPageContentStream cs, PDPage page, float y) throws IOException {
        float pageWidth = page.getMediaBox().getWidth();

        String[] lines = {
                "Please bring this card to every vaccination visit.",
                "You will receive an SMS reminder 2–3 days before each due date.",
                "If you miss the due date, please visit the hospital as soon as possible.",
                "Keep this card safe — it is your child's official immunization record."
        };

        drawText(cs, BOLD, 10, MARGIN, y, "Important:");
        y -= LINE;
        for (String line : lines) {
            drawText(cs, REGULAR, 9, MARGIN, y, "• " + line);
            y -= LINE - 2;
        }

        // bottom-right signature line
        y = 60;
        drawText(cs, REGULAR, 9, pageWidth - MARGIN - 160, y, "Nurse's signature: ______________");
        drawText(cs, REGULAR, 9, pageWidth - MARGIN - 160, y - LINE,
                "Date issued: " + DateUtil.display(LocalDate.now()));
    }

    // ---- small helpers ------------------------------------------------------

    private void drawText(PDPageContentStream cs, PDType1Font font, float size,
                          float x, float y, String text) throws IOException {
        drawText(cs, font, size, x, y, text, null);
    }

    private void drawText(PDPageContentStream cs, PDType1Font font, float size,
                          float x, float y, String text, Color color) throws IOException {
        if (text == null) text = "";
        cs.beginText();
        cs.setFont(font, size);
        if (color != null) cs.setNonStrokingColor(color);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitize(text));
        cs.endText();
        if (color != null) cs.setNonStrokingColor(Color.BLACK);
    }

    /** Strip characters the Standard-14 fonts can't render (non-Latin1). */
    private String sanitize(String s) {
        StringBuilder sb = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            sb.append(c < 256 ? c : '?');
        }
        return sb.toString();
    }

    private float stringWidth(PDType1Font font, float size, String s) throws IOException {
        return font.getStringWidth(sanitize(s)) / 1000f * size;
    }

    /** "3 months, 12 days" style. */
    private String formatAge(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) return "not born yet";
        Period p = Period.between(from, to);
        if (p.getYears() > 0)  return p.getYears()  + " yr " + p.getMonths() + " mo";
        if (p.getMonths() > 0) return p.getMonths() + " mo "  + p.getDays()   + " d";
        return p.getDays() + " days";
    }

    /** Nice age label like "6 weeks" for a due-date relative to DOB. */
    private String ageAt(LocalDate dueDate, LocalDate dob) {
        long days = java.time.temporal.ChronoUnit.DAYS.between(dob, dueDate);
        if (days == 0)   return "At birth";
        if (days < 60)   return (days / 7) + " weeks";
        long months = Math.round(days / 30.0);
        return months + " months";
    }
}
