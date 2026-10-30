package com.sam.gogotimeline

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/** App-level Hilt entry point; app-wide singletons such as the database hang off this component. */
@HiltAndroidApp
class GogoTimelineApp : Application()
