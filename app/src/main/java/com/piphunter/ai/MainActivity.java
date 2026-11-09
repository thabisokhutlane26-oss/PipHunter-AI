
package com.piphunter.ai;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final int REQUEST_CHART_IMAGE = 101;

    private final int bgColor = Color.rgb(7, 16, 27);
    private final int cardColor = Color.rgb(17, 34, 49);
    private final int green = Color.rgb(48, 220, 145);
    private final int white = Color.WHITE;
    private final int muted = Color.rgb(151, 171, 188);

    private ImageView chartPreview;
    private TextView imageStatus;
    private TextView scanStatus;
    private Button scanButton;
    private Uri selectedImageUri;

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
                cardColor, Color.rgb(37, 62, 79)));

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        -1, -2);

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

        getWindow().setStatusBarColor(bgColor);
        getWindow().setNavigationBarColor(bgColor);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(bgColor);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(22), dp(18), dp(28));

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout heading = new LinearLayout(this);
        heading.setOrientation(LinearLayout.VERTICAL);
        heading.setLayoutParams(
                new LinearLayout.LayoutParams(0, -2, 1));

        addLabel(heading, "PipHunter AI", 27, white, true);

        TextView subtitle = makeText(
                "SMART FOREX SCREENSHOT SCANNER",
                10, muted, true);

        subtitle.setPadding(0, dp(4), 0, 0);
        heading.addView(subtitle);
        header.addView(heading);

        TextView ready = makeText("● READY", 11, green, true);
        ready.setPadding(dp(10), dp(8), dp(10), dp(8));
        ready.setBackground(roundedBackground(
                Color.rgb(17, 55, 47),
                Color.rgb(35, 115, 88)));

        header.addView(ready);
        root.addView(header);

        TextView intro = makeText(
                "Upload a chart. Let AI study it. Get a clear trading decision.",
                14, muted, false);

        intro.setPadding(0, dp(15), 0, dp(20));
        root.addView(intro);

        // UPLOAD CARD

        LinearLayout uploadCard = makeCard();

        addLabel(uploadCard,
                "01  UPLOAD YOUR CHART",
                14, white, true);

        TextView uploadHelp = makeText(
                "Choose a screenshot from MT4, MT5, TradingView or your phone gallery.",
                12, muted, false);

        uploadHelp.setPadding(0, dp(7), 0, dp(14));
        uploadCard.addView(uploadHelp);

        Button uploadButton = makeButton(
                "＋  CHOOSE CHART SCREENSHOT", true);

        uploadCard.addView(uploadButton);
        uploadButton.setOnClickListener(v -> openImagePicker());

        root.addView(uploadCard);

        // IMAGE PREVIEW CARD

        LinearLayout previewCard = makeCard();

        addLabel(previewCard,
                "02  CHART PREVIEW",
                14, white, true);

        chartPreview = new ImageView(this);
        chartPreview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        chartPreview.setAdjustViewBounds(true);
        chartPreview.setMinimumHeight(dp(170));
        chartPreview.setMaxHeight(dp(320));

        chartPreview.setBackground(roundedBackground(
                Color.rgb(8, 20, 31),
                Color.rgb(43, 68, 84)));

        chartPreview.setImageResource(
                android.R.drawable.ic_menu_gallery);

        LinearLayout.LayoutParams imageParams =
                new LinearLayout.LayoutParams(-1, dp(190));

        imageParams.topMargin = dp(12);

        previewCard.addView(chartPreview, imageParams);

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
                14, white, true);

        TextView scanHelp = makeText(
                "The image-analysis engine will inspect your uploaded chart.",
                12, muted, false);

        scanHelp.setPadding(0, dp(7), 0, dp(14));
        scanCard.addView(scanHelp);

        scanButton = makeButton("⌕  SCAN CHART", true);
        scanButton.setEnabled(false);
        scanButton.setAlpha(0.55f);

        scanCard.addView(scanButton);

        scanButton.setOnClickListener(v -> {

            if (selectedImageUri == null) {
                Toast.makeText(
                        this,
                        "Choose a chart screenshot first.",
                        Toast.LENGTH_SHORT
                ).show();
                return;
            }

            scanStatus.setText(
                    "Screenshot loaded successfully. " +
                    "The AI image-analysis engine is the next development step."
            );

            Toast.makeText(
                    this,
                    "Chart ready for AI analysis integration.",
                    Toast.LENGTH_LONG
            ).show();
        });

        scanStatus = makeText(
                "Waiting for a chart screenshot.",
                12, muted, false);

        scanStatus.setPadding(0, dp(12), 0, 0);
        scanCard.addView(scanStatus);

        root.addView(scanCard);

        // RESULT CARD

        LinearLayout resultCard = makeCard();

        addLabel(resultCard,
                "04  SCAN RESULT",
                14, white, true);

        TextView decision = makeText(
                "BUY  /  SELL  /  WAIT",
                22, green, true);

        decision.setGravity(Gravity.CENTER);
        decision.setPadding(0, dp(22), 0, dp(8));

        resultCard.addView(decision);

        TextView resultHelp = makeText(
                "Your decision will appear here after real AI image analysis. " +
                "No result is generated from an unscanned image.",
                12, muted, false);

        resultHelp.setGravity(Gravity.CENTER);
        resultCard.addView(resultHelp);

        View line = new View(this);
        line.setBackgroundColor(Color.rgb(48, 70, 86));

        LinearLayout.LayoutParams lineParams =
                new LinearLayout.LayoutParams(-1, dp(1));

        lineParams.topMargin = dp(16);
        lineParams.bottomMargin = dp(14);

        resultCard.addView(line, lineParams);

        addResultRow(resultCard, "Entry", "—");
        addResultRow(resultCard, "Stop Loss", "—");
        addResultRow(resultCard, "Take Profit 1", "—");
        addResultRow(resultCard, "Take Profit 2", "—");
        addResultRow(resultCard, "Confidence", "—");

        root.addView(resultCard);

        // FOOTER

        LinearLayout footer = new LinearLayout(this);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(8), 0, 0);

        TextView footerText = makeText(
                "SCAN HISTORY     •     SETTINGS",
                11, muted, true);

        footer.addView(footerText);
        root.addView(footer);

        TextView disclaimer = makeText(
                "For educational analysis only. Always manage trading risk.",
                10, muted, false);

        disclaimer.setGravity(Gravity.CENTER);
        disclaimer.setPadding(0, dp(14), 0, 0);

        root.addView(disclaimer);

        scroll.addView(root);
        setContentView(scroll);
    }

    private void addResultRow(
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
    }

    private void openImagePicker() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("image/*");

        startActivityForResult(
                intent, REQUEST_CHART_IMAGE);
    }

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data) {

        super.onActivityResult(
                requestCode, resultCode, data);

        if (requestCode == REQUEST_CHART_IMAGE
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null) {

            selectedImageUri = data.getData();

            try {
                chartPreview.setImageURI(selectedImageUri);

                imageStatus.setText(
                        "Screenshot selected successfully");

                scanStatus.setText(
                        "Chart image loaded. Ready for AI engine integration.");

                scanButton.setEnabled(true);
                scanButton.setAlpha(1.0f);

            } catch (Exception e) {

                selectedImageUri = null;

                imageStatus.setText(
                        "Could not open this image. Please choose another screenshot.");

                scanButton.setEnabled(false);
                scanButton.setAlpha(0.55f);
            }
        }
    }
}
