package com.example.mediaplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Bundle;
import android.view.View;

public class MainActivity extends AppCompatActivity {

    private MediaPlayer mMediaPlayer;
    private static final float MEDIA_VOLUME_DEFAULT = 1.0f;
    private static final float MEDIA_VOLUME_DUCK = 0.2f;
    private AudioManager mAudioManager;
    private boolean focusGained = false;
    private boolean pause = false;
    private boolean focusReleased = false;
    private boolean appInBackground = false;
    private boolean focusLossTransientCanDuck = false;
    private boolean isPlaying = false;

    /**
     * This listener gets triggered when the MediaPlayer has completed
     * playing the audio file.
     */
    private MediaPlayer.OnCompletionListener mCompletionListener = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            // Now that the sound file has finished playing, release the media player resources.
            releaseMediaPlayer();
        }
    };

    /**
     * This listener gets triggered whenever the audio focus changes
     * (i.e., we gain or lose audio focus because of another app or device).
     */
    private AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener = new AudioManager.OnAudioFocusChangeListener() {
        @Override
        public void onAudioFocusChange(int focusChange) {
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    focusGained = true;
                    if (focusLossTransientCanDuck) {
                        mMediaPlayer.setVolume(MEDIA_VOLUME_DEFAULT, MEDIA_VOLUME_DEFAULT);
                        focusLossTransientCanDuck = false;
                    }
                    if (pause && isPlaying && !appInBackground) {
                        mMediaPlayer.start();
                        pause = false;
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    mMediaPlayer.setVolume(MEDIA_VOLUME_DUCK, MEDIA_VOLUME_DUCK);
                    focusLossTransientCanDuck = true;
                    break;
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    focusGained = false;
                    if (mMediaPlayer != null && isPlaying) {
                        mMediaPlayer.pause();
                        pause = true;
                    }
                    break;
                case AudioManager.AUDIOFOCUS_LOSS:
                    mAudioManager.abandonAudioFocus(this);
                    focusGained = false;
                    releaseMediaPlayer();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        callAudioFocus();
    }

    public void play(View v) {
        if (focusGained) {
            if (mMediaPlayer == null) {
                startMediaPlayer();
            } else {
                mMediaPlayer.start();
            }
            isPlaying = true;
        }
    }

    public void pause(View v) {
        if (mMediaPlayer != null) {
            mMediaPlayer.pause();
            isPlaying = false;
        }
    }

    public void restart(View v) {
        if (focusGained) {
            if (mMediaPlayer != null) {
                releaseMediaPlayer();
            }
            startMediaPlayer();
            isPlaying = true;
        }
    }

    public void stop(View v) {
        releaseMediaPlayer();
        isPlaying = false;
    }

    @Override
    protected void onStart() {
        super.onStart();
        appInBackground = false;
    }

    /**
     * Resume media player when onResume method is called if it was playing
     */
    @Override
    protected void onResume() {
        super.onResume();
        appInBackground = false;
        if (pause && isPlaying) {
            mMediaPlayer.start();
            pause = false;
        }

    }

    /**
     * Pause media player when onPause method is called
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mMediaPlayer != null && isPlaying) {
            mMediaPlayer.pause();
            pause = true;
        }
        if(isPlaying){
            appInBackground = true;
        }
    }

    /**
     * depends on apps needed we can release media player on onStop method,
     * on this project I'm destroy it in onDestroy method
     */
    @Override
    protected void onStop() {
        super.onStop();
        //releaseMediaPlayer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseMediaPlayer();
    }

    /**
     * Clean up the media player by releasing its resources.
     */
    private void releaseMediaPlayer() {
        // If the media player is not null, then it may be currently playing a sound.
        if (mMediaPlayer != null) {
            // Regardless of the current state of the media player, release its resources
            // because we no longer need it.
            mMediaPlayer.release();

            // Set the media player back to null. For our code, we've decided that
            // setting the media player to null is an easy way to tell that the media player
            // is not configured to play an audio file at the moment.
            mMediaPlayer = null;

            // Regardless of whether or not we were granted audio focus, abandon it. This also
            // unregisters the AudioFocusChangeListener so we don't get anymore callbacks.
            mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
            focusReleased = true;
        }
    }

    private void startMediaPlayer() {

        //after releaseMediaPlayer() method is called, it's necessary to call audioFocus again because it's
        //released currently
        if (focusReleased) {
            callAudioFocus();
        }
        mMediaPlayer = MediaPlayer.create(this, R.raw.rondo_alla_turca);
        mMediaPlayer.setOnCompletionListener(mCompletionListener);
        mMediaPlayer.start();
    }

    private void callAudioFocus() {
        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.O) {
            AudioAttributes mAudioAttributes =
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build();
            AudioFocusRequest mAudioFocusRequest =
                    new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                            .setAudioAttributes(mAudioAttributes)
                            .setAcceptsDelayedFocusGain(true)
                            .setOnAudioFocusChangeListener(mOnAudioFocusChangeListener)
                            .build();
            int focusRequest = mAudioManager.requestAudioFocus(mAudioFocusRequest);
            switch (focusRequest) {
                case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                    focusGained = false;
                case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:
                    focusGained = true;
            }
        }
        else {
            int focusRequest = mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN);
            switch (focusRequest) {
                case AudioManager.AUDIOFOCUS_REQUEST_FAILED:
                    focusGained = false;
                case AudioManager.AUDIOFOCUS_REQUEST_GRANTED:
                    focusGained = true;
            }
        }

        focusReleased = false;
    }
}