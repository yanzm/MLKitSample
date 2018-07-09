package net.yanzm.mlkitsample

import android.graphics.Rect

sealed class GraphicData

data class BoxData(
        val text: String,
        val boundingBox: Rect
) : GraphicData()

data class TextsData(
        val texts: List<String>
) : GraphicData()
