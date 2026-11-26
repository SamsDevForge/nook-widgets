package com.nook.widgets;

import android.app.*;
import android.content.*;
import android.net.Uri;

public class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context c, Intent intent) {
        String id = intent.getStringExtra("id");
        Store.Item item = Store.find(c, "reminders", id);
        if (item == null || item.done || item.notified) return;
        Reminders.channel(c);
        if (!Reminders.notificationsAllowed(c)) return;
        Intent open = new Intent(c, MainActivity.class).putExtra("section", "reminders").putExtra("edit", id)
            .setData(Uri.parse("nook://open/reminders/" + id));
        PendingIntent content = PendingIntent.getActivity(c, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent done = new Intent(c, WidgetActionReceiver.class).setAction("toggle").putExtra("type", "reminders").putExtra("id", id)
            .setData(Uri.parse("nook://done/" + id));
        PendingIntent complete = PendingIntent.getBroadcast(c, 0, done, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(c, Reminders.CHANNEL)
            .setSmallIcon(R.drawable.ic_notification).setContentTitle(item.title)
            .setContentText(item.body.trim().isEmpty() ? "A little nudge from Nook" : item.body)
            .setStyle(new Notification.BigTextStyle().bigText(item.body.trim().isEmpty() ? "A little nudge from Nook" : item.body))
            .setContentIntent(content).setAutoCancel(true).setCategory(Notification.CATEGORY_REMINDER)
            .setColor(0xffc4d7b2).addAction(new Notification.Action.Builder(null, "Mark done", complete).build()).build();
        try {
            c.getSystemService(NotificationManager.class).notify(id, 0, notification);
            item.notified = true;
            Store.save(c, "reminders", item);
        } catch (SecurityException e) { android.util.Log.w("Nook", "Notification permission changed", e); }
    }
}
