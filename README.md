# MediaPlayer-AudioFocus
Android, Java, MediaPlayer, AudioFocus, Lifecycle

This project is focused on MediPlayer and AudioFocus which are two important components for developing applications with audio or/and video.

A MediaPlayer can consume valuable system resources so when you are done with it, you should always call release() to make sure any system
resources allocated to it are properly released. 
On this point I called an OnCompletionListener which gets triggered when the MediaPlayer has completed playing the audio file also I follow
media and application lifecycle to release it when the app or media is stopped.

To call mediaPlayer firs we need to check if we have audioFocus because if we haven't it maybe another application is using it and if we start mediaPlayer without audioFocus it will be overlying. Triggered mediaPlayer with audioFocus will remove the focus where priority is higher.

