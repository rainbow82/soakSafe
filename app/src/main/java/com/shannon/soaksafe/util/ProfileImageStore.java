package com.shannon.soaksafe.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.RoundedBitmapDrawable;
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory;

import com.google.android.material.imageview.ShapeableImageView;
import com.shannon.soaksafe.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Stores profile photos in app-private storage ({@code files/profile_images/{userId}.jpg}).
 */
public final class ProfileImageStore {

    private static final String DIR = "profile_images";
    private static final String EXT = ".jpg";
    private static final int MAX_EDGE_PX = 512;
    private static final int JPEG_QUALITY = 88;

    private ProfileImageStore() {
    }

    @NonNull
    public static File imageFile(@NonNull Context context, long userId) {
        return new File(profileDir(context), userId + EXT);
    }

    public static boolean hasImage(@NonNull Context context, long userId) {
        File file = imageFile(context, userId);
        return file.isFile() && file.length() > 0L;
    }

    public static boolean saveFromUri(@NonNull Context context, long userId, @NonNull Uri sourceUri) {
        Bitmap decoded = decodeScaledBitmap(context, sourceUri);
        if (decoded == null) {
            return false;
        }
        File out = imageFile(context, userId);
        File parent = out.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            decoded.recycle();
            return false;
        }
        try (FileOutputStream stream = new FileOutputStream(out)) {
            if (!decoded.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, stream)) {
                decoded.recycle();
                return false;
            }
            stream.flush();
            return true;
        } catch (IOException e) {
            //noinspection ResultOfMethodCallIgnored
            out.delete();
            return false;
        } finally {
            decoded.recycle();
        }
    }

    public static void delete(@NonNull Context context, long userId) {
        File file = imageFile(context, userId);
        if (file.exists()) {
            //noinspection ResultOfMethodCallIgnored
            file.delete();
        }
    }

    public static void bindAvatar(
            @NonNull ShapeableImageView imageView,
            @NonNull Context context,
            long userId
    ) {
        if (userId > 0L && hasImage(context, userId)) {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile(context, userId).getAbsolutePath());
            if (bitmap != null) {
                imageView.setImageBitmap(bitmap);
                imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
                return;
            }
        }
        bindPlaceholder(imageView);
    }

    public static void bindUriPreview(
            @NonNull ShapeableImageView imageView,
            @NonNull Context context,
            @NonNull Uri uri
    ) {
        Bitmap bitmap = decodeScaledBitmap(context, uri);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
            imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
        } else {
            bindPlaceholder(imageView);
        }
    }

    public static void bindPlaceholder(@NonNull ShapeableImageView imageView) {
        imageView.setImageResource(R.drawable.ic_profile_circle_24);
        imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER);
    }

    @NonNull
    public static Drawable menuIcon(@NonNull Context context, long userId, int sizePx) {
        if (userId <= 0L || !hasImage(context, userId)) {
            Drawable fallback = ContextCompat.getDrawable(context, R.drawable.ic_profile_circle_24);
            if (fallback == null) {
                return new BitmapDrawable(context.getResources(), Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
            }
            fallback.setBounds(0, 0, sizePx, sizePx);
            return fallback;
        }
        Bitmap source = BitmapFactory.decodeFile(imageFile(context, userId).getAbsolutePath());
        if (source == null) {
            Drawable fallback = ContextCompat.getDrawable(context, R.drawable.ic_profile_circle_24);
            return fallback != null ? fallback : new BitmapDrawable(context.getResources(),
                    Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888));
        }
        Bitmap scaled = Bitmap.createScaledBitmap(source, sizePx, sizePx, true);
        if (scaled != source) {
            source.recycle();
        }
        RoundedBitmapDrawable round = RoundedBitmapDrawableFactory.create(context.getResources(), scaled);
        round.setCircular(true);
        return round;
    }

    @Nullable
    private static Bitmap decodeScaledBitmap(@NonNull Context context, @NonNull Uri uri) {
        try {
            Bitmap bounds = decodeStream(context, uri, null);
            if (bounds == null) {
                return null;
            }
            int sample = 1;
            int maxDim = Math.max(bounds.getWidth(), bounds.getHeight());
            while (maxDim / sample > MAX_EDGE_PX) {
                sample *= 2;
            }
            bounds.recycle();

            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = sample;
            Bitmap decoded = decodeStream(context, uri, opts);
            if (decoded == null) {
                return null;
            }
            return squareCrop(decoded);
        } catch (IOException e) {
            return null;
        }
    }

    @Nullable
    private static Bitmap decodeStream(
            @NonNull Context context,
            @NonNull Uri uri,
            @Nullable BitmapFactory.Options options
    ) throws IOException {
        try (InputStream in = context.getContentResolver().openInputStream(uri)) {
            if (in == null) {
                return null;
            }
            return BitmapFactory.decodeStream(in, null, options);
        }
    }

    @NonNull
    private static Bitmap squareCrop(@NonNull Bitmap source) {
        int size = Math.min(source.getWidth(), source.getHeight());
        if (size <= 0) {
            return source;
        }
        int x = (source.getWidth() - size) / 2;
        int y = (source.getHeight() - size) / 2;
        Bitmap cropped = Bitmap.createBitmap(source, x, y, size, size);
        if (cropped != source) {
            source.recycle();
        }
        if (cropped.getWidth() <= MAX_EDGE_PX) {
            return cropped;
        }
        Bitmap scaled = Bitmap.createScaledBitmap(cropped, MAX_EDGE_PX, MAX_EDGE_PX, true);
        if (scaled != cropped) {
            cropped.recycle();
        }
        return scaled;
    }

    @NonNull
    private static File profileDir(@NonNull Context context) {
        File dir = new File(context.getApplicationContext().getFilesDir(), DIR);
        if (!dir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            dir.mkdirs();
        }
        return dir;
    }
}
