package com.notes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity {

    private Theme theme;
    private NotesAdapter adapter;
    private TextView countView;
    private TextView emptyView;
    private EditText searchInput;
    private TextView clearBtn;
    private final List<NoteStorage.Note> allNotes = new ArrayList<NoteStorage.Note>();
    private final List<NoteStorage.Note> filteredNotes = new ArrayList<NoteStorage.Note>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = Theme.of(this);
        theme.applySystemBars(this);

        int pad = Theme.dp(this, 18);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(theme.bg);

        // Шапка: Заметки + переключатель темы
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(pad, Theme.dp(this, 20), pad, Theme.dp(this, 10));

        TextView title = new TextView(this);
        title.setText("Заметки");
        title.setTextSize(32f);
        title.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        title.setTextColor(theme.textPrimary);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        header.addView(title);

        TextView themeBtn = new TextView(this);
        themeBtn.setText(theme.dark ? "☀" : "☾");
        themeBtn.setTextSize(18f);
        themeBtn.setTextColor(theme.accent);
        themeBtn.setGravity(Gravity.CENTER);
        themeBtn.setBackground(Theme.rippleRounded(theme.card, theme.ripple, Theme.dp(this, 18)));
        int btnSize = Theme.dp(this, 38);
        themeBtn.setLayoutParams(new LinearLayout.LayoutParams(btnSize, btnSize));
        themeBtn.setOnClickListener(v -> {
            Theme.toggle(this);
            recreate();
        });
        header.addView(themeBtn);
        root.addView(header);

        // Поле поиска в стиле iOS
        LinearLayout searchWrap = new LinearLayout(this);
        searchWrap.setOrientation(LinearLayout.HORIZONTAL);
        searchWrap.setGravity(Gravity.CENTER_VERTICAL);
        searchWrap.setBackground(Theme.rounded(theme.searchBg, Theme.dp(this, 12)));
        searchWrap.setPadding(Theme.dp(this, 14), 0, Theme.dp(this, 10), 0);
        LinearLayout.LayoutParams searchLp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Theme.dp(this, 42));
        searchLp.setMargins(pad, Theme.dp(this, 2), pad, Theme.dp(this, 12));
        searchWrap.setLayoutParams(searchLp);

        searchInput = new EditText(this);
        searchInput.setHint("Поиск");
        searchInput.setHintTextColor(theme.textHint);
        searchInput.setTextColor(theme.textPrimary);
        searchInput.setTextSize(16f);
        searchInput.setSingleLine(true);
        searchInput.setBackground(null);
        searchInput.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));
        searchWrap.addView(searchInput);

        clearBtn = new TextView(this);
        clearBtn.setText("×");
        clearBtn.setTextSize(20f);
        clearBtn.setTextColor(theme.textSecondary);
        clearBtn.setPadding(Theme.dp(this, 8), 0, Theme.dp(this, 4), 0);
        clearBtn.setVisibility(View.GONE);
        clearBtn.setOnClickListener(v -> searchInput.setText(""));
        searchWrap.addView(clearBtn);
        root.addView(searchWrap);

        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                clearBtn.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                applyFilter();
            }
        });

        // Контейнер списка заметок
        FrameLayout contentWrap = new FrameLayout(this);
        contentWrap.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        ListView listView = new ListView(this);
        listView.setDivider(null);
        listView.setDividerHeight(0);
        listView.setSelector(android.R.color.transparent);
        listView.setClipToPadding(false);
        listView.setPadding(pad, Theme.dp(this, 4), pad, Theme.dp(this, 16));

        adapter = new NotesAdapter();
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, pos, id) -> openEditor(filteredNotes.get(pos).id));
        listView.setOnItemLongClickListener((parent, view, pos, id) -> {
            showNoteOptions(filteredNotes.get(pos));
            return true;
        });

        emptyView = new TextView(this);
        emptyView.setText("Нет заметок\nНажмите + внизу, чтобы создать");
        emptyView.setTextSize(15f);
        emptyView.setTextColor(theme.textSecondary);
        emptyView.setGravity(Gravity.CENTER);
        emptyView.setLineSpacing(Theme.dp(this, 4), 1f);
        listView.setEmptyView(emptyView);

        contentWrap.addView(listView);
        contentWrap.addView(emptyView);
        root.addView(contentWrap);

        // Тонкий разделитель перед нижней панелью
        View line = new View(this);
        line.setBackgroundColor(theme.divider);
        line.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        root.addView(line);

        // Нижняя панель Apple Notes: счётчик по центру + кнопка создания справа
        FrameLayout bottomBar = new FrameLayout(this);
        bottomBar.setBackgroundColor(theme.barBg);
        bottomBar.setPadding(pad, 0, pad, 0);
        bottomBar.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, Theme.dp(this, 58)));

        countView = new TextView(this);
        countView.setTextSize(13f);
        countView.setTextColor(theme.textSecondary);
        FrameLayout.LayoutParams countLp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        countView.setLayoutParams(countLp);
        bottomBar.addView(countView);

        TextView addBtn = new TextView(this);
        addBtn.setText("+");
        addBtn.setTextSize(26f);
        addBtn.setTypeface(Typeface.DEFAULT_BOLD);
        addBtn.setTextColor(theme.dark ? Color.BLACK : Color.WHITE);
        addBtn.setGravity(Gravity.CENTER);
        addBtn.setIncludeFontPadding(false);
        addBtn.setBackground(Theme.rippleRounded(theme.accent, theme.ripple, Theme.dp(this, 21)));
        int fab = Theme.dp(this, 42);
        FrameLayout.LayoutParams fabLp = new FrameLayout.LayoutParams(fab, fab, Gravity.END | Gravity.CENTER_VERTICAL);
        addBtn.setLayoutParams(fabLp);
        addBtn.setOnClickListener(v -> openEditor(null));
        bottomBar.addView(addBtn);

        root.addView(bottomBar);
        setContentView(root);
    }

    @Override
    protected void onResume() {
        super.onResume();
        reloadNotes();
    }

    private void reloadNotes() {
        allNotes.clear();
        allNotes.addAll(NoteStorage.getAll(this));
        applyFilter();
    }

    private void applyFilter() {
        String q = searchInput.getText().toString().toLowerCase(new Locale("ru")).trim();
        filteredNotes.clear();
        if (q.isEmpty()) {
            filteredNotes.addAll(allNotes);
            emptyView.setText("Нет заметок\nНажмите + внизу, чтобы создать");
        } else {
            for (NoteStorage.Note n : allNotes) {
                if (n.title.toLowerCase(new Locale("ru")).contains(q)
                        || n.body.toLowerCase(new Locale("ru")).contains(q)) {
                    filteredNotes.add(n);
                }
            }
            emptyView.setText("Ничего не найдено");
        }
        countView.setText(NoteStorage.formatCount(filteredNotes.size()));
        adapter.notifyDataSetChanged();
    }

    private void openEditor(String noteId) {
        Intent i = new Intent(this, EditorActivity.class);
        if (noteId != null) i.putExtra("note_id", noteId);
        startActivity(i);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void showNoteOptions(NoteStorage.Note note) {
        String pinLabel = note.pinned ? "Открепить" : "Закрепить";
        String[] items = new String[]{pinLabel, "Поделиться", "Удалить"};
        new AlertDialog.Builder(this, theme.dialogTheme())
                .setTitle(note.displayTitle())
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        NoteStorage.togglePin(this, note.id);
                        reloadNotes();
                    } else if (which == 1) {
                        shareNote(note);
                    } else if (which == 2) {
                        NoteStorage.delete(this, note.id);
                        reloadNotes();
                    }
                })
                .show();
    }

    private void shareNote(NoteStorage.Note note) {
        String text = (note.title.trim() + "\n" + note.body.trim()).trim();
        if (text.isEmpty()) return;
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_TEXT, text);
        startActivity(Intent.createChooser(i, "Поделиться заметкой"));
    }

    private static class ViewHolder {
        TextView pin;
        TextView title;
        TextView date;
        TextView preview;
    }

    private class NotesAdapter extends BaseAdapter {
        @Override public int getCount() { return filteredNotes.size(); }
        @Override public Object getItem(int pos) { return filteredNotes.get(pos); }
        @Override public long getItemId(int pos) { return pos; }

        @Override
        public View getView(int pos, View convertView, ViewGroup parent) {
            ViewHolder h;
            if (convertView == null) {
                FrameLayout outer = new FrameLayout(MainActivity.this);
                outer.setPadding(0, Theme.dp(MainActivity.this, 5), 0, Theme.dp(MainActivity.this, 5));

                LinearLayout card = new LinearLayout(MainActivity.this);
                card.setOrientation(LinearLayout.VERTICAL);
                card.setBackground(Theme.rippleRounded(theme.card, theme.ripple, Theme.dp(MainActivity.this, 14)));
                int px = Theme.dp(MainActivity.this, 16);
                int py = Theme.dp(MainActivity.this, 13);
                card.setPadding(px, py, px, py);

                LinearLayout topRow = new LinearLayout(MainActivity.this);
                topRow.setOrientation(LinearLayout.HORIZONTAL);
                topRow.setGravity(Gravity.CENTER_VERTICAL);

                TextView pin = new TextView(MainActivity.this);
                pin.setText("★ ");
                pin.setTextSize(14f);
                pin.setTextColor(theme.accent);
                topRow.addView(pin);

                TextView title = new TextView(MainActivity.this);
                title.setTextSize(17f);
                title.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                title.setTextColor(theme.textPrimary);
                title.setSingleLine(true);
                title.setEllipsize(TextUtils.TruncateAt.END);
                topRow.addView(title);
                card.addView(topRow);

                LinearLayout subRow = new LinearLayout(MainActivity.this);
                subRow.setOrientation(LinearLayout.HORIZONTAL);
                subRow.setPadding(0, Theme.dp(MainActivity.this, 4), 0, 0);

                TextView date = new TextView(MainActivity.this);
                date.setTextSize(14f);
                date.setTextColor(theme.textSecondary);
                date.setPadding(0, 0, Theme.dp(MainActivity.this, 10), 0);
                subRow.addView(date);

                TextView preview = new TextView(MainActivity.this);
                preview.setTextSize(14f);
                preview.setTextColor(theme.textSecondary);
                preview.setSingleLine(true);
                preview.setEllipsize(TextUtils.TruncateAt.END);
                subRow.addView(preview);
                card.addView(subRow);

                outer.addView(card);
                h = new ViewHolder();
                h.pin = pin;
                h.title = title;
                h.date = date;
                h.preview = preview;
                outer.setTag(h);
                convertView = outer;
            } else {
                h = (ViewHolder) convertView.getTag();
            }

            NoteStorage.Note n = filteredNotes.get(pos);
            h.pin.setVisibility(n.pinned ? View.VISIBLE : View.GONE);
            h.title.setText(n.displayTitle());
            h.date.setText(NoteStorage.formatDate(n.updatedAt));
            h.preview.setText(n.displayPreview());
            return convertView;
        }
    }
          }
      
