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
        recordDate(account, userId, seen);
    }

    public static void recordDate(int account, long userId, int date) {
        if (!SharedConfig.ghostSaveLastSeen || userId <= 0 || date <= 0 || userId == UserConfig.getInstance(account).getClientUserId()) {
            return;
        }
        SharedPreferences prefs = prefs();
        String key = key(account, userId);
        if (date - prefs.getInt(key, 0) >= 30) {
            prefs.edit().putInt(key, date).apply();
        }
    }

    // A user who sends a message or reacts was online at that moment.
    public static void recordMessages(int account, java.util.ArrayList<MessageObject> messages) {
        if (!SharedConfig.ghostSaveLastSeen || messages == null) {
            return;
        }
        for (int i = 0; i < messages.size(); i++) {
            MessageObject messageObject = messages.get(i);
            if (messageObject == null || messageObject.messageOwner == null || messageObject.isOut() || messageObject.scheduled) {
                continue;
            }
            long fromId = messageObject.getFromChatId();
            if (fromId > 0) {
                recordDate(account, fromId, messageObject.messageOwner.date);
            }
        }
    }

    public static void recordReactions(int account, TLRPC.TL_messageReactions reactions) {
        if (!SharedConfig.ghostSaveLastSeen || reactions == null || reactions.recent_reactions == null) {
            return;
        }
        for (int i = 0; i < reactions.recent_reactions.size(); i++) {
            TLRPC.MessagePeerReaction reaction = reactions.recent_reactions.get(i);
            if (reaction != null && reaction.peer_id != null && reaction.date > 0) {
                recordDate(account, MessageObject.getPeerId(reaction.peer_id), reaction.date);
            }
        }
    }

    public static int get(int account, long userId) {
        if (!SharedConfig.ghostSaveLastSeen) {
            return 0;
        }
        return prefs().getInt(key(account, userId), 0);
    }
}
