package com.example.mediatemplate

import android.app.Application
import android.app.Dialog
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.DialogFragment

class MediaApplication :Application() {

    override fun onCreate() {
        super.onCreate()
        if(Build.VERSION.SDK_INT <29){
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }else{
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

    }
}