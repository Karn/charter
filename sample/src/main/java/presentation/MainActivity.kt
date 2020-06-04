package presentation

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import io.karn.charter.sample.R
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pie_chart.setChartColor(R.color.colorAccent)
        pie_chart.setData(arrayListOf(1, 3, 6, 9))
    }
}
