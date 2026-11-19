package com.notes.app;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NoteStorage {

    public static class Note {
        public String id;
        public String title;
        public String body;
        public long updatedAt;
        public boolean pinned;

        public Note(String id, String title, String body, long updatedAt, boolean pinned) {
            this.id = id;
            this.title = title != null ? title : "";
            this.body = body != null ? body : "";
            this.updatedAt = updatedAt;
            this.pinned = pinned;
        }

        public String displayTitle() {
            String t = title.trim();
            if (!t.isEmpty()) return t;
            String b = body.trim();
            if (!b.isEmpty()) {
                int nl = b.indexOf('\n');
                return nl > 0 ? b.substring(0, nl).trim() : b;
            }
            return "Новая заметка";
        }

        public String displayPreview() {
            String t = title.trim();
            String b = body.trim();
            if (!t.isEmpty()) {
                return b.isEmpty() ? "Нет дополнительного текста" : b.replace('\n', ' ');
            }
            if (!b.isEmpty()) {
                int nl = b.indexOf('\n');
                if (nl > 0 && nl + 1 < b.length()) {
                    return b.substring(nl + 1).trim().replace('\n', ' ');
                }
            }
            return "Нет дополнительного текста";
        }
    }

    private static final String FILE_NAME = "notes_v1.json";
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();
    private static List<Note> cache = null;

    public static synchronized List<Note> getAll(Context ctx) {
        if (cache == null) {
            cache = loadFromDisk(ctx.getApplicationContext());
        }
        sort(cache);
        return new ArrayList<Note>(cache);
    }

    public static synchronized Note findById(Context ctx, String id) {
        if (id == null) return null;
        for (Note n : getAll(ctx)) {
            if (id.equals(n.id)) return n;
        }
        return null;
    }

    public static synchronized Note createDraft() {
        return new Note(UUID.randomUUID().toString(), "", "", System.currentTimeMillis(), false);
    }

    public static synchronized void upsert(Context ctx, Note note) {
        if (cache == null) cache = loadFromDisk(ctx.getApplicationContext());
        boolean updated = false;
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).id.equals(note.id)) {
                cache.set(i, note);
                updated = true;
                break;
            }
        }
        if (!updated) cache.add(note);
        sort(cache);
        persistAsync(ctx.getApplicationContext());
    }

    public static synchronized void delete(Context ctx, String id) {
        if (cache == null) cache = loadFromDisk(ctx.getApplicationContext());
        for (int i = 0; i < cache.size(); i++) {
            if (cache.get(i).id.equals(id)) {
                cache.remove(i);
                break;
            }
        }
        persistAsync(ctx.getApplicationContext());
    }

    public static synchronized void togglePin(Context ctx, String id) {
        if (cache == null) cache = loadFromDisk(ctx.getApplicationContext());
        for (Note n : cache) {
            if (n.id.equals(id)) {
                n.pinned = !n.pinned;
                break;
            }
        }
        sort(cache);
        persistAsync(ctx.getApplicationContext());
    }

    private static void sort(List<Note> list) {
        Collections.sort(list, (a, b) -> {
            if (a.pinned != b.pinned) return a.pinned ? -1 : 1;
            return Long.compare(b.updatedAt, a.updatedAt);
        });
    }

    private static void persistAsync(Context appCtx) {
        JSONArray arr = new JSONArray();
        try {
            for (Note n : cache) {
                JSONObject o = new JSONObject();
                o.put("id", n.id);
                o.put("title", n.title);
                o.put("body", n.body);
                o.put("updatedAt", n.updatedAt);
                o.put("pinned", n.pinned);
                arr.put(o);
            }
        } catch (Exception ignored) {
            return;
        }
        final byte[] data = arr.toString().getBytes(StandardCharsets.UTF_8);
        IO.execute(() -> {
            File tmp = new File(appCtx.getFilesDir(), FILE_NAME + ".tmp");
            File target = new File(appCtx.getFilesDir(), FILE_NAME);
            try (FileOutputStream fos = new FileOutputStream(tmp)) {
                fos.write(data);
                fos.flush();
            } catch (Exception ignored) {
                return;
            }
            tmp.renameTo(target);
        });
    }

    private static List<Note> loadFromDisk(Context appCtx) {
        List<Note> list = new ArrayList<Note>();
        File file = new File(appCtx.getFilesDir(), FILE_NAME);
        if (!file.exists()) return list;
        try (FileInputStream fis = new FileInputStream(file);
             ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            byte[] buf = new byte[4096];
            int read;
            while ((read = fis.read(buf)) != -1) bos.write(buf, 0, read);
            String json = new String(bos.toByteArray(), StandardCharsets.UTF_8);
            JSONArray arr = new JSONArray(json);
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                list.add(new Note(
                        o.optString("id", UUID.randomUUID().toString()),
                        o.optString("title", ""),
                        o.optString("body", ""),
                        o.optLong("updatedAt", System.currentTimeMillis()),
                        o.optBoolean("pinned", false)
                ));
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public static String formatDate(long ts) {
        Calendar now = Calendar.getInstance();
        Calendar then = Calendar.getInstance();
        then.setTimeInMillis(ts);
        Locale ru = new Locale("ru");
        if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)) {
            return new SimpleDateFormat("HH:mm", ru).format(new Date(ts));
        }
        now.add(Calendar.DAY_OF_YEAR, -1);
        if (now.get(Calendar.YEAR) == then.get(Calendar.YEAR)
                && now.get(Calendar.DAY_OF_YEAR) == then.get(Calendar.DAY_OF_YEAR)) {
            return "Вчера";
        }
        return new SimpleDateFormat("d MMM", ru).format(new Date(ts));
    }

    public static String formatCount(int count) {
        int n = Math.abs(count) % 100;
        int n1 = n % 10;
        if (n > 10 && n < 20) return count + " заметок";
        if (n1 == 1) return count + " заметка";
        if (n1 >= 2 && n1 <= 4) return count + " заметки";
        return count + " заметок";
    }
}
