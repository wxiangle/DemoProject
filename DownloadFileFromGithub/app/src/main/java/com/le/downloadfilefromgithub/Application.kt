package com.le.downloadfilefromgithub

import android.app.Application

/**
 * Created by wangxl1 on 2024/7/8 14:57
 * E-Mail Address： wang_x_le@163.com
 */
open class LeApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        application = this
    }

    companion object {
        var application: LeApplication? = null
    }
}