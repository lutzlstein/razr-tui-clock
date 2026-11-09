package com.coryblische.razrclock.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

internal val Context.clockDataStore by preferencesDataStore(name = "clock")
