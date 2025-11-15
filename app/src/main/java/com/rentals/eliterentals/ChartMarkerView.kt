package com.rentals.eliterentals

import android.content.Context
import android.widget.TextView
import com.github.mikephil.charting.components.MarkerView
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.highlight.Highlight
import com.github.mikephil.charting.utils.MPPointF
import com.rentals.eliterentals.R

class ChartMarkerView(context: Context, layoutResource: Int) : MarkerView(context, layoutResource) {
    private val tvContent: TextView = findViewById(R.id.tvMarkerContent)

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        tvContent.text = "R ${e?.y?.toInt()}"
        super.refreshContent(e, highlight)
    }

    override fun getOffset(): MPPointF {
        return MPPointF((-width / 2).toFloat(), -height.toFloat())
    }
}
