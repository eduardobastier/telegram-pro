package org.telegram.messenger;

import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaMuxer;
import android.text.TextUtils;
import android.util.Base64;

import org.json.JSONArray;
import org.telegram.tgnet.TLRPC;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

// Voice/round message transcription without Premium: on-device Android speech recognition,
// or Whisper on Cloudflare Workers AI (same approach as Nekogram's WhisperHelper)
public class ProTranscriber {

    private static final ExecutorService executor = Executors.newCachedThreadPool();
    private static OkHttpClient client;

    public interface Callback {
        void run(String text, String error);
    }

    // true when transcription is handled here instead of Telegram's Premium transcribeAudio
    public static boolean isEnabled(int account) {
        return SharedConfig.transcribeProvider == SharedConfig.TRANSCRIBE_PROVIDER_WORKERS_AI ||
            SharedConfig.transcribeProvider == SharedConfig.TRANSCRIBE_PROVIDER_DEVICE ||
            SharedConfig.transcribeProvider == SharedConfig.TRANSCRIBE_PROVIDER_AUTO && !UserConfig.getInstance(account).isPremium();
    }

    // own messages keep the recorded file in attachPath; received ones live in the download cache
    public static File findMediaFile(MessageObject messageObject) {
        final TLRPC.Message message = messageObject.messageOwner;
        if (!TextUtils.isEmpty(message.attachPath)) {
            final File file = new File(message.attachPath);
            if (file.exists()) {
                return file;
            }
        }
        final FileLoader loader = FileLoader.getInstance(messageObject.currentAccount);
        File file = loader.getPathToMessage(message);
        if (file != null && file.exists()) {
            return file;
        }
        file = loader.getPathToAttach(messageObject.getDocument(), true);
        return file != null && file.exists() ? file : null;
    }

    public static boolean hasCredentials() {
        return !TextUtils.isEmpty(SharedConfig.cfAccountId) && !TextUtils.isEmpty(SharedConfig.cfApiToken);
    }

    // callback may run on any thread
    public static void transcribe(File file, boolean video, Callback callback) {
        if (SharedConfig.transcribeProvider == SharedConfig.TRANSCRIBE_PROVIDER_WORKERS_AI) {
            transcribeWorkersAi(file, video, callback);
        } else {
            ProDeviceTranscriber.transcribe(file, callback);
        }
    }

    private static void transcribeWorkersAi(File file, boolean video, Callback callback) {
        if (!hasCredentials()) {
            callback.run(null, LocaleController.getString(R.string.ProCloudflareNotSet));
            return;
        }
        executor.submit(() -> {
            File audio = file;
            File extracted = null;
            if (video) {
                extracted = new File(file.getAbsolutePath() + ".m4a");
                try {
                    extractAudio(file.getAbsolutePath(), extracted.getAbsolutePath());
                    audio = extracted;
                } catch (Exception e) {
                    FileLog.e(e);
                }
            }
            try {
                final JSONObject payload = new JSONObject();
                payload.put("audio", Base64.encodeToString(readAll(audio), Base64.NO_WRAP));
                payload.put("vad_filter", true);
                final Request request = new Request.Builder()
                    .url("https://api.cloudflare.com/client/v4/accounts/" + SharedConfig.cfAccountId + "/ai/run/@cf/openai/whisper-large-v3-turbo")
                    .header("Authorization", "Bearer " + SharedConfig.cfApiToken)
                    .post(RequestBody.create(payload.toString(), MediaType.get("application/json")))
                    .build();
                try (Response response = getClient().newCall(request).execute()) {
                    final JSONObject json = new JSONObject(response.body().string());
                    if (json.optBoolean("success") && json.optJSONObject("result") != null) {
                        callback.run(json.getJSONObject("result").optString("text", "").trim(), null);
                    } else {
                        final JSONArray errors = json.optJSONArray("errors");
                        final String message = errors != null && errors.length() > 0 ? errors.getJSONObject(0).optString("message") : ("HTTP " + response.code());
                        callback.run(null, message);
                    }
                }
            } catch (Exception e) {
                FileLog.e(e);
                callback.run(null, e.getLocalizedMessage() != null ? e.getLocalizedMessage() : e.toString());
            } finally {
                if (extracted != null) {
                    extracted.delete();
                }
            }
        });
    }

    private static OkHttpClient getClient() {
        if (client == null) {
            client = new OkHttpClient.Builder()
                .connectTimeout(120, TimeUnit.SECONDS)
                .readTimeout(120, TimeUnit.SECONDS)
                .writeTimeout(120, TimeUnit.SECONDS)
                .build();
        }
        return client;
    }

    private static byte[] readAll(File file) throws IOException {
        final byte[] data = new byte[(int) file.length()];
        try (FileInputStream in = new FileInputStream(file)) {
            int offset = 0;
            while (offset < data.length) {
                final int read = in.read(data, offset, data.length - offset);
                if (read < 0) {
                    break;
                }
                offset += read;
            }
        }
        return data;
    }

    // round videos: copy the audio track into an .m4a so Whisper gets audio only
    private static void extractAudio(String input, String output) throws IOException {
        final MediaExtractor extractor = new MediaExtractor();
        extractor.setDataSource(input);
        int track = -1;
        MediaFormat format = null;
        for (int i = 0; i < extractor.getTrackCount(); i++) {
            final MediaFormat f = extractor.getTrackFormat(i);
            final String mime = f.getString(MediaFormat.KEY_MIME);
            if (mime != null && mime.startsWith("audio/")) {
                track = i;
                format = f;
                break;
            }
        }
        if (format == null) {
            extractor.release();
            throw new IOException("No audio track in " + input);
        }
        final MediaMuxer muxer = new MediaMuxer(output, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        final int outTrack = muxer.addTrack(format);
        muxer.start();
        extractor.selectTrack(track);
        final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
        final ByteBuffer buffer = ByteBuffer.allocate(65536);
        while (true) {
            final int size = extractor.readSampleData(buffer, 0);
            if (size < 0) {
                break;
            }
            info.offset = 0;
            info.size = size;
            info.presentationTimeUs = extractor.getSampleTime();
            info.flags = 0;
            muxer.writeSampleData(outTrack, buffer, info);
            extractor.advance();
        }
        muxer.stop();
        muxer.release();
        extractor.release();
    }
}
