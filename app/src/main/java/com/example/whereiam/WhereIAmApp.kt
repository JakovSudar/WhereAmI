package com.example.whereiam

import android.app.Application
import android.content.Context

class WhereIAmApp : Application(){
    companion object {
        lateinit var ApplicationContext: Context
            private set
    }
    override fun onCreate() {
        super.onCreate()
        ApplicationContext = this

    }

}