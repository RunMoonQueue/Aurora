/*
 * Copyright 2017 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.camera2basic

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.TextureView
import android.view.View
import android.hardware.camera2.CaptureRequest
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.support.v4.view.MotionEventCompat.getPointerCount





/**
 * A [TextureView] that can be adjusted to a specified aspect ratio.
 */
class AutoFitTextureView @JvmOverloads constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyle: Int = 0
) : TextureView(context, attrs, defStyle) {

    private var ratioWidth = 0
    private var ratioHeight = 0

    //Zooming
    private var fingerSpacing = 0f
    private var zoomLevel = 1f
    private var maximumZoomLevel: Float = 0.toFloat()
    var zoom: Rect? = null

    var cameraCharacteristics: CameraCharacteristics? = null // ok
    var captureSession: CameraCaptureSession? = null // ok
    var previewRequestBuilder: CaptureRequest.Builder? = null // ok
    var captureCallback: CameraCaptureSession.CaptureCallback? = null // ok
    /**
     * Sets the aspect ratio for this view. The size of the view will be measured based on the ratio
     * calculated from the parameters. Note that the actual sizes of parameters don't matter, that
     * is, calling setAspectRatio(2, 3) and setAspectRatio(4, 6) make the same result.
     *
     * @param width  Relative horizontal size
     * @param height Relative vertical size
     */
    fun setAspectRatio(width: Int, height: Int) {
        if (width < 0 || height < 0) {
            throw IllegalArgumentException("Size cannot be negative.")
        }
        ratioWidth = width
        ratioHeight = height
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val width = View.MeasureSpec.getSize(widthMeasureSpec)
        val height = View.MeasureSpec.getSize(heightMeasureSpec)
        if (ratioWidth == 0 || ratioHeight == 0) {
            setMeasuredDimension(width, height)
        } else {
            if (width < height * ratioWidth / ratioHeight) {
                setMeasuredDimension(width, width * ratioHeight / ratioWidth)
            } else {
                setMeasuredDimension(height * ratioWidth / ratioHeight, height)
            }
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if(event == null ) super.onTouchEvent(event)
        try {
            maximumZoomLevel = cameraCharacteristics!!.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)

            val rect = cameraCharacteristics!!.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE)
                    ?: return false
            val currentFingerSpacing: Float

            if (event!!.pointerCount === 2) { //Multi touch.
                currentFingerSpacing = getFingerSpacing(event!!)
                var delta = 0.05f //Control this value to control the zooming sensibility
                if (fingerSpacing !== 0f) {
                    if (currentFingerSpacing > fingerSpacing) { //Don't over zoom-in
                        if (maximumZoomLevel - zoomLevel <= delta) {
                            delta = maximumZoomLevel - zoomLevel
                        }
                        zoomLevel = zoomLevel + delta
                    } else if (currentFingerSpacing < fingerSpacing) { //Don't over zoom-out
                        if (zoomLevel - delta < 1f) {
                            delta = zoomLevel - 1f
                        }
                        zoomLevel -= delta
                    }
                    val ratio = 1.toFloat() / zoomLevel //This ratio is the ratio of cropped Rect to Camera's original(Maximum) Rect
                    //croppedWidth and croppedHeight are the pixels cropped away, not pixels after cropped
                    val croppedWidth = rect.width() - Math.round(rect.width().toFloat() * ratio)
                    val croppedHeight = rect.height() - Math.round(rect.height().toFloat() * ratio)
                    //Finally, zoom represents the zoomed visible area
                    zoom = Rect(croppedWidth / 2, croppedHeight / 2,
                            rect.width() - croppedWidth / 2, rect.height() - croppedHeight / 2)
                    previewRequestBuilder!!.set(CaptureRequest.SCALER_CROP_REGION, zoom)
                }
                fingerSpacing = currentFingerSpacing
            } else { //Single touch point, needs to return true in order to detect one more touch point
                return true
            }
            captureSession!!.setRepeatingRequest(previewRequestBuilder!!.build(), captureCallback, null)
            return true
        } catch (e: Exception) {
            //Error handling up to you
            return true
        }

    }

    private fun getFingerSpacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return Math.sqrt((x * x + y * y).toDouble()).toFloat()
    }
}
