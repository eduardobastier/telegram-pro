package org.telegram.ui;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.util.SparseIntArray;

// With Liquid Glass on, icons load the Telegram iOS version (ios_<name>, from the GPL Telegram-iOS
// repository) instead of the Android one, everywhere the activity's resources are used.
@SuppressWarnings("deprecation")
public class ProIconResources extends Resources {

    // Android icons that have an iOS replacement (generated drawables ios_<name>).
    private static final String[] ICONS = { "msg_copy", "msg_forward", "msg_delete", "msg_edit", "msg_pin", "msg_unpin", "msg_translate", "msg_report", "msg_link", "msg_select", "msg_info", "msg_saved", "msg_archive", "msg_unarchive", "msg_mute", "msg_unmute", "msg_shareout", "msg_share", "msg_download", "msg_fave", "msg_unfave", "msg_calendar2", "msg_markread", "msg_markunread", "msg_search", "msg_settings", "msg_block", "msg_clear", "msg_retry", "msg_stats", "msg_viewreplies", "msg_customize", "msg_folders", "msg_addfolder", "msg_contacts", "msg_groups", "msg_channel", "msg_mention", "msg_reactions", "msg_camera", "msg_videocall", "msg_callback", "msg_voicechat", "msg_secret", "msg_help", "msg_language", "msg_palette", "msg_theme", "msg_message", "msg_openprofile", "msg_go_up", "msg_go_down", "msg_sticker", "msg_photos", "msg_addcontact", "msg_leave", "msg_admins", "msg_input_attach2", "input_attach", "input_mic", "input_video", "ic_ab_back" };

    private final SparseIntArray map = new SparseIntArray();

    // Lottie animations with an iOS version (raw ios_<name>), e.g. the bottom tab icons.
    private static final String[] RAWS = { "tab_chats", "tab_contacts", "tab_settings" };
    private static SparseIntArray rawMap;

    public static int mapRaw(int rawRes) {
        boolean enabled;
        try {
            enabled = ProGlass.isEnabled();
        } catch (Throwable e) {
            enabled = false;
        }
        if (!enabled) {
            return rawRes;
        }
        if (rawMap == null) {
            final android.content.Context context = org.telegram.messenger.ApplicationLoader.applicationContext;
            final SparseIntArray built = new SparseIntArray();
            for (String name : RAWS) {
                final int original = context.getResources().getIdentifier(name, "raw", context.getPackageName());
                final int ios = context.getResources().getIdentifier("ios_" + name, "raw", context.getPackageName());
                if (original != 0 && ios != 0) {
                    built.put(original, ios);
                }
            }
            rawMap = built;
        }
        return rawMap.get(rawRes, rawRes);
    }

    public ProIconResources(Resources base, String packageName) {
        super(base.getAssets(), base.getDisplayMetrics(), base.getConfiguration());
        for (String name : ICONS) {
            final int original = base.getIdentifier(name, "drawable", packageName);
            final int ios = base.getIdentifier("ios_" + name, "drawable", packageName);
            if (original != 0 && ios != 0) {
                map.put(original, ios);
            }
        }
    }

    private int resolve(int id) {
        return map.get(id, id);
    }

    @Override
    public Drawable getDrawable(int id) throws NotFoundException {
        return super.getDrawable(resolve(id));
    }

    @Override
    public Drawable getDrawable(int id, Theme theme) throws NotFoundException {
        return super.getDrawable(resolve(id), theme);
    }

    @Override
    public Drawable getDrawableForDensity(int id, int density, Theme theme) {
        return super.getDrawableForDensity(resolve(id), density, theme);
    }
}
