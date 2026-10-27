package org.telegram.ui;

import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Build;
import android.view.Gravity;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.BuildVars;
import org.telegram.messenger.LiteMode;
import org.telegram.messenger.SharedConfig;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.LayoutHelper;
import org.telegram.ui.Components.RecyclerListView;
import org.telegram.ui.Components.blur3.BlurredBackgroundDrawableViewFactory;
import org.telegram.ui.Components.blur3.DownscaleScrollableNoiseSuppressor;
import org.telegram.ui.Components.blur3.ViewGroupPartRenderer;
import org.telegram.ui.Components.blur3.drawable.color.impl.BlurredBackgroundProviderImpl;
import org.telegram.ui.Components.blur3.source.BlurredBackgroundSourceRenderNode;

import java.util.ArrayList;

// Turns a screen's action bar into floating Liquid Glass controls over its scrolling list,
// using the same blur3 pipeline Telegram uses for the chat list and the chat header.
public class ProGlass {

    // Same conditions Telegram uses for its Liquid Glass option.
    public static boolean isSupported() {
        return Build.VERSION.SDK_INT >= 33 && (SharedConfig.getDevicePerformanceClass() >= SharedConfig.PERFORMANCE_CLASS_AVERAGE || BuildVars.DEBUG_PRIVATE_VERSION);
    }

    public static boolean isEnabled() {
        return isSupported() && LiteMode.isEnabled(LiteMode.FLAG_LIQUID_GLASS);
    }

    // Liquid Glass transparency chosen in Telegram Pro > Appearance, 0-100%.
    public static final int DEFAULT_TRANSPARENCY = 40;

    public static int getTransparency() {
        return org.telegram.messenger.MessagesController.getGlobalMainSettings().getInt("proGlassTransparency", DEFAULT_TRANSPARENCY);
    }

    public static void setTransparency(int percent) {
        org.telegram.messenger.MessagesController.getGlobalMainSettings().edit().putInt("proGlassTransparency", Math.max(0, Math.min(100, percent))).apply();
    }

    // Opacity of Liquid Glass surfaces (Telegram uses 0.85); lower = more see-through.
    public static float glassAlpha() {
        return 1f - getTransparency() / 100f;
    }

    // Panels over a blurred window (menus, sheets) follow the same setting, a bit more opaque.
    public static int panelAlpha() {
        return Math.min(255, Math.round(255 * glassAlpha() * 1.08f));
    }

    // Dialogs and sheets are separate windows: use Android's native blur of what is behind them.
    public static boolean blurDialog(android.app.Dialog dialog) {
        if (!isEnabled() || Build.VERSION.SDK_INT < 31 || dialog == null || dialog.getWindow() == null) {
            return false;
        }
        return blurWindow(dialog.getWindow().getWindowManager(), dialog.getWindow().getDecorView(), dialog.getWindow());
    }

    // Popup windows: same native blur, applied to the popup's own window.
    public static boolean blurPopup(android.widget.PopupWindow popup) {
        if (!isEnabled() || Build.VERSION.SDK_INT < 31 || popup == null || popup.getContentView() == null) {
            return false;
        }
        final android.view.View root = popup.getContentView().getRootView();
        final android.view.WindowManager wm = (android.view.WindowManager) root.getContext().getSystemService(android.content.Context.WINDOW_SERVICE);
        return blurWindow(wm, root, null);
    }

    private static boolean blurWindow(android.view.WindowManager wm, android.view.View root, android.view.Window window) {
        if (Build.VERSION.SDK_INT < 31 || wm == null || root == null || !wm.isCrossWindowBlurEnabled()) {
            return false;
        }
        try {
            if (window != null) {
                window.addFlags(android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND);
                final android.view.WindowManager.LayoutParams params = window.getAttributes();
                params.setBlurBehindRadius(AndroidUtilities.dp(24));
                window.setAttributes(params);
            } else if (root.getLayoutParams() instanceof android.view.WindowManager.LayoutParams) {
                final android.view.WindowManager.LayoutParams params = (android.view.WindowManager.LayoutParams) root.getLayoutParams();
                params.flags |= android.view.WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
                params.setBlurBehindRadius(AndroidUtilities.dp(16));
                wm.updateViewLayout(root, params);
            } else {
                return false;
            }
            return true;
        } catch (Exception e) {
            org.telegram.messenger.FileLog.e(e);
            return false;
        }
    }

    // Frosted glass for panels in their own windows (sheets, popup menus): a blurred snapshot of the app,
    // drawn behind the panel at its position on screen. Works on devices that block system window blur.
    public static class FrostedDrawable extends android.graphics.drawable.Drawable {
        private final android.view.View host;
        private final android.graphics.drawable.Drawable panel;
        private final float radiusTop, radiusBottom;
        private final android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG | android.graphics.Paint.FILTER_BITMAP_FLAG);
        private final android.graphics.Matrix matrix = new android.graphics.Matrix();
        private final android.graphics.Path path = new android.graphics.Path();
        private final android.graphics.RectF rect = new android.graphics.RectF();
        private final android.graphics.Rect padding = new android.graphics.Rect();
        private final int[] location = new int[2];
        private android.graphics.BitmapShader shader;
        private int alpha = 255;

        public FrostedDrawable(android.view.View host, android.graphics.drawable.Drawable panel, float radiusTop, float radiusBottom) {
            this.host = host;
            this.panel = panel;
            this.radiusTop = radiusTop;
            this.radiusBottom = radiusBottom;
            panel.getPadding(padding);
            AndroidUtilities.makeGlobalBlurBitmap(bitmap -> {
                if (bitmap != null) {
                    shader = new android.graphics.BitmapShader(bitmap, android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP);
                    paint.setShader(shader);
                    host.invalidate();
                }
            }, 8);
        }

        public android.graphics.drawable.Drawable getPanel() {
            return panel;
        }

        @Override
        public void draw(@NonNull Canvas canvas) {
            final android.graphics.Rect b = getBounds();
            if (shader != null) {
                host.getLocationOnScreen(location);
                // Bounds are in host coordinates; the snapshot is in screen coordinates at 1/8 scale.
                matrix.setScale(8f, 8f);
                matrix.postTranslate(-location[0], -location[1]);
                shader.setLocalMatrix(matrix);
                rect.set(b.left + padding.left, b.top + padding.top, b.right - padding.right, b.bottom - padding.bottom);
                path.reset();
                path.addRoundRect(rect, new float[] { radiusTop, radiusTop, radiusTop, radiusTop, radiusBottom, radiusBottom, radiusBottom, radiusBottom }, android.graphics.Path.Direction.CW);
                paint.setAlpha(alpha);
                canvas.drawPath(path, paint);
            }
            panel.setBounds(b);
            panel.draw(canvas);
        }

        @Override
        public boolean getPadding(@NonNull android.graphics.Rect out) {
            return panel.getPadding(out);
        }

        @Override
        public void setAlpha(int alpha) {
            this.alpha = alpha;
            panel.setAlpha(alpha * panelAlpha() / 255);
        }

        @Override
        public void setColorFilter(android.graphics.ColorFilter colorFilter) {
            panel.setColorFilter(colorFilter);
        }

        @Override
        public int getOpacity() {
            return android.graphics.PixelFormat.TRANSLUCENT;
        }
    }

    // Wraps a panel background with frosted glass when Liquid Glass is on; returns the original otherwise.
    public static android.graphics.drawable.Drawable frost(android.view.View host, android.graphics.drawable.Drawable panel, float radiusTop, float radiusBottom) {
        if (!isEnabled() || host == null || panel == null || panel instanceof FrostedDrawable) {
            return panel;
        }
        final FrostedDrawable drawable = new FrostedDrawable(host, panel, radiusTop, radiusBottom);
        drawable.setAlpha(255);
        return drawable;
    }

    // Settings-like screens that get the glass top bar automatically when Liquid Glass is on.
    private static final java.util.HashSet<String> AUTO_SCREENS = new java.util.HashSet<>(java.util.Arrays.asList(
        "PrivacySettingsActivity", "PrivacyControlActivity", "NotificationsSettingsActivity",
        "NotificationsCustomSettingsActivity", "ProfileNotificationsActivity", "DataSettingsActivity",
        "DataAutoDownloadActivity", "ThemeActivity", "FiltersSetupActivity", "SessionsActivity",
        "LanguageSelectActivity", "ProxyListActivity", "UserInfoActivity", "WebBrowserSettings"
    ));

    // Screens that already have Telegram's own glass/header, or are not list screens.
    private static final java.util.HashSet<String> EXCLUDED_SCREENS = new java.util.HashSet<>(java.util.Arrays.asList(
        "DialogsActivity", "ChatActivity", "ProfileActivity", "MainTabsActivity", "TopicsFragment",
        "ContactsActivity", "CallLogActivity", "StatisticActivity", "ChannelAdminLogActivity",
        "LoginActivity", "IntroActivity", "PasscodeActivity", "ProSettingsActivity", "SettingsActivity",
        "MediaActivity", "PhotoViewer", "ArticleViewer", "StoryViewer", "CameraScanActivity"
    ));

    // Called by BaseFragment right after a screen's view is created.
    public static void autoApply(BaseFragment fragment, android.view.View view) {
        if (!isEnabled() || fragment == null) {
            return;
        }
        final String name = fragment.getClass().getSimpleName();
        if (EXCLUDED_SCREENS.contains(name) || fragment.getClass().isAnonymousClass() && !AUTO_SCREENS.contains(fragment.getClass().getSuperclass().getSimpleName())) {
            return;
        }
        if (!(view instanceof FrameLayout) || fragment.getActionBar() == null || !fragment.getActionBar().shouldAddToContainer()) {
            return;
        }
        final FrameLayout content = (FrameLayout) view;
        for (int i = 0; i < content.getChildCount(); i++) {
            final android.view.View child = content.getChildAt(i);
            if (child instanceof RecyclerListView && child.getLayoutParams() != null && child.getLayoutParams().height == ViewGroup.LayoutParams.MATCH_PARENT) {
                apply(fragment, content, (RecyclerListView) child);
                return;
            }
        }
    }

    // Call at the end of createView(), after the list has been added to the content view.
    public static void apply(BaseFragment fragment, FrameLayout content, RecyclerListView list) {
        if (!isEnabled() || Build.VERSION.SDK_INT < 33 || content == null || list == null) {
            return;
        }
        final ActionBar actionBar = fragment.getActionBar();
        if (actionBar == null) {
            return;
        }

        final DownscaleScrollableNoiseSuppressor noiseSuppressor = new DownscaleScrollableNoiseSuppressor();
        final BlurredBackgroundSourceRenderNode source = new BlurredBackgroundSourceRenderNode(null);
        final ViewGroupPartRenderer capture = new ViewGroupPartRenderer(list, content, list::drawChild);
        final ArrayList<RectF> positions = new ArrayList<>();
        final RectF actionBarPosition = new RectF();
        positions.add(actionBarPosition);

        // Refresh what is behind the glass right before every frame.
        content.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
            @Override
            public boolean onPreDraw() {
                final int width = content.getMeasuredWidth();
                final int height = content.getMeasuredHeight();
                if (width <= 0 || height <= 0 || source.inRecording()) {
                    return true;
                }
                actionBarPosition.set(0, -AndroidUtilities.dp(48), width, actionBar.getMeasuredHeight() + AndroidUtilities.dp(48));
                noiseSuppressor.setupRenderNodes(positions, 1);
                noiseSuppressor.invalidateResultRenderNodes(capture, width, height);

                final Canvas canvas = source.beginRecording(width, height);
                canvas.drawColor(fragment.getThemedColor(Theme.key_windowBackgroundGray));
                noiseSuppressor.draw(canvas, DownscaleScrollableNoiseSuppressor.DRAW_GLASS);
                source.endRecording();
                return true;
            }
        });

        final BlurredBackgroundDrawableViewFactory factory = new BlurredBackgroundDrawableViewFactory(source);
        factory.setLiquidGlassEffectAllowed(true);
        actionBar.setupGlass(factory, BlurredBackgroundProviderImpl.topPanelChatActivity(fragment.getResourceProvider()));
        actionBar.setCastShadows(false);

        // The action bar floats over the list instead of sitting above it.
        actionBar.setAddToContainer(false);
        if (actionBar.getParent() instanceof ViewGroup) {
            ((ViewGroup) actionBar.getParent()).removeView(actionBar);
        }
        // The glass pills have 6dp of padding; 6dp of margin lines them up with the 12dp section cards.
        content.addView(actionBar, LayoutHelper.createFrame(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, Gravity.TOP, 6, 0, 6, 0));

        // A little air between the floating bar and the first card, like iOS.
        final int top = ActionBar.getCurrentActionBarHeight() + (actionBar.getOccupyStatusBar() ? AndroidUtilities.statusBarHeight : 0) + AndroidUtilities.dp(12);
        // Space for the floating bar before the first item. An item decoration survives screens that
        // reset the list padding later (setPadding would be overwritten).
        list.setClipToPadding(false);
        list.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull android.view.View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                if (parent.getChildAdapterPosition(view) == 0) {
                    outRect.top += top;
                }
            }
        });
        list.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                actionBar.invalidate();
            }
        });
    }
}
