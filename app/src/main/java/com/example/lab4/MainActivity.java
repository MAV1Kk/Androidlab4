package com.example.lab4;

import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.database.Cursor;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.OptIn;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

public class MainActivity extends AppCompatActivity {

    private enum MediaMode {
        AUDIO,
        VIDEO,
        RADIO
    }

    private static final String STATION_1_NAME = "Lo-Fi Radio";
    private static final String STATION_1_URL = "https://ice5.somafm.com/groovesalad-128-mp3";

    private static final String STATION_2_NAME = "Ambient Radio";
    private static final String STATION_2_URL = "https://ice5.somafm.com/dronezone-128-mp3";

    private static final String STATION_3_NAME = "Indie Radio";
    private static final String STATION_3_URL = "https://ice5.somafm.com/indiepop-128-mp3";

    private ScrollView scrollRoot;
    private LinearLayout mainContainer;
    private LinearLayout headerBlock;
    private LinearLayout modeCard;
    private LinearLayout internetCard;
    private LinearLayout radioCard;
    private LinearLayout sourceCard;
    private LinearLayout controlsCard;
    private FrameLayout videoContainer;

    private RadioGroup rgMode;
    private RadioButton rbAudio;
    private RadioButton rbVideo;
    private RadioButton rbRadio;

    private Button btnPickFile;
    private Button btnPlayUrl;
    private Button btnDownload;
    private Button btnStation1;
    private Button btnStation2;
    private Button btnStation3;
    private Button btnPlay;
    private Button btnPause;
    private Button btnStop;
    private Button btnFullscreen;

    private EditText etUrl;
    private TextView tvCurrent;
    private PlayerView playerView;

    private ExoPlayer player;
    private Uri currentUri;
    private String currentLabel = "Медіафайл ще не вибрано";
    private boolean isFullscreen = false;

    private final ActivityResultLauncher<Intent> filePickerLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.StartActivityForResult(),
                    result -> {
                        if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                            Uri uri = result.getData().getData();

                            if (uri == null) {
                                Toast.makeText(this, "Файл не вибрано", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            final int flags = result.getData().getFlags()
                                    & (Intent.FLAG_GRANT_READ_URI_PERMISSION
                                    | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

                            try {
                                getContentResolver().takePersistableUriPermission(uri, flags);
                            } catch (Exception ignored) {
                            }

                            String fileName = getFileName(uri);
                            loadMedia(uri, fileName, true);
                        }
                    }
            );

    @OptIn(markerClass = UnstableApi.class)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initPlayer();
        initListeners();
        applyModeUi();
    }

    private void initViews() {
        scrollRoot = findViewById(R.id.scrollRoot);
        mainContainer = findViewById(R.id.mainContainer);
        headerBlock = findViewById(R.id.headerBlock);
        modeCard = findViewById(R.id.modeCard);
        internetCard = findViewById(R.id.internetCard);
        radioCard = findViewById(R.id.radioCard);
        sourceCard = findViewById(R.id.sourceCard);
        controlsCard = findViewById(R.id.controlsCard);
        videoContainer = findViewById(R.id.videoContainer);

        rgMode = findViewById(R.id.rgMode);
        rbAudio = findViewById(R.id.rbAudio);
        rbVideo = findViewById(R.id.rbVideo);
        rbRadio = findViewById(R.id.rbRadio);

        btnPickFile = findViewById(R.id.btnPickFile);
        btnPlayUrl = findViewById(R.id.btnPlayUrl);
        btnDownload = findViewById(R.id.btnDownload);
        btnStation1 = findViewById(R.id.btnStation1);
        btnStation2 = findViewById(R.id.btnStation2);
        btnStation3 = findViewById(R.id.btnStation3);
        btnPlay = findViewById(R.id.btnPlay);
        btnPause = findViewById(R.id.btnPause);
        btnStop = findViewById(R.id.btnStop);
        btnFullscreen = findViewById(R.id.btnFullscreen);

        etUrl = findViewById(R.id.etUrl);
        tvCurrent = findViewById(R.id.tvCurrent);
        playerView = findViewById(R.id.playerView);
    }

    private void initPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
    }

    private void initListeners() {
        rgMode.setOnCheckedChangeListener((group, checkedId) -> {
            applyModeUi();

            if (getSelectedMode() == MediaMode.RADIO) {
                etUrl.setText("");
                tvCurrent.setText("Активовано режим цифрового радіо.\nОберіть одну з готових онлайн-станцій.");
            } else if (getSelectedMode() == MediaMode.VIDEO) {
                tvCurrent.setText("Режим відео.\nОберіть відеофайл з пристрою або вставте пряме URL-посилання.");
            } else {
                tvCurrent.setText("Режим аудіо.\nОберіть аудіофайл з пристрою або вставте пряме URL-посилання.");
            }
        });

        btnPickFile.setOnClickListener(v -> openFilePicker());

        btnPlayUrl.setOnClickListener(v -> playFromUrl());

        btnDownload.setOnClickListener(v -> downloadFromUrl());

        btnStation1.setOnClickListener(v -> playRadioStation(STATION_1_NAME, STATION_1_URL));
        btnStation2.setOnClickListener(v -> playRadioStation(STATION_2_NAME, STATION_2_URL));
        btnStation3.setOnClickListener(v -> playRadioStation(STATION_3_NAME, STATION_3_URL));

        btnPlay.setOnClickListener(v -> {
            if (currentUri == null) {
                Toast.makeText(this, "Спочатку оберіть файл, URL або радіостанцію", Toast.LENGTH_SHORT).show();
                return;
            }

            player.play();
            Toast.makeText(this, "Відтворення запущено", Toast.LENGTH_SHORT).show();
        });

        btnPause.setOnClickListener(v -> {
            player.pause();
            Toast.makeText(this, "Відтворення призупинено", Toast.LENGTH_SHORT).show();
        });

        btnStop.setOnClickListener(v -> {
            player.pause();
            player.seekTo(0);
            Toast.makeText(this, "Відтворення зупинено", Toast.LENGTH_SHORT).show();
        });

        btnFullscreen.setOnClickListener(v -> toggleFullscreen());
    }

    private MediaMode getSelectedMode() {
        int checkedId = rgMode.getCheckedRadioButtonId();

        if (checkedId == R.id.rbVideo) {
            return MediaMode.VIDEO;
        }

        if (checkedId == R.id.rbRadio) {
            return MediaMode.RADIO;
        }

        return MediaMode.AUDIO;
    }

    private void applyModeUi() {
        MediaMode mode = getSelectedMode();

        if (mode == MediaMode.VIDEO) {
            videoContainer.setVisibility(View.VISIBLE);
            btnFullscreen.setVisibility(View.VISIBLE);
            radioCard.setVisibility(View.GONE);
            btnPickFile.setEnabled(true);
            btnPickFile.setAlpha(1f);
            etUrl.setHint("Вставте пряме URL-посилання на відео");
        } else if (mode == MediaMode.RADIO) {
            videoContainer.setVisibility(View.GONE);
            radioCard.setVisibility(View.VISIBLE);
            btnPickFile.setEnabled(false);
            btnPickFile.setAlpha(0.5f);
            etUrl.setHint("Для радіо використайте готові станції нижче");
        } else {
            videoContainer.setVisibility(View.GONE);
            btnFullscreen.setVisibility(View.GONE);
            radioCard.setVisibility(View.GONE);
            btnPickFile.setEnabled(true);
            btnPickFile.setAlpha(1f);
            etUrl.setHint("Вставте пряме URL-посилання на аудіо");
        }
    }

    private void openFilePicker() {
        MediaMode mode = getSelectedMode();

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        if (mode == MediaMode.VIDEO) {
            intent.setType("video/*");
        } else {
            intent.setType("audio/*");
        }

        filePickerLauncher.launch(intent);
    }

    private void playFromUrl() {
        String url = etUrl.getText().toString().trim();

        if (url.isEmpty()) {
            Toast.makeText(this, "Вставте URL-посилання", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, "URL має починатися з http:// або https://", Toast.LENGTH_SHORT).show();
            return;
        }

        String label;

        if (getSelectedMode() == MediaMode.VIDEO) {
            label = "Відео з Інтернету";
        } else if (getSelectedMode() == MediaMode.RADIO) {
            label = "Потокове радіо";
        } else {
            label = "Аудіо з Інтернету";
        }

        loadMedia(Uri.parse(url), label, true);
    }

    private void playRadioStation(String stationName, String stationUrl) {
        rbRadio.setChecked(true);

        currentLabel = stationName;
        currentUri = Uri.parse(stationUrl);

        player.setMediaItem(MediaItem.fromUri(currentUri));
        player.prepare();
        player.play();

        tvCurrent.setText(
                "Зараз відтворюється цифрове радіо:\n" +
                        stationName + "\n\n" +
                        "Потік:\n" +
                        stationUrl
        );

        Toast.makeText(this, "Запущено станцію: " + stationName, Toast.LENGTH_SHORT).show();
    }

    private void loadMedia(Uri uri, String label, boolean autoPlay) {
        currentUri = uri;
        currentLabel = label;

        player.setMediaItem(MediaItem.fromUri(uri));
        player.prepare();

        if (autoPlay) {
            player.play();
        }

        tvCurrent.setText(
                "Зараз відтворюється:\n" +
                        label + "\n\n" +
                        "Джерело:\n" +
                        uri.toString()
        );

        if (getSelectedMode() == MediaMode.VIDEO) {
            videoContainer.setVisibility(View.VISIBLE);
            btnFullscreen.setVisibility(View.VISIBLE);
        }

        Toast.makeText(this, "Медіа завантажено", Toast.LENGTH_SHORT).show();
    }

    private void downloadFromUrl() {
        String url = etUrl.getText().toString().trim();

        if (url.isEmpty()) {
            Toast.makeText(this, "Вставте URL для завантаження", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, "Некоректне URL-посилання", Toast.LENGTH_SHORT).show();
            return;
        }

        String fileName = createDownloadFileName(url);

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Завантаження медіафайлу");
        request.setDescription(fileName);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(true);

        DownloadManager manager = (DownloadManager) getSystemService(DOWNLOAD_SERVICE);

        if (manager != null) {
            manager.enqueue(request);
            Toast.makeText(this, "Файл завантажується у папку Downloads", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Не вдалося запустити завантаження", Toast.LENGTH_SHORT).show();
        }
    }

    private String createDownloadFileName(String url) {
        String cleanUrl = url.split("\\?")[0];
        String name = cleanUrl.substring(cleanUrl.lastIndexOf("/") + 1);

        if (name.trim().isEmpty() || !name.contains(".")) {
            if (getSelectedMode() == MediaMode.VIDEO) {
                name = "downloaded_video.mp4";
            } else {
                name = "downloaded_audio.mp3";
            }
        }

        return name;
    }

    private String getFileName(Uri uri) {
        String result = null;

        if ("content".equals(uri.getScheme())) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);

                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (result == null) {
            result = uri.getLastPathSegment();
        }

        if (result == null) {
            result = "selected_media";
        }

        return result;
    }

    private void toggleFullscreen() {
        if (getSelectedMode() != MediaMode.VIDEO) {
            return;
        }

        if (isFullscreen) {
            exitFullscreen();
        } else {
            enterFullscreen();
        }
    }

    private void enterFullscreen() {
        isFullscreen = true;

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);

        headerBlock.setVisibility(View.GONE);
        modeCard.setVisibility(View.GONE);
        internetCard.setVisibility(View.GONE);
        radioCard.setVisibility(View.GONE);
        sourceCard.setVisibility(View.GONE);
        controlsCard.setVisibility(View.GONE);

        scrollRoot.setBackgroundColor(0xFF000000);
        mainContainer.setPadding(0, 0, 0, 0);

        ViewGroup.LayoutParams containerParams = videoContainer.getLayoutParams();
        containerParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        containerParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        videoContainer.setLayoutParams(containerParams);

        ViewGroup.LayoutParams playerParams = playerView.getLayoutParams();
        playerParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        playerParams.height = ViewGroup.LayoutParams.MATCH_PARENT;
        playerView.setLayoutParams(playerParams);

        btnFullscreen.setText("Exit");

        hideSystemBars();
    }

    private void exitFullscreen() {
        isFullscreen = false;

        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        headerBlock.setVisibility(View.VISIBLE);
        modeCard.setVisibility(View.VISIBLE);
        internetCard.setVisibility(View.VISIBLE);
        sourceCard.setVisibility(View.VISIBLE);
        controlsCard.setVisibility(View.VISIBLE);

        applyModeUi();

        scrollRoot.setBackgroundColor(0xFFEAF0F7);
        mainContainer.setPadding(dpToPx(18), dpToPx(18), dpToPx(18), dpToPx(18));

        ViewGroup.LayoutParams containerParams = videoContainer.getLayoutParams();
        containerParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        containerParams.height = ViewGroup.LayoutParams.WRAP_CONTENT;
        videoContainer.setLayoutParams(containerParams);

        ViewGroup.LayoutParams playerParams = playerView.getLayoutParams();
        playerParams.width = ViewGroup.LayoutParams.MATCH_PARENT;
        playerParams.height = dpToPx(230);
        playerView.setLayoutParams(playerParams);

        btnFullscreen.setText("Full");

        showSystemBars();
    }

    private void hideSystemBars() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();

            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(
                        WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                );
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        }
    }

    private void showSystemBars() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            WindowInsetsController controller = getWindow().getInsetsController();

            if (controller != null) {
                controller.show(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private int dpToPx(int dp) {
        return Math.round(dp * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (isFullscreen) {
            exitFullscreen();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (player != null) {
            player.release();
        }
    }
}