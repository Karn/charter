package io.karn.charter.internal.util

import android.content.Context
import android.content.res.TypedArray
import android.util.AttributeSet

internal object Utils {
    fun dpToPx(dip: Int, context: Context): Float {
        val scale = context.resources.displayMetrics.density
        return dip * scale + 0.5f
    }
}

internal inline fun AttributeSet?.withStyleable(context: Context, styleableResource: IntArray, setter: TypedArray.() -> Unit) {
    if (this == null) {
        return
    }

    val styledAttrs = context.obtainStyledAttributes(this, styleableResource)
    styledAttrs?.setter()
    styledAttrs?.recycle()
}
