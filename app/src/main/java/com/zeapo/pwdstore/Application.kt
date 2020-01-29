/*
 * Copyright © 2014-2020 The Android Password Store Authors. All Rights Reserved.
 * SPDX-License-Identifier: GPL-3.0-only
 */
package com.zeapo.pwdstore

import com.haroldadmin.whatthestack.WhatTheStack
import com.zeapo.pwdstore.di.AppComponent
import com.zeapo.pwdstore.di.DaggerAppComponent
import com.zeapo.pwdstore.di.InjectorProvider
import timber.log.Timber

@Suppress("Unused")
class Application : android.app.Application(), InjectorProvider {

    override val component: AppComponent by lazy {
        DaggerAppComponent.factory().create(this)
    }

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        if (BuildConfig.ENABLE_DEBUG_FEATURES) {
            WhatTheStack(this).init()
        }
    }
}
