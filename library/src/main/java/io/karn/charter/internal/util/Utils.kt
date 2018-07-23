package io.karn.charter.internal.util

import android.content.Context

internal object Utils {
    fun dpToPx(dip: Int, context: Context): Float {
        val scale = context.resources.displayMetrics.density
        return dip * scale + 0.5f
    }
}
