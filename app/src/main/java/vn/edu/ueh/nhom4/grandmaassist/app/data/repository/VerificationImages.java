package vn.edu.ueh.nhom4.grandmaassist.app.data.repository;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.ImageDecoder;
import android.graphics.BitmapFactory;
import android.graphics.ColorSpace;
import android.net.Uri;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import android.util.Base64;

/** Bounded local decode, oriented pixels, recompress without EXIF/location metadata. */
public final class VerificationImages {
    public static final int MAX = 10 * 1024 * 1024;
    private VerificationImages() { }
    public static final class Prepared {
        public final byte[] image, qrSource;
        Prepared(byte[] image,byte[] qrSource) { this.image=image;this.qrSource=qrSource; }
    }
    public static byte[] read(Context context, Uri uri) throws Exception { return prepare(context,uri,false).image; }
    public static Prepared prepare(Context context,Uri uri,boolean identity) throws Exception {
        byte[] bytes;
        try (InputStream in = context.getContentResolver().openInputStream(uri);
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (in == null) throw new IllegalArgumentException();
            byte[] buffer = new byte[8192]; int n;
            while ((n = in.read(buffer)) != -1) {
                if (out.size() + n > MAX) throw new IllegalArgumentException();
                out.write(buffer, 0, n);
            }
            bytes = out.toByteArray();
        }
        boolean jpeg = bytes.length > 3 && (bytes[0] & 255) == 255 && (bytes[1] & 255) == 216;
        boolean png = bytes.length > 8 && bytes[0] == (byte)137 && bytes[1] == 80
                && bytes[2] == 78 && bytes[3] == 71;
        if (!jpeg && !png) throw new IllegalArgumentException();
        Bitmap image = ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(bytes)), (decoder, info, source) -> {
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            int w = info.getSize().getWidth(), h = info.getSize().getHeight();
            float factor = Math.min(1f, 3072f / Math.max(w, h));
            decoder.setTargetSize(Math.max(1, (int)(w * factor)), Math.max(1, (int)(h * factor)));
        });
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            if (!image.compress(Bitmap.CompressFormat.JPEG, 92, out) || out.size() > MAX)
                throw new IllegalArgumentException();
            return new Prepared(out.toByteArray(),identity?bytes:null);
        } finally { image.recycle(); }
    }
    /** Scan oriented source pixels, not the re-encoded upload or the RGB_565 UI thumbnail. */
    public static Bitmap qrBitmap(byte[] source) throws Exception {
        return ImageDecoder.decodeBitmap(ImageDecoder.createSource(ByteBuffer.wrap(source)),(decoder,info,input)->{
            decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB));
            int width=info.getSize().getWidth(),height=info.getSize().getHeight();
            float ratio=Math.min(1f,3072f/Math.max(width,height));
            decoder.setTargetSize(Math.max(1,Math.round(width*ratio)),Math.max(1,Math.round(height*ratio)));
        });
    }
    public static String md5(byte[] bytes) throws Exception {
        return Base64.encodeToString(MessageDigest.getInstance("MD5").digest(bytes), Base64.NO_WRAP);
    }
    public static Bitmap preview(byte[] bytes,int maxSide) {
        if(bytes==null)return null;
        BitmapFactory.Options options=new BitmapFactory.Options(); options.inJustDecodeBounds=true;
        BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
        if(options.outWidth<=0 || options.outHeight<=0)return null;
        options.inSampleSize=1;
        while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>maxSide)options.inSampleSize*=2;
        options.inJustDecodeBounds=false; options.inPreferredConfig=Bitmap.Config.RGB_565;
        return BitmapFactory.decodeByteArray(bytes,0,bytes.length,options);
    }
}
