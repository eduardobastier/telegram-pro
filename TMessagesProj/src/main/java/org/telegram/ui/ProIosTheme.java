package org.telegram.ui;

import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;
import org.telegram.messenger.MessagesController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.ui.ActionBar.Theme;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;

// iOS look for Liquid Glass: light and dark themes with Apple's system colors, applied when the option
// is turned on; the user's previous themes come back when it is turned off.
public class ProIosTheme {

    private static final String LIGHT = "iOS Light";
    private static final String DARK = "iOS Dark";

    // iOS system colors (light).
    private static final String[][] LIGHT_COLORS = {
        // Values from Telegram iOS DefaultDayPresentationTheme ("Day", the default theme).
        {"windowBackgroundGray", "#EFEFF4"}, {"windowBackgroundWhite", "#FFFFFF"},
        {"windowBackgroundWhiteBlackText", "#000000"}, {"windowBackgroundWhiteGrayText", "#8E8E93"},
        {"windowBackgroundWhiteGrayText2", "#8E8E93"}, {"windowBackgroundWhiteGrayText3", "#8E8E93"},
        {"windowBackgroundWhiteGrayText4", "#8E8E93"}, {"windowBackgroundWhiteGrayText8", "#8E8E93"},
        {"windowBackgroundWhiteValueText", "#8E8E93"}, {"windowBackgroundWhiteBlueHeader", "#6D6D72"},
        {"windowBackgroundWhiteBlueText", "#0088FF"}, {"windowBackgroundWhiteBlueText2", "#0088FF"},
        {"windowBackgroundWhiteBlueText4", "#0088FF"}, {"windowBackgroundWhiteLinkText", "#0088FF"},
        {"windowBackgroundWhiteBlueButton", "#0088FF"}, {"windowBackgroundWhiteBlueIcon", "#0088FF"},
        {"windowBackgroundWhiteGrayIcon", "#8E8E93"}, {"windowBackgroundWhiteRedText", "#FF3B30"},
        {"windowBackgroundWhiteGreenText", "#00C900"}, {"divider", "#C8C7CC"},
        {"actionBarDefault", "#F9F9F9"}, {"actionBarDefaultIcon", "#000000"},
        {"actionBarDefaultTitle", "#000000"}, {"actionBarDefaultSubtitle", "#8E8E93"},
        {"actionBarDefaultSelector", "#1F0088FF"}, {"actionBarActionModeDefaultIcon", "#0088FF"},
        {"actionBarDefaultSubmenuBackground", "#FFFFFF"}, {"actionBarDefaultSubmenuItem", "#000000"},
        {"actionBarDefaultSubmenuItemIcon", "#000000"}, {"actionBarTabActiveText", "#0088FF"},
        {"actionBarTabLine", "#0088FF"}, {"actionBarTabUnactiveText", "#8E8E93"},
        {"switchTrack", "#E9E9EA"}, {"switchTrackChecked", "#35C759"},
        {"switchTrackBlue", "#E9E9EA"}, {"switchTrackBlueChecked", "#35C759"},
        {"switch2TrackChecked", "#35C759"}, {"checkboxSquareBackground", "#0088FF"},
        {"radioBackgroundChecked", "#0088FF"}, {"featuredStickers_addButton", "#0088FF"},
        {"dialogBackground", "#FFFFFF"}, {"dialogTextBlack", "#000000"}, {"dialogTextGray", "#8E8E93"},
        {"dialogTextBlue", "#0088FF"}, {"dialogTextBlue2", "#0088FF"}, {"dialogButton", "#0088FF"},
        {"dialogTextLink", "#0088FF"}, {"dialogRedIcon", "#FF3B30"}, {"text_RedRegular", "#FF3B30"},
        {"chats_name", "#000000"}, {"chats_message", "#8E8E93"}, {"chats_date", "#8E8E93"},
        {"chats_draft", "#DD4B39"}, {"chats_unreadCounter", "#0088FF"}, {"chats_unreadCounterMuted", "#B6B6BB"},
        {"chats_actionBackground", "#0088FF"}, {"chats_menuBackground", "#FFFFFF"},
        {"chats_pinnedIcon", "#B6B6BB"}, {"chats_pinnedOverlay", "#F7F7F7"}, {"chats_muteIcon", "#A7A7AD"},
        {"chats_onlineCircle", "#4CC91F"}, {"chats_secretName", "#00B12C"}, {"chats_secretIcon", "#00B12C"},
        {"chats_sentCheck", "#0088FF"}, {"chats_sentReadCheck", "#0088FF"}, {"chats_nameMessage", "#000000"}, {"chats_nameMessage_threeLines", "#000000"}, {"chats_verifiedBackground", "#0088FF"},
        // Bubbles: Telegram iOS "Day Classic" (default): incoming white, outgoing #E1FFC7 with black text.
        {"chat_inBubble", "#FFFFFF"}, {"chat_outBubble", "#E1FFC7"}, {"chat_outBubbleGradient", "#E1FFC7"},
        {"chat_outBubbleSelected", "#BAFF93"}, {"chat_outBubbleShadow", "#00000000"},
        {"chat_messageTextIn", "#000000"}, {"chat_messageTextOut", "#000000"},
        {"chat_messageLinkIn", "#004BAD"}, {"chat_messageLinkOut", "#004BAD"},
        {"chat_inTimeText", "#8E8E93"}, {"chat_outTimeText", "#CC008C09"},
        {"chat_outSentCheck", "#19C700"}, {"chat_outSentCheckRead", "#19C700"}, {"chat_outSentClock", "#CC008C09"},
        {"chat_outViews", "#CC008C09"}, {"chat_outMenu", "#CC008C09"},
        {"chat_outReplyLine", "#3FC33B"}, {"chat_outReplyNameText", "#00A700"},
        {"chat_outReplyMessageText", "#000000"}, {"chat_outReplyMediaMessageText", "#CC008C09"},
        {"chat_outForwardedNameText", "#00A700"}, {"chat_outViaBotNameText", "#00A700"},
        {"chat_outFileNameText", "#3FAA3C"}, {"chat_outFileInfoText", "#6FB26A"},
        {"chat_outAudioTitleText", "#3FAA3C"}, {"chat_outAudioPerfomerText", "#6FB26A"},
        {"chat_outAudioDurationText", "#CC008C09"}, {"chat_outContactNameText", "#3FAA3C"},
        {"chat_outVenueInfoText", "#6FB26A"}, {"chat_outSiteNameText", "#00A700"},
        {"chat_outPreviewLine", "#3FC33B"}, {"chat_outInstant", "#3FC33B"},
        {"chat_outLoader", "#3FC33B"}, {"chat_outFileBackground", "#D2F2B6"},
        {"chat_outAudioSeekbarFill", "#3FC33B"}, {"chat_outAudioSeekbar", "#93D987"},
        {"chat_outVoiceSeekbarFill", "#3FC33B"}, {"chat_outVoiceSeekbar", "#93D987"},
        {"chat_outAdminText", "#CC008C09"}, {"chat_outPollCorrectAnswer", "#00A700"},
        {"chat_outReactionButtonBackground", "#1F3FC33B"}, {"chat_outReactionButtonText", "#3FC33B"},
        {"chat_inReplyLine", "#0088FF"}, {"chat_inReplyNameText", "#0088FF"},
        {"chat_inForwardedNameText", "#0088FF"}, {"chat_inViaBotNameText", "#0088FF"},
        {"chat_messagePanelBackground", "#F9F9F9"}, {"chat_messagePanelIcons", "#858E99"},
        {"chat_messagePanelHint", "#B6B6BB"}, {"chat_messagePanelSend", "#0088FF"},
        {"chat_messagePanelVoiceBackground", "#0088FF"},
        {"chat_topPanelBackground", "#FFFFFF"}, {"chat_topPanelTitle", "#0088FF"}, {"chat_topPanelLine", "#0088FF"},
        {"chat_topPanelMessage", "#8E8E93"}, {"chat_topPanelClose", "#8E8E93"},
        {"glass_tabSelected", "#0088FF"}, {"glass_tabSelectedText", "#0088FF"}, {"glass_tabUnselected", "#959595"},
        {"glass_targetMainTabs", "#FFFFFF"}, {"glass_targetMainTopPanel", "#FFFFFF"},
    };

    // iOS system colors (dark), close to Telegram iOS "Night".
    private static final String[][] DARK_COLORS = {
        {"windowBackgroundGray", "#050506"}, {"windowBackgroundWhite", "#111113"},
        {"windowBackgroundWhiteBlackText", "#FFFFFF"}, {"windowBackgroundWhiteGrayText", "#98989E"},
        {"windowBackgroundWhiteGrayText2", "#98989E"}, {"windowBackgroundWhiteGrayText3", "#98989E"},
        {"windowBackgroundWhiteGrayText4", "#98989E"}, {"windowBackgroundWhiteGrayText8", "#98989E"},
        {"windowBackgroundWhiteValueText", "#98989E"}, {"windowBackgroundWhiteBlueHeader", "#8D8D93"},
        {"windowBackgroundWhiteBlueText", "#0A84FF"}, {"windowBackgroundWhiteBlueText2", "#0A84FF"},
        {"windowBackgroundWhiteBlueText4", "#0A84FF"}, {"windowBackgroundWhiteLinkText", "#0A84FF"},
        {"windowBackgroundWhiteBlueButton", "#0A84FF"}, {"windowBackgroundWhiteBlueIcon", "#0A84FF"},
        {"windowBackgroundWhiteGrayIcon", "#98989E"}, {"windowBackgroundWhiteRedText", "#FF453A"},
        {"windowBackgroundWhiteGreenText", "#30D158"}, {"divider", "#38383A"},
        {"actionBarDefault", "#111113"}, {"actionBarDefaultIcon", "#FFFFFF"},
        {"actionBarDefaultTitle", "#FFFFFF"}, {"actionBarDefaultSubtitle", "#98989E"},
        {"actionBarDefaultSelector", "#1F0A84FF"}, {"actionBarActionModeDefaultIcon", "#0A84FF"},
        {"actionBarDefaultSubmenuBackground", "#1F1F21"}, {"actionBarDefaultSubmenuItem", "#FFFFFF"},
        {"actionBarDefaultSubmenuItemIcon", "#FFFFFF"}, {"actionBarTabActiveText", "#0A84FF"},
        {"actionBarTabLine", "#0A84FF"}, {"actionBarTabUnactiveText", "#98989E"},
        {"switchTrack", "#39393D"}, {"switchTrackChecked", "#30D158"},
        {"switchTrackBlue", "#39393D"}, {"switchTrackBlueChecked", "#30D158"},
        {"switch2TrackChecked", "#30D158"}, {"checkboxSquareBackground", "#0A84FF"},
        {"radioBackgroundChecked", "#0A84FF"}, {"featuredStickers_addButton", "#0A84FF"},
        {"dialogBackground", "#1F1F21"}, {"dialogTextBlack", "#FFFFFF"}, {"dialogTextGray", "#98989E"},
        {"dialogTextBlue", "#0A84FF"}, {"dialogTextBlue2", "#0A84FF"}, {"dialogButton", "#0A84FF"},
        {"dialogTextLink", "#0A84FF"}, {"dialogRedIcon", "#FF453A"}, {"text_RedRegular", "#FF453A"},
        {"chats_name", "#FFFFFF"}, {"chats_message", "#98989E"}, {"chats_date", "#98989E"},
        {"chats_unreadCounter", "#0A84FF"}, {"chats_unreadCounterMuted", "#48484A"},
        {"chats_actionBackground", "#0A84FF"}, {"chats_menuBackground", "#111113"},
        {"chats_pinnedIcon", "#48484A"}, {"chats_onlineCircle", "#30D158"},
        {"chats_sentCheck", "#0A84FF"}, {"chats_sentReadCheck", "#0A84FF"}, {"chats_nameMessage", "#FFFFFF"}, {"chats_nameMessage_threeLines", "#FFFFFF"},
        {"chat_inBubble", "#1E1E20"}, {"chat_outBubble", "#2B63CC"}, {"chat_outBubbleGradient", "#2B63CC"}, {"chat_outBubbleSelected", "#2557B5"},
        {"chat_outTimeText", "#B3FFFFFF"}, {"chat_outViews", "#B3FFFFFF"}, {"chat_outReplyLine", "#FFFFFF"},
        {"chat_outReplyNameText", "#FFFFFF"}, {"chat_outReplyMessageText", "#FFFFFF"}, {"chat_outForwardedNameText", "#FFFFFF"},
        {"chat_outPreviewLine", "#FFFFFF"}, {"chat_outSiteNameText", "#FFFFFF"}, {"chat_outBubbleShadow", "#00000000"},
        {"chat_messagePanelBackground", "#111113"}, {"chat_messagePanelIcons", "#98989E"},
        {"chat_messagePanelSend", "#0A84FF"}, {"chat_messagePanelVoiceBackground", "#0A84FF"},
        {"chat_messageTextIn", "#FFFFFF"}, {"chat_messageTextOut", "#FFFFFF"},
        {"chat_outSentCheckRead", "#FFFFFF"}, {"chat_outSentCheck", "#FFFFFF"},
        {"chat_inTimeText", "#98989E"},
        {"chat_messageLinkIn", "#0A84FF"}, {"chat_messageLinkOut", "#FFFFFF"},
        {"chat_inReplyLine", "#0A84FF"}, {"chat_inReplyNameText", "#0A84FF"},
        {"chat_inForwardedNameText", "#0A84FF"},
        {"chat_topPanelBackground", "#111113"}, {"chat_topPanelTitle", "#0A84FF"}, {"chat_topPanelLine", "#0A84FF"},
        {"chat_topPanelMessage", "#98989E"}, {"chat_topPanelClose", "#98989E"},
        {"glass_defaultIcon", "#E6FFFFFF"}, {"glass_defaultText", "#E6FFFFFF"},
        {"glass_tabSelected", "#FFFFFF"}, {"glass_tabSelectedText", "#FFFFFF"}, {"glass_tabUnselected", "#FFFFFF"},
        {"glass_targetMainTabs", "#111113"}, {"glass_targetMainTopPanel", "#111113"}, {"undo_background", "#1F1F21"},
    };

    private static SharedPreferences prefs() {
        return MessagesController.getGlobalMainSettings();
    }

    // baseAsset: a complete Telegram theme from assets used for every color not listed in colors.
    private static File write(String name, String[][] colors, String baseAsset) throws Exception {
        final java.util.LinkedHashMap<String, String> values = new java.util.LinkedHashMap<>();
        if (baseAsset != null) {
            try (java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(
                    ApplicationLoader.applicationContext.getAssets().open(baseAsset), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("WPS") || line.startsWith("WLS")) {
                        break;
                    }
                    final int eq = line.indexOf('=');
                    if (eq > 0) {
                        values.put(line.substring(0, eq), line.substring(eq + 1));
                    }
                }
            }
        }
        if (colors == DARK_COLORS) {
            // No solid wallpaper color: the dark theme uses Telegram's default pattern (drawn darkened).
            values.keySet().removeIf(key -> key.startsWith("chat_wallpaper"));
        }
        for (String[] c : colors) {
            values.put(c[0], String.valueOf(android.graphics.Color.parseColor(c[1])));
        }
        final StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<String, String> e : values.entrySet()) {
            sb.append(e.getKey()).append('=').append(e.getValue()).append('\n');
        }
        final File file = new File(ApplicationLoader.applicationContext.getCacheDir(), name + ".attheme");
        try (FileOutputStream out = new FileOutputStream(file)) {
            out.write(sb.toString().getBytes(StandardCharsets.UTF_8));
        }
        return file;
    }

    // Bump when the colors above change, so installed iOS themes get regenerated on the next start.
    private static final int VERSION = 10;

    // Call at startup: refresh the iOS themes if Liquid Glass is on and they are outdated.
    public static void ensureUpToDate() {
        try {
            // Never rewrite a theme that is already installed: the user's current theme must survive updates.
            if (!ProGlass.isEnabled() || prefs().getInt("proIosThemeVersion", 0) != 0) {
                return;
            }
            apply();
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    // Call when Liquid Glass is turned on.
    public static void apply() {
        try {
            final Theme.ThemeInfo previousDay = Theme.getCurrentTheme();
            final Theme.ThemeInfo previousNight = Theme.getCurrentNightTheme();
            final boolean wasDark = Theme.isCurrentThemeDark();
            if (previousDay != null && !previousDay.getKey().startsWith(LIGHT)) {
                prefs().edit()
                    .putString("proPrevDayTheme", previousDay.getKey())
                    .putString("proPrevNightTheme", previousNight != null ? previousNight.getKey() : "")
                    .apply();
            }
            final Theme.ThemeInfo light = Theme.installThemeFile(write(LIGHT, LIGHT_COLORS, null), LIGHT);
            final Theme.ThemeInfo dark = Theme.installThemeFile(write(DARK, DARK_COLORS, "night.attheme"), DARK);
            if (dark != null) {
                Theme.setCurrentNightTheme(dark);
            }
            prefs().edit().putInt("proIosThemeVersion", VERSION).apply();
            final Theme.ThemeInfo target = wasDark && dark != null ? dark : light;
            if (target != null) {
                Theme.applyTheme(target, target == dark);
                NotificationCenter.getGlobalInstance().postNotificationName(NotificationCenter.themeListUpdated);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    // Call when Liquid Glass is turned off: bring the user's themes back.
    public static void restore() {
        try {
            final Theme.ThemeInfo day = Theme.getTheme(prefs().getString("proPrevDayTheme", ""));
            final Theme.ThemeInfo night = Theme.getTheme(prefs().getString("proPrevNightTheme", ""));
            if (night != null) {
                Theme.setCurrentNightTheme(night);
            }
            final boolean dark = Theme.isCurrentThemeDark();
            final Theme.ThemeInfo target = dark && night != null ? night : day;
            if (target != null) {
                Theme.applyTheme(target, target == night);
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }
}
