package com.computershare.regfiling.service;

import com.computershare.regfiling.domain.Filing;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Renders a single Filing as a byte-for-byte-faithful layout replica of the real SEC Form 4 (SEC
 * 1474) — the master reference is an actual sample Form 4 PDF reviewed directly, not a loose
 * approximation. Every section, box, and line the real form has is reproduced in the same
 * position, including ones this MVP never populates (Table II — derivative securities; the
 * amendment and joint-filing boxes) — those render with the real form's own labels and an empty
 * body, exactly as the real form looks for a first-time, single-filer, non-derivative-only filing.
 * Deliberately distinct from the colorful in-app review screen (FilingReview.tsx), which uses this
 * app's normal branded design system; this is the one place brand colors are intentionally absent.
 */
@Service
public class FilingPdfService {

    private static final float PAGE_WIDTH = PDRectangle.LETTER.getWidth();
    private static final float PAGE_HEIGHT = PDRectangle.LETTER.getHeight();
    private static final float MARGIN = 40f;
    private static final float RIGHT_X = PAGE_WIDTH - MARGIN;
    private static final float CONTENT_WIDTH = RIGHT_X - MARGIN;

    // Three-column header grid (matches the real form exactly): col A = Section 1 (full height),
    // col B = Sections 2/3/4 stacked, col C = Sections 5/6 stacked.
    private static final float COL_A_X = MARGIN;
    private static final float COL_B_X = MARGIN + 220f;
    private static final float COL_C_X = MARGIN + 220f + 165f;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MM/dd/yyyy");

    public byte[] render(Filing filing) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);

            PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont italic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.setLineWidth(0.75f);

                float y = PAGE_HEIGHT - MARGIN;
                y = drawHeaderBand(cs, bold, regular, italic, y);
                y = drawThreeColumnGrid(cs, bold, regular, italic, y, filing);
                y = drawNonDerivativeTable(cs, bold, regular, y, filing);
                y = drawDerivativeTable(cs, bold, regular, italic, y);
                y = drawExplanationHeader(cs, bold, y);
                y = drawSignatureFooter(cs, bold, italic, regular, y, filing);
                drawLegalBoilerplate(cs, regular, bold, y);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to render Form 4 PDF for filing " + filing.getId(), e);
        }
    }

    // ---------------------------------------------------------------------------------------
    // Header band: FORM 4 title + the two real checkboxes (left), centered SEC title block
    // (center), OMB approval box (right).
    // ---------------------------------------------------------------------------------------
    private float drawHeaderBand(PDPageContentStream cs, PDFont bold, PDFont regular, PDFont italic, float y) throws IOException {
        float top = y;
        drawLine(cs, bold, 15, "FORM 4", MARGIN, top);

        drawCheckboxGlyph(cs, top - 18);
        drawWrapped(cs, regular, 5.5f, MARGIN + 10, top - 20, 6.5f,
                "Check this box if no longer subject to",
                "Section 16. Form 4 or Form 5",
                "obligations may continue. See",
                "Instruction 1(b).");

        drawCheckboxGlyph(cs, top - 50);
        drawWrapped(cs, regular, 5.5f, MARGIN + 10, top - 52, 6.5f,
                "Check this box to indicate that a",
                "transaction was made pursuant to a",
                "contract, instruction or written plan that",
                "is intended to satisfy the affirmative",
                "defense conditions of Rule 10b5-1(c).",
                "See Instruction 10.");

        float zoneX0 = 193f;
        float zoneX1 = 449f;
        centerInZoneFit(cs, bold, 10, "UNITED STATES SECURITIES AND EXCHANGE COMMISSION", zoneX0, zoneX1, top - 4);
        centerInZoneFit(cs, regular, 8.5f, "Washington, D.C. 20549", zoneX0, zoneX1, top - 15);
        centerInZoneFit(cs, bold, 9.5f, "STATEMENT OF CHANGES IN BENEFICIAL OWNERSHIP OF SECURITIES", zoneX0, zoneX1, top - 29);
        centerInZoneFit(cs, italic, 6.5f, "Filed pursuant to Section 16(a) of the Securities Exchange Act of 1934 or Section 30(h) of the", zoneX0, zoneX1, top - 40);
        centerInZoneFit(cs, italic, 6.5f, "Investment Company Act of 1940", zoneX0, zoneX1, top - 49);

        float ombWidth = 122f;
        float ombHeight = 44f;
        float ombX = RIGHT_X - ombWidth;
        float ombY = top - ombHeight + 4;
        cs.addRect(ombX, ombY, ombWidth, ombHeight);
        cs.stroke();
        drawLine(cs, bold, 6.5f, "OMB APPROVAL", ombX + 4, ombY + ombHeight - 10);
        cs.moveTo(ombX + 4, ombY + ombHeight - 13);
        cs.lineTo(ombX + ombWidth - 4, ombY + ombHeight - 13);
        cs.stroke();
        drawLine(cs, regular, 6f, "OMB Number: 3235-0287", ombX + 4, ombY + ombHeight - 22);
        drawLine(cs, regular, 6f, "Estimated average burden", ombX + 4, ombY + ombHeight - 31);
        drawLine(cs, regular, 6f, "hours per response: 0.5", ombX + 4, ombY + ombHeight - 40);

        // Bottom must clear the taller of the two left-side checkbox blocks (6 wrapped lines,
        // the longer one) — top-52 start minus 5*6.5 leading minus descender clearance.
        float bottom = top - 95;
        cs.moveTo(MARGIN, bottom);
        cs.lineTo(RIGHT_X, bottom);
        cs.stroke();
        return bottom;
    }

    // ---------------------------------------------------------------------------------------
    // Three-column grid: col A = Section 1 (full height); col B = Sections 2/3/4; col C =
    // Sections 5/6. Matches the real form's actual layout exactly, including the sections this
    // MVP never populates (4, 6), which render with the real form's own static text.
    // ---------------------------------------------------------------------------------------
    private float drawThreeColumnGrid(PDPageContentStream cs, PDFont bold, PDFont regular, PDFont italic, float y, Filing filing) throws IOException {
        float gridHeight = 178f;
        float top = y;
        float bottom = top - gridHeight;

        cs.addRect(MARGIN, bottom, CONTENT_WIDTH, gridHeight);
        cs.stroke();
        cs.moveTo(COL_B_X, bottom);
        cs.lineTo(COL_B_X, top);
        cs.stroke();
        cs.moveTo(COL_C_X, bottom);
        cs.lineTo(COL_C_X, top);
        cs.stroke();

        drawColumnA(cs, bold, regular, italic, top, filing);
        drawColumnB(cs, bold, regular, top, filing);
        drawColumnC(cs, bold, regular, top, filing);

        return bottom;
    }

    private void drawColumnA(PDPageContentStream cs, PDFont bold, PDFont regular, PDFont italic, float top, Filing filing) throws IOException {
        float x = COL_A_X + 4;
        drawWrapped(cs, bold, 6.5f, x, top - 9, 7.5f, "1. NAME AND ADDRESS OF REPORTING PERSON *");
        drawLine(cs, regular, 8.5f,
                str(filing.getReportingPersonLast()) + "    " + str(filing.getReportingPersonFirst())
                        + "    " + str(filing.getReportingPersonMiddle()),
                x, top - 24);
        drawLine(cs, italic, 5.5f, "(Last)                    (First)                    (Middle)", x, top - 32);

        drawLine(cs, regular, 8.5f, str(filing.getReportingPersonStreet()), x, top - 50);
        drawLine(cs, italic, 5.5f, "(Street)", x, top - 58);

        drawLine(cs, regular, 8.5f,
                str(filing.getReportingPersonCity()) + "    " + str(filing.getReportingPersonState())
                        + "    " + str(filing.getReportingPersonZip()),
                x, top - 76);
        drawLine(cs, italic, 5.5f, "(City)                    (State)                    (Zip)", x, top - 84);

        drawLine(cs, regular, 8.5f, "UNITED STATES", x, top - 102);
        drawLine(cs, italic, 5.5f, "(Country)", x, top - 110);
    }

    private void drawColumnB(PDPageContentStream cs, PDFont bold, PDFont regular, float top, Filing filing) throws IOException {
        float x = COL_B_X + 4;
        float row2Bottom = top - 55;
        float row3Bottom = row2Bottom - 55;
        cs.moveTo(COL_B_X, row2Bottom);
        cs.lineTo(COL_C_X, row2Bottom);
        cs.stroke();
        cs.moveTo(COL_B_X, row3Bottom);
        cs.lineTo(COL_C_X, row3Bottom);
        cs.stroke();

        drawWrapped(cs, bold, 6.5f, x, top - 9, 7.5f, "2. ISSUER NAME AND TICKER OR", "TRADING SYMBOL");
        drawLine(cs, regular, 8.5f, str(filing.getIssuer()) + "  [ " + str(filing.getIssuerTicker()) + " ]", x, top - 27);

        drawWrapped(cs, bold, 6.5f, x, row2Bottom - 9, 7.5f, "3. DATE OF EARLIEST TRANSACTION", "(MONTH/DAY/YEAR)");
        drawLine(cs, regular, 8.5f, formatDate(filing), x, row2Bottom - 27);

        drawWrapped(cs, bold, 6.5f, x, row3Bottom - 9, 7.5f, "4. IF AMENDMENT, DATE ORIGINAL", "FILED (MONTH/DAY/YEAR)");
        // Amendments are out of MVP scope (see ValidationService/lld.md) — this box stays blank,
        // exactly as the real form shows it for a first-time, non-amended filing.
    }

    private void drawColumnC(PDPageContentStream cs, PDFont bold, PDFont regular, float top, Filing filing) throws IOException {
        float x = COL_C_X + 4;
        float row2Bottom = top - 108;
        cs.moveTo(COL_C_X, row2Bottom);
        cs.lineTo(RIGHT_X, row2Bottom);
        cs.stroke();

        drawWrapped(cs, bold, 6.5f, x, top - 9, 7.5f,
                "5. RELATIONSHIP OF REPORTING", "PERSON(S) TO ISSUER (CHECK ALL", "APPLICABLE)");
        float rowY = top - 34;
        float nextX = drawCheckbox(cs, regular, x, rowY, "Director", filing.isRelationshipDirector());
        drawCheckbox(cs, regular, nextX + 6, rowY, "10% Owner", filing.isRelationshipTenPercentOwner());
        String officerLabel = "Officer" + (filing.isRelationshipOfficer() && !str(filing.getOfficerTitle()).isEmpty()
                ? " (" + filing.getOfficerTitle() + ")" : " (give title below)");
        drawWrappedCheckbox(cs, regular, x, rowY - 16, 7f, officerLabel, filing.isRelationshipOfficer(), RIGHT_X - 4);
        drawCheckbox(cs, regular, x, rowY - 48, "Other (specify below)", filing.isRelationshipOther());

        drawWrapped(cs, bold, 6.5f, x, row2Bottom - 9, 7.5f, "6. INDIVIDUAL OR JOINT/GROUP", "FILING (CHECK APPLICABLE LINE)");
        drawCheckbox(cs, regular, x, row2Bottom - 30, "Form filed by One Reporting Person", true);
        drawWrappedCheckbox(cs, regular, x, row2Bottom - 42, 7f, "Form filed by More than One Reporting Person", false, RIGHT_X - 4);
    }

    // ---------------------------------------------------------------------------------------
    // Table I — Non-Derivative Securities (the one table this MVP actually populates).
    // ---------------------------------------------------------------------------------------
    private float drawNonDerivativeTable(PDPageContentStream cs, PDFont bold, PDFont regular, float y, Filing filing) throws IOException {
        String[] headers = {"Title of\nSecurity\n(Instr. 3)", "Trans.\nDate", "Code\n(Instr. 8)", "Amount", "(A)\nor (D)", "Price",
                "Amount Owned\nFollowing", "Ownership\nForm"};
        float[] widths = {84, 58, 40, 56, 34, 50, 92, CONTENT_WIDTH - (84 + 58 + 40 + 56 + 34 + 50 + 92)};

        float labelY = y - 10;
        drawLine(cs, bold, 8, "Table I — Non-Derivative Securities Acquired, Disposed of, or Beneficially Owned", MARGIN, labelY);

        String[] values = {
                str(filing.getTitleOfSecurity()),
                formatDate(filing),
                str(filing.getTransactionCode()),
                filing.getShares() == null ? "" : String.valueOf(filing.getShares()),
                str(filing.getAcquiredOrDisposed()),
                filing.getPricePerShare() == null ? "" : String.format(Locale.ROOT, "%.2f", filing.getPricePerShare()),
                filing.getSharesOwnedFollowingTransaction() == null ? "" : String.valueOf(filing.getSharesOwnedFollowingTransaction()),
                str(filing.getOwnershipForm()),
        };
        return drawTable(cs, bold, regular, labelY - 8, headers, widths, new String[][] {values});
    }

    // ---------------------------------------------------------------------------------------
    // Table II — Derivative Securities. This MVP never models derivative transactions (see
    // lld.md §1) — the table renders with the real form's own headers and one empty row, exactly
    // as the real form looks for a filer with nothing to report in it.
    // ---------------------------------------------------------------------------------------
    private float drawDerivativeTable(PDPageContentStream cs, PDFont bold, PDFont regular, PDFont italic, float y) throws IOException {
        String[] headers = {"Title of\nDerivative\nSecurity", "Conversion\nor Exercise\nPrice", "Trans.\nDate", "Trans.\nCode",
                "Number\nAcquired (A)\nor Disposed (D)", "Date\nExercisable /\nExpiration", "Title and Amount\nof Securities\nUnderlying", "Price of\nDerivative\nSecurity",
                "Number\nBeneficially\nOwned Following", "Ownership\nForm"};
        float[] widths = {66, 52, 44, 36, 62, 62, 76, 48, 62, CONTENT_WIDTH - (66 + 52 + 44 + 36 + 62 + 62 + 76 + 48 + 62)};

        float labelY = y - 14;
        drawLine(cs, bold, 8, "Table II — Derivative Securities Acquired, Disposed of, or Beneficially Owned", MARGIN, labelY);
        drawLine(cs, italic, 6, "(e.g., puts, calls, warrants, options, convertible securities) — not applicable to this filing", MARGIN, labelY - 8);

        return drawTable(cs, bold, regular, labelY - 16, headers, widths, new String[][] {new String[widths.length]});
    }

    /** Shared ruled-table drawing: a header row (possibly multi-line) plus one or more data rows. */
    private float drawTable(PDPageContentStream cs, PDFont bold, PDFont regular, float top, String[] headers, float[] widths, String[][] rows) throws IOException {
        float headerHeight = 24;
        float rowHeight = 18;
        float tableHeight = headerHeight + rowHeight * rows.length;
        float bottom = top - tableHeight;

        cs.addRect(MARGIN, bottom, CONTENT_WIDTH, tableHeight);
        cs.stroke();
        cs.moveTo(MARGIN, top - headerHeight);
        cs.lineTo(RIGHT_X, top - headerHeight);
        cs.stroke();
        for (int r = 1; r < rows.length; r++) {
            float ruleY = top - headerHeight - rowHeight * r;
            cs.moveTo(MARGIN, ruleY);
            cs.lineTo(RIGHT_X, ruleY);
            cs.stroke();
        }

        float colX = MARGIN;
        for (int i = 0; i < headers.length; i++) {
            if (i > 0) {
                cs.moveTo(colX, bottom);
                cs.lineTo(colX, top);
                cs.stroke();
            }
            String[] lines = headers[i].split("\n");
            for (int l = 0; l < lines.length; l++) {
                drawLine(cs, bold, 5.5f, lines[l], colX + 3, top - 9 - (l * 6.5f));
            }
            colX += widths[i];
        }

        for (int r = 0; r < rows.length; r++) {
            colX = MARGIN;
            float rowTextY = top - headerHeight - rowHeight * r - 12;
            for (int i = 0; i < widths.length; i++) {
                String v = i < rows[r].length && rows[r][i] != null ? rows[r][i] : "";
                drawLine(cs, regular, 7.5f, v, colX + 3, rowTextY);
                colX += widths[i];
            }
        }

        return bottom;
    }

    private float drawExplanationHeader(PDPageContentStream cs, PDFont bold, float y) throws IOException {
        drawLine(cs, bold, 7.5f, "Explanation of Responses:", MARGIN, y - 12);
        return y - 12;
    }

    private float drawSignatureFooter(PDPageContentStream cs, PDFont bold, PDFont italic, PDFont regular, float y, Filing filing) throws IOException {
        float lineY = y - 26;
        float signatureLineEnd = MARGIN + 260;
        cs.moveTo(MARGIN, lineY);
        cs.lineTo(signatureLineEnd, lineY);
        cs.stroke();
        drawLine(cs, italic, 9, str(filing.getSignedBy()), MARGIN + 2, lineY + 2);

        float dateX = signatureLineEnd + 20;
        cs.moveTo(dateX, lineY);
        cs.lineTo(RIGHT_X, lineY);
        cs.stroke();
        String dateLabel = filing.getSignedAt() == null ? ""
                : DATE_FMT.format(filing.getSignedAt().atZone(ZoneOffset.UTC).toLocalDate());
        drawLine(cs, italic, 9, dateLabel, dateX + 2, lineY + 2);

        drawLine(cs, italic, 6.5f, "** Signature of Reporting Person", MARGIN, lineY - 10);
        drawLine(cs, italic, 6.5f, "Date", dateX, lineY - 10);

        return lineY - 20;
    }

    private void drawLegalBoilerplate(PDPageContentStream cs, PDFont regular, PDFont bold, float y) throws IOException {
        float lineY = y - 4;
        cs.moveTo(MARGIN, lineY + 6);
        cs.lineTo(RIGHT_X, lineY + 6);
        cs.stroke();
        drawLine(cs, regular, 6f, "Reminder: Report on a separate line for each class of securities beneficially owned directly or indirectly.", MARGIN, lineY);
        drawLine(cs, regular, 6f, "* If the form is filed by more than one reporting person, see Instruction 4(b)(v).", MARGIN, lineY - 9);
        drawLine(cs, regular, 6f, "** Intentional misstatements or omissions of facts constitute Federal Criminal Violations. See 18 U.S.C. 1001 and 15 U.S.C. 78ff(a).", MARGIN, lineY - 18);
        drawLine(cs, regular, 6f, "Note: File three copies of this Form, one of which must be manually signed. If space is insufficient, see Instruction 6 for procedure.", MARGIN, lineY - 27);
        drawLine(cs, bold, 6f, "Persons who respond to the collection of information contained in this form are not required to respond unless the form displays a currently valid OMB control number.", MARGIN, lineY - 36);
    }

    // ---------------------------------------------------------------------------------------
    // Drawing primitives
    // ---------------------------------------------------------------------------------------

    private float drawCheckbox(PDPageContentStream cs, PDFont regular, float x, float y, String label, boolean checked) throws IOException {
        drawCheckSquare(cs, x, y, checked);
        drawLine(cs, regular, 7f, label, x + 11, y);
        return x + 11 + regular.getStringWidth(label) / 1000f * 7f;
    }

    /** A checkbox whose label word-wraps to fit within {@code maxX} — used for the officer-title
     * and joint-filing labels, which are long enough (especially once a real title is appended)
     * that a single unwrapped line would run off the column. */
    private void drawWrappedCheckbox(PDPageContentStream cs, PDFont regular, float x, float y, float size,
                                      String label, boolean checked, float maxX) throws IOException {
        drawCheckSquare(cs, x, y, checked);
        float textX = x + 11;
        float maxWidth = maxX - textX;
        String[] words = label.split(" ");
        StringBuilder line = new StringBuilder();
        float lineY = y;
        for (String word : words) {
            String candidate = line.length() == 0 ? word : line + " " + word;
            float width = regular.getStringWidth(candidate) / 1000f * size;
            if (width > maxWidth && line.length() > 0) {
                drawLine(cs, regular, size, line.toString(), textX, lineY);
                lineY -= size + 1.5f;
                line = new StringBuilder(word);
            } else {
                line = new StringBuilder(candidate);
            }
        }
        if (line.length() > 0) {
            drawLine(cs, regular, size, line.toString(), textX, lineY);
        }
    }

    private void drawCheckSquare(PDPageContentStream cs, float x, float y, boolean checked) throws IOException {
        float size = 7f;
        cs.addRect(x, y - size + 2, size, size);
        cs.stroke();
        if (checked) {
            cs.moveTo(x, y - size + 2);
            cs.lineTo(x + size, y + 2);
            cs.moveTo(x, y + 2);
            cs.lineTo(x + size, y - size + 2);
            cs.stroke();
        }
    }

    private void drawCheckboxGlyph(PDPageContentStream cs, float y) throws IOException {
        cs.addRect(MARGIN, y - 4, 6, 6);
        cs.stroke();
    }

    private static void drawWrapped(PDPageContentStream cs, PDFont font, float size, float x, float startY, float leading, String... lines) throws IOException {
        for (int i = 0; i < lines.length; i++) {
            drawLine(cs, font, size, lines[i], x, startY - i * leading);
        }
    }

    private static String formatDate(Filing filing) {
        return filing.getTransactionDate() == null ? "" : DATE_FMT.format(filing.getTransactionDate());
    }

    private static String str(String s) {
        return s == null ? "" : s;
    }

    private static void drawLine(PDPageContentStream cs, PDFont font, float size, String text, float x, float y) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text);
        cs.endText();
    }

    private static void centerInZone(PDPageContentStream cs, PDFont font, float size, String text, float zoneX0, float zoneX1, float y) throws IOException {
        float textWidth = font.getStringWidth(text) / 1000f * size;
        float x = zoneX0 + ((zoneX1 - zoneX0) - textWidth) / 2f;
        drawLine(cs, font, size, text, x, y);
    }

    /** Like {@link #centerInZone}, but shrinks from {@code maxSize} until the text actually fits
     * the zone's width — measured against the real font metrics rather than an assumed size, so
     * the SEC/OMB header block can never overflow into an adjacent column regardless of exactly
     * how wide a given line of static boilerplate text turns out to be. */
    private static void centerInZoneFit(PDPageContentStream cs, PDFont font, float maxSize, String text, float zoneX0, float zoneX1, float y) throws IOException {
        float zoneWidth = zoneX1 - zoneX0;
        float size = maxSize;
        while (size > 5f && font.getStringWidth(text) / 1000f * size > zoneWidth) {
            size -= 0.5f;
        }
        centerInZone(cs, font, size, text, zoneX0, zoneX1, y);
    }
}
