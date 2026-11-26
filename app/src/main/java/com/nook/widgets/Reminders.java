package com.nook.widgets;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;

public final class Reminders {
    private Reminders() {}
    public static final String CHANNEL = "nook_reminders";
    public static void channel(Context c) {
        NotificationChannel channel = new NotificationChannel(CHANNEL, "Your reminders", NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Notes and tasks you asked Nook to remind you about");
        channel.enableVibration(true);
        c.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }
    public static boolean notificationsAllowed(Context c) {
        if (Build.VERSION.SDK_INT >= 33 && c.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return false;
        NotificationManager manager = c.getSystemService(NotificationManager.class);
        NotificationChannel channel = manager.getNotificationChannel(CHANNEL);
        return manager.areNotificationsEnabled() && (channel == null || channel.getImportance() != NotificationManager.IMPORTANCE_NONE);
    }
    public static boolean precise(Context c) {
        return Build.VERSION.SDK_INT < 31 || c.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }
    private static PendingIntent pending(Context c, String id) {
        Intent intent = new Intent(c, ReminderReceiver.class).setData(Uri.parse("nook://reminder/" + id)).putExtra("id", id);
        return PendingIntent.getBroadcast(c, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    public static void schedule(Context c, Store.Item i) {
        if (i.done || i.notified || i.due <= 0) return;
        AlarmManager manager = c.getSystemService(AlarmManager.class);
        long time = Math.max(System.currentTimeMillis() + 1000, i.due);
        try {
            if (precise(c)) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending(c, i.id));
            else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending(c, i.id));
        } catch (SecurityException e) {
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, time, pending(c, i.id));
        }
    }
    public static void cancel(Context c, String id) {
        c.getSystemService(AlarmManager.class).cancel(pending(c, id));
        c.getSystemService(NotificationManager.class).cancel(id, 0);
    }
    public static void restore(Context c) {
        for (Store.Item i : Store.list(c, "reminders")) schedule(c, i);
    }
    public static void permissions(Activity a) {
        channel(a);
        if (Build.VERSION.SDK_INT >= 33 && a.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            a.requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        } else {
            a.startActivity(new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, a.getPackageName()));
        }
    }
    public static void exactPermission(Activity a) {
        if (Build.VERSION.SDK_INT >= 31) {
            try { a.startActivity(new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + a.getPackageName()))); }
            catch (ActivityNotFoundException e) { android.widget.Toast.makeText(a, "Open Settings → Apps → Nook → Alarms & reminders", android.widget.Toast.LENGTH_LONG).show(); }
        }
    }
}
