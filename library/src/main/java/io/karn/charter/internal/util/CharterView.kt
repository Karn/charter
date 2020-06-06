package io.karn.charter.internal.util

import android.content.Context
import android.util.AttributeSet
import android.view.View

// Provide an base class which defines key ares such as title, margins (x/y labels), legend, etc.
abstract class CharterView(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    /**


    ----------------------------------------------------------------
    HEADER
    ----------------------------------------------------------------


    CHART AREA


    ----------------------------------------------------------------
    FOOTER
    ----------------------------------------------------------------


     */

    protected var computedHeight: Int = 0
    protected var computedWidth: Int = 0

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        //
        computedHeight = (h - paddingTop - paddingBottom)
        computedWidth = (w - paddingLeft - paddingRight)
    }
}
