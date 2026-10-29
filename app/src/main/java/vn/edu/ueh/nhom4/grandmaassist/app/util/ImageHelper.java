package vn.edu.ueh.nhom4.grandmaassist.app.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageDecoder;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Base64;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Tiện ích xử lý ảnh nơi làm việc (nén, chuyển đổi Base64 và giải mã hiển thị an toàn).
 */
public final class ImageHelper {
    private ImageHelper() {}

    /**
     * Giải mã chuỗi Base64 (hoặc Data URI `data:image/...;base64,...`) thành Bitmap.
     */
    public static Bitmap decodeBase64(String dataUriOrBase64) {
        if (dataUriOrBase64 == null || dataUriOrBase64.trim().isEmpty()) return null;
        try {
            String s = dataUriOrBase64.trim();
            int comma = s.indexOf(',');
            if (comma >= 0) s = s.substring(comma + 1);
            byte[] bytes = Base64.decode(s, Base64.DEFAULT);
            if (bytes == null || bytes.length == 0) return null;
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Đọc ảnh từ Uri, giới hạn kích thước cạnh tối đa và nén JPEG để chuỗi Base64
     * không vượt quá maxChars (đảm bảo phù hợp giới hạn document Firestore & LocalJobDraft).
     */
    public static String uriToCompressedBase64(Context context, Uri uri, int maxSide, int maxChars) {
        try {
            byte[] raw;
            try (InputStream in = context.getContentResolver().openInputStream(uri);
                 ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
                if (in == null) return null;
                byte[] buf = new byte[8192];
                int n;
                while ((n = in.read(buf)) != -1) {
                    bos.write(buf, 0, n);
                }
                raw = bos.toByteArray();
            }
            if (raw.length == 0) return null;

            Bitmap bitmap;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(raw)), (decoder, info, source) -> {
                    decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
                    int w = info.getSize().getWidth();
                    int h = info.getSize().getHeight();
                    float scale = Math.min(1f, (float) maxSide / Math.max(w, h));
                    decoder.setTargetSize(Math.max(1, (int) (w * scale)), Math.max(1, (int) (h * scale)));
                });
            } else {
                BitmapFactory.Options opts = new BitmapFactory.Options();
                opts.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(raw, 0, raw.length, opts);
                int w = opts.outWidth;
                int h = opts.outHeight;
                int sample = 1;
                while (Math.max(w, h) / sample > maxSide) {
                    sample *= 2;
                }
                opts.inSampleSize = sample;
                opts.inJustDecodeBounds = false;
                bitmap = BitmapFactory.decodeByteArray(raw, 0, raw.length, opts);
            }

            if (bitmap == null) return null;

            int quality = 75;
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
            byte[] compressed = out.toByteArray();

            while (compressed.length * 4 / 3 > maxChars && quality > 35) {
                quality -= 15;
                out.reset();
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out);
                compressed = out.toByteArray();
            }
            bitmap.recycle();

            return "data:image/jpeg;base64," + Base64.encodeToString(compressed, Base64.NO_WRAP);
        } catch (Exception e) {
            return null;
        }
    }
}
