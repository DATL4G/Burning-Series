package dev.datlag.burningseries.color.quantize


class QuantizerMap : Quantizer {
    var colorToCount: Map<Int, Int>? = null

    override fun quantize(pixels: IntArray?, maxColors: Int): QuantizerResult {
        val pixelByCount: MutableMap<Int, Int> = LinkedHashMap()
        for (pixel in pixels!!) {
            val currentPixelCount = pixelByCount[pixel]
            val newPixelCount = if (currentPixelCount == null) 1 else currentPixelCount + 1
            pixelByCount[pixel] = newPixelCount
        }
        colorToCount = pixelByCount
        return QuantizerResult(pixelByCount)
    }
}