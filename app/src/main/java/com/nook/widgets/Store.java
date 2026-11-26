package com.nook.widgets;

import android.content.Context;
import android.content.SharedPreferences;
import org.json.JSONArray;
import org.json.JSONObject;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/** Small, offline data store shared by the app, widgets and alarm receivers. */
public final class Store {
    private Store() {}
    private static SharedPreferences prefs(Context c) {
        return c.getSharedPreferences("nook", Context.MODE_PRIVATE);
    }
    public static final class Item {
        public String id = UUID.randomUUID().toString();
        public String title = "", body = "", day = "";
        public long due, created = System.currentTimeMillis();
        public boolean done, notified;
        public long expiresAt, expiryElapsed;
        public int expiryBoot = -1;
        public boolean archived, expired;
        public int count, target = 1;
        JSONObject json() {
            JSONObject j = new JSONObject();
            try {
                j.put("id", id).put("title", title).put("body", body).put("day", day)
                    .put("due", due).put("created", created).put("done", done)
                    .put("notified", notified).put("count", count).put("target", target)
                    .put("expiresAt",expiresAt).put("expiryElapsed",expiryElapsed).put("expiryBoot",expiryBoot)
                    .put("archived",archived).put("expired",expired);
            } catch (org.json.JSONException e) { throw new IllegalStateException(e); }
            return j;
        }
        static Item from(JSONObject j) {
            Item i = new Item();
            i.id = j.optString("id", i.id); i.title = j.optString("title");
            i.body = j.optString("body"); i.day = j.optString("day");
            i.due = j.optLong("due"); i.created = j.optLong("created", i.created);
            i.done = j.optBoolean("done"); i.notified = j.optBoolean("notified");
            i.count = j.optInt("count"); i.target = Math.max(1, j.optInt("target", 1));
            i.expiresAt=j.optLong("expiresAt");i.expiryElapsed=j.optLong("expiryElapsed");i.expiryBoot=j.optInt("expiryBoot",-1);
            i.archived=j.optBoolean("archived");i.expired=j.optBoolean("expired");
            return i;
        }
        public int todayCount() { return day.equals(LocalDate.now().toString()) ? count : 0; }
    }
    public static synchronized List<Item> list(Context c, String type) {
        List<Item> items = new ArrayList<>();
        try {
            JSONArray a = new JSONArray(prefs(c).getString(type, "[]"));
            for (int n = 0; n < a.length(); n++) items.add(Item.from(a.getJSONObject(n)));
        } catch (org.json.JSONException e) {
            // Leave the original preference intact if it cannot be read.
            android.util.Log.e("Nook", "Cannot read " + type, e);
        }
        if (type.equals("reminders")) items.sort(Comparator.comparing((Item i) -> i.done).thenComparingLong(i -> i.due));
        if (type.equals("notes")) items.sort(Comparator.comparingLong((Item i) -> i.created).reversed());
        return items;
    }
    public static Item find(Context c, String type, String id) {
        for (Item i : list(c, type)) if (i.id.equals(id)) return i;
        return null;
    }
    static void write(Context c, String type, List<Item> items) {
        JSONArray a = new JSONArray();
        for (Item i : items) a.put(i.json());
        // Widget receivers may be killed after onReceive; commit before returning.
        if (!prefs(c).edit().putString(type, a.toString()).commit())
            throw new IllegalStateException("Unable to save data");
    }
    public static synchronized void save(Context c, String type, Item item) {
        List<Item> items = list(c, type);
        items.removeIf(i -> i.id.equals(item.id));
        items.add(item);
        write(c, type, items);
        if(type.equals("notes"))NoteLife.refresh(c);
        Widgets.updateAll(c);
    }
    public static synchronized void delete(Context c, String type, String id) {
        if (type.equals("reminders")) Reminders.cancel(c, id);
        List<Item> items = list(c, type);
        items.removeIf(i -> i.id.equals(id));
        write(c, type, items);
        if(type.equals("notes"))NoteLife.refresh(c);
        Widgets.updateAll(c);
    }
    public static synchronized void toggle(Context c, String type, String id) {
        Item i = find(c, type, id);
        if (i == null) return;
        i.done = !i.done;
        save(c, type, i);
        if (type.equals("reminders")) {
            if (i.done) Reminders.cancel(c, id);
            else { i.notified = false; save(c, type, i); Reminders.schedule(c, i); }
        }
    }
    public static synchronized void increment(Context c, String id, int delta) {
        Item i = find(c, "habits", id);
        if (i == null) return;
        i.count = Math.max(0, Math.min(i.target, i.todayCount() + delta));
        i.day = LocalDate.now().toString();
        save(c, "habits", i);
    }
    public static JSONObject config(Context c, int widgetId) {
        try { return new JSONObject(prefs(c).getString("widget_" + widgetId, "{}")); }
        catch (org.json.JSONException e) { return new JSONObject(); }
    }
    public static void config(Context c, int widgetId, JSONObject config) {
        prefs(c).edit().putString("widget_" + widgetId, config.toString()).commit();
    }
    public static void removeConfig(Context c, int widgetId) {
        prefs(c).edit().remove("widget_" + widgetId).commit();
    }
    public static void pending(Context c, String token, JSONObject config) {
        prefs(c).edit().putString("pin_" + token, config.toString()).commit();
    }
    public static JSONObject takePending(Context c, String token) {
        String value = prefs(c).getString("pin_" + token, "{}");
        prefs(c).edit().remove("pin_" + token).commit();
        try { return new JSONObject(value); } catch (org.json.JSONException e) { return new JSONObject(); }
    }
}
