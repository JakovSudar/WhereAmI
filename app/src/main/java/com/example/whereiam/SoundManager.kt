package com.example.whereiam

import android.content.Context
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import java.util.HashMap

class SoundManager(val mContext : Context) {
    private lateinit var mSoundPool: SoundPool
    var mSoundMap: HashMap<Int, Int> = HashMap()
    private var mLoaded: Boolean = false

     fun loadSounds() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            this.mSoundPool = SoundPool.Builder().setMaxStreams(10).build()
        } else {
            this.mSoundPool = SoundPool(10, AudioManager.STREAM_MUSIC, 0)
        }
        this.mSoundPool.setOnLoadCompleteListener { _, _, _ -> mLoaded = true }
        this.mSoundMap[R.raw.tap_sound] = this.mSoundPool.load(mContext, R.raw.tap_sound, 1)
    }
     fun playSound(soundToPlay: Int) {
        val soundID = this.mSoundMap[soundToPlay] ?: 0
        this.mSoundPool.play(soundID, 1f, 1f, 1, 0, 1f)
    }
}