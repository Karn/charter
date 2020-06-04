package io.karn.charter.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.util.Log
import android.view.View
import io.karn.charter.R
import io.karn.charter.internal.util.Utils

class PieChart(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    // Attributes
    private var textColor: Int = 0
    private var chartColor: Int = 0


    private var computedWidth: Int = 0
    private var computedHeight: Int = 0


    private var labelHeight: Float = 0f

    private val paint: Paint
    private val textPaint: Paint

    private var rect = RectF(paddingStart.toFloat(), paddingTop.toFloat(), (paddingStart + computedWidth).toFloat(), (paddingTop + computedWidth).toFloat())

    private var data = arrayListOf<Int>()

    init {
        // Parse styling
        val styledAttrs = context.obtainStyledAttributes(attrs, R.styleable.PieChart)
        styledAttrs?.run {
            // Text color for the labels.
            textColor = this.getColor(R.styleable.PieChart_labelColor, resources.getColor(android.R.color.primary_text_light))
        }
        styledAttrs.recycle()

        /*
         * Initialize the paint object used to draw the data segments. The color of the segment is
         * taken from the DataMatrix object.
         */
        paint = Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = chartColor
        }

        labelHeight = Utils.dpToPx(16, context)

        /*
         * Initialize the paint object used to draw the textLabels.
         */
        textPaint = Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = textColor
            it.textSize = labelHeight - Utils.dpToPx(2, context)
            it.textAlign = Paint.Align.CENTER
        }
    }


    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        computedHeight = (h - paddingTop - paddingBottom - labelHeight).toInt()
        computedWidth = (w - paddingStart - paddingEnd)

        rect = RectF(paddingStart.toFloat(), paddingTop.toFloat(), (paddingStart + computedWidth).toFloat(), (paddingTop + computedWidth).toFloat())
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return

        var start = -90F
        val total = data.sum().toDouble()
        for (value in data) {

            val prop = if (total > 0) (value / total) else 0.0
            val angle = if (prop != 0.0) 360.0 * prop else 0.0

            Log.v("TAG", "Drawing: $value, total: $total, prop: $prop, angle: $angle")

            val paint = paint
            paint.alpha -= (prop * 100).toInt()

            canvas.drawArc(rect, start, angle.toFloat(), true, paint)
            start += angle.toFloat()
        }

        canvas.drawArc(rect, start, -90F, true, textPaint.also { it.alpha = 1 })
    }

    fun setChartColor(colorId: Int) {
        paint.color = this.resources.getColor(colorId)

        invalidate()
    }

    fun setData(data: ArrayList<Int>) {
        this.data = data

        invalidate()
    }
}
