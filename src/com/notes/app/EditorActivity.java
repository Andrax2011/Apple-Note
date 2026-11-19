package com.notes.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EditorActivity extends Activity {

    private Theme theme;
    private NoteStorage.Note note;
    private boolean existedInStorage = false;
    private boolean deleted = false;

    private EditText titleInput;
    private EditText bodyInput;
    private TextView metaView;
    private TextView pinBtn;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable autoSaveRunnable = this::saveNow;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        theme = Theme.of(this);
        theme.applySystemBars(this);

        String noteId = getIntent().getStringExtra("note_id");
        NoteStorage.Note existing = NoteStorage.findById(this, noteId);
        if (existing != null) {
            note = new NoteStorage.Note(existing.id, existing.title, existing.body, existing.updatedAt, existing.pinned);
            existedInStorage = true;
        } else {
            note = NoteStorage.createDraft();
        }

        int pad = Theme.dp(this, 18);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(theme.bg);

        // Верхняя панель: ‹ Заметки  |  ★  •••  Готово
        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setGravity(Gravity.CENTER_VERTICAL);
        topBar.setPadding(Theme.dp(this, 10), Theme.dp(this, 10), Theme.dp(this, 12), Theme.dp(this, 8));

        TextView backBtn = new TextView(this);
        backBtn.setText("‹ Заметки");
        backBtn.setTextSize(17f);
        backBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        backBtn.setTextColor(theme.accent);
        backBtn.setPadding(Theme.dp(this, 8), Theme.dp(this, 8), Theme.dp(this, 12), Theme.dp(this, 8));
        backBtn.setOnClickListener(v -> finishClean());
        topBar.addView(backBtn);

        View spacer = new View(this);
        spacer.setLayoutParams(new LinearLayout.LayoutParams(0, 1, 1f));
        topBar.addView(spacer);

        pinBtn = new TextView(this);
        pinBtn.setTextSize(20f);
        pinBtn.setPadding(Theme.dp(this, 10), Theme.dp(this, 6), Theme.dp(this, 10), Theme.dp(this, 6));
        updatePinUi();
        pinBtn.setOnClickListener(v -> {
            note.pinned = !note.pinned;
            updatePinUi();
            saveNow();
        });
        topBar.addView(pinBtn);

        TextView moreBtn = new TextView(this);
        moreBtn.setText("•••");
        moreBtn.setTextSize(16f);
        moreBtn.setTextColor(theme.accent);
        moreBtn.setPadding(Theme.dp(this, 10), Theme.dp(this, 8), Theme.dp(this, 10), Theme.dp(this, 8));
        moreBtn.setOnClickListener(v -> showMoreMenu());
        topBar.addView(moreBtn);

        TextView doneBtn = new TextView(this);
        doneBtn.setText("Готово");
        doneBtn.setTextSize(16f);
        doneBtn.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
        doneBtn.setTextColor(theme.accent);
        doneBtn.setPadding(Theme.dp(this, 10), Theme.dp(this, 8), Theme.dp(this, 8), Theme.dp(this, 8));
        doneBtn.setOnClickListener(v -> {
            saveNow();
            hideKeyboard();
            titleInput.clearFocus();
            bodyInput.clearFocus();
        });
        topBar.addView(doneBtn);
        root.addView(topBar);

        // Мета-строка (дата и счётчик символов)
        metaView = new TextView(this);
        metaView.setTextSize(12f);
        metaView.setTextColor(theme.textSecondary);
        metaView.setGravity(Gravity.CENTER);
        metaView.setPadding(pad, 0, pad, Theme.dp(this, 8));
        root.addView(metaView);

        // Прокручиваемая область редактора
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout editorCol = new LinearLayout(this);
        editorCol.setOrientation(LinearLayout.VERTICAL);
        editorCol.setPadding(pad, Theme.dp(this, 4), pad, Theme.dp(this, 48));

        titleInput = new EditText(this);
        titleInput.setHint("Заголовок");
        titleInput.setHintTextColor(theme.textHint);
        titleInput.setTextColor(theme.textPrimary);
        titleInput.setTextSize(24f);
        titleInput.setTypeface(Typeface.create("sans-serif", Typeface.BOLD));
        titleInput.setBackground(null);
        titleInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        titleInput.setPadding(0, Theme.dp(this, 4), 0, Theme.dp(this, 10));
        titleInput.setText(note.title);
        editorCol.addView(titleInput);

        bodyInput = new EditText(this);
        bodyInput.setHint("Начните писать...");
        bodyInput.setHintTextColor(theme.textHint);
        bodyInput.setTextColor(theme.textPrimary);
        bodyInput.setTextSize(17f);
        bodyInput.setLineSpacing(0f, 1.38f);
        bodyInput.setGravity(Gravity.TOP | Gravity.START);
        bodyInput.setBackground(null);
        bodyInput.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
                | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        bodyInput.setMinHeight(Theme.dp(this, 320));
        bodyInput.setText(note.body);
        bodyInput.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        editorCol.addView(bodyInput);

        scroll.addView(editorCol);
        root.addView(scroll);
        setContentView(root);

        updateMeta();

        TextWatcher watcher = new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {
                updateMeta();
                handler.removeCallbacks(autoSaveRunnable);
                handler.postDelayed(autoSaveRunnable, 350);
            }
        };
        titleInput.addTextChangedListener(watcher);
        bodyInput.addTextChangedListener(watcher);

        if (!existedInStorage) {
            titleInput.requestFocus();
            titleInput.postDelayed(() -> {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.showSoftInput(titleInput, InputMethodManager.SHOW_IMPLICIT);
            }, 120);
        }
    }

    private void updatePinUi() {
        pinBtn.setText(note.pinned ? "★" : "☆");
        pinBtn.setTextColor(note.pinned ? theme.accent : theme.textSecondary);
    }

    private void updateMeta() {
        int chars = titleInput.getText().length() + bodyInput.getText().length();
        String dt = new SimpleDateFormat("d MMMM, HH:mm", new Locale("ru")).format(new Date(note.updatedAt));
        metaView.setText(dt + "  •  " + chars + " симв.");
    }

    private void saveNow() {
        if (deleted) return;
        String t = titleInput.getText().toString();
        String b = bodyInput.getText().toString();
        if (t.trim().isEmpty() && b.trim().isEmpty()) {
            if (existedInStorage) {
                NoteStorage.delete(this, note.id);
                existedInStorage = false;
            }
            return;
        }
        boolean changed = !t.equals(note.title) || !b.equals(note.body) || !existedInStorage;
        note.title = t;
        note.body = b;
        if (changed) note.updatedAt = System.currentTimeMillis();
        NoteStorage.upsert(this, note);
        existedInStorage = true;
    }

    private void showMoreMenu() {
        String[] items = new String[]{"Поделиться", "Удалить заметку"};
        new AlertDialog.Builder(this, theme.dialogTheme())
                .setItems(items, (d, which) -> {
                    if (which == 0) {
                        String text = (titleInput.getText().toString().trim() + "\n"
                                + bodyInput.getText().toString().trim()).trim();
                        if (!text.isEmpty()) {
                            Intent i = new Intent(Intent.ACTION_SEND);
                            i.setType("text/plain");
                            i.putExtra(Intent.EXTRA_TEXT, text);
                            startActivity(Intent.createChooser(i, "Поделиться"));
                        }
                    } else if (which == 1) {
                        deleted = true;
                        handler.removeCallbacks(autoSaveRunnable);
                        if (existedInStorage) NoteStorage.delete(this, note.id);
                        finishClean();
                    }
                })
                .show();
    }

    private void hideKeyboard() {
        View v = getCurrentFocus();
        if (v != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);
        }
    }

    private void finishClean() {
        hideKeyboard();
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(autoSaveRunnable);
        saveNow();
        super.onPause();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
          }
