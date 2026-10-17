package org.telegram.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.view.View;

import org.telegram.messenger.ProTranslators;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.TranslateController;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.Components.TranslateAlert2;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;
import java.util.HashSet;

public class ProSettingsActivity extends UniversalFragment {

    private static final int BOOST_DOWNLOAD = 1;
    private static final int BOOST_UPLOAD = 2;
    private static final int FREE_CHAT_TRANSLATE = 3;
    private static final int TRANSLATE_STYLE = 4;
    private static final int TRANSLATE_SHOW_ORIGINAL = 5;
    private static final int TRANSLATE_PROVIDER = 6;
    private static final int TRANSLATE_TO = 7;
    private static final int DO_NOT_TRANSLATE = 8;
    private static final int AUTO_TRANSLATE = 9;

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null) {
            listView.adapter.update(false);
        }
    }

    @Override
    protected CharSequence getTitle() {
        return getString(R.string.ProSettings);
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        items.add(UItem.asHeader(getString(R.string.ProSpeedHeader)));
        items.add(UItem.asCheck(BOOST_DOWNLOAD, getString(R.string.ProBoostDownload)).setChecked(SharedConfig.boostDownloadSpeed));
        items.add(UItem.asCheck(BOOST_UPLOAD, getString(R.string.ProBoostUpload)).setChecked(SharedConfig.boostUploadSpeed));
        items.add(UItem.asShadow(getString(R.string.ProSpeedInfo)));

        items.add(UItem.asHeader(getString(R.string.ProTranslateHeader)));
        items.add(UItem.asCheck(FREE_CHAT_TRANSLATE, getString(R.string.ProFreeChatTranslate)).setChecked(SharedConfig.freeChatTranslate));
        items.add(UItem.asButton(TRANSLATE_STYLE, getString(R.string.ProTranslateStyle), getStyleName(SharedConfig.translateStyle)));
        items.add(UItem.asCheck(TRANSLATE_SHOW_ORIGINAL, getString(R.string.ProTranslateShowOriginal)).setChecked(SharedConfig.translateShowOriginal));
        items.add(UItem.asButton(TRANSLATE_PROVIDER, getString(R.string.ProTranslateProvider), ProTranslators.getName(SharedConfig.translateProvider)));
        items.add(UItem.asButton(TRANSLATE_TO, getString(R.string.ProTranslateTo), TranslateAlert2.capitalFirst(TranslateAlert2.languageName(TranslateAlert2.getToLanguage()))));
        items.add(UItem.asButton(DO_NOT_TRANSLATE, getString(R.string.ProDoNotTranslate), getRestrictedLanguagesValue()));
        items.add(UItem.asCheck(AUTO_TRANSLATE, getString(R.string.ProAutoTranslate)).setChecked(SharedConfig.autoTranslate));
        items.add(UItem.asShadow(getString(R.string.ProTranslateInfo)));
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        switch (item.id) {
            case BOOST_DOWNLOAD:
                SharedConfig.toggleBoostDownloadSpeed();
                break;
            case BOOST_UPLOAD:
                SharedConfig.toggleBoostUploadSpeed();
                break;
            case FREE_CHAT_TRANSLATE:
                SharedConfig.toggleFreeChatTranslate();
                break;
            case TRANSLATE_SHOW_ORIGINAL:
                SharedConfig.toggleTranslateShowOriginal();
                break;
            case AUTO_TRANSLATE:
                SharedConfig.toggleAutoTranslate();
                break;
            case TRANSLATE_STYLE:
                showChoice(getString(R.string.ProTranslateStyle), new CharSequence[] {
                    getStyleName(SharedConfig.TRANSLATE_STYLE_POPUP),
                    getStyleName(SharedConfig.TRANSLATE_STYLE_INLINE)
                }, SharedConfig::setTranslateStyle);
                return;
            case TRANSLATE_PROVIDER:
                final CharSequence[] providerNames = new CharSequence[ProTranslators.PROVIDERS.length];
                for (int i = 0; i < providerNames.length; ++i) {
                    providerNames[i] = ProTranslators.getName(ProTranslators.PROVIDERS[i]);
                }
                showChoice(getString(R.string.ProTranslateProvider), providerNames, which -> SharedConfig.setTranslateProvider(ProTranslators.PROVIDERS[which]));
                return;
            case TRANSLATE_TO:
                showLanguageChoice();
                return;
            case DO_NOT_TRANSLATE:
                presentFragment(new RestrictedLanguagesSelectActivity());
                return;
            default:
                return;
        }
        listView.adapter.update(true);
    }

    private interface IntSetter {
        void set(int value);
    }

    private void showChoice(CharSequence title, CharSequence[] options, IntSetter setter) {
        if (getParentActivity() == null) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(title);
        builder.setItems(options, (dialog, which) -> {
            setter.set(which);
            listView.adapter.update(true);
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private void showLanguageChoice() {
        if (getParentActivity() == null) {
            return;
        }
        final ArrayList<TranslateController.Language> languages = TranslateController.getLanguages();
        final CharSequence[] names = new CharSequence[languages.size()];
        for (int i = 0; i < languages.size(); ++i) {
            names[i] = languages.get(i).displayName;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString(R.string.ProTranslateTo));
        builder.setItems(names, (dialog, which) -> {
            TranslateAlert2.setToLanguage(languages.get(which).code);
            listView.adapter.update(true);
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private static String getStyleName(int style) {
        return getString(style == SharedConfig.TRANSLATE_STYLE_INLINE ? R.string.ProTranslateStyleInline : R.string.ProTranslateStylePopup);
    }


    private static String getRestrictedLanguagesValue() {
        final HashSet<String> languages = RestrictedLanguagesSelectActivity.getRestrictedLanguages();
        if (languages.size() == 1) {
            return TranslateAlert2.capitalFirst(TranslateAlert2.languageName(languages.iterator().next()));
        }
        return String.valueOf(languages.size());
    }

    @Override
    protected boolean onLongClick(UItem item, View view, int position, float x, float y) {
        return false;
    }
}
