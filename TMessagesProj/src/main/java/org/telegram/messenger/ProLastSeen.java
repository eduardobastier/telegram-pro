package org.telegram.messenger;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.tgnet.ConnectionsManager;
import org.telegram.tgnet.TLRPC;

// Remembers the last time each user was seen online, to show it when the server only says "recently".
public class ProLastSeen {

    private static SharedPreferences prefs() {
        return ApplicationLoader.applicationContext.getSharedPreferences("pro_lastseen", Context.MODE_PRIVATE);
    }

    private static String key(int account, long userId) {
        return account + "_" + userId;
    }

    public static void record(int account, long userId, TLRPC.UserStatus status) {
        if (!SharedConfig.ghostSaveLastSeen || status == null || userId == UserConfig.getInstance(account).getClientUserId()) {
            return;
        }
        int now = ConnectionsManager.getInstance(account).getCurrentTime();
        int seen;
        if (status.expires > now) {
            seen = now;
        } else if (status.expires > 0) {
            seen = status.expires;
        } else {
            return;
        }
        SharedPreferences prefs = prefs();
        String key = key(account, userId);
        if (seen - prefs.getInt(key, 0) >= 30) {
            prefs.edit().putInt(key, seen).apply();
        }
    }

    public static int get(int account, long userId) {
        if (!SharedConfig.ghostSaveLastSeen) {
            return 0;
        }
        return prefs().getInt(key(account, userId), 0);
    }
}
