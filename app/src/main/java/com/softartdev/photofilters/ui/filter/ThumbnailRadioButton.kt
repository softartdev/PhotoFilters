package com.softartdev.photofilters.ui.filter

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.ShapeDrawable
import android.graphics.drawable.StateListDrawable
import android.graphics.drawable.shapes.RectShape
import android.util.AttributeSet
import android.view.Gravity
import androidx.appcompat.widget.AppCompatRadioButton

/**
 * A button with Thumbnail which extends Radio Button.
 *
 *
 * The widget override a background drawable of Radio Button with a StateList Drawable. Each
 * state has a LayerDrawable with a Thumbnail image and a Focus rectangle. It's using original
 * Radio Buttons text as a label, because LayerDrawable showed some issues with
 * Canvas.drawText().
 */
class ThumbnailRadioButton @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = androidx.appcompat.R.attr.radioButtonStyle
) : AppCompatRadioButton(context, attrs, defStyleAttr) {

    init {
        setButtonDrawable(android.R.color.transparent)
    }

    /**
     * Create thumbNail for UI. It invokes RenderScript kernel synchronously in UI-thread,
     * which is OK for small thumbnail (but not ideal).
     */
    fun setThumbnail(bitmap: Bitmap) {
        val width = 72
        val height = 96
        val scale = resources.displayMetrics.density
        val pixelsWidth = (width * scale + 0.5f).toInt()
        val pixelsHeight = (height * scale + 0.5f).toInt()

        val scaledBitmap = Bitmap.createScaledBitmap(bitmap, pixelsWidth, pixelsHeight, false)
        setScaledThumbnail(scaledBitmap)
    }

    private fun setScaledThumbnail(bitmap: Bitmap) {
        // Bitmap drawable
        val bmp = BitmapDrawable(resources, bitmap)
        bmp.gravity = Gravity.CENTER

        val strokeWidth = 24
        // Checked state
        val rectChecked = ShapeDrawable(RectShape()).apply {
            paint.color = -0x1
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth.toFloat()
            intrinsicWidth = bitmap.width + strokeWidth
            intrinsicHeight = bitmap.height + strokeWidth
        }
        val checkedDrawables = arrayOf(bmp, rectChecked)
        val layerChecked = LayerDrawable(checkedDrawables)

        // Unchecked state
        val rectUnchecked = ShapeDrawable(RectShape()).apply {
            paint.color = 0x0
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = strokeWidth.toFloat()
            intrinsicWidth = bitmap.width + strokeWidth
            intrinsicHeight = bitmap.height + strokeWidth
        }
        val uncheckedDrawables = arrayOf(bmp, rectUnchecked)
        val layerUnchecked = LayerDrawable(uncheckedDrawables)
        // StateList drawable
        background = StateListDrawable().apply {
            addState(intArrayOf(android.R.attr.state_checked), layerChecked)
            addState(intArrayOf(), layerUnchecked)
        }
        //Offset text to center/bottom of the checkbox
        val paint = Paint()
        paint.isAntiAlias = true
        paint.textSize = textSize
        paint.typeface = typeface
        val w = paint.measureText(text, 0, text.length)
        setPadding(
            paddingLeft + ((bitmap.width - w) / 2f + .5f).toInt(),
            paddingTop + (bitmap.height * 0.70).toInt(),
            paddingRight,
            paddingBottom
        )
        setShadowLayer(5f, 0f, 0f, Color.BLACK)
    }
}
