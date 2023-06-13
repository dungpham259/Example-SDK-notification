package util

import android.content.Context
import android.content.pm.ApplicationInfo

internal fun isDebugBuild(context: Context): Boolean {
    return 0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
}
