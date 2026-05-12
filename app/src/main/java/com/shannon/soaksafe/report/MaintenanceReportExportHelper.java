package com.shannon.soaksafe.report;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;

import com.shannon.soaksafe.R;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Writes PDF bytes to cache and opens share / save flows via {@link FileProvider}.
 */
public final class MaintenanceReportExportHelper {

    private static final String PDF_NAME = "SoakSafe_maintenance_report.pdf";

    private MaintenanceReportExportHelper() {
    }

    @NonNull
    public static File pdfCacheFile(@NonNull Context context) {
        File dir = new File(context.getCacheDir(), "reports");
        if (!dir.isDirectory() && !dir.mkdirs()) {
            throw new IllegalStateException("Cannot create reports cache dir");
        }
        return new File(dir, PDF_NAME);
    }

    @NonNull
    public static Uri pdfUriForShare(@NonNull Activity activity, @NonNull File file) {
        return FileProvider.getUriForFile(
                activity,
                activity.getPackageName() + ".fileprovider",
                file
        );
    }

    public static void sharePdf(@NonNull Activity activity, @NonNull File file) {
        Uri uri = pdfUriForShare(activity, file);
        Intent send = new Intent(Intent.ACTION_SEND);
        send.setType("application/pdf");
        send.putExtra(Intent.EXTRA_STREAM, uri);
        send.putExtra(Intent.EXTRA_SUBJECT, activity.getString(R.string.pdf_share_subject));
        send.putExtra(Intent.EXTRA_TEXT, activity.getString(R.string.pdf_share_body));
        send.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        activity.startActivity(Intent.createChooser(
                send,
                activity.getString(R.string.pdf_share_chooser_title)
        ));
    }

    public static void copyToUri(
            @NonNull Context context,
            @NonNull File source,
            @NonNull Uri destination
    ) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = context.getContentResolver().openOutputStream(destination)) {
            if (out == null) {
                throw new IOException("Could not open destination");
            }
            byte[] buf = new byte[8192];
            int n;
            while ((n = in.read(buf)) > 0) {
                out.write(buf, 0, n);
            }
        }
    }
}
