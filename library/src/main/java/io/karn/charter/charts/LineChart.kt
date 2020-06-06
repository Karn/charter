package io.karn.charter.charts

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import io.karn.charter.R
import io.karn.charter.internal.util.Utils
import io.karn.charter.internal.util.withStyleable
import kotlin.math.roundToInt

/**
 * Re-inventing the wheel -- but with cooler spokes.
 *
 * dataPath.moveTo(i * interval + paddingLeft, z)
 * dataPath.lineTo(i * interval + paddingLeft, z)
 * moves creates dots.
 */
class LineChart(context: Context, attrs: AttributeSet? = null) : View(context, attrs) {

    class DataMatrix(val labels: List<String>, var data: List<DataObject>) {

        internal var dataMax: Float = 0f
        internal var dataMin: Float = 0f

        init {
            val allottedSize = labels.size

            val maxSize = data.maxBy { it.data.size }?.data?.size ?: 0
            val minSize = data.minBy { it.data.size }?.data?.size ?: 0

            if (maxSize != minSize) {
                throw IllegalArgumentException("Data lists must be the same size.")
            }

            if (allottedSize != maxSize) {
                throw IllegalArgumentException("List of labels and dataMatrix lists must be the same size.")
            }

            dataMax = data.maxBy { it.data.max() ?: 0f }?.data?.max() ?: 0f
            dataMin = data.minBy { it.data.min() ?: 0f }?.data?.min() ?: 0f
        }
    }

    class DataObject(val label: String, var color: Int, var data: List<Float>) {
        var path: Path? = null
    }

    private var computedWidth: Int = 0
    private var computedHeight: Int = 0

    // Flags to enable intervals.

    // X Intervals
    // Drawn lines?

    private val axisPaint: Paint
    private val paint: Paint
    private val textPaint: Paint
    private var axis: Path? = null
    private var xLabelsPath: Path? = null

    private var labelHeight: Float = 0f

    private var toolTipDrawLocation: Float? = null

    // Attributes
    private var axisColor: Int = 0
    private var textColor: Int = 0

    private var handler: ((List<Pair<String, Float>>) -> Unit)? = null

    /**
     * Primary data source.
     */
    private var dataMatrix: DataMatrix? = null

    init {
        // Parse styling
        attrs.withStyleable(this.context, R.styleable.LineChart) {
            // Just the x-y axis colors.
            axisColor = this.getColor(R.styleable.LineChart_axisColor, resources.getColor(android.R.color.darker_gray))
            // Text color for the labels.
            textColor = this.getColor(R.styleable.LineChart_labelColor, resources.getColor(android.R.color.primary_text_light))
        }

        /*
         * Initialize the paint object used to draw the axis.
         */
        axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.color = axisColor
            it.style = Paint.Style.STROKE
            it.strokeWidth = Utils.dpToPx(2, context)
            it.strokeCap = Paint.Cap.ROUND
        }

        /*
         * Initialize the paint object used to draw the data segments. The color of the segment is
         * taken from the DataMatrix object.
         */
        paint = Paint(Paint.ANTI_ALIAS_FLAG).also {
            it.style = Paint.Style.STROKE
            it.strokeWidth = Utils.dpToPx(2, context)
            it.strokeCap = Paint.Cap.ROUND
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
        computedWidth = (w - originLeft - originRight).toInt()
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        canvas ?: return

        val labels = dataMatrix?.labels ?: return
        val dataPoints = dataMatrix?.data ?: return

        if (axis == null) {
            axis = generateAxis()
        }
        // Switch to axis paint.
        canvas.drawPath(axis, axisPaint)

        for (item in dataPoints) {
            if (item.path == null) {
                item.path = generatePathForData(item.data, dataMatrix!!.dataMax, dataMatrix!!.dataMin)
            }

            // Path
            canvas.drawPath(item.path, paint.also {
                it.color = item.color
            })
        }

        if (xLabelsPath == null) {
            // Generate Labels.
            xLabelsPath = generateXAxisLabels(labels)
        }
        canvas.drawPath(xLabelsPath, textPaint)

        toolTipDrawLocation?.let {
            for (dataSet in dataPoints) {
                if (dataSet.data.isEmpty()) {
                    continue
                }

                val max = dataMatrix?.dataMax ?: continue
                val min = dataMatrix?.dataMin ?: continue

                val index = nearestDateNodeIndex(dataSet.data.size, it)

                if (dataSet.data[index] < 0) {
                    // The value is not populated.
                    return@let
                }

                val z = computedHeight - ((dataSet.data[index] - min) / (max - min) * computedHeight) + paddingBottom

                canvas.drawCircle(originLeft + it, z, Utils.dpToPx(2, context), paint.also { it.color = dataSet.color })
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        event ?: return super.onTouchEvent(event)

        if (event.action != MotionEvent.ACTION_DOWN) return super.onTouchEvent(event)

        val dataSize = dataMatrix?.labels?.size ?: return super.onTouchEvent(event)

        val nodePos = nearestDataNode(dataSize, event.x)
        toolTipDrawLocation = nodePos

        invalidate()

        val dataPoints = dataMatrix?.data ?: return false

        val datePointsAtLocation = ArrayList<Pair<String, Float>>()
        for (dataSet in dataPoints) {
            val index = nearestDateNodeIndex(dataSet.data.size, nodePos)

            val z = dataSet.data[index]
            if (z < 0) return false

            // Add the value and label at that index to the list of datapoints for the tooltip.
            datePointsAtLocation.add(Pair(dataSet.label, z))
        }

        handler?.invoke(datePointsAtLocation)

        return true
    }

    private inline val originLeft: Float
        get() = paddingLeft.toFloat()

    private inline val originRight: Float
        get() = paddingRight.toFloat()

    private fun nearestDataNode(dataSize: Int, x: Float): Float {
        val interval = computedWidth / if (dataSize == 1) 1 else dataSize - 1

        return interval.toFloat() * ((x / interval).roundToInt())
    }

    private fun nearestDateNodeIndex(dataSize: Int, x: Float): Int {
        val interval = computedWidth / if (dataSize == 1) 1 else dataSize - 1

        return Math.min(dataSize - 1, (x / interval).roundToInt())
    }

    private fun generateAxis(): Path {
        val path = Path()

        // x axis
        path.moveTo(originLeft, paddingTop.toFloat())
        path.lineTo(originLeft, computedHeight + paddingBottom.toFloat())

        // y axis
        path.lineTo(originLeft + computedWidth, computedHeight + paddingBottom.toFloat())

        return path
    }

    private fun generateXAxisLabels(labels: List<String>): Path {
        val path = Path()

        val count = if (labels.size == 1) 1f else (labels.size - 1).toFloat()
        val xInterval = computedWidth / count

        // X yIntervalPath.
        for (i in labels.indices) {
            val textLabel = labels[i]
            val tempPath = Path()
            textPaint.getTextPath(textLabel, 0, textLabel.length, originLeft + i * xInterval, paddingTop.toFloat() + computedHeight + labelHeight, tempPath)
            path.addPath(tempPath)
        }

        return path
    }

    private fun generatePathForData(data: List<Float>, max: Float, min: Float): Path {
        val path = Path()

        if (data.isEmpty()) {
            return path
        }

        val interval = computedWidth / if (data.size == 1) 1f else (data.size - 1).toFloat()

        var isPreviousZero = false;
        for (i in data.indices) {
            // Normalize the dataMatrix to the height of the dataMatrix.
            val z = computedHeight - ((data[i] - min) / (max - min) * computedHeight) + paddingBottom

            // Account for the padding.
            if (i == 0) {
                path.moveTo(originLeft, z)
            }

            // draw 0 points
            if (data[i] < 0) {
                isPreviousZero = true
                path.moveTo(i * interval + originLeft, z)
            } else {
                if (isPreviousZero) {
                    path.moveTo(i * interval + originLeft, z)
                    isPreviousZero = false
                }
                // Plot each of the normalized values at the correct interval.
                path.lineTo(i * interval + originLeft, z)
            }
        }

        return path
    }

    fun setData(matrix: DataMatrix) {
        this.dataMatrix = matrix

        // Reset values
        this.axis = null
        this.xLabelsPath = null
        this.toolTipDrawLocation = null

        invalidate()
    }

    fun setOnTooltipChanged(handler: (List<Pair<String, Float>>) -> Unit) {
        this.handler = handler
    }
}
