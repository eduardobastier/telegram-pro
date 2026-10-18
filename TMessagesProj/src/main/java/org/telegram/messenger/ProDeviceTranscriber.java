package org.telegram.messenger;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.text.TextUtils;

import androidx.annotation.RequiresApi;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.util.ArrayList;

// Transcribes an audio file with Android's speech recognizer (Android 13+): the file is decoded
// to 16-bit mono PCM and fed to the recognizer as an audio source in a segmented session, so
// long voice messages are recognized piece by piece.
public class ProDeviceTranscriber {

    public static void transcribe(File file, ProTranscriber.Callback callback) {
        final Context context = ApplicationLoader.applicationContext;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            callback.run(null, LocaleController.getString(R.string.ProTranscribeDeviceUnsupported));
            return;
        }
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            callback.run(null, LocaleController.getString(R.string.ProTranscribeDeviceUnavailable));
            return;
        }
        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            callback.run(null, LocaleController.getString(R.string.ProTranscribeMicPermission));
            return;
        }
        Utilities.globalQueue.postRunnable(() -> {
            final File pcm = new File(context.getCacheDir(), "pro_transcribe_" + Utilities.random.nextLong() + ".pcm");
            final int sampleRate;
            try {
                sampleRate = decodeToMonoPcm(file, pcm);
            } catch (Exception e) {
                FileLog.e(e);
                pcm.delete();
                callback.run(null, LocaleController.getString(R.string.ProTranscribeDecodeFailed));
                return;
            }
            AndroidUtilities.runOnUIThread(() -> new Session(context, pcm, sampleRate, callback).start(true));
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    private static class Session implements RecognitionListener {
        private final Context context;
        private final File pcm;
        private final int sampleRate;
        private final ProTranscriber.Callback callback;
        private final ArrayList<String> segments = new ArrayList<>();
        private final long timeoutMs;
        private SpeechRecognizer recognizer;
        private ParcelFileDescriptor source;
        private boolean onDevice;
        private boolean finished;
        private final Runnable timeout = this::finishWithText;

        Session(Context context, File pcm, int sampleRate, ProTranscriber.Callback callback) {
            this.context = context;
            this.pcm = pcm;
            this.sampleRate = sampleRate;
            this.callback = callback;
            final long durationMs = pcm.length() * 1000L / (2L * sampleRate);
            this.timeoutMs = durationMs * 2 + 30_000;
        }

        void start(boolean preferOnDevice) {
            onDevice = preferOnDevice && SpeechRecognizer.isOnDeviceRecognitionAvailable(context);
            try {
                source = ParcelFileDescriptor.open(pcm, ParcelFileDescriptor.MODE_READ_ONLY);
            } catch (IOException e) {
                FileLog.e(e);
                finishWithError(LocaleController.getString(R.string.ProTranscribeDecodeFailed));
                return;
            }
            recognizer = onDevice ? SpeechRecognizer.createOnDeviceSpeechRecognizer(context) : SpeechRecognizer.createSpeechRecognizer(context);
            recognizer.setRecognitionListener(this);

            final Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, LocaleController.getInstance().getCurrentLocale().toLanguageTag());
            intent.putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true);
            intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, source);
            intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, 1);
            intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT);
            intent.putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, sampleRate);
            intent.putExtra(RecognizerIntent.EXTRA_SEGMENTED_SESSION, RecognizerIntent.EXTRA_AUDIO_SOURCE);
            AndroidUtilities.cancelRunOnUIThread(timeout);
            AndroidUtilities.runOnUIThread(timeout, timeoutMs);
            recognizer.startListening(intent);
        }

        private void release() {
            AndroidUtilities.cancelRunOnUIThread(timeout);
            if (recognizer != null) {
                recognizer.destroy();
                recognizer = null;
            }
            if (source != null) {
                try {
                    source.close();
                } catch (IOException ignore) {}
                source = null;
            }
        }

        private void finishWithText() {
            if (finished) return;
            final String text = TextUtils.join(" ", segments).trim();
            if (text.isEmpty()) {
                finishWithError(LocaleController.getString(R.string.ProTranscribeNothingRecognized));
                return;
            }
            finished = true;
            release();
            pcm.delete();
            callback.run(text, null);
        }

        private void finishWithError(String error) {
            if (finished) return;
            finished = true;
            release();
            pcm.delete();
            callback.run(null, error);
        }

        private void addResults(Bundle results) {
            final ArrayList<String> matches = results == null ? null : results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            if (matches != null && !matches.isEmpty() && !TextUtils.isEmpty(matches.get(0))) {
                segments.add(matches.get(0).trim());
            }
        }

        @Override
        public void onSegmentResults(Bundle segmentResults) {
            addResults(segmentResults);
        }

        @Override
        public void onEndOfSegmentedSession() {
            finishWithText();
        }

        @Override
        public void onResults(Bundle results) {
            addResults(results);
            finishWithText();
        }

        @Override
        public void onError(int error) {
            if (finished) return;
            if (!segments.isEmpty()) {
                finishWithText();
            } else if (onDevice) {
                // the on-device recognizer may not accept file input or the language; retry with the default service
                release();
                start(false);
            } else if (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE) {
                finishWithError(LocaleController.getString(R.string.ProTranscribeLanguageUnavailable));
            } else if (error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                finishWithError(LocaleController.getString(R.string.ProTranscribeNothingRecognized));
            } else {
                finishWithError(LocaleController.formatString(R.string.ProTranscribeDeviceError, error));
            }
        }

        @Override public void onReadyForSpeech(Bundle params) {}
        @Override public void onBeginningOfSpeech() {}
        @Override public void onRmsChanged(float rmsdB) {}
        @Override public void onBufferReceived(byte[] buffer) {}
        @Override public void onEndOfSpeech() {}
        @Override public void onPartialResults(Bundle partialResults) {}
        @Override public void onEvent(int eventType, Bundle params) {}
    }

    // Decodes the first audio track to 16-bit little-endian mono PCM; returns its sample rate.
    private static int decodeToMonoPcm(File input, File output) throws IOException {
        final MediaExtractor extractor = new MediaExtractor();
        MediaCodec codec = null;
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(output))) {
            extractor.setDataSource(input.getAbsolutePath());
            MediaFormat format = null;
            for (int i = 0; i < extractor.getTrackCount(); i++) {
                final MediaFormat f = extractor.getTrackFormat(i);
                final String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) {
                    extractor.selectTrack(i);
                    format = f;
                    break;
                }
            }
            if (format == null) {
                throw new IOException("No audio track in " + input);
            }
            codec = MediaCodec.createDecoderByType(format.getString(MediaFormat.KEY_MIME));
            codec.configure(format, null, null, 0);
            codec.start();

            int sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
            final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
            boolean inputDone = false;
            byte[] chunk = new byte[0];
            while (true) {
                if (!inputDone) {
                    final int inIndex = codec.dequeueInputBuffer(10_000);
                    if (inIndex >= 0) {
                        final ByteBuffer inBuffer = codec.getInputBuffer(inIndex);
                        final int size = extractor.readSampleData(inBuffer, 0);
                        if (size < 0) {
                            codec.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inputDone = true;
                        } else {
                            codec.queueInputBuffer(inIndex, 0, size, extractor.getSampleTime(), 0);
                            extractor.advance();
                        }
                    }
                }
                final int outIndex = codec.dequeueOutputBuffer(info, 10_000);
                if (outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    final MediaFormat outFormat = codec.getOutputFormat();
                    sampleRate = outFormat.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                    channels = outFormat.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                } else if (outIndex >= 0) {
                    final ByteBuffer outBuffer = codec.getOutputBuffer(outIndex);
                    if (outBuffer != null && info.size > 0) {
                        outBuffer.position(info.offset).limit(info.offset + info.size);
                        final ShortBuffer samples = outBuffer.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
                        final int frames = samples.remaining() / channels;
                        if (chunk.length < frames * 2) {
                            chunk = new byte[frames * 2];
                        }
                        for (int frame = 0; frame < frames; frame++) {
                            int sum = 0;
                            for (int c = 0; c < channels; c++) {
                                sum += samples.get();
                            }
                            final short mono = (short) (sum / channels);
                            chunk[frame * 2] = (byte) (mono & 0xff);
                            chunk[frame * 2 + 1] = (byte) ((mono >> 8) & 0xff);
                        }
                        out.write(chunk, 0, frames * 2);
                    }
                    codec.releaseOutputBuffer(outIndex, false);
                    if ((info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        break;
                    }
                }
            }
            return sampleRate;
        } finally {
            if (codec != null) {
                try {
                    codec.stop();
                } catch (Exception ignore) {}
                codec.release();
            }
            extractor.release();
        }
    }
}
