package org.telegram.messenger;

import android.app.Activity;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONObject;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.LaunchActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

// OTA updates from the GitHub Releases of the Telegram Pro repository
public class ProUpdater {

    private static final String LATEST_RELEASE_URL = "https://api.github.com/repos/eduardobastier/telegram-pro/releases/latest";
    private static final long AUTO_CHECK_INTERVAL_MS = 6 * 60 * 60 * 1000L;

    private static OkHttpClient client;
    private static boolean checking;
    private static boolean downloading;

    public static class Release {
        public String version;
        public String notes;
        public String apkUrl;
        public long apkSize;
    }

    public static String currentVersion() {
        return BuildConfig.PRO_VERSION;
    }

    // called from LaunchActivity.onResume
    public static void checkAuto(Activity activity) {
        if (!SharedConfig.proAutoUpdate || checking || downloading) {
            return;
        }
        final long now = System.currentTimeMillis();
        if (now - SharedConfig.proLastUpdateCheck < AUTO_CHECK_INTERVAL_MS) {
            return;
        }
        SharedConfig.setProLastUpdateCheck(now);
        check(activity, false);
    }

    public static void check(Activity activity, boolean manual) {
        if (checking) {
            return;
        }
        checking = true;
        Utilities.globalQueue.postRunnable(() -> {
            Release release = null;
            String error = null;
            try {
                release = fetchLatest();
            } catch (Exception e) {
                FileLog.e(e);
                error = e.getLocalizedMessage();
            }
            final Release finalRelease = release;
            final String finalError = error;
            AndroidUtilities.runOnUIThread(() -> {
                checking = false;
                if (activity == null || activity.isFinishing()) {
                    return;
                }
                if (finalRelease == null) {
                    if (manual) {
                        showBulletin(LocaleController.getString(R.string.ProUpdateCheckFailed) + (finalError != null ? " (" + finalError + ")" : ""));
                    }
                } else if (compareVersions(finalRelease.version, currentVersion()) > 0) {
                    if (manual || !finalRelease.version.equals(SharedConfig.proSkippedUpdate)) {
                        showUpdateDialog(activity, finalRelease);
                    }
                } else if (manual) {
                    showBulletin(LocaleController.formatString(R.string.ProUpdateNone, currentVersion()));
                }
            });
        });
    }

    private static OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .build();
        }
        return client;
    }

    private static Release fetchLatest() throws Exception {
        final Request request = new Request.Builder()
            .url(LATEST_RELEASE_URL)
            .header("Accept", "application/vnd.github+json")
            .build();
        try (Response response = getClient().newCall(request).execute()) {
            if (!response.isSuccessful()) {
                throw new Exception("HTTP " + response.code());
            }
            final JSONObject json = new JSONObject(response.body().string());
            final Release release = new Release();
            release.version = json.getString("tag_name");
            release.notes = json.optString("body", "");
            final JSONArray assets = json.optJSONArray("assets");
            if (assets != null) {
                for (int i = 0; i < assets.length(); i++) {
                    final JSONObject asset = assets.getJSONObject(i);
                    if (asset.optString("name").endsWith(".apk")) {
                        release.apkUrl = asset.getString("browser_download_url");
                        release.apkSize = asset.optLong("size");
                        break;
                    }
                }
            }
            if (release.apkUrl == null) {
                throw new Exception("no APK in release " + release.version);
            }
            return release;
        }
    }

    // compares "vX.Y.Z" versions; returns >0 when a is newer than b
    public static int compareVersions(String a, String b) {
        final String[] pa = a.replaceFirst("^v", "").split("\\.");
        final String[] pb = b.replaceFirst("^v", "").split("\\.");
        for (int i = 0; i < Math.max(pa.length, pb.length); i++) {
            final int na = i < pa.length ? Utilities.parseInt(pa[i]) : 0;
            final int nb = i < pb.length ? Utilities.parseInt(pb[i]) : 0;
            if (na != nb) {
                return Integer.compare(na, nb);
            }
        }
        return 0;
    }

    private static void showUpdateDialog(Activity activity, Release release) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle(LocaleController.formatString(R.string.ProUpdateAvailable, release.version));
        builder.setMessage(formatNotes(release.notes));
        builder.setPositiveButton(LocaleController.getString(R.string.ProUpdateInstall), (dialog, which) -> download(activity, release));
        builder.setNegativeButton(LocaleController.getString(R.string.ProUpdateLater), null);
        builder.setNeutralButton(LocaleController.getString(R.string.ProUpdateSkip), (dialog, which) -> SharedConfig.setProSkippedUpdate(release.version));
        builder.show();
    }

    // release notes are Markdown; keep plain lines and turn "- " items into bullets
    private static String formatNotes(String notes) {
        final StringBuilder sb = new StringBuilder();
        for (String line : notes.replace("\r", "").split("\n")) {
            String l = line.trim();
            if (l.isEmpty() || l.startsWith("#")) {
                continue;
            }
            l = l.replaceAll("\\[([^\\]]+)]\\([^)]+\\)", "$1");
            if (l.startsWith("- ")) {
                l = "• " + l.substring(2);
            }
            if (sb.length() > 0) {
                sb.append('\n');
            }
            sb.append(l);
        }
        return sb.toString();
    }

    private static void download(Activity activity, Release release) {
        if (downloading) {
            return;
        }
        downloading = true;
        final AlertDialog progress = new AlertDialog(activity, AlertDialog.ALERT_TYPE_LOADING);
        progress.setTitle(LocaleController.formatString(R.string.ProUpdateDownloading, release.version));
        progress.setCanceledOnTouchOutside(false);
        progress.setCancelable(false);
        progress.show();

        Utilities.globalQueue.postRunnable(() -> {
            final File dir = new File(ApplicationLoader.applicationContext.getExternalCacheDir(), "updates");
            dir.mkdirs();
            final File apk = new File(dir, "TelegramPro-" + release.version + ".apk");
            boolean ok = false;
            try (Response response = getClient().newCall(new Request.Builder().url(release.apkUrl).build()).execute()) {
                if (!response.isSuccessful()) {
                    throw new Exception("HTTP " + response.code());
                }
                final long total = release.apkSize > 0 ? release.apkSize : response.body().contentLength();
                try (InputStream in = response.body().byteStream(); OutputStream out = new FileOutputStream(apk)) {
                    final byte[] buffer = new byte[64 * 1024];
                    long done = 0;
                    int lastPercent = -1;
                    int read;
                    while ((read = in.read(buffer)) > 0) {
                        out.write(buffer, 0, read);
                        done += read;
                        final int percent = total > 0 ? (int) (done * 100 / total) : 0;
                        if (percent != lastPercent) {
                            lastPercent = percent;
                            AndroidUtilities.runOnUIThread(() -> progress.setProgress(percent));
                        }
                    }
                }
                ok = true;
            } catch (Exception e) {
                FileLog.e(e);
                apk.delete();
            }
            final boolean success = ok;
            AndroidUtilities.runOnUIThread(() -> {
                downloading = false;
                try {
                    progress.dismiss();
                } catch (Exception ignore) {}
                if (!success) {
                    showBulletin(LocaleController.getString(R.string.ProUpdateDownloadFailed));
                    return;
                }
                AndroidUtilities.openForView(apk, apk.getName(), "application/vnd.android.package-archive", activity, null, false);
            });
        });
    }

    private static void showBulletin(String text) {
        if (TextUtils.isEmpty(text) || LaunchActivity.getSafeLastFragment() == null) {
            return;
        }
        BulletinFactory.of(LaunchActivity.getSafeLastFragment()).createSimpleBulletin(R.raw.info, text).show();
    }
}
