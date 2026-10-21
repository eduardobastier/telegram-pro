package org.telegram.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.util.TypedValue;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;

import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.ProMessageStore;
import org.telegram.messenger.ProTranscriber;
import org.telegram.messenger.ProTranslators;
import org.telegram.messenger.ProUpdater;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.TranslateController;
import org.telegram.ui.ActionBar.AlertDialog;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.TranslateAlert2;
import org.telegram.ui.Components.UItem;
import org.telegram.ui.Components.UniversalAdapter;
import org.telegram.ui.Components.UniversalFragment;

import java.util.ArrayList;
import java.util.HashSet;

public class ProSettingsActivity extends UniversalFragment {

    public static final int PAGE_MAIN = 0;
    public static final int PAGE_SPEED = 1;
    public static final int PAGE_TRANSLATE = 2;
    public static final int PAGE_TRANSCRIBE = 3;
    public static final int PAGE_GHOST = 4;
    public static final int PAGE_CHATS = 5;
    public static final int PAGE_UPDATES = 6;
    public static final int PAGE_MESSAGES = 7;

    private static final int BOOST_DOWNLOAD = 1;
    private static final int BOOST_UPLOAD = 2;
    private static final int FREE_CHAT_TRANSLATE = 3;
    private static final int TRANSLATE_STYLE = 4;
    private static final int TRANSLATE_SHOW_ORIGINAL = 5;
    private static final int TRANSLATE_PROVIDER = 6;
    private static final int TRANSLATE_TO = 7;
    private static final int DO_NOT_TRANSLATE = 8;
    private static final int AUTO_TRANSLATE = 9;
    private static final int TRANSCRIBE_PROVIDER = 10;
    private static final int CLOUDFLARE_CREDENTIALS = 11;
    private static final int AUTO_TRANSCRIBE = 12;
    private static final int AUTO_UPDATE = 13;
    private static final int CHECK_UPDATE = 14;
    private static final int GHOST_MODE = 15;
    private static final int GHOST_NO_READ = 16;
    private static final int GHOST_NO_TYPING = 17;
    private static final int GHOST_OFFLINE = 18;
    private static final int GHOST_NO_STORY_VIEWS = 19;
    private static final int PULL_TO_OPEN_ARCHIVE = 20;
    private static final int SAVE_DELETED = 21;
    private static final int SAVE_EDITS = 22;
    private static final int CLEAR_SAVED = 23;
    private static final int OPEN_PAGE = 100;

    private static final int[] TRANSCRIBE_PROVIDERS = {
        SharedConfig.TRANSCRIBE_PROVIDER_AUTO,
        SharedConfig.TRANSCRIBE_PROVIDER_DEVICE,
        SharedConfig.TRANSCRIBE_PROVIDER_WORKERS_AI,
        SharedConfig.TRANSCRIBE_PROVIDER_PREMIUM
    };

    private final int page;

    public ProSettingsActivity() {
        this(PAGE_MAIN);
    }

    public ProSettingsActivity(int page) {
        this.page = page;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (listView != null) {
            listView.adapter.update(false);
        }
    }

    @Override
    protected CharSequence getTitle() {
        switch (page) {
            case PAGE_SPEED: return getString(R.string.ProSpeedHeader);
            case PAGE_TRANSLATE: return getString(R.string.ProTranslateHeader);
            case PAGE_TRANSCRIBE: return getString(R.string.ProTranscribeHeader);
            case PAGE_GHOST: return getString(R.string.ProGhostHeader);
            case PAGE_CHATS: return getString(R.string.ProChatsHeader);
            case PAGE_UPDATES: return getString(R.string.ProUpdateHeader);
            case PAGE_MESSAGES: return getString(R.string.ProMessagesHeader);
            default: return getString(R.string.ProSettings);
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        switch (page) {
            case PAGE_MAIN:
                items.add(UItem.asButton(OPEN_PAGE + PAGE_SPEED, R.drawable.msg_speed, getString(R.string.ProSpeedHeader)));
                items.add(UItem.asButton(OPEN_PAGE + PAGE_TRANSLATE, R.drawable.msg_translate, getString(R.string.ProTranslateHeader)));
                items.add(UItem.asButton(OPEN_PAGE + PAGE_TRANSCRIBE, R.drawable.msg_voicechat, getString(R.string.ProTranscribeHeader)));
                items.add(UItem.asButton(OPEN_PAGE + PAGE_MESSAGES, R.drawable.msg_edit, getString(R.string.ProMessagesHeader)));
                items.add(UItem.asButton(OPEN_PAGE + PAGE_GHOST, R.drawable.msg_secret, getString(R.string.ProGhostHeader), getString(SharedConfig.ghostMode ? R.string.ProOn : R.string.ProOff)));
                items.add(UItem.asButton(OPEN_PAGE + PAGE_CHATS, R.drawable.msg_archive, getString(R.string.ProChatsHeader)));
                items.add(UItem.asButton(OPEN_PAGE + PAGE_UPDATES, R.drawable.msg_retry, getString(R.string.ProUpdateHeader), ProUpdater.currentVersion()));
                items.add(UItem.asShadow(null));
                break;
            case PAGE_SPEED:
                items.add(UItem.asCheck(BOOST_DOWNLOAD, getString(R.string.ProBoostDownload)).setChecked(SharedConfig.boostDownloadSpeed));
                items.add(UItem.asCheck(BOOST_UPLOAD, getString(R.string.ProBoostUpload)).setChecked(SharedConfig.boostUploadSpeed));
                items.add(UItem.asShadow(getString(R.string.ProSpeedInfo)));
                break;
            case PAGE_TRANSLATE:
                items.add(UItem.asCheck(FREE_CHAT_TRANSLATE, getString(R.string.ProFreeChatTranslate)).setChecked(SharedConfig.freeChatTranslate));
                items.add(UItem.asCheck(AUTO_TRANSLATE, getString(R.string.ProAutoTranslate)).setChecked(SharedConfig.autoTranslate));
                items.add(UItem.asShadow(null));
                items.add(UItem.asHeader(getString(R.string.ProTranslateProviderHeader)));
                items.add(UItem.asButton(TRANSLATE_PROVIDER, getString(R.string.ProTranslateProvider), ProTranslators.getName(SharedConfig.translateProvider)));
                items.add(UItem.asButton(TRANSLATE_TO, getString(R.string.ProTranslateTo), TranslateAlert2.capitalFirst(TranslateAlert2.languageName(TranslateAlert2.getToLanguage()))));
                items.add(UItem.asButton(DO_NOT_TRANSLATE, getString(R.string.ProDoNotTranslate), getRestrictedLanguagesValue()));
                items.add(UItem.asShadow(null));
                items.add(UItem.asHeader(getString(R.string.ProTranslateLookHeader)));
                items.add(UItem.asButton(TRANSLATE_STYLE, getString(R.string.ProTranslateStyle), getStyleName(SharedConfig.translateStyle)));
                items.add(UItem.asCheck(TRANSLATE_SHOW_ORIGINAL, getString(R.string.ProTranslateShowOriginal)).setChecked(SharedConfig.translateShowOriginal));
                items.add(UItem.asShadow(getString(R.string.ProTranslateInfo)));
                break;
            case PAGE_TRANSCRIBE:
                items.add(UItem.asButton(TRANSCRIBE_PROVIDER, getString(R.string.ProTranscribeProvider), getTranscribeProviderName(SharedConfig.transcribeProvider)));
                if (SharedConfig.transcribeProvider == SharedConfig.TRANSCRIBE_PROVIDER_WORKERS_AI) {
                    items.add(UItem.asButton(CLOUDFLARE_CREDENTIALS, getString(R.string.ProCloudflareCredentials), getString(ProTranscriber.hasCredentials() ? R.string.ProCloudflareSet : R.string.ProCloudflareNotSetShort)));
                }
                items.add(UItem.asCheck(AUTO_TRANSCRIBE, getString(R.string.ProAutoTranscribe)).setChecked(SharedConfig.autoTranscribe));
                items.add(UItem.asShadow(getString(R.string.ProTranscribeInfo)));
                break;
            case PAGE_GHOST:
                items.add(UItem.asCheck(GHOST_MODE, getString(R.string.ProGhostEnable)).setChecked(SharedConfig.ghostMode));
                items.add(UItem.asShadow(getString(R.string.ProGhostInfo)));
                if (SharedConfig.ghostMode) {
                    items.add(UItem.asHeader(getString(R.string.ProGhostOptionsHeader)));
                    items.add(UItem.asCheck(GHOST_NO_READ, getString(R.string.ProGhostNoRead)).setChecked(SharedConfig.ghostNoRead));
                    items.add(UItem.asCheck(GHOST_NO_TYPING, getString(R.string.ProGhostNoTyping)).setChecked(SharedConfig.ghostNoTyping));
                    items.add(UItem.asCheck(GHOST_OFFLINE, getString(R.string.ProGhostOffline)).setChecked(SharedConfig.ghostOffline));
                    items.add(UItem.asCheck(GHOST_NO_STORY_VIEWS, getString(R.string.ProGhostNoStoryViews)).setChecked(SharedConfig.ghostNoStoryViews));
                    items.add(UItem.asShadow(getString(R.string.ProGhostOptionsInfo)));
                }
                break;
            case PAGE_MESSAGES:
                items.add(UItem.asCheck(SAVE_DELETED, getString(R.string.ProSaveDeleted)).setChecked(SharedConfig.saveDeletedMessages));
                items.add(UItem.asCheck(SAVE_EDITS, getString(R.string.ProSaveEdits)).setChecked(SharedConfig.saveEditHistory));
                items.add(UItem.asShadow(getString(R.string.ProMessagesInfo)));
                items.add(UItem.asButton(CLEAR_SAVED, getString(R.string.ProClearSaved)).red());
                items.add(UItem.asShadow(null));
                break;
            case PAGE_CHATS:
                items.add(UItem.asCheck(PULL_TO_OPEN_ARCHIVE, getString(R.string.ProPullToOpenArchive)).setChecked(SharedConfig.pullToOpenArchive));
                items.add(UItem.asShadow(getString(R.string.ProPullToOpenArchiveInfo)));
                break;
            case PAGE_UPDATES:
                items.add(UItem.asCheck(AUTO_UPDATE, getString(R.string.ProUpdateAuto)).setChecked(SharedConfig.proAutoUpdate));
                items.add(UItem.asButton(CHECK_UPDATE, getString(R.string.ProUpdateCheckNow), ProUpdater.currentVersion()));
                items.add(UItem.asShadow(getString(R.string.ProUpdateInfo)));
                break;
        }
    }

    @Override
    protected void onClick(UItem item, View view, int position, float x, float y) {
        if (item.id > OPEN_PAGE) {
            presentFragment(new ProSettingsActivity(item.id - OPEN_PAGE));
            return;
        }
        switch (item.id) {
            case GHOST_MODE:
                SharedConfig.setProBoolean("ghostMode", !SharedConfig.ghostMode);
                break;
            case GHOST_NO_READ:
                SharedConfig.setProBoolean("ghostNoRead", !SharedConfig.ghostNoRead);
                break;
            case GHOST_NO_TYPING:
                SharedConfig.setProBoolean("ghostNoTyping", !SharedConfig.ghostNoTyping);
                break;
            case GHOST_OFFLINE:
                SharedConfig.setProBoolean("ghostOffline", !SharedConfig.ghostOffline);
                break;
            case GHOST_NO_STORY_VIEWS:
                SharedConfig.setProBoolean("ghostNoStoryViews", !SharedConfig.ghostNoStoryViews);
                break;
            case SAVE_DELETED:
                SharedConfig.setProBoolean("saveDeletedMessages", !SharedConfig.saveDeletedMessages);
                break;
            case SAVE_EDITS:
                SharedConfig.setProBoolean("saveEditHistory", !SharedConfig.saveEditHistory);
                break;
            case CLEAR_SAVED:
                ProMessageStore.clearAll();
                BulletinFactory.of(this).createSimpleBulletin(R.raw.done, getString(R.string.ProClearSavedDone)).show();
                return;
            case PULL_TO_OPEN_ARCHIVE:
                SharedConfig.setProBoolean("pullToOpenArchive", !SharedConfig.pullToOpenArchive);
                if (SharedConfig.pullToOpenArchive && !SharedConfig.archiveHidden) {
                    SharedConfig.toggleArchiveHidden();
                    NotificationCenter.getInstance(currentAccount).postNotificationName(NotificationCenter.dialogsNeedReload);
                }
                break;
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
            case AUTO_TRANSCRIBE:
                SharedConfig.toggleAutoTranscribe();
                break;
            case AUTO_UPDATE:
                SharedConfig.toggleProAutoUpdate();
                break;
            case CHECK_UPDATE:
                ProUpdater.check(getParentActivity(), true);
                return;
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
            case TRANSCRIBE_PROVIDER:
                showChoice(getString(R.string.ProTranscribeProvider), new CharSequence[] {
                    getTranscribeProviderName(SharedConfig.TRANSCRIBE_PROVIDER_AUTO),
                    getTranscribeProviderName(SharedConfig.TRANSCRIBE_PROVIDER_DEVICE),
                    getTranscribeProviderName(SharedConfig.TRANSCRIBE_PROVIDER_WORKERS_AI),
                    getTranscribeProviderName(SharedConfig.TRANSCRIBE_PROVIDER_PREMIUM)
                }, which -> SharedConfig.setTranscribeProvider(TRANSCRIBE_PROVIDERS[which]));
                return;
            case CLOUDFLARE_CREDENTIALS:
                showCloudflareCredentials();
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

    private void showCloudflareCredentials() {
        if (getParentActivity() == null) {
            return;
        }
        final LinearLayout layout = new LinearLayout(getParentActivity());
        layout.setOrientation(LinearLayout.VERTICAL);
        final EditText accountId = createInput(getString(R.string.ProCloudflareAccountId), SharedConfig.cfAccountId);
        final EditText apiToken = createInput(getString(R.string.ProCloudflareApiToken), SharedConfig.cfApiToken);
        layout.addView(accountId, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 8, 24, 0));
        layout.addView(apiToken, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 24, 8, 24, 0));

        AlertDialog.Builder builder = new AlertDialog.Builder(getParentActivity());
        builder.setTitle(getString(R.string.ProCloudflareCredentials));
        builder.setView(layout);
        builder.setPositiveButton(getString(R.string.OK), (dialog, which) -> {
            SharedConfig.setCloudflareCredentials(accountId.getText().toString().trim(), apiToken.getText().toString().trim());
            listView.adapter.update(true);
        });
        builder.setNegativeButton(getString(R.string.Cancel), null);
        showDialog(builder.create());
    }

    private EditText createInput(String hint, String value) {
        final EditText editText = new EditText(getParentActivity());
        editText.setHint(hint);
        editText.setText(value);
        editText.setSingleLine(true);
        editText.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 16);
        editText.setTextColor(Theme.getColor(Theme.key_dialogTextBlack));
        editText.setHintTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteHintText));
        return editText;
    }

    private static String getTranscribeProviderName(int provider) {
        switch (provider) {
            case SharedConfig.TRANSCRIBE_PROVIDER_PREMIUM:
                return getString(R.string.ProTranscribePremium);
            case SharedConfig.TRANSCRIBE_PROVIDER_WORKERS_AI:
                return getString(R.string.ProTranscribeWorkersAi);
            case SharedConfig.TRANSCRIBE_PROVIDER_DEVICE:
                return getString(R.string.ProTranscribeDevice);
            default:
                return getString(R.string.ProTranscribeAuto);
        }
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
