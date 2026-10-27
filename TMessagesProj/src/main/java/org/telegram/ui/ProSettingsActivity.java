package org.telegram.ui;

import static org.telegram.messenger.LocaleController.getString;

import android.content.Context;
import android.util.TypedValue;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.TextView;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import android.content.pm.PackageInfo;
import android.os.Build;
import android.text.TextUtils;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.ProMessageStore;
import org.telegram.messenger.ProTranscriber;
import org.telegram.messenger.ProTranslators;
import org.telegram.messenger.ProUpdater;
import org.telegram.messenger.R;
import org.telegram.messenger.SharedConfig;
import org.telegram.messenger.TranslateController;
import org.telegram.messenger.browser.Browser;
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
    public static final int PAGE_CONVERSATIONS = 8;
    public static final int PAGE_OTHER = 10;
    public static final int PAGE_APPEARANCE = 11;

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
    private static final int GHOST_SAVE_LAST_SEEN = 24;
    private static final int FORWARD_NO_QUOTE = 25;
    private static final int LINK_SOURCE = 26;
    private static final int LINK_RELEASES = 27;
    private static final int LINK_CHANNEL = 28;
    private static final int VERSION_PRO = 42;
    private static final int VERSION_BASE = 43;
    private static final int CLEAR_SKIPPED = 44;
    private static final int LIQUID_GLASS = 45;
    private static final String CHANNEL_USERNAME = "Channel_TelegramPro";
    private static final String REPO_URL = "https://github.com/eduardobastier/telegram-pro";
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
    public View createView(Context context) {
        View view = super.createView(context);
        actionBar.setAllowOverlayTitle(false);
        actionBar.setTitle(getTitle());
        listView.setSections();
        if (ProGlass.isEnabled()) {
            ProGlass.apply(this, (FrameLayout) view, listView);
        } else {
            actionBar.setAdaptiveBackground(listView);
        }
        return view;
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
            case PAGE_CONVERSATIONS: return getString(R.string.ProConversationsHeader);
            case PAGE_OTHER: return getString(R.string.ProOtherHeader);
            case PAGE_APPEARANCE: return getString(R.string.ProAppearanceHeader);
            default: return getString(R.string.ProSettings);
        }
    }

    @Override
    protected void fillItems(ArrayList<UItem> items, UniversalAdapter adapter) {
        switch (page) {
            case PAGE_MAIN:
                items.add(UItem.asButton(OPEN_PAGE + PAGE_CONVERSATIONS, R.drawable.msg_discussion, getString(R.string.ProConversationsHeader)));
                items.add(UItem.asButton(OPEN_PAGE + PAGE_APPEARANCE, R.drawable.msg_theme, getString(R.string.ProAppearanceHeader)));
                items.add(UItem.asButton(OPEN_PAGE + PAGE_OTHER, R.drawable.msg_fave, getString(R.string.ProOtherHeader)));
                items.add(UItem.asShadow(null));
                fillUpdates(items);
                items.add(UItem.asButton(LINK_SOURCE, R.drawable.msg_link, getString(R.string.ProSourceCode), "GitHub"));
                items.add(UItem.asButton(LINK_RELEASES, R.drawable.msg_download, getString(R.string.ProReleases), getString(R.string.ProReleasesValue)));
                items.add(UItem.asButton(LINK_CHANNEL, R.drawable.msg_channel, getString(R.string.ProChannel), "@" + CHANNEL_USERNAME));
                items.add(UItem.asShadow(null));
                break;
            case PAGE_CONVERSATIONS:
                fillTranslate(items);
                fillTranscribe(items);
                fillMessages(items);
                fillChats(items);
                break;
            case PAGE_APPEARANCE:
                fillAppearance(items);
                break;
            case PAGE_OTHER:
                fillSpeed(items);
                fillGhost(items);
                break;
        }
    }

    private void fillTranslate(ArrayList<UItem> items) {
        items.add(UItem.asHeader(getString(R.string.ProTranslateHeader)));
        items.add(UItem.asButton(TRANSLATE_STYLE, getString(R.string.ProTranslateStyle), getStyleName(SharedConfig.translateStyle)));
        items.add(UItem.asCheck(TRANSLATE_SHOW_ORIGINAL, getString(R.string.ProTranslateShowOriginal)).setChecked(SharedConfig.translateShowOriginal));
        items.add(UItem.asButton(TRANSLATE_PROVIDER, getString(R.string.ProTranslateProvider), ProTranslators.getName(SharedConfig.translateProvider)));
        items.add(UItem.asButton(TRANSLATE_TO, getString(R.string.ProTranslateTo), TranslateAlert2.capitalFirst(TranslateAlert2.languageName(TranslateAlert2.getToLanguage()))));
        items.add(UItem.asButton(DO_NOT_TRANSLATE, getString(R.string.ProDoNotTranslate), getRestrictedLanguagesValue()));
        items.add(UItem.asCheck(AUTO_TRANSLATE, getString(R.string.ProAutoTranslate)).setChecked(SharedConfig.autoTranslate));
        items.add(UItem.asShadow(getString(R.string.ProTranslateInfo)));
    }

    private void fillTranscribe(ArrayList<UItem> items) {
        items.add(UItem.asHeader(getString(R.string.ProTranscribeHeader)));
        items.add(UItem.asButton(TRANSCRIBE_PROVIDER, getString(R.string.ProTranscribeProvider), getTranscribeProviderName(SharedConfig.transcribeProvider)));
        if (SharedConfig.transcribeProvider == SharedConfig.TRANSCRIBE_PROVIDER_WORKERS_AI) {
            items.add(UItem.asButton(CLOUDFLARE_CREDENTIALS, getString(R.string.ProCloudflareCredentials), getString(ProTranscriber.hasCredentials() ? R.string.ProCloudflareSet : R.string.ProCloudflareNotSetShort)));
        }
        items.add(UItem.asCheck(AUTO_TRANSCRIBE, getString(R.string.ProAutoTranscribe)).setChecked(SharedConfig.autoTranscribe));
        items.add(UItem.asShadow(getString(R.string.ProTranscribeInfo)));
    }

    private void fillMessages(ArrayList<UItem> items) {
        items.add(UItem.asHeader(getString(R.string.ProMessagesHeader)));
        items.add(UItem.asCheck(SAVE_DELETED, getString(R.string.ProSaveDeleted)).setChecked(SharedConfig.saveDeletedMessages));
        items.add(UItem.asCheck(SAVE_EDITS, getString(R.string.ProSaveEdits)).setChecked(SharedConfig.saveEditHistory));
        items.add(UItem.asButton(CLEAR_SAVED, getString(R.string.ProClearSaved)).red());
        items.add(UItem.asShadow(getString(R.string.ProMessagesInfo)));
    }

    private void fillChats(ArrayList<UItem> items) {
        items.add(UItem.asHeader(getString(R.string.ProChatsHeader)));
        items.add(UItem.asCheck(PULL_TO_OPEN_ARCHIVE, getString(R.string.ProPullToOpenArchive)).setChecked(SharedConfig.pullToOpenArchive));
        items.add(UItem.asCheck(FORWARD_NO_QUOTE, getString(R.string.ProForwardNoQuote)).setChecked(SharedConfig.forwardNoQuoteButton));
        items.add(UItem.asShadow(getString(R.string.ProChatsInfo)));
    }

    // Same conditions Telegram uses in Power Saving > Animations for its Liquid Glass effect.
    private static boolean isLiquidGlassSupported() {
        return Build.VERSION.SDK_INT >= 33 && (SharedConfig.getDevicePerformanceClass() >= SharedConfig.PERFORMANCE_CLASS_AVERAGE || BuildVars.DEBUG_PRIVATE_VERSION);
    }

    private void fillAppearance(ArrayList<UItem> items) {
        items.add(UItem.asHeader(getString(R.string.ProAppearanceHeader)));
        UItem glass = UItem.asCheck(LIQUID_GLASS, getString(R.string.LiteOptionsLiquidGlass)).setChecked(isLiquidGlassSupported() && LiteMode.isEnabledSetting(LiteMode.FLAG_LIQUID_GLASS));
        if (!isLiquidGlassSupported()) {
            glass.subtext = getString(R.string.ProLiquidGlassUnsupported);
        }
        items.add(glass);
        items.add(UItem.asShadow(getString(R.string.ProLiquidGlassInfo)));
        if (ProGlass.isEnabled()) {
            items.add(UItem.asHeader(getString(R.string.ProGlassTransparency)));
            items.add(UItem.asIntSlideView(
                1,
                0, ProGlass.getTransparency(), 100,
                val -> val + "%",
                val -> {
                    ProGlass.setTransparency(val);
                    if (fragmentView != null) {
                        AndroidUtilities.forEachViews(fragmentView, View::invalidate);
                    }
                }
            ));
            items.add(UItem.asShadow(getString(R.string.ProGlassTransparencyInfo)));
        }
    }

    private void fillSpeed(ArrayList<UItem> items) {
        items.add(UItem.asHeader(getString(R.string.ProSpeedHeader)));
        items.add(UItem.asCheck(BOOST_DOWNLOAD, getString(R.string.ProBoostDownload)).setChecked(SharedConfig.boostDownloadSpeed));
        items.add(UItem.asCheck(BOOST_UPLOAD, getString(R.string.ProBoostUpload)).setChecked(SharedConfig.boostUploadSpeed));
        items.add(UItem.asShadow(getString(R.string.ProSpeedInfo)));
    }

    private void fillGhost(ArrayList<UItem> items) {
        items.add(UItem.asHeader(getString(R.string.ProGhostHeader)));
        items.add(UItem.asCheck(GHOST_MODE, getString(R.string.ProGhostEnable)).setChecked(SharedConfig.ghostMode));
        if (SharedConfig.ghostMode) {
            items.add(UItem.asCheck(GHOST_NO_READ, getString(R.string.ProGhostNoRead)).setChecked(SharedConfig.ghostNoRead));
            items.add(UItem.asCheck(GHOST_NO_TYPING, getString(R.string.ProGhostNoTyping)).setChecked(SharedConfig.ghostNoTyping));
            UItem offlineItem = UItem.asCheck(GHOST_OFFLINE, getString(R.string.ProGhostOffline)).setChecked(SharedConfig.ghostOffline);
            offlineItem.subtext = getString(R.string.ProGhostOfflineInfo);
            items.add(offlineItem);
            items.add(UItem.asCheck(GHOST_NO_STORY_VIEWS, getString(R.string.ProGhostNoStoryViews)).setChecked(SharedConfig.ghostNoStoryViews));
        }
        items.add(UItem.asCheck(GHOST_SAVE_LAST_SEEN, getString(R.string.ProGhostSaveLastSeen)).setChecked(SharedConfig.ghostSaveLastSeen));
        items.add(UItem.asShadow(getString(SharedConfig.ghostMode ? R.string.ProGhostOptionsInfo : R.string.ProGhostInfo)));
    }

    private void fillUpdates(ArrayList<UItem> items) {
        items.add(UItem.asHeader(getString(R.string.ProUpdateHeader)));
        items.add(UItem.asButton(VERSION_PRO, R.drawable.msg_info, getString(R.string.ProVersionPro), ProUpdater.currentVersion()));
        items.add(UItem.asButton(VERSION_BASE, R.drawable.msg_info, getString(R.string.ProVersionBase), getBaseVersion()));
        items.add(UItem.asButton(CHECK_UPDATE, R.drawable.msg_retry, getString(R.string.ProUpdateCheckNow), getLastCheck()));
        items.add(UItem.asCheck(AUTO_UPDATE, getString(R.string.ProUpdateAuto)).setChecked(SharedConfig.proAutoUpdate));
        if (!TextUtils.isEmpty(SharedConfig.proSkippedUpdate)) {
            items.add(UItem.asButton(CLEAR_SKIPPED, getString(R.string.ProUpdateSkipped), SharedConfig.proSkippedUpdate));
        }
        items.add(UItem.asShadow(getString(R.string.ProUpdateInfo)));
    }

    private String getBaseVersion() {
        try {
            PackageInfo info = ApplicationLoader.applicationContext.getPackageManager().getPackageInfo(ApplicationLoader.applicationContext.getPackageName(), 0);
            return String.format(java.util.Locale.US, "%s (%d)", info.versionName, info.versionCode / 10);
        } catch (Exception e) {
            return BuildVars.BUILD_VERSION_STRING;
        }
    }

    private String getLastCheck() {
        if (SharedConfig.proLastUpdateCheck <= 0) {
            return getString(R.string.ProUpdateNeverChecked);
        }
        return LocaleController.formatDateAudio(SharedConfig.proLastUpdateCheck / 1000, true);
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
            case LINK_CHANNEL:
                Browser.openUrl(getParentActivity(), "https://t.me/" + CHANNEL_USERNAME);
                return;
            case VERSION_PRO:
                Browser.openUrl(getParentActivity(), REPO_URL + "/releases");
                return;
            case VERSION_BASE:
                return;
            case CLEAR_SKIPPED:
                SharedConfig.setProSkippedUpdate("");
                break;
            case LIQUID_GLASS:
                if (!isLiquidGlassSupported()) {
                    BulletinFactory.of(this).createSimpleBulletin(R.raw.error, getString(R.string.ProLiquidGlassUnsupported)).show();
                    return;
                }
                LiteMode.toggleFlag(LiteMode.FLAG_LIQUID_GLASS, !LiteMode.isEnabledSetting(LiteMode.FLAG_LIQUID_GLASS));
                // The iOS look comes with Liquid Glass: iOS themes on, the user's themes back when off.
                if (LiteMode.isEnabledSetting(LiteMode.FLAG_LIQUID_GLASS)) {
                    ProIosTheme.apply();
                } else {
                    ProIosTheme.restore();
                }
                listView.adapter.update(true);
                // The effect is set up when screens are created: recreate them so it applies everywhere.
                AndroidUtilities.runOnUIThread(() -> {
                    if (getParentActivity() != null) {
                        // Come back to this screen once the app has been recreated.
                        org.telegram.messenger.MessagesController.getGlobalMainSettings().edit().putBoolean("proReopenAppearance", true).apply();
                        getParentActivity().recreate();
                    }
                }, 250);
                return;
            case GHOST_SAVE_LAST_SEEN:
                SharedConfig.setProBoolean("ghostSaveLastSeen", !SharedConfig.ghostSaveLastSeen);
                break;
            case FORWARD_NO_QUOTE:
                SharedConfig.setProBoolean("forwardNoQuoteButton", !SharedConfig.forwardNoQuoteButton);
                break;
            case LINK_SOURCE:
                Browser.openUrl(getParentActivity(), REPO_URL);
                return;
            case LINK_RELEASES:
                Browser.openUrl(getParentActivity(), REPO_URL + "/releases");
                return;
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
                SharedConfig.setProLastUpdateCheck(System.currentTimeMillis());
                ProUpdater.check(getParentActivity(), true);
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
