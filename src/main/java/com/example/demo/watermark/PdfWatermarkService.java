package com.example.demo.watermark;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageContentStream.AppendMode;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;

/**
 * Stamps a dynamic, per-request diagonal watermark on every page of a PDF.
 *
 * Text is composed by the caller (PdfController) from live request data:
 *   line1 = user email
 *   line2 = IP address + UTC timestamp
 */
@Service
public class PdfWatermarkService {

    private static final float FONT_SIZE    = 18f;
    private static final float LINE_SPACING = 24f;
    // Diagonal angle in radians (~30°)
    private static final double ANGLE_RAD   = Math.toRadians(30);

    /**
     * @param filePath      absolute path to the source PDF
     * @param line1         first watermark line (email)
     * @param line2         second watermark line (IP | timestamp)
     * @return watermarked PDF bytes, ready to stream to the client
     */
    public byte[] addWatermark(String filePath, String line1, String line2) throws IOException {
        PDDocument document = PDDocument.load(new File(filePath));

        for (PDPage page : document.getPages()) {
            PDRectangle pageSize = page.getMediaBox();
            float cx = pageSize.getWidth()  / 2f;
            float cy = pageSize.getHeight() / 2f;

            PDPageContentStream cs = new PDPageContentStream(document, page, AppendMode.APPEND, true, true);
            cs.saveGraphicsState();

            // Light grey, semi-transparent feel
            cs.setNonStrokingColor(180, 180, 180);

            // Apply rotation matrix around page centre
            float cosA = (float) Math.cos(ANGLE_RAD);
            float sinA = (float) Math.sin(ANGLE_RAD);
            cs.transform(new org.apache.pdfbox.util.Matrix(cosA, sinA, -sinA, cosA, cx, cy));

            cs.beginText();
            cs.setFont(PDType1Font.HELVETICA_BOLD, FONT_SIZE);

            // Line 1 — centred by estimating string width
            float w1 = PDType1Font.HELVETICA_BOLD.getStringWidth(line1) / 1000f * FONT_SIZE;
            cs.newLineAtOffset(-w1 / 2f, LINE_SPACING / 2f);
            cs.showText(line1);

            // Line 2 — directly below line 1
            float w2 = PDType1Font.HELVETICA_BOLD.getStringWidth(line2) / 1000f * FONT_SIZE;
            cs.newLineAtOffset((-w2 + w1) / 2f, -LINE_SPACING);
            cs.showText(line2);

            cs.endText();
            cs.restoreGraphicsState();
            cs.close();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        document.save(out);
        document.close();
        return out.toByteArray();
    }

    /**
     * Convenience overload — single-string watermark (backward-compatible with legacy calls).
     */
    public byte[] addWatermark(String filePath, String watermarkText) throws IOException {
        return addWatermark(filePath, watermarkText, "");
    }
}
