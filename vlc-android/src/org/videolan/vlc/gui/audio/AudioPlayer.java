/*****************************************************************************
 * AudioPlayer.java
 *****************************************************************************
 * Copyright © 2011-2014 VLC authors and VideoLAN
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *****************************************************************************/

package org.videolan.vlc.gui.audio;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.RequiresPermission;
import android.support.design.widget.Snackbar;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PopupMenu;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.TextView;
import android.widget.ViewSwitcher;

import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.vlc.gui.helpers.UiTools;
import org.videolan.vlc.media.MediaWrapper;
import org.videolan.vlc.PlaybackService;
import org.videolan.vlc.R;
import org.videolan.vlc.VLCApplication;
import org.videolan.vlc.gui.AudioPlayerContainerActivity;
import org.videolan.vlc.gui.PlaybackServiceFragment;
import org.videolan.vlc.gui.helpers.SwipeDragItemTouchHelperCallback;
import org.videolan.vlc.gui.view.CoverMediaSwitcher;
import org.videolan.vlc.gui.view.HeaderMediaSwitcher;
import org.videolan.vlc.gui.dialogs.AdvOptionsDialog;
import org.videolan.vlc.gui.dialogs.SavePlaylistDialog;
import org.videolan.vlc.gui.preferences.PreferencesActivity;
import org.videolan.vlc.util.Strings;
import org.videolan.vlc.util.Util;
import org.videolan.vlc.gui.view.AudioMediaSwitcher.AudioMediaSwitcherListener;

import java.util.ArrayList;
import java.util.List;

public class AudioPlayer extends PlaybackServiceFragment implements PlaybackService.Callback, View.OnClickListener {
    public static final String TAG = "VLC/AudioPlayer";

    private ProgressBar mProgressBar;
    private HeaderMediaSwitcher mHeaderMediaSwitcher;
    private CoverMediaSwitcher mCoverMediaSwitcher;
    private TextView mTime;
    private TextView mHeaderTime;
    private TextView mLength;
    private ImageButton mResumeToVideo;
    private ImageButton mPlayPause;
    private ImageButton mHeaderPlayPause;
    private ImageButton mNext;
    private ImageButton mPrevious;
    private ImageButton mShuffle;
    private ImageButton mRepeat;
    private ImageButton mAdvFunc;
    private ImageButton mPlaylistSwitch, mPlaylistSave;
    private SeekBar mTimeline;
    private RecyclerView mSongsList;

    ViewSwitcher mSwitcher;

    private boolean mShowRemainingTime = false;
    private boolean mPreviewingSeek = false;
    private boolean mSwitchedToVideo = false;

    private PlaylistAdapter mPlaylistAdapter;

    private boolean mAdvFuncVisible;
    private boolean mPlaylistSwitchVisible;
    private boolean mPlaylistSaveVisible;
    private boolean mHeaderPlayPauseVisible;
    private boolean mProgressBarVisible;
    private boolean mHeaderTimeVisible;

    // Tips
    private static final String PREF_PLAYLIST_TIPS_SHOWN = "playlist_tips_shown";
    private static final String PREF_AUDIOPLAYER_TIPS_SHOWN = "audioplayer_tips_shown";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mPlaylistAdapter = new PlaylistAdapter(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.audio_player, container, false);

        mProgressBar = (ProgressBar) v.findViewById(R.id.progressBar);

        mHeaderMediaSwitcher = (HeaderMediaSwitcher) v.findViewById(R.id.audio_media_switcher);
        mHeaderMediaSwitcher.setAudioMediaSwitcherListener(mHeaderMediaSwitcherListener);
        mCoverMediaSwitcher = (CoverMediaSwitcher) v.findViewById(R.id.cover_media_switcher);
        mCoverMediaSwitcher.setAudioMediaSwitcherListener(mCoverMediaSwitcherListener);

        mTime = (TextView) v.findViewById(R.id.time);
        mHeaderTime = (TextView) v.findViewById(R.id.header_time);
        mLength = (TextView) v.findViewById(R.id.length);
        mResumeToVideo = (ImageButton) v.findViewById(R.id.playlist_playasaudio_off);
        mPlayPause = (ImageButton) v.findViewById(R.id.play_pause);
        mHeaderPlayPause = (ImageButton) v.findViewById(R.id.header_play_pause);
        mNext = (ImageButton) v.findViewById(R.id.next);
        mPrevious = (ImageButton) v.findViewById(R.id.previous);
        mShuffle = (ImageButton) v.findViewById(R.id.shuffle);
        mRepeat = (ImageButton) v.findViewById(R.id.repeat);
        mAdvFunc = (ImageButton) v.findViewById(R.id.adv_function);
        mPlaylistSwitch = (ImageButton) v.findViewById(R.id.playlist_switch);
        mPlaylistSave = (ImageButton) v.findViewById(R.id.playlist_save);
        mTimeline = (SeekBar) v.findViewById(R.id.timeline);

        mSongsList = (RecyclerView) v.findViewById(R.id.songs_list);
        final LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        layoutManager.setOrientation(LinearLayoutManager.VERTICAL);
        mSongsList.setLayoutManager(layoutManager);
        mSongsList.setAdapter(mPlaylistAdapter);

        ItemTouchHelper.Callback callback =  new SwipeDragItemTouchHelperCallback(mPlaylistAdapter);
        ItemTouchHelper touchHelper = new ItemTouchHelper(callback);
        touchHelper.attachToRecyclerView(mSongsList);

        mSwitcher = (ViewSwitcher) v.findViewById(R.id.view_switcher);
        mSwitcher.setInAnimation(getActivity(), android.R.anim.fade_in);
        mSwitcher.setOutAnimation(getActivity(), android.R.anim.fade_out);

        mAdvFuncVisible = false;
        mPlaylistSwitchVisible = false;
        mPlaylistSaveVisible = false;
        mHeaderPlayPauseVisible = true;
        mProgressBarVisible = true;
        mHeaderTimeVisible = true;
        restoreHedaderButtonVisibilities();

        mTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onTimeLabelClick(v);
            }
        });
        mResumeToVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mService != null) {
                    mService.switchToVideo();
                    mSwitchedToVideo = true;
                }
            }
        });
        mPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlayPauseClick(v);
            }
        });
        mPlayPause.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onStopClick(v);
                return true;
            }
        });
        mHeaderPlayPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onPlayPauseClick(v);
            }
        });
        mHeaderPlayPause.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                onStopClick(v);
                return true;
            }
        });
        mNext.setOnTouchListener(new LongSeekListener(true,
                UiTools.getResourceFromAttribute(getActivity(), R.attr.ic_next),
                R.drawable.ic_next_pressed));
        mPrevious.setOnTouchListener(new LongSeekListener(false,
                UiTools.getResourceFromAttribute(getActivity(), R.attr.ic_previous),
                R.drawable.ic_previous_pressed));
        mShuffle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onShuffleClick(v);
            }
        });
        mRepeat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onRepeatClick(v);
            }
        });
        mAdvFunc.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showAdvancedOptions(v);
            }
        });
        mPlaylistSave.setOnClickListener(this);
        mPlaylistSwitch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSwitcher.showNext();
                if (mSwitcher.getDisplayedChild() == 0)
                    mPlaylistSwitch.setImageResource(UiTools.getResourceFromAttribute(getActivity(),
                            R.attr.ic_playlist_on));
                else
                    mPlaylistSwitch.setImageResource(UiTools.getResourceFromAttribute(getActivity(),
                            R.attr.ic_playlist));
            }
        });
        registerForContextMenu(mSongsList);

        getActivity().setVolumeControlStream(AudioManager.STREAM_MUSIC);

        return v;
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        getView().cancelLongPress();
    }

    public void onPopupMenu(View anchor, final int position) {
        PopupMenu popupMenu = new PopupMenu(getActivity(), anchor);
        popupMenu.getMenuInflater().inflate(R.menu.audio_player, popupMenu.getMenu());

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if(item.getItemId() == R.id.audio_player_mini_remove) {
                    if (mService != null) {
                        mService.remove(position);
                        return true;
                    }
                }
                return false;
            }
        });
        popupMenu.show();
    }

    /**
     * Show the audio player from an intent
     *
     * @param context The context of the activity
     */
    public static void start(Context context) {
        Intent intent = new Intent();
        intent.setAction(AudioPlayerContainerActivity.ACTION_SHOW_PLAYER);
        context.getApplicationContext().sendBroadcast(intent);
    }

    @Override
    public void update() {
        if (mService == null || getActivity() == null)
            return;

        if (mService.hasMedia() && !mService.isVideoPlaying()) {
            SharedPreferences mSettings= PreferenceManager.getDefaultSharedPreferences(getActivity());
            if (mSettings.getBoolean(PreferencesActivity.VIDEO_RESTORE, false)){
                Util.commitPreferences(mSettings.edit().putBoolean(PreferencesActivity.VIDEO_RESTORE, false));
                mService.switchToVideo();
                mSwitchedToVideo = true;
                return;
            } else
                show();
        } else {
            hide();
            return;
        }

        mHeaderMediaSwitcher.updateMedia(mService);
        mCoverMediaSwitcher.updateMedia(mService);

        FragmentActivity act = getActivity();
        mResumeToVideo.setVisibility(mService.getVideoTracksCount() > 0 ? View.VISIBLE : View.GONE);

        if (mService.isPlaying()) {
            mPlayPause.setImageResource(UiTools.getResourceFromAttribute(act, R.attr.ic_pause));
            mPlayPause.setContentDescription(getString(R.string.pause));
            mHeaderPlayPause.setImageResource(UiTools.getResourceFromAttribute(act, R.attr.ic_pause));
            mHeaderPlayPause.setContentDescription(getString(R.string.pause));
        } else {
            mPlayPause.setImageResource(UiTools.getResourceFromAttribute(act, R.attr.ic_play));
            mPlayPause.setContentDescription(getString(R.string.play));
            mHeaderPlayPause.setImageResource(UiTools.getResourceFromAttribute(act, R.attr.ic_play));
            mHeaderPlayPause.setContentDescription(getString(R.string.play));
        }
        if (mService.isShuffling()) {
            mShuffle.setImageResource(UiTools.getResourceFromAttribute(act, R.attr.ic_shuffle_on));
            mShuffle.setContentDescription(getResources().getString(R.string.shuffle_on));
        } else {
            mShuffle.setImageResource(UiTools.getResourceFromAttribute(act, R.attr.ic_shuffle));
            mShuffle.setContentDescription(getResources().getString(R.string.shuffle));
        }
        switch(mService.getRepeatType()) {
        case PlaybackService.REPEAT_NONE:
            mRepeat.setImageResource(UiTools.getResourceFromAttribute(act, R.attr.ic_repeat));
            mRepeat.setContentDescription(getResources().getString(R.string.repeat));
            break;
        case PlaybackService.REPEAT_ONE:
            mRepeat.setImageResource(UiTools.getResourceFromAttribute(act, R.attr.ic_repeat_one));
            mRepeat.setContentDescription(getResources().getString(R.string.repeat_single));
            break;
        default:
        case PlaybackService.REPEAT_ALL:
            mRepeat.setImageResource(UiTools.getResourceFromAttribute(act, R.attr.ic_repeat_on));
            mRepeat.setContentDescription(getResources().getString(R.string.repeat_all));
            break;
        }

        final List<String> mediaLocations = mService.getMediaLocations();
        mShuffle.setVisibility(mediaLocations != null && mediaLocations.size() > 2 ? View.VISIBLE : View.INVISIBLE);
        mTimeline.setOnSeekBarChangeListener(mTimelineListner);

        updateList();
    }

    @Override
    public void updateProgress() {
        if (mService == null)
            return;
        int time = (int) mService.getTime();
        int length = (int) mService.getLength();

        mHeaderTime.setText(Strings.millisToString(time));
        mLength.setText(Strings.millisToString(length));
        mTimeline.setMax(length);
        mProgressBar.setMax(length);

        if(!mPreviewingSeek) {
            mTime.setText(Strings.millisToString(mShowRemainingTime ? time-length : time));
            mTimeline.setProgress(time);
            mProgressBar.setProgress(time);
        }
    }

    @Override
    public void onMediaEvent(Media.Event event) {}

    @Override
    public void onMediaPlayerEvent(MediaPlayer.Event event) {
        switch (event.type) {
            case MediaPlayer.Event.Opening:
                mSwitchedToVideo = false;
                break;
            case MediaPlayer.Event.ESAdded:
                final boolean forceAudio = (mService.getCurrentMediaWrapper().getFlags() & MediaWrapper.MEDIA_FORCE_AUDIO) != 0;
                if (!forceAudio && !mSwitchedToVideo && event.getEsChangedType() == Media.Track.Type.Video) {
                    mService.switchToVideo();
                    mSwitchedToVideo = true;
                }
                break;
            case MediaPlayer.Event.Stopped:
                hide();
                break;
        }
    }

    public void updateList() {
        int currentIndex = -1, oldCount = mPlaylistAdapter.getItemCount();
        if (mService == null)
            return;

        final List<MediaWrapper> previousAudioList = mPlaylistAdapter.getMedias();
        mPlaylistAdapter.clear();

        final List<MediaWrapper> audioList = mService.getMedias();
        final String currentItem = mService.getCurrentMediaLocation();

        if (audioList != null) {
            for (int i = 0; i < audioList.size(); i++) {
                final MediaWrapper media = audioList.get(i);
                if (currentItem != null && currentItem.equals(media.getLocation()))
                    currentIndex = i;
                mPlaylistAdapter.add(media);
            }
        }
        mPlaylistAdapter.setCurrentIndex(currentIndex);
        int count = mPlaylistAdapter.getItemCount();
        if (oldCount != count)
            mPlaylistAdapter.notifyDataSetChanged();
        else
            mPlaylistAdapter.notifyItemRangeChanged(0, count);

        final int selectionIndex = currentIndex;
        if (!previousAudioList.equals(audioList))
            mSongsList.post(new Runnable() {
                @Override
                public void run() {
                    mPlaylistAdapter.setCurrentIndex(selectionIndex);
                }
            });
    }

    OnSeekBarChangeListener mTimelineListner = new OnSeekBarChangeListener() {

        @Override
        public void onStopTrackingTouch(SeekBar arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onStartTrackingTouch(SeekBar arg0) {
            // TODO Auto-generated method stub

        }

        @Override
        public void onProgressChanged(SeekBar sb, int prog, boolean fromUser) {
            if (fromUser && mService != null) {
                mService.setTime(prog);
                mTime.setText(Strings.millisToString(mShowRemainingTime ? prog- mService.getLength() : prog));
                mHeaderTime.setText(Strings.millisToString(prog));
        }
    }
    };

    public void onTimeLabelClick(View view) {
        mShowRemainingTime = !mShowRemainingTime;
        update();
    }

    public void onPlayPauseClick(View view) {
        if (mService == null)
            return;
        if (mService.isPlaying()) {
            mService.pause();
        } else {
            mService.play();
        }
    }

    public void onStopClick(View view) {
        if (mService != null)
            mService.stop();
    }

    public void onNextClick(View view) {
        if (mService == null)
            return;
        if (mService.hasNext())
            mService.next();
        else
            Snackbar.make(getView(), R.string.lastsong, Snackbar.LENGTH_SHORT).show();
    }

    public void onPreviousClick(View view) {
        if (mService == null)
            return;
        if (mService.hasPrevious())
            mService.previous();
        else
            Snackbar.make(getView(), R.string.firstsong, Snackbar.LENGTH_SHORT).show();
    }

    public void onRepeatClick(View view) {
        if (mService == null)
            return;

        switch (mService.getRepeatType()) {
            case PlaybackService.REPEAT_NONE:
                mService.setRepeatType(PlaybackService.REPEAT_ALL);
                break;
            case PlaybackService.REPEAT_ALL:
                mService.setRepeatType(PlaybackService.REPEAT_ONE);
                break;
            default:
            case PlaybackService.REPEAT_ONE:
                mService.setRepeatType(PlaybackService.REPEAT_NONE);
                break;
        }
        update();
    }

    public void onShuffleClick(View view) {
        if (mService != null)
            mService.shuffle();
        update();
    }

    public void showAdvancedOptions(View v) {
        FragmentManager fm = getActivity().getSupportFragmentManager();
        AdvOptionsDialog advOptionsDialog = new AdvOptionsDialog();
        Bundle args = new Bundle();
        args.putInt(AdvOptionsDialog.MODE_KEY, AdvOptionsDialog.MODE_AUDIO);
        advOptionsDialog.setArguments(args);
        advOptionsDialog.show(fm, "fragment_adv_options");
    }

    public void show() {
        AudioPlayerContainerActivity activity = (AudioPlayerContainerActivity)getActivity();
        if (activity != null)
            activity.showAudioPlayer();
    }

    public void hide() {
        AudioPlayerContainerActivity activity = (AudioPlayerContainerActivity)getActivity();
        if (activity != null)
            activity.hideAudioPlayer();
    }

    /**
     * Set the visibilities of the player header elements.
     * @param advFuncVisible
     * @param playlistSwitchVisible
     * @param headerPlayPauseVisible
     */
    public void setHeaderVisibilities(boolean advFuncVisible, boolean playlistSwitchVisible,
                                      boolean headerPlayPauseVisible, boolean progressBarVisible,
                                      boolean headerTimeVisible, boolean playlistSaveVisible) {
        mAdvFuncVisible = advFuncVisible;
        mPlaylistSwitchVisible = playlistSwitchVisible;
        mHeaderPlayPauseVisible = headerPlayPauseVisible;
        mProgressBarVisible = progressBarVisible;
        mHeaderTimeVisible = headerTimeVisible;
        mPlaylistSaveVisible = playlistSaveVisible;
        restoreHedaderButtonVisibilities();
    }

    private void restoreHedaderButtonVisibilities() {
        mAdvFunc.setVisibility(mAdvFuncVisible ? ImageButton.VISIBLE : ImageButton.GONE);
        mPlaylistSwitch.setVisibility(mPlaylistSwitchVisible ? ImageButton.VISIBLE : ImageButton.GONE);
        mPlaylistSave.setVisibility(mPlaylistSaveVisible ? ImageButton.VISIBLE : ImageButton.GONE);
        mHeaderPlayPause.setVisibility(mHeaderPlayPauseVisible ? ImageButton.VISIBLE : ImageButton.GONE);
        mProgressBar.setVisibility(mProgressBarVisible ? ProgressBar.VISIBLE : ProgressBar.GONE);
        mHeaderTime.setVisibility(mHeaderTimeVisible ? TextView.VISIBLE : TextView.GONE);
    }

    private void hideHedaderButtons() {
        mAdvFunc.setVisibility(ImageButton.GONE);
        mPlaylistSwitch.setVisibility(ImageButton.GONE);
        mPlaylistSave.setVisibility(ImageButton.GONE);
        mHeaderPlayPause.setVisibility(ImageButton.GONE);
        mHeaderTime.setVisibility(TextView.GONE);
    }

    private final AudioMediaSwitcherListener mHeaderMediaSwitcherListener = new AudioMediaSwitcherListener() {

        @Override
        public void onMediaSwitching() {}

        @Override
        public void onMediaSwitched(int position) {
            if (mService == null)
                return;
            if (position == AudioMediaSwitcherListener.PREVIOUS_MEDIA)
                mService.previous();
            else if (position == AudioMediaSwitcherListener.NEXT_MEDIA)
                mService.next();
        }

        @Override
        public void onTouchDown() {
            hideHedaderButtons();
        }

        @Override
        public void onTouchUp() {
            restoreHedaderButtonVisibilities();
        }

        @Override
        public void onTouchClick() {
            AudioPlayerContainerActivity activity = (AudioPlayerContainerActivity)getActivity();
            activity.slideUpOrDownAudioPlayer();
        }
    };

    private final AudioMediaSwitcherListener mCoverMediaSwitcherListener = new AudioMediaSwitcherListener() {

        @Override
        public void onMediaSwitching() {}

        @Override
        public void onMediaSwitched(int position) {
            if (mService == null)
                return;
            if (position == AudioMediaSwitcherListener.PREVIOUS_MEDIA)
                mService.previous();
            else if (position == AudioMediaSwitcherListener.NEXT_MEDIA)
                mService.next();
        }

        @Override
        public void onTouchDown() {}

        @Override
        public void onTouchUp() {}

        @Override
        public void onTouchClick() {}
    };

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.playlist_save:
                if (mService == null)
                    return;
                FragmentManager fm = getActivity().getSupportFragmentManager();
                SavePlaylistDialog savePlaylistDialog = new SavePlaylistDialog();
                Bundle args = new Bundle();
                args.putParcelableArrayList(SavePlaylistDialog.KEY_TRACKS, (ArrayList<MediaWrapper>) mService.getMedias());
                savePlaylistDialog.setArguments(args);
                savePlaylistDialog.show(fm, "fragment_save_playlist");
                break;
        }
    }

    @Override
    public void onConnected(PlaybackService service) {
        super.onConnected(service);
        mService.addCallback(this);
        mPlaylistAdapter.setService(service);
        update();
    }

    @Override
    public void onStop() {
        /* unregister before super.onStop() since mService is set to null from this call */
        if (mService != null)
            mService.removeCallback(this);
        super.onStop();
    }

    class LongSeekListener implements View.OnTouchListener {
        boolean forward;
        int normal, pressed;
        long length;

        public LongSeekListener(boolean forwards, int normalRes, int pressedRes) {
            this.forward = forwards;
            this.normal = normalRes;
            this.pressed = pressedRes;
            this.length = -1;
        }

        int possibleSeek;
        boolean vibrated;

        @RequiresPermission(Manifest.permission.VIBRATE)
        Runnable seekRunnable = new Runnable() {
            @Override
            public void run() {
                if(!vibrated) {
                    ((android.os.Vibrator) VLCApplication.getAppContext().getSystemService(Context.VIBRATOR_SERVICE))
                                .vibrate(80);
                    vibrated = true;
                }

                if(forward) {
                    if(length <= 0 || possibleSeek < length)
                        possibleSeek += 4000;
                } else {
                    if(possibleSeek > 4000)
                        possibleSeek -= 4000;
                    else if(possibleSeek <= 4000)
                        possibleSeek = 0;
                }

                mTime.setText(Strings.millisToString(mShowRemainingTime ? possibleSeek-length : possibleSeek));
                mTimeline.setProgress(possibleSeek);
                mProgressBar.setProgress(possibleSeek);
                h.postDelayed(seekRunnable, 50);
            }
        };

        Handler h = new Handler();

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mService == null)
                return false;
            switch(event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                (forward ? mNext : mPrevious).setImageResource(this.pressed);

                possibleSeek = (int) mService.getTime();

                mPreviewingSeek = true;
                vibrated = false;
                length = mService.getLength();

                h.postDelayed(seekRunnable, 1000);
                return true;

            case MotionEvent.ACTION_UP:
                (forward ? mNext : mPrevious).setImageResource(this.normal);
                h.removeCallbacks(seekRunnable);
                mPreviewingSeek = false;

                if(event.getEventTime()-event.getDownTime() < 1000) {
                    if(forward)
                        onNextClick(v);
                    else
                        onPreviousClick(v);
                } else {
                    if(forward) {
                        if(possibleSeek < mService.getLength())
                            mService.setTime(possibleSeek);
                        else
                            onNextClick(v);
                    } else {
                        if(possibleSeek > 0)
                            mService.setTime(possibleSeek);
                        else
                            onPreviousClick(v);
                    }
                }
                return true;
            }
            return false;
        }
    }

    public void showPlaylistTips() {
        AudioPlayerContainerActivity activity = (AudioPlayerContainerActivity)getActivity();
        if(activity != null)
            activity.showTipViewIfNeeded(R.layout.audio_playlist_tips, PREF_PLAYLIST_TIPS_SHOWN);
    }

    public void showAudioPlayerTips() {
        AudioPlayerContainerActivity activity = (AudioPlayerContainerActivity)getActivity();
        if(activity != null)
            activity.showTipViewIfNeeded(R.layout.audio_player_tips, PREF_AUDIOPLAYER_TIPS_SHOWN);
    }

    /*
     * Override this method to prefent NPE on mFragmentManager reference.
     */
    @Override
    public void setUserVisibleHint(boolean isVisibleToUser) {
        if (getFragmentManager() != null)
            super.setUserVisibleHint(isVisibleToUser);
    }
}
