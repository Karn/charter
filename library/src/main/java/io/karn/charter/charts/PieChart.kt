package io.karn.charter.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.RectF
import android.util.AttributeSet
import io.karn.charter.R
import io.karn.charter.internal.util.CharterView
import io.karn.charter.internal.util.Utils
import io.karn.charter.internal.util.withStyleable
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

class PieChart(context: Context, attrs: AttributeSet? = null) : CharterView(context, attrs) {

    // Attributes
    private var textColor: Int = 0
    private var chartColor: Int = 0


    private var labelHeight: Float = 0f

    private val paint: Paint
    private val textPaint: Paint

    private var rect = RectF(paddingStart.toFloat(), paddingTop.toFloat(), (paddingStart + computedHeight).toFloat(), (paddingTop + computedWidth).toFloat())

    // Convert this to something more robust
    private var data = arrayListOf<Pair<String, Int>>()

    init {
        // Parse styling
        attrs.withStyleable(this.context, R.styleable.PieChart) {
            textColor = this.getColor(R.styleable.PieChart_labelColor, resources.getColor(android.R.color.primary_text_light))
        }

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

        // TODO: Define terminology around workable area. e.g is width actually view width - insets?
        // Since a pie chart is a circle the boundaries are the smallest working space
        val diameter = min(computedWidth, computedHeight)

        rect = RectF(paddingStart.toFloat(), paddingTop.toFloat(), (paddingStart + diameter).toFloat(), (paddingTop + diameter).toFloat())
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return

        var start = -90F
        val total = data.sumBy { it.second }.toDouble()

        val paint = paint
        val originalAlpha = paint.alpha

        for ((label, value) in data) {

            val prop = if (total > 0) (value / total) else 0.0
            val angle = if (prop != 0.0) 360.0 * prop else 0.0

            paint.alpha = originalAlpha - (prop * originalAlpha).toInt()

            canvas.drawArc(rect, start, angle.toFloat(), true, paint)
            start += angle.toFloat()
        }

        // Catch straggler
        canvas.drawArc(rect, start, -90F, true, paint.also { it.alpha = 1 })

        paint.alpha = originalAlpha
    }

    private fun getLabelPoint(center: PointF, angle: Float, radius: Float): PointF {
        val x = center.x + (radius * sin(angle))
        val y = center.y + (radius * cos(angle))

        return PointF(x, y)
    }

    fun setChartColor(colorId: Int) {
        paint.color = this.resources.getColor(colorId)

        invalidate()
    }

    fun setData(data: ArrayList<Pair<String, Int>>) {
        this.data = data

        // TODO: Handle caching of proportions and alpha here.

        invalidate()
    }
}
