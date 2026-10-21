package org.telegram.tgnet;

import org.telegram.messenger.Utilities;
import org.telegram.messenger.SharedConfig;
import org.telegram.tgnet.tl.TL_account;
import org.telegram.tgnet.tl.TL_stories;

public class ProGhostMode {

    public static boolean needsOfflineAfter(TLObject object) {
        return SharedConfig.ghostMode && SharedConfig.ghostOffline && (
            object instanceof TLRPC.TL_messages_sendMessage ||
            object instanceof TLRPC.TL_messages_sendMedia ||
            object instanceof TLRPC.TL_messages_sendMultiMedia ||
            object instanceof TLRPC.TL_messages_forwardMessages ||
            object instanceof TLRPC.TL_messages_sendInlineBotResult
        );
    }

    // Sending a message makes the server mark us online: once the server answers, go back offline.
    public static class OfflineAfterSend implements RequestDelegate {
        private final RequestDelegate original;
        private final ConnectionsManager connectionsManager;

        public OfflineAfterSend(RequestDelegate original, ConnectionsManager connectionsManager) {
            this.original = original;
            this.connectionsManager = connectionsManager;
        }

        @Override
        public void run(TLObject response, TLRPC.TL_error error) {
            if (original != null) {
                original.run(response, error);
            }
            for (long delay : new long[] { 0, 1000, 5000 }) {
                Utilities.globalQueue.postRunnable(() -> {
                    TL_account.updateStatus req = new TL_account.updateStatus();
                    req.offline = true;
                    connectionsManager.sendRequest(req, null);
                }, delay);
            }
        }
    }

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
            }
        }
        return false;
    }
}
