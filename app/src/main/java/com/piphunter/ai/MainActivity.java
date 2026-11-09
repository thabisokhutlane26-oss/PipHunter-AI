package com.piphunter.ai;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.animation.LinearInterpolator;
import android.animation.ValueAnimator;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class MainActivity extends Activity {

    private static final int REQUEST_CHART_IMAGE = 101;
    private static final int PREVIEW_MAX_SIZE = 900;

    private final int bgColor = Color.rgb(6, 12, 29);
    private final int cardColor = Color.rgb(18, 27, 52);
    private final int green = Color.rgb(48, 235, 160);
    private final int cyan = Color.rgb(55, 205, 255);
    private final int purple = Color.rgb(160, 115, 255);
    private final int gold = Color.rgb(255, 194, 92);
    private final int white = Color.WHITE;
    private final int muted = Color.rgb(166, 181, 207);

    private ImageView chartPreview;
    private ScanBeamView scanBeamView;
    private TextView imageStatus;
    private TextView scanStatus;

    private TextView decisionText;
    private TextView explanationText;
    private TextView entryText;
    private TextView stopLossText;
    private TextView tp1Text;
    private TextView tp2Text;
    private TextView confidenceText;

    private Button scanButton;

    private Uri selectedImageUri;

    private ExecutorService imageExecutor;

    private final AtomicInteger previewRequestId =
            new AtomicInteger(0);

    private volatile boolean activityDestroyed = false;

    private int dp(float value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private GradientDrawable roundedBackground(
            int color, int strokeColor) {

        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(18));

        if (strokeColor != Color.TRANSPARENT) {
            drawable.setStroke(dp(1), strokeColor);
        }

        return drawable;
    }

    private TextView makeText(
            String value, float size, int color, boolean bold) {

        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);

        if (bold) {
            view.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        }

        return view;
    }

    private void addLabel(
            LinearLayout parent,
            String value,
            float size,
            int color,
            boolean bold) {

        parent.addView(makeText(value, size, color, bold));
    }

    private LinearLayout makeCard() {

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16), dp(16), dp(16), dp(16));

        card.setBackground(roundedBackground(
                cardColor, Color.rgb(66, 83, 135)));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(-1, -2);

        params.bottomMargin = dp(14);
        card.setLayoutParams(params);

        return card;
    }

    private Button makeButton(
            String label, boolean primary) {

        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(14);
        button.setAllCaps(false);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);

        button.setTextColor(
                primary ? Color.rgb(5, 25, 19) : white);

        button.setBackground(roundedBackground(
                primary ? green : Color.rgb(29, 49, 65),
                primary ? green : Color.rgb(63, 91, 108)));

        button.setMinHeight(dp(52));
        button.setPadding(dp(12), dp(8), dp(12), dp(8));

        return button;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        imageExecutor = Executors.newSingleThreadExecutor();

        getWindow().setStatusBarColor(bgColor);
        getWindow().setNavigationBarColor(Color.rgb(9, 16, 36));

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bgColor);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(28));

        root.setBackground(new GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                new int[]{
                        Color.rgb(9, 17, 42),
                        Color.rgb(7, 25, 39),
                        Color.rgb(20, 12, 42)
                }));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);

        heading.setLayoutParams(
                new LinearLayout.LayoutParams(0, -2, 1));

        addLabel(heading, "PipHunter AI", 27, cyan, true);

        TextView subtitle = makeText(
                "SMART FOREX SCREENSHOT SCANNER",
                10, purple, true);

        subtitle.setPadding(0, dp(4), 0, 0);
        heading.addView(subtitle);
        header.addView(heading);

        TextView ready = makeText(
                "● READY", 11, green, true);

        ready.setPadding(dp(10), dp(8), dp(10), dp(8));

        ready.setBackground(roundedBackground(
                Color.rgb(15, 65, 57),
                Color.rgb(48, 190, 145)));

        header.addView(ready);
        root.addView(header);

        TextView intro = makeText(
                "Upload a chart. Let AI study it. Get a clear trading decision.",
                14, muted, false);

        intro.setPadding(0, dp(15), 0, dp(20));
        intro.setTextColor(Color.rgb(193, 205, 225));
        root.addView(intro);

        // UPLOAD CARD

        LinearLayout uploadCard = makeCard();

        addLabel(uploadCard,
                "01  UPLOAD YOUR CHART",
                14, cyan, true);

        TextView uploadHelp = makeText(
                "Choose a screenshot from MT4, MT5, TradingView or your gallery.",
                12, muted, false);

        uploadHelp.setPadding(0, dp(7), 0, dp(14));
        uploadCard.addView(uploadHelp);

        Button uploadButton = makeButton(
                "＋  CHOOSE CHART SCREENSHOT", true);

        uploadCard.addView(uploadButton);
        uploadButton.setOnClickListener(v -> openImagePicker());

        root.addView(uploadCard);

        // PREVIEW CARD

        LinearLayout previewCard = makeCard();

        addLabel(previewCard,
                "02  CHART PREVIEW",
                14, purple, true);

        chartPreview = new ImageView(this);
        chartPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        chartPreview.setAdjustViewBounds(true);

        chartPreview.setBackground(roundedBackground(
                Color.rgb(8, 20, 31),
                Color.rgb(43, 68, 84)));

        chartPreview.setImageResource(
                android.R.drawable.ic_menu_gallery);

        FrameLayout previewFrame = new FrameLayout(this);

        previewFrame.setBackground(roundedBackground(
                Color.rgb(5, 15, 31),
                Color.rgb(55, 205, 255)));

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(-1, dp(190));

        imageParams.topMargin = dp(12);

        previewFrame.addView(chartPreview,
                new FrameLayout.LayoutParams(-1, -1));

        scanBeamView = new ScanBeamView();
        scanBeamView.setVisibility(View.GONE);

        previewFrame.addView(scanBeamView,
                new FrameLayout.LayoutParams(-1, -1));

        previewCard.addView(previewFrame, imageParams);

        imageStatus = makeText(
                "No screenshot selected yet",
                12, muted, false);

        imageStatus.setGravity(Gravity.CENTER);
        imageStatus.setPadding(0, dp(10), 0, 0);

        previewCard.addView(imageStatus);
        root.addView(previewCard);

        // SCAN CARD

        LinearLayout scanCard = makeCard();

        addLabel(scanCard,
                "03  AI CHART SCAN",
                14, green, true);

        TextView scanHelp = makeText(
                "Gemini AI will analyse your uploaded chart screenshot.",
                12, muted, false);

        scanHelp.setPadding(0, dp(7), 0, dp(14));
        scanCard.addView(scanHelp);

        scanButton = makeButton("⌕  SCAN WITH GEMINI AI", true);
        scanButton.setEnabled(false);
        scanButton.setAlpha(0.55f);

        scanCard.addView(scanButton);

        scanStatus = makeText(
                "Waiting for a chart screenshot.",
                12, muted, false);

        scanStatus.setPadding(0, dp(12), 0, 0);
        scanCard.addView(scanStatus);

        scanButton.setOnClickListener(v -> startChartScan());

        root.addView(scanCard);

        // RESULT CARD

        LinearLayout resultCard = makeCard();

        addLabel(resultCard,
                "04  SCAN RESULT",
                14, gold, true);

        decisionText = makeText(
                "WAIT", 24, gold, true);

        decisionText.setGravity(Gravity.CENTER);
        decisionText.setPadding(0, dp(20), 0, dp(8));

        resultCard.addView(decisionText);

        explanationText = makeText(
                "No chart has been analyzed yet.",
                12, muted, false);

        explanationText.setGravity(Gravity.CENTER);
        resultCard.addView(explanationText);

        View line = new View(this);
        line.setBackgroundColor(Color.rgb(48, 70, 86));

        LinearLayout.LayoutParams lineParams =
                new LinearLayout.LayoutParams(-1, dp(1));

        lineParams.topMargin = dp(16);
        lineParams.bottomMargin = dp(14);

        resultCard.addView(line, lineParams);

        entryText = addResultRow(resultCard, "Entry", "—");
        stopLossText = addResultRow(resultCard, "Stop Loss", "—");
        tp1Text = addResultRow(resultCard, "Take Profit 1", "—");
        tp2Text = addResultRow(resultCard, "Take Profit 2", "—");
        confidenceText = addResultRow(resultCard, "Confidence", "—");

        root.addView(resultCard);

        TextView footerText = makeText(
                "SCAN HISTORY     •     SETTINGS",
                11, muted, true);

        footerText.setGravity(Gravity.CENTER);
        footerText.setPadding(0, dp(8), 0, 0);
        root.addView(footerText);

        TextView disclaimer = makeText(
                "For educational analysis only. Always manage trading risk.",
                10, muted, false);

        disclaimer.setGravity(Gravity.CENTER);
        disclaimer.setPadding(0, dp(14), 0, 0);

        root.addView(disclaimer);

        scroll.addView(root);
        setContentView(scroll);
    }

    private TextView addResultRow(
            LinearLayout parent,
            String label,
            String value) {

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(0, dp(5), 0, dp(5));

        TextView left = makeText(label, 13, muted, false);

        left.setLayoutParams(
                new LinearLayout.LayoutParams(0, -2, 1));

        TextView right = makeText(value, 13, white, true);

        row.addView(left);
        row.addView(right);
        parent.addView(row);

        return right;
    }

    // IMAGE PICKER

    private void openImagePicker() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        startActivityForResult(intent, REQUEST_CHART_IMAGE);
    }

    // PREVIEW

    private void loadPreview(Uri uri) {

        final int requestId = previewRequestId.incrementAndGet();

        imageStatus.setText("Loading chart preview...");

        chartPreview.setImageResource(
                android.R.drawable.ic_menu_gallery);

        imageExecutor.execute(() -> {

            Bitmap preview = null;
            String error = null;

            try {
                preview = decodeScaledBitmap(uri, PREVIEW_MAX_SIZE);

                if (preview == null) {
                    error = "Preview could not be created.";
                }

            } catch (Exception e) {
                error = "Preview loading failed.";
            }

            final Bitmap finalPreview = preview;
            final String finalError = error;

            runOnUiThread(() -> {

                if (activityDestroyed
                        || requestId != previewRequestId.get()) {

                    if (finalPreview != null
                            && !finalPreview.isRecycled()) {
                        finalPreview.recycle();
                    }
                    return;
                }

                if (finalPreview != null) {
                    chartPreview.setImageBitmap(finalPreview);
                    imageStatus.setText(
                            "Screenshot preview loaded successfully.");
                } else {
                    imageStatus.setText(finalError);
                }
            });
        });
    }

    private Bitmap decodeScaledBitmap(
            Uri uri, int maximumSize) throws Exception {

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;

        InputStream firstStream =
                getContentResolver().openInputStream(uri);

        if (firstStream == null) {
            throw new Exception("Image unavailable");
        }

        try {
            BitmapFactory.decodeStream(firstStream, null, bounds);
        } finally {
            firstStream.close();
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new Exception("Invalid image dimensions");
        }

        int sampleSize = 1;

        while ((bounds.outWidth / (sampleSize * 2)) >= maximumSize
                && (bounds.outHeight / (sampleSize * 2)) >= maximumSize) {
            sampleSize *= 2;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = sampleSize;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        InputStream secondStream =
                getContentResolver().openInputStream(uri);

        if (secondStream == null) {
            throw new Exception("Unable to reopen image");
        }

        try {
            return BitmapFactory.decodeStream(
                    secondStream, null, options);
        } finally {
            secondStream.close();
        }
    }

    // GEMINI API KEY

    private void startChartScan() {

        if (selectedImageUri == null) {
            Toast.makeText(this,
                    "Choose a chart screenshot first.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String savedKey = getSharedPreferences(
                "piphunter_secure_test",
                MODE_PRIVATE
        ).getString("gemini_api_key", "");

        if (savedKey == null || savedKey.trim().isEmpty()) {
            askForGeminiKey();
        } else {
            runGeminiScan(savedKey);
        }
    }

    private void askForGeminiKey() {

        EditText keyInput = new EditText(this);

        keyInput.setSingleLine(true);
        keyInput.setHint("Paste your Gemini API key");
        keyInput.setInputType(
                InputType.TYPE_CLASS_TEXT
                        | InputType.TYPE_TEXT_VARIATION_PASSWORD);

        LinearLayout container = new LinearLayout(this);
        container.setPadding(dp(20), dp(8), dp(20), dp(4));
        container.addView(keyInput);

        new AlertDialog.Builder(this)
                .setTitle("Connect Gemini AI")
                .setMessage(
                        "Enter your Gemini API key on this phone. "
                        + "It will be saved locally for personal testing. "
                        + "Do not share your key with anyone.")
                .setView(container)
                .setNegativeButton("CANCEL", (dialog, which) -> {
                    scanStatus.setText("Gemini connection cancelled.");
                })
                .setPositiveButton("SAVE & SCAN", (dialog, which) -> {

                    String key = keyInput.getText()
                            .toString().trim();

                    if (key.isEmpty()) {
                        Toast.makeText(this,
                                "Enter your Gemini API key.",
                                Toast.LENGTH_LONG).show();
                        return;
                    }

                    getSharedPreferences(
                            "piphunter_secure_test",
                            MODE_PRIVATE
                    ).edit()
                            .putString("gemini_api_key", key)
                            .apply();

                    runGeminiScan(key);
                })
                .show();
    }

    // RUN REAL GEMINI VISION ANALYSIS

    private void runGeminiScan(String apiKey) {

        if (selectedImageUri == null) {
            Toast.makeText(this,
                    "Choose a chart screenshot first.",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        scanButton.setEnabled(false);
        scanButton.setText("GEMINI AI ANALYZING...");
        scanButton.setAlpha(0.65f);

        scanBeamView.startScanning();

        scanStatus.setText(
                "ONLINE AI • Sending chart to Gemini...");

        decisionText.setText("WAIT");
        decisionText.setTextColor(gold);

        explanationText.setText(
                "Gemini is studying the chart. Please wait.");

        entryText.setText("—");
        stopLossText.setText("—");
        tp1Text.setText("—");
        tp2Text.setText("—");
        confidenceText.setText("—");

        final Uri imageUri = selectedImageUri;

        imageExecutor.execute(() -> {

            Bitmap bitmap = null;
            ChartResult result;

            try {

                bitmap = loadBitmap(imageUri);

                result = new GeminiVisionAnalyzer()
                        .analyze(bitmap, apiKey);

            } catch (Exception e) {

                String message = e.getMessage();

                if (message == null || message.trim().isEmpty()) {
                    message = "Unknown analysis error.";
                }

                result = ChartResult.waitForMoreData(
                        "Gemini analysis failed: " + message);

            } finally {

                if (bitmap != null && !bitmap.isRecycled()) {
                    bitmap.recycle();
                }
            }

            final ChartResult finalResult = result;

            runOnUiThread(() -> {

                if (activityDestroyed) {
                    return;
                }

                scanBeamView.stopScanning();

                showAnalysisResult(finalResult);

                scanButton.setEnabled(true);
                scanButton.setAlpha(1.0f);
                scanButton.setText("⌕  SCAN WITH GEMINI AI");
            });
        });
    }

    // LOAD FULL CHART IMAGE

    private Bitmap loadBitmap(Uri uri) throws Exception {

        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;

        InputStream firstStream =
                getContentResolver().openInputStream(uri);

        if (firstStream == null) {
            throw new Exception("Image stream unavailable");
        }

        try {
            BitmapFactory.decodeStream(firstStream, null, bounds);
        } finally {
            firstStream.close();
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
            throw new Exception("Invalid image dimensions");
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;

        while ((bounds.outWidth / options.inSampleSize) > 1800
                || (bounds.outHeight / options.inSampleSize) > 1800) {
            options.inSampleSize *= 2;
        }

        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        InputStream secondStream =
                getContentResolver().openInputStream(uri);

        if (secondStream == null) {
            throw new Exception("Unable to reopen image");
        }

        try {
            Bitmap bitmap = BitmapFactory.decodeStream(
                    secondStream, null, options);

            if (bitmap == null) {
                throw new Exception("Image decoding failed");
            }

            return bitmap;

        } finally {
            secondStream.close();
        }
    }

    // DISPLAY RESULT

    private void showAnalysisResult(ChartResult result) {

        decisionText.setText(result.signal);
        explanationText.setText(result.explanation);

        entryText.setText(result.entry);
        stopLossText.setText(result.stopLoss);
        tp1Text.setText(result.takeProfit1);
        tp2Text.setText(result.takeProfit2);
        confidenceText.setText(result.confidence);

        if ("BUY".equals(result.signal)) {
            decisionText.setTextColor(green);

        } else if ("SELL".equals(result.signal)) {
            decisionText.setTextColor(
                    Color.rgb(255, 105, 105));

        } else {
            decisionText.setTextColor(gold);
        }

        scanStatus.setText(
                "GEMINI AI ANALYSIS COMPLETE.");
    }

    // IMAGE PICKER RESULT

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CHART_IMAGE
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            Uri newImageUri = data.getData();

            try {

                int readPermission =
                        data.getFlags()
                                & Intent.FLAG_GRANT_READ_URI_PERMISSION;

                if (readPermission != 0) {
                    getContentResolver()
                            .takePersistableUriPermission(
                                    newImageUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }

                InputStream testStream =
                        getContentResolver().openInputStream(newImageUri);

                if (testStream == null) {
                    throw new Exception("Image cannot be opened");
                }

                testStream.close();

                selectedImageUri = newImageUri;

                scanStatus.setText(
                        "Chart selected. Loading preview...");

                scanButton.setEnabled(true);
                scanButton.setAlpha(1.0f);

                decisionText.setText("WAIT");
                decisionText.setTextColor(gold);

                explanationText.setText(
                        "Waiting for Gemini AI analysis.");

                entryText.setText("—");
                stopLossText.setText("—");
                tp1Text.setText("—");
                tp2Text.setText("—");
                confidenceText.setText("—");

                loadPreview(newImageUri);

            } catch (Exception e) {

                selectedImageUri = null;
                previewRequestId.incrementAndGet();

                chartPreview.setImageResource(
                        android.R.drawable.ic_menu_gallery);

                imageStatus.setText(
                        "Could not open this image. Choose another screenshot.");

                scanStatus.setText("Image selection failed.");

                scanButton.setEnabled(false);
                scanButton.setAlpha(0.55f);

                Toast.makeText(this,
                        "Unable to access that image. Please choose another.",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

    // SCAN BEAM ANIMATION

    private class ScanBeamView extends View {

        private final Paint glowPaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private final Paint linePaint =
                new Paint(Paint.ANTI_ALIAS_FLAG);

        private ValueAnimator animator;
        private float scanY = 0f;

        ScanBeamView() {
            super(MainActivity.this);
            setWillNotDraw(false);
        }

        void startScanning() {

            stopAnimatorOnly();
            setVisibility(View.VISIBLE);

            animator = ValueAnimator.ofFloat(0f, 1f);
            animator.setDuration(1250);
            animator.setRepeatCount(ValueAnimator.INFINITE);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setInterpolator(new LinearInterpolator());

            animator.addUpdateListener(a -> {
                scanY = (float) a.getAnimatedValue();
                invalidate();
            });

            animator.start();
        }

        private void stopAnimatorOnly() {

            if (animator != null) {
                animator.cancel();
                animator = null;
            }
        }

        void stopScanning() {

            stopAnimatorOnly();
            scanY = 0f;
            setVisibility(View.GONE);
            invalidate();
        }

        @Override
        protected void onDraw(Canvas canvas) {

            super.onDraw(canvas);

            if (getVisibility() != View.VISIBLE
                    || getHeight() <= 0) {
                return;
            }

            float y = scanY * getHeight();
            float band = dp(24);

            glowPaint.setColor(
                    Color.argb(54, 40, 255, 177));

            canvas.drawRect(
                    0,
                    Math.max(0, y - band),
                    getWidth(),
                    Math.min(getHeight(), y + band),
                    glowPaint);

            linePaint.setColor(Color.rgb(64, 255, 190));
            linePaint.setStrokeWidth(dp(2));

            canvas.drawLine(
                    0, y, getWidth(), y, linePaint);

            linePaint.setColor(
                    Color.argb(110, 55, 205, 255));

            linePaint.setStrokeWidth(dp(1));

            canvas.drawLine(
                    0,
                    Math.min(getHeight(), y + dp(3)),
                    getWidth(),
                    Math.min(getHeight(), y + dp(3)),
                    linePaint);
        }
    }

    @Override
    protected void onDestroy() {

        activityDestroyed = true;
        previewRequestId.incrementAndGet();

        if (scanBeamView != null) {
            scanBeamView.stopScanning();
        }

        if (imageExecutor != null) {
            imageExecutor.shutdownNow();
        }

        super.onDestroy();
    }
}