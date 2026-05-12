package com.shannon.soaksafe.report;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.pdf.PdfDocument;
import android.text.TextPaint;
import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.shannon.soaksafe.R;
import com.shannon.soaksafe.data.MaintenanceEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Renders a printable maintenance report: title, generated timestamp, owner line, and a
 * multi-column table (date/time, item, value/status) with multiple rows per saved snapshot.
 */
public final class MaintenancePdfReportGenerator {

    private static final int PAGE_W = 612;
    private static final int PAGE_H = 792;
    private static final float MARGIN = 40f;
    private static final float LINE = 14f;
    private static final float GAP_SECTION = 6f;

    private MaintenancePdfReportGenerator() {
    }

    public static void writeReport(
            @NonNull Context context,
            @NonNull List<MaintenanceEvent> events,
            @NonNull String ownerLine,
            @NonNull File outputFile
    ) throws IOException {
        SimpleDateFormat rowTime = new SimpleDateFormat("MM/dd/yyyy hh:mm a", Locale.US);
        SimpleDateFormat generated = new SimpleDateFormat("MM/dd/yyyy HH:mm z", Locale.US);

        TextPaint titlePaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        titlePaint.setColor(Color.rgb(10, 22, 40));
        titlePaint.setTextSize(20f);
        titlePaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        TextPaint subPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        subPaint.setColor(Color.rgb(55, 65, 81));
        subPaint.setTextSize(11f);

        TextPaint headPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        headPaint.setColor(Color.WHITE);
        headPaint.setTextSize(10f);
        headPaint.setTypeface(Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD));

        TextPaint bodyPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        bodyPaint.setColor(Color.rgb(17, 24, 39));
        bodyPaint.setTextSize(10f);

        Paint headerFill = new Paint();
        headerFill.setColor(Color.rgb(0, 100, 148));

        Paint rowAlt = new Paint();
        rowAlt.setColor(0xFFF7F9FC);

        PdfDocument doc = new PdfDocument();
        PageWriter pw = new PageWriter(doc);

        float y = pw.y;
        y = drawWrapped(pw.canvas, context.getString(R.string.pdf_report_document_title), MARGIN,
                PAGE_W - MARGIN, y, titlePaint, LINE + 4);
        y += 6;
        y = drawWrapped(pw.canvas, context.getString(R.string.pdf_report_generated_line,
                generated.format(new Date())), MARGIN, PAGE_W - MARGIN, y, subPaint, LINE);
        if (!ownerLine.isEmpty()) {
            y = drawWrapped(pw.canvas, ownerLine, MARGIN, PAGE_W - MARGIN, y, subPaint, LINE);
        }
        y += GAP_SECTION * 2;
        pw.y = y;

        float col1 = MARGIN;
        float col2 = MARGIN + 128f;
        float col3 = MARGIN + 128f + 210f;
        float right = PAGE_W - MARGIN;

        pw.ensureSpace(LINE + 10);
        pw.canvas.drawRect(col1 - 4, pw.y - 2, right + 4, pw.y + LINE + 4, headerFill);
        pw.canvas.drawText(context.getString(R.string.pdf_col_datetime), col1, pw.y + LINE, headPaint);
        pw.canvas.drawText(context.getString(R.string.pdf_col_item), col2, pw.y + LINE, headPaint);
        pw.canvas.drawText(context.getString(R.string.pdf_col_value_status), col3, pw.y + LINE, headPaint);
        pw.y += LINE + 10;

        boolean alt = false;
        for (MaintenanceEvent event : events) {
            String time = rowTime.format(new Date(event.getEventTimeMillis()));
            List<ReportDetailLine> lines = ReportEventRowsFactory.buildDetailRows(context, event);
            String type = event.getEventType() != null ? event.getEventType().trim() : "";
            String typeSuffix = type.isEmpty() ? "" : " (" + type + ")";

            pw.ensureSpace(LINE + 6);
            if (alt) {
                pw.canvas.drawRect(MARGIN - 4, pw.y - 2, PAGE_W - MARGIN + 4, pw.y + LINE + 4, rowAlt);
            }
            alt = !alt;
            pw.y = drawThreeColumnRow(pw.canvas, time,
                    context.getString(R.string.report_card_subtitle) + typeSuffix,
                    "—", col1, col2, col3, right, pw.y, bodyPaint, LINE);
            pw.y += 2;

            for (ReportDetailLine line : lines) {
                pw.ensureSpace(LINE + 6);
                if (alt) {
                    pw.canvas.drawRect(MARGIN - 4, pw.y - 2, PAGE_W - MARGIN + 4, pw.y + LINE + 4, rowAlt);
                }
                alt = !alt;
                String val = line.showCheckmark
                        ? context.getString(R.string.pdf_value_completed)
                        : (line.valueText != null ? line.valueText : "");
                pw.y = drawThreeColumnRow(pw.canvas, time, line.label, val,
                        col1, col2, col3, right, pw.y, bodyPaint, LINE);
                pw.y += 2;
            }
            pw.y += GAP_SECTION;
        }

        if (events.isEmpty()) {
            pw.ensureSpace(LINE * 2);
            pw.y = drawWrapped(pw.canvas, context.getString(R.string.report_no_events),
                    MARGIN, PAGE_W - MARGIN, pw.y, bodyPaint, LINE);
        }

        pw.finish();
        try (FileOutputStream fos = new FileOutputStream(outputFile)) {
            doc.writeTo(fos);
        }
        doc.close();
    }

    private static final class PageWriter {
        final PdfDocument doc;
        PdfDocument.Page page;
        Canvas canvas;
        int pageNum = 0;
        float y = MARGIN;

        PageWriter(PdfDocument doc) {
            this.doc = doc;
            newPage();
        }

        void newPage() {
            if (page != null) {
                doc.finishPage(page);
            }
            pageNum++;
            page = doc.startPage(
                    new PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create()
            );
            canvas = page.getCanvas();
            y = MARGIN;
        }

        void ensureSpace(float needed) {
            if (y + needed > PAGE_H - MARGIN) {
                newPage();
            }
        }

        void finish() {
            if (page != null) {
                doc.finishPage(page);
                page = null;
            }
        }
    }

    private static float drawWrapped(
            Canvas c,
            @NonNull String text,
            float left,
            float right,
            float y,
            TextPaint paint,
            float lineHeight
    ) {
        float w = right - left;
        int start = 0;
        while (start < text.length()) {
            int count = paint.breakText(text, start, text.length(), true, w, null);
            if (count <= 0) {
                break;
            }
            int end = start + count;
            c.drawText(text, start, end, left, y + lineHeight, paint);
            y += lineHeight;
            start = end;
            while (start < text.length() && text.charAt(start) == ' ') {
                start++;
            }
        }
        return y;
    }

    private static float drawThreeColumnRow(
            Canvas c,
            @NonNull String colDate,
            @NonNull String colItem,
            @NonNull String colValue,
            float x1,
            float x2,
            float x3,
            float right,
            float y,
            TextPaint paint,
            float lineHeight
    ) {
        float w1 = x2 - 8f - x1;
        float w2 = x3 - 8f - x2;
        float w3 = right - x3;
        float baseline = y + lineHeight;
        c.drawText(ellipsize(paint, colDate, w1), x1, baseline, paint);
        c.drawText(ellipsize(paint, colItem, w2), x2, baseline, paint);
        c.drawText(ellipsize(paint, colValue, w3), x3, baseline, paint);
        return y + lineHeight + 4f;
    }

    @NonNull
    private static String ellipsize(@NonNull TextPaint paint, @NonNull String text, float maxWidth) {
        if (text.isEmpty()) {
            return "";
        }
        return TextUtils.ellipsize(text, paint, maxWidth, TextUtils.TruncateAt.END).toString();
    }
}
