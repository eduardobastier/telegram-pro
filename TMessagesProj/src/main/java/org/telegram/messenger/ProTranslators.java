package org.telegram.messenger;

import java.util.concurrent.TimeUnit;

import app.nekogram.translator.BaiduTranslator;
import app.nekogram.translator.BaseTranslator;
import app.nekogram.translator.GoogleAppTranslator;
import app.nekogram.translator.LingoTranslator;
import app.nekogram.translator.MicrosoftTranslator;
import app.nekogram.translator.SogouTranslator;
import app.nekogram.translator.TranslatorConfig;
import app.nekogram.translator.YandexTranslator;
import app.nekogram.translator.exception.Http429Exception;
import okhttp3.OkHttpClient;

// Translation providers from Nekogram's translator library (app.nekogram.translator, GPLv3)
public class ProTranslators {

    public static final int[] PROVIDERS = {
        SharedConfig.TRANSLATE_PROVIDER_GOOGLE,
        SharedConfig.TRANSLATE_PROVIDER_MICROSOFT,
        SharedConfig.TRANSLATE_PROVIDER_YANDEX,
        SharedConfig.TRANSLATE_PROVIDER_LINGO,
        SharedConfig.TRANSLATE_PROVIDER_BAIDU,
        SharedConfig.TRANSLATE_PROVIDER_SOGOU,
        SharedConfig.TRANSLATE_PROVIDER_TELEGRAM
    };

    private static boolean configured;

    public static BaseTranslator get(int provider) {
        if (!configured) {
            TranslatorConfig.setCallFactory(new OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build());
            configured = true;
        }
        switch (provider) {
            case SharedConfig.TRANSLATE_PROVIDER_GOOGLE:
                return GoogleAppTranslator.getInstance();
            case SharedConfig.TRANSLATE_PROVIDER_MICROSOFT:
                return MicrosoftTranslator.getInstance();
            case SharedConfig.TRANSLATE_PROVIDER_YANDEX:
                return YandexTranslator.getInstance();
            case SharedConfig.TRANSLATE_PROVIDER_LINGO:
                return LingoTranslator.getInstance();
            case SharedConfig.TRANSLATE_PROVIDER_BAIDU:
                return BaiduTranslator.getInstance();
            case SharedConfig.TRANSLATE_PROVIDER_SOGOU:
                return SogouTranslator.getInstance();
            default:
                return null;
        }
    }

    public static String getName(int provider) {
        switch (provider) {
            case SharedConfig.TRANSLATE_PROVIDER_MICROSOFT:
                return "Microsoft Translator";
            case SharedConfig.TRANSLATE_PROVIDER_YANDEX:
                return "Yandex Translate";
            case SharedConfig.TRANSLATE_PROVIDER_LINGO:
                return "Lingocloud";
            case SharedConfig.TRANSLATE_PROVIDER_BAIDU:
                return "Baidu Translate";
            case SharedConfig.TRANSLATE_PROVIDER_SOGOU:
                return "Sogou Translate";
            case SharedConfig.TRANSLATE_PROVIDER_TELEGRAM:
                return LocaleController.getString(R.string.ProTranslateProviderTelegram);
            default:
                return LocaleController.getString(R.string.ProTranslateProviderGoogle);
        }
    }

    // Runs on a background thread; done is called on the UI thread with (translation, rateLimited).
    // translation is null when the provider failed or does not support the target language.
    public static boolean translate(String text, String toLanguage, Utilities.Callback2<String, Boolean> done) {
        final BaseTranslator translator = get(SharedConfig.translateProvider);
        if (translator == null || !translator.supportLanguage(toLanguage)) {
            return false;
        }
        new Thread(() -> {
            String translation = null;
            boolean rateLimit = false;
            try {
                translation = translator.translate(text, null, toLanguage).getTranslation();
            } catch (Exception e) {
                rateLimit = e instanceof Http429Exception;
                FileLog.e(e);
            }
            final String finalTranslation = translation;
            final boolean finalRateLimit = rateLimit;
            AndroidUtilities.runOnUIThread(() -> done.run(finalTranslation, finalRateLimit));
        }).start();
        return true;
    }
}
