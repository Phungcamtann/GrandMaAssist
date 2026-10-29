package vn.edu.ueh.nhom4.grandmaassist.app.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

public final class MapNavigationHelper {

    private MapNavigationHelper() {}

    public static void openDirections(Context context, String address, String area) {
        if (context == null) return;
        StringBuilder sb = new StringBuilder();
        if (address != null && !address.trim().isEmpty()) {
            sb.append(address.trim());
        }
        if (area != null && !area.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(area.trim());
        }
        if (sb.length() == 0) {
            Toast.makeText(context, "Chưa có thông tin địa chỉ để xem đường đi", Toast.LENGTH_SHORT).show();
            return;
        }

        String destination = sb.toString();
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + Uri.encode(destination));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");

        if (mapIntent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(mapIntent);
        } else {
            Uri webUri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=" + Uri.encode(destination));
            Intent webIntent = new Intent(Intent.ACTION_VIEW, webUri);
            try {
                context.startActivity(webIntent);
            } catch (Exception e) {
                Toast.makeText(context, "Không thể mở ứng dụng bản đồ chỉ đường", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
