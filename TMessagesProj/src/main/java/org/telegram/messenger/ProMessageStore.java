package org.telegram.messenger;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.text.TextUtils;

import org.telegram.SQLite.SQLiteCursor;
import org.telegram.tgnet.NativeByteBuffer;
import org.telegram.tgnet.TLRPC;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

// Keeps deleted message ids and previous versions of edited messages in a local database.
public class ProMessageStore extends SQLiteOpenHelper {

    public static class Version {
        public final int date;
        public final String text;

        Version(int date, String text) {
            this.date = date;
            this.text = text;
        }
    }

    private static volatile ProMessageStore instance;

    private final Set<String> deleted = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Set<String> edited = Collections.newSetFromMap(new ConcurrentHashMap<>());

    private ProMessageStore() {
        super(ApplicationLoader.applicationContext, "pro_messages.db", null, 1);
        try {
            SQLiteDatabase db = getReadableDatabase();
            try (Cursor c = db.rawQuery("SELECT account, dialog, mid FROM deleted", null)) {
                while (c.moveToNext()) {
                    deleted.add(key(c.getInt(0), c.getLong(1), c.getInt(2)));
                }
            }
            try (Cursor c = db.rawQuery("SELECT DISTINCT account, dialog, mid FROM edits", null)) {
                while (c.moveToNext()) {
                    edited.add(key(c.getInt(0), c.getLong(1), c.getInt(2)));
                }
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
    }

    public static ProMessageStore getInstance() {
        ProMessageStore local = instance;
        if (local == null) {
            synchronized (ProMessageStore.class) {
                local = instance;
                if (local == null) {
                    instance = local = new ProMessageStore();
                }
            }
        }
        return local;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE deleted (account INTEGER, dialog INTEGER, mid INTEGER, date INTEGER, PRIMARY KEY (account, dialog, mid))");
        db.execSQL("CREATE TABLE edits (account INTEGER, dialog INTEGER, mid INTEGER, date INTEGER, text TEXT)");
        db.execSQL("CREATE INDEX edits_msg ON edits (account, dialog, mid)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }

    private static String key(int account, long dialog, int mid) {
        return account + "_" + dialog + "_" + mid;
    }

    // Server delete updates carry dialog 0 for private chats and groups, and -channel_id for channels.
    private static long storeDialog(MessageObject messageObject) {
        TLRPC.Message owner = messageObject.messageOwner;
        if (owner != null && owner.peer_id != null && owner.peer_id.channel_id != 0) {
            return -owner.peer_id.channel_id;
        }
        return 0;
    }

    private static long storeDialog(TLRPC.Message message) {
        if (message.peer_id != null && message.peer_id.channel_id != 0) {
            return -message.peer_id.channel_id;
        }
        return 0;
    }

    // Returns true when the messages must stay in the chat instead of being removed.
    public static boolean onMessagesDeleted(int account, long dialog, ArrayList<Integer> ids) {
        if (!SharedConfig.saveDeletedMessages || ids == null || ids.isEmpty()) {
            return false;
        }
        final ArrayList<Integer> copy = new ArrayList<>(ids);
        final ProMessageStore store = getInstance();
        for (int id : copy) {
            store.deleted.add(key(account, dialog, id));
        }
        Utilities.globalQueue.postRunnable(() -> {
            try {
                SQLiteDatabase db = store.getWritableDatabase();
                int now = (int) (System.currentTimeMillis() / 1000);
                db.beginTransaction();
                try {
                    for (int id : copy) {
                        ContentValues values = new ContentValues();
                        values.put("account", account);
                        values.put("dialog", dialog);
                        values.put("mid", id);
                        values.put("date", now);
                        db.insertWithOnConflict("deleted", null, values, SQLiteDatabase.CONFLICT_IGNORE);
                    }
                    db.setTransactionSuccessful();
                } finally {
                    db.endTransaction();
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
        AndroidUtilities.runOnUIThread(() -> NotificationCenter.getInstance(account).postNotificationName(NotificationCenter.proMessagesDeleted));
        return true;
    }

    public static boolean isDeleted(int account, MessageObject messageObject) {
        if (messageObject == null || messageObject.getId() <= 0 || instance == null && !SharedConfig.saveDeletedMessages) {
            return false;
        }
        return getInstance().deleted.contains(key(account, storeDialog(messageObject), messageObject.getId()));
    }

    // Called before an edit update is applied: saves the text the message had until now.
    public static void onMessageEdited(int account, TLRPC.Message newMessage) {
        if (!SharedConfig.saveEditHistory || newMessage == null || newMessage.id <= 0) {
            return;
        }
        final long dialogId = MessageObject.getDialogId(newMessage);
        final int mid = newMessage.id;
        final String newText = newMessage.message;
        final MessagesStorage storage = MessagesStorage.getInstance(account);
        storage.getStorageQueue().postRunnable(() -> {
            SQLiteCursor cursor = null;
            try {
                cursor = storage.getDatabase().queryFinalized(String.format(Locale.US, "SELECT data FROM messages_v2 WHERE mid = %d AND uid = %d LIMIT 1", mid, dialogId));
                if (!cursor.next()) {
                    return;
                }
                NativeByteBuffer data = cursor.byteBufferValue(0);
                if (data == null) {
                    return;
                }
                TLRPC.Message old = TLRPC.Message.TLdeserialize(data, data.readInt32(false), false);
                data.reuse();
                if (old == null || TextUtils.equals(old.message, newText)) {
                    return;
                }
                final ProMessageStore store = getInstance();
                ContentValues values = new ContentValues();
                values.put("account", account);
                values.put("dialog", storeDialog(newMessage));
                values.put("mid", mid);
                values.put("date", old.edit_date != 0 ? old.edit_date : old.date);
                values.put("text", old.message == null ? "" : old.message);
                store.getWritableDatabase().insert("edits", null, values);
                store.edited.add(key(account, storeDialog(newMessage), mid));
            } catch (Exception e) {
                FileLog.e(e);
            } finally {
                if (cursor != null) {
                    cursor.dispose();
                }
            }
        });
    }

    public static boolean hasEditHistory(int account, MessageObject messageObject) {
        if (messageObject == null || messageObject.getId() <= 0) {
            return false;
        }
        return getInstance().edited.contains(key(account, storeDialog(messageObject), messageObject.getId()));
    }

    public static ArrayList<Version> getEditHistory(int account, MessageObject messageObject) {
        ArrayList<Version> result = new ArrayList<>();
        try (Cursor c = getInstance().getReadableDatabase().rawQuery(
                "SELECT date, text FROM edits WHERE account = ? AND dialog = ? AND mid = ? ORDER BY rowid",
                new String[] { String.valueOf(account), String.valueOf(storeDialog(messageObject)), String.valueOf(messageObject.getId()) })) {
            while (c.moveToNext()) {
                result.add(new Version(c.getInt(0), c.getString(1)));
            }
        } catch (Exception e) {
            FileLog.e(e);
        }
        return result;
    }

    public static void clearAll() {
        ProMessageStore store = getInstance();
        store.deleted.clear();
        store.edited.clear();
        Utilities.globalQueue.postRunnable(() -> {
            try {
                SQLiteDatabase db = store.getWritableDatabase();
                db.delete("deleted", null, null);
                db.delete("edits", null, null);
            } catch (Exception e) {
                FileLog.e(e);
            }
        });
    }
}
