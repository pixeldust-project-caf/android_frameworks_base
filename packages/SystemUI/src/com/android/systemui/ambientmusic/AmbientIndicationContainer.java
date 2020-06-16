package com.android.systemui.ambientmusic;

import android.content.ContentResolver;
import android.content.Context;
import android.database.ContentObserver;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.media.MediaMetadata;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Handler;
import android.os.UserHandle;
import android.provider.Settings;
import android.text.TextUtils;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.systemui.AutoReinflateContainer;
import com.android.systemui.Dependency;
import com.android.systemui.R;
import com.android.systemui.doze.DozeLog;
import com.android.systemui.statusbar.phone.DozeParameters;
import com.android.systemui.statusbar.NotificationLockscreenUserManager;
import com.android.systemui.statusbar.NotificationMediaManager;
import com.android.systemui.statusbar.phone.StatusBar;
import com.android.systemui.util.wakelock.SettableWakeLock;
import com.android.systemui.util.wakelock.WakeLock;

import com.android.systemui.ambientmusic.AmbientIndicationInflateListener;

import java.util.Locale;

public class AmbientIndicationContainer extends AutoReinflateContainer implements
        NotificationMediaManager.MediaListener {

    public static final boolean DEBUG_AMBIENTMUSIC = true;

    private View mAmbientIndication;
    private CharSequence mIndication;
    private StatusBar mStatusBar;
    private AnimatedVectorDrawable mAnimatedIcon;
    private TextView mText;
    private Context mContext;
    private CharSequence mMediaTitle;
    private CharSequence mMediaArtist;
    protected MediaMetadata mMediaMetaData;
    private String mMediaText;
    private boolean mForcedMediaDoze;
    private Handler mHandler;
    private Handler mMediaHandler;
    private boolean mInfoAvailable;
    private String mInfoToSet;
    private boolean mKeyguard;
    private boolean mDozing;
    private String mLastInfo;

    private boolean mNpInfoAvailable;

    protected NotificationMediaManager mMediaManager;

    private boolean mMediaIsVisible;
    private SettableWakeLock mMediaWakeLock;

    private String mTrackInfoSeparator;

    private boolean mShowMusicTicker;
    private CustomSettingsObserver mCustomSettingsObserver;

    public AmbientIndicationContainer(Context context, AttributeSet attributeSet) {
        super(context, attributeSet);
        initDependencies();
        mContext = context;
        initializeMedia();
        mTrackInfoSeparator = getResources().getString(R.string.ambientmusic_songinfo);
        final int iconSize = mContext.getResources().getDimensionPixelSize(
                R.dimen.notification_menu_icon_padding);
        mAnimatedIcon = (AnimatedVectorDrawable) mContext.getDrawable(
                R.drawable.audioanim_animation).getConstantState().newDrawable();
        mAnimatedIcon.setBounds(0, 0, iconSize, iconSize);
    }

    private class CustomSettingsObserver extends ContentObserver {
        CustomSettingsObserver(Handler handler) {
            super(handler);
        }

        void observe() {
            ContentResolver resolver = mContext.getContentResolver();
            resolver.registerContentObserver(Settings.System.getUriFor(
                    Settings.System.FORCE_AMBIENT_FOR_MEDIA),
                    false, this, UserHandle.USER_ALL);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            if (uri.equals(Settings.System.getUriFor(
                    Settings.System.FORCE_AMBIENT_FOR_MEDIA))) {
                mShowMusicTicker =
                        Settings.System.getIntForUser(mContext.getContentResolver(),
                        Settings.System.FORCE_AMBIENT_FOR_MEDIA, 1,
                        UserHandle.USER_CURRENT) == 1;
            }
            update();
        }

        private void update() {
            updateAmbientIndicationView(AmbientIndicationContainer.this);
        }
    }

    private void hideIndication() {
        setIndication(null, null, false);
        mAnimatedIcon.stop();
    }

    public void initializeView(StatusBar statusBar, Handler handler) {
        mStatusBar = statusBar;
        addInflateListener(new AmbientIndicationInflateListener(this));
        mHandler = handler;
        mCustomSettingsObserver = new CustomSettingsObserver(mHandler);
        mCustomSettingsObserver.observe();
        mCustomSettingsObserver.update();
        if (DEBUG_AMBIENTMUSIC) {
            Log.d("AmbientIndicationContainer", "initializeView");
        }
    }

    private void initializeMedia() {
        mMediaHandler = new Handler();
        mMediaWakeLock = new SettableWakeLock(WakeLock.createPartial(mContext, "media"),
                "media");
        if (DEBUG_AMBIENTMUSIC) {
            Log.d("AmbientIndicationContainer", "initializeMedia");
        }
    }

    public void updateAmbientIndicationView(View view) {
        mAmbientIndication = findViewById(R.id.ambient_indication);
        mText = (TextView)findViewById(R.id.ambient_indication_text);
        if (mShowMusicTicker) {
            setIndication(mMediaMetaData, mMediaText, false);
        } else {
            hideIndication();
        }
        if (DEBUG_AMBIENTMUSIC) {
            Log.d("AmbientIndicationContainer", "updateAmbientIndicationView; mShowMusicTicker = " + mShowMusicTicker);
        }
    }

    public void initDependencies() {
        mMediaManager = Dependency.get(NotificationMediaManager.class);
        mMediaManager.addCallback(this);
    }

    public void updateKeyguardState(boolean keyguard) {
        if (keyguard && (mInfoAvailable || mNpInfoAvailable)) {
            mText.setText(mInfoToSet);
            mLastInfo = mInfoToSet;
            updatePosition();
        } else {
            setCleanLayout(-1);
            mText.setText(null);
        }
        if (mKeyguard != keyguard) {
            setTickerMarquee(keyguard, false);
        }
        mKeyguard = keyguard;
        // StatusBar.updateKeyguardState will call updateDozingState later
    }

    public void updateDozingState(boolean dozing) {
        if (mDozing != dozing) {
            mDozing = dozing;
            if (isAod()) {
                setTickerMarquee(true, false);
            }
        }
        mAmbientIndication.setVisibility(shouldShow() ? View.VISIBLE : View.INVISIBLE);
    }

    private boolean isAod() {
        return DozeParameters.getInstance(mContext).getAlwaysOn() && mDozing;
    }

    private boolean shouldShow() {
        // if not dozing, show ambient music info only for Google Now Playing,
        // not for local media players if they are showing a lockscreen media notification
        final NotificationLockscreenUserManager lockscreenManager =
                mStatusBar.getNotificationLockscreenUserManager();
        boolean filtered = lockscreenManager.shouldHideNotifications(
                lockscreenManager.getCurrentUserId()) || lockscreenManager.shouldHideNotifications(
                        mMediaManager.getMediaNotificationKey());
        return (mKeyguard || isAod())
                && ((mDozing && (mInfoAvailable || mNpInfoAvailable))
                || (!mDozing && mNpInfoAvailable && !mInfoAvailable)
                || (!mDozing && mInfoAvailable && filtered));
    }

    private void setTickerMarquee(boolean enable, boolean extendPulseOnNewTrack) {
        // If it's enabled and we are supposed to show.
        if (enable && shouldShow()) {
            setTickerMarquee(false, false);
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mText.setEllipsize(TruncateAt.MARQUEE);
                    mText.setMarqueeRepeatLimit(2);
                    boolean rtl = TextUtils.getLayoutDirectionFromLocale(
                            Locale.getDefault()) == View.LAYOUT_DIRECTION_RTL;
                    mText.setCompoundDrawables(rtl ? null : mAnimatedIcon, null, rtl ?
                            mAnimatedIcon : null, null);
                    mText.setSelected(true);
                    mAnimatedIcon.start();
                    if (extendPulseOnNewTrack && mStatusBar.isPulsing()) {
                        mStatusBar.getDozeScrimController().extendPulseForMusicTicker();
                    }
                }
            }, 1600);
        } else {
            mText.setEllipsize(null);
            mText.setSelected(false);
            mAnimatedIcon.stop();
        }
    }

    public void setOnPulseEvent(int reason, boolean pulsing) {
        setCleanLayout(reason);
        setTickerMarquee(pulsing,
                reason == DozeLog.PULSE_REASON_FORCED_MEDIA_NOTIFICATION);
    }

    public void setCleanLayout(int reason) {
        mForcedMediaDoze =
                reason == DozeLog.PULSE_REASON_FORCED_MEDIA_NOTIFICATION;
        updatePosition();
        if (DEBUG_AMBIENTMUSIC) {
            Log.d("AmbientIndicationContainer", "setCleanLayout, reason=" + reason);
        }
    }

    public void updatePosition() {
        FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) this.getLayoutParams();
        lp.gravity = mForcedMediaDoze ? Gravity.CENTER : Gravity.BOTTOM;
        this.setLayoutParams(lp);
    }

    private void setNowPlayingIndication(String trackInfo) {
        setIndication(null, trackInfo, true);
    }

    public void setIndication(MediaMetadata mediaMetaData, String notificationText, boolean nowPlaying) {
        // never override local music ticker but be sure to delete Now Playing info when needed
        if (nowPlaying && notificationText == null) {
            mMediaText = null;
            mNpInfoAvailable = false;
        }
        if (nowPlaying && mInfoAvailable || mAmbientIndication == null) return;

        CharSequence charSequence = null;
        mInfoToSet = null;
        if (mediaMetaData != null) {
            CharSequence artist = mediaMetaData.getText(MediaMetadata.METADATA_KEY_ARTIST);
            CharSequence album = mediaMetaData.getText(MediaMetadata.METADATA_KEY_ALBUM);
            CharSequence title = mediaMetaData.getText(MediaMetadata.METADATA_KEY_TITLE);
            if (artist != null && title != null) {
                /* considering we are in Ambient mode here, it's not worth it to show
                    too many infos, so let's skip album name to keep a smaller text */
                charSequence = String.format(mTrackInfoSeparator, title.toString(), artist.toString());
            }
        }

        // if we are already showing an Ambient Notification with track info,
        // stop the current scrolling and start it delayed again for the next song
        setTickerMarquee(true, true);

        if (!TextUtils.isEmpty(charSequence)) {
            mInfoToSet = charSequence.toString();
        } else if (!TextUtils.isEmpty(notificationText)) {
            mInfoToSet = notificationText;
        }

        if (nowPlaying) {
            mNpInfoAvailable = mInfoToSet != null;
        } else {
            mInfoAvailable = mInfoToSet != null;
        }

        if (mInfoAvailable || mNpInfoAvailable) {
            mMediaMetaData = mediaMetaData;
            mMediaText = notificationText;
            boolean isAnotherTrack = (mInfoAvailable || mNpInfoAvailable)
                    && (TextUtils.isEmpty(mLastInfo) || (!TextUtils.isEmpty(mLastInfo)
                    && !mLastInfo.equals(mInfoToSet)));
            if (!isAod() && mStatusBar != null
                    && isAnotherTrack) {
                mStatusBar.triggerAmbientForMedia();
            }
            if (mKeyguard) {
                mLastInfo = mInfoToSet;
            }
        }
        if (mInfoToSet != null) {
            mText.setText(mInfoToSet);
            mAmbientIndication.setVisibility(shouldShow() ? View.VISIBLE : View.INVISIBLE);
        } else {
            mAmbientIndication.setVisibility(View.INVISIBLE);
        }
    }

    public View getIndication() {
        return mAmbientIndication;
    }

    @Override
    public void onMetadataOrStateChanged(MediaMetadata metadata, @PlaybackState.State int state) {
        synchronized (this) {
            if (DEBUG_AMBIENTMUSIC) {
                switch(state) {
                case 0:
                    Log.d("AmbientIndicationContainer", "onMetadataOrStateChanged: PlaybackState.State = STATE_NONE");
                    break;
                case 1:
                    Log.d("AmbientIndicationContainer", "onMetadataOrStateChanged: PlaybackState.State = STATE_STOPPED");
                    break;
                case 2:
                    Log.d("AmbientIndicationContainer", "onMetadataOrStateChanged: PlaybackState.State = STATE_PAUSED");
                    break;
                case 3:
                    Log.d("AmbientIndicationContainer", "onMetadataOrStateChanged: PlaybackState.State = STATE_PLAYING");
                    break;
                default:
                    Log.d("AmbientIndicationContainer", "onMetadataOrStateChanged: PlaybackState.State = UNKNOWN " + state);
                    break;
                }
            }
            boolean nextVisible = state == 3 || mMediaManager.getNowPlayingTrack() != null;
            if (mMediaHandler != null) {
                mMediaHandler.removeCallbacksAndMessages(null);
                if (mMediaIsVisible && !nextVisible) {
                    // We need to delay this event for a few millis when stopping to avoid jank in the
                    // animation. The media app might not send its update when buffering, and the slice
                    // would end up without a header for 0.5 second.
                    mMediaHandler.postDelayed(() -> {
                        synchronized (this) {
                            updateMediaStateLocked(metadata, state);
                            mMediaWakeLock.setAcquired(false);
                        }
                    }, 2000);;
                } else {
                    mMediaWakeLock.setAcquired(false);
                    updateMediaStateLocked(metadata, state);
                }
            }
        }
    }

    private void updateMediaStateLocked(MediaMetadata metadata, @PlaybackState.State int state) {
        boolean nowPlayingAvailable = mMediaManager.getNowPlayingTrack() != null;
        boolean nextVisible = state == 3 || nowPlayingAvailable;
        CharSequence title = null;
        if (metadata != null) {
            title = metadata.getText(MediaMetadata.METADATA_KEY_TITLE);
            if (TextUtils.isEmpty(title)) {
                title = getContext().getResources().getString(R.string.music_controls_no_title);
            }
        }
        CharSequence artist = metadata == null ? null : metadata.getText(
                MediaMetadata.METADATA_KEY_ARTIST);

        if (nextVisible == mMediaIsVisible && TextUtils.equals(title, mMediaTitle)
                && TextUtils.equals(artist, mMediaArtist)) {
            return;
        }
        mMediaTitle = title;
        mMediaArtist = artist;
        mMediaIsVisible = nextVisible;

        if (mMediaTitle == null && nowPlayingAvailable) {
            mMediaTitle = mMediaManager.getNowPlayingTrack();
            mMediaIsVisible = true;
            mMediaArtist = null;
        }

        if (mShowMusicTicker && nowPlayingAvailable) {
            setNowPlayingIndication(mMediaManager.getNowPlayingTrack());
            if (DEBUG_AMBIENTMUSIC) {
                Log.d("AmbientIndicationContainer", "onMetadataOrStateChanged: Now Playing: track=" + mMediaManager.getNowPlayingTrack());
            }
        } else if (mShowMusicTicker && !TextUtils.isEmpty(mMediaTitle)) {
            setIndication(metadata, null, false); //2nd param must be null here
            if (DEBUG_AMBIENTMUSIC) {
                Log.d("AmbientIndicationContainer", "onMetadataOrStateChanged: Music Ticker: artist=" + artist + "; title=" + title);
            }
        } else {
            // Make sure that track info is hidden when playback is paused or stopped
            if (mAnimatedIcon != null) {            
                hideIndication();
                if (DEBUG_AMBIENTMUSIC) {
                    Log.d("AmbientIndicationContainer", "onMetadataOrStateChanged: hideIndication(); mShowMusicTicker = " + mShowMusicTicker);
                }           
            } 
        }
    }
}
