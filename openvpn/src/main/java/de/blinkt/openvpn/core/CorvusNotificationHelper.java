package de.blinkt.openvpn.core;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.widget.RemoteViews;

import androidx.core.app.NotificationCompat;

import de.blinkt.openvpn.R;
import de.blinkt.openvpn.VpnProfile;

/**
 * CorvusNotificationHelper — Single, Unified, Ultra-Professional Notification
 * for Corvus VPN across all connection engines (OpenVPN, SingBox, IKEv2).
 *
 * Displays ONLY:
 *  - Official Corvus Logo
 *  - Country Name & Flag Emoji
 *  - Protected / Connecting Status
 *  - Sleek Turn Off (Disconnect) Button
 *  - Tap opens MainActivity directly
 */
public class CorvusNotificationHelper {
    public static final String CHANNEL_ID = "corvus_vpn_status";
    public static final int NOTIFICATION_ID = 2026;
    public static final String ACTION_DISCONNECT = "com.corvus.vpn.ACTION_DISCONNECT";

    private static volatile String sCountryCode = "";
    private static volatile String sCountryName = "";
    private static volatile String sFlagEmoji = "";
    private static volatile String sProtocol = "VPN";

    public static void updateServerInfo(String countryCode, String countryName, String flag, String protocol) {
        if (countryCode != null && !countryCode.trim().isEmpty()) {
            sCountryCode = countryCode.trim();
        }
        if (countryName != null && !countryName.trim().isEmpty()) {
            sCountryName = countryName.trim();
        }
        if (flag != null && !flag.trim().isEmpty()) {
            sFlagEmoji = flag.trim();
        }
        if (protocol != null && !protocol.trim().isEmpty()) {
            sProtocol = protocol.trim();
        }
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                String name = context.getString(R.string.notification_channel_name);
                String desc = context.getString(R.string.notification_channel_desc);
                NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    name,
                    NotificationManager.IMPORTANCE_LOW
                );
                channel.setDescription(desc);
                channel.setShowBadge(false);
                channel.enableLights(false);
                channel.enableVibration(false);
                nm.createNotificationChannel(channel);
            }
        }
    }

    public static Notification buildNotification(Context context, String fallbackStatus, boolean isConnected) {
        createNotificationChannel(context);

        String displayName = sCountryName;
        String displayFlag = sFlagEmoji;

        if (displayName == null || displayName.isEmpty() || displayName.equalsIgnoreCase("Secure Server")) {
            VpnProfile profile = ProfileManager.getLastConnectedVpn();
            if (profile != null && profile.mName != null && !profile.mName.isEmpty()) {
                displayName = profile.mName;
            } else {
                displayName = "Secure Server";
            }
        }

        if (displayFlag == null || displayFlag.isEmpty()) {
            displayFlag = "🌐";
        }

        String title = displayFlag + "  " + displayName;
        String statusText = isConnected 
            ? context.getString(R.string.notification_protected) 
            : context.getString(R.string.notification_connecting);

        // 1. PendingIntent to open MainActivity
        Intent mainIntent = new Intent();
        mainIntent.setComponent(new ComponentName(context.getPackageName(), "com.corvus.vpn.MainActivity"));
        mainIntent.setAction(Intent.ACTION_MAIN);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent contentPendingIntent = PendingIntent.getActivity(
            context,
            1000,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 2. PendingIntent for Turn Off / Disconnect action (broadcast receiver)
        Intent disconnectIntent = new Intent(ACTION_DISCONNECT);
        disconnectIntent.setPackage(context.getPackageName());
        PendingIntent disconnectPendingIntent = PendingIntent.getBroadcast(
            context,
            1001,
            disconnectIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // 3. Official Logo Bitmap
        Bitmap logoBm = null;
        try {
            logoBm = BitmapFactory.decodeResource(context.getResources(), R.drawable.corvus_logo);
        } catch (Exception ignored) {}

        // 4. RemoteViews (Custom Dark-Cyber UI)
        RemoteViews compactView = new RemoteViews(context.getPackageName(), R.layout.notification_corvus_compact);
        compactView.setTextViewText(R.id.notification_title, title);
        compactView.setTextViewText(R.id.notification_subtitle, statusText);
        compactView.setTextColor(R.id.notification_subtitle, isConnected ? 0xFF00E676 : 0xFFFFA000);
        compactView.setOnClickPendingIntent(R.id.notification_btn_disconnect, disconnectPendingIntent);
        compactView.setOnClickPendingIntent(R.id.notification_container, contentPendingIntent);

        RemoteViews expandedView = new RemoteViews(context.getPackageName(), R.layout.notification_corvus_expanded);
        expandedView.setTextViewText(R.id.notification_title, title);
        expandedView.setTextViewText(R.id.notification_subtitle, statusText);
        expandedView.setTextColor(R.id.notification_subtitle, isConnected ? 0xFF00E676 : 0xFFFFA000);
        expandedView.setTextViewText(R.id.notification_proto_badge, sProtocol.toUpperCase());
        expandedView.setOnClickPendingIntent(R.id.notification_btn_disconnect, disconnectPendingIntent);
        expandedView.setOnClickPendingIntent(R.id.notification_container, contentPendingIntent);

        if (logoBm != null) {
            compactView.setImageViewBitmap(R.id.notification_logo, logoBm);
            expandedView.setImageViewBitmap(R.id.notification_logo, logoBm);
        }

        // 5. NotificationCompat.Builder
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_vpn)
            .setContentTitle(title)
            .setContentText(statusText)
            .setColor(0xFF7C6CF0)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(contentPendingIntent)
            .addAction(R.drawable.ic_menu_close_clear_cancel, context.getString(R.string.notification_turn_off), disconnectPendingIntent)
            .setCustomContentView(compactView)
            .setCustomBigContentView(expandedView)
            .setStyle(new NotificationCompat.DecoratedCustomViewStyle());

        if (logoBm != null) {
            builder.setLargeIcon(logoBm);
        }

        return builder.build();
    }

    public static void cancelNotification(Context context) {
        try {
            NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.cancel(NOTIFICATION_ID);
            }
        } catch (Exception ignored) {}
    }
}
