package org.telegram.tgnet;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_stories;

public class ProGhostMode {

    public static boolean shouldDrop(TLObject object, ConnectionsManager connectionsManager) {
        if (!SharedConfig.ghostMode || object == null) {
            return false;
        }
        if (SharedConfig.ghostNoRead && (
            object instanceof TLRPC.TL_messages_readHistory ||
            object instanceof TLRPC.TL_channels_readHistory ||
            object instanceof TLRPC.TL_messages_readDiscussion ||
            object instanceof TLRPC.TL_messages_readMessageContents ||
            object instanceof TLRPC.TL_channels_readMessageContents
        )) {
            return true;
        }
        if (SharedConfig.ghostNoTyping && object instanceof TLRPC.TL_messages_setTyping) {
            return true;
        }
        if (SharedConfig.ghostNoStoryViews && (
            object instanceof TL_stories.TL_stories_readStories ||
            object instanceof TL_stories.TL_stories_incrementStoryViews
        )) {
            return true;
        }
        if (SharedConfig.ghostOffline) {
            if (object instanceof TL_account.updateStatus) {
                ((TL_account.updateStatus) object).offline = true;
            } else if (object instanceof TLRPC.TL_messages_sendMessage ||
                object instanceof TLRPC.TL_messages_sendMedia ||
                object instanceof TLRPC.TL_messages_sendMultiMedia ||
                object instanceof TLRPC.TL_messages_forwardMessages ||
                object instanceof TLRPC.TL_messages_sendInlineBotResult) {
                // Sending a message makes the server mark us online; go back offline right after.
                for (long delay : new long[] { 800, 3000 }) {
                    AndroidUtilities.runOnUIThread(() -> {
                        TL_account.updateStatus req = new TL_account.updateStatus();
                        req.offline = true;
                        connectionsManager.sendRequest(req, null);
                    }, delay);
                }
            }
        }
        return false;
    }
}
