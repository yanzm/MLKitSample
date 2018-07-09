package net.yanzm.mlkitsample

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.support.v7.app.AppCompatActivity
import android.widget.ArrayAdapter
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.max

class MainActivity : AppCompatActivity(), ImagePickFragment.ImagePickListener {

    private var bitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val detectors = listOf(
                TEXT_DETECTION,
                CLOUD_TEXT_DETECTION,
                FACE_DETECTION,
                BARCODE_DETECTION,
                LABELING,
                CLOUD_LABELING,
                CLOUD_LANDMARK
        )
        detectorSpinner.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, detectors).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        detectButton.setOnClickListener {
            bitmap?.let { detect(it) }
        }
    }

    override fun onImagePicked(imageUri: Uri) {
        val imageBitmap = MediaStore.Images.Media.getBitmap(contentResolver, imageUri)

        val scaleFactor = max(
                imageBitmap.width.toFloat() / imageView.width.toFloat(),
                imageBitmap.height.toFloat() / imageView.height.toFloat()
        )

        val targetWidth = (imageBitmap.width / scaleFactor).toInt()
        val targetHeight = (imageBitmap.height / scaleFactor).toInt()

        bitmap = Bitmap.createScaledBitmap(
                imageBitmap,
                targetWidth,
                targetHeight,
                true
        )

        imageView.setImageBitmap(bitmap)

        overlay.clear()
        overlay.targetWidth = targetWidth
        overlay.targetHeight = targetHeight

        detectButton.isEnabled = true
    }

    private fun detect(bitmap: Bitmap) {
        overlay.clear()

        val detectorName = detectorSpinner.selectedItem as String
        when (detectorName) {
            TEXT_DETECTION -> {
                // TODO: 1 on-device テキスト認識
                // https://firebase.google.com/docs/ml-kit/android/recognize-text#on-device
            }
            FACE_DETECTION -> {
                // TODO: 2 on-device 顔検出
                // https://firebase.google.com/docs/ml-kit/android/detect-faces#on-device
            }
            BARCODE_DETECTION -> {
                // TODO: 3 on-device バーコードスキャン
                // https://firebase.google.com/docs/ml-kit/android/read-barcodes#configure-the-barcode-detector
            }
            LABELING -> {
                // TODO: 4 on-device ラベルづけ
                // https://firebase.google.com/docs/ml-kit/android/label-images#on-device
            }
            CLOUD_TEXT_DETECTION -> {
                // TODO: 5 cloud テキスト認識
                // https://firebase.google.com/docs/ml-kit/android/recognize-text#cloud-based
            }
            CLOUD_LABELING -> {
                // TODO: 6 cloud ラベルづけ
                // https://firebase.google.com/docs/ml-kit/android/label-images#cloud-based
            }
            CLOUD_LANDMARK -> {
                // TODO: 7 cloud ランドマーク認識
                // https://firebase.google.com/docs/ml-kit/android/recognize-landmarks#configure-the-landmark-detector
            }
        }
    }

    companion object {
        private const val TEXT_DETECTION = "Text"
        private const val CLOUD_TEXT_DETECTION = "Cloud Text"
        private const val FACE_DETECTION = "Face"
        private const val BARCODE_DETECTION = "Barcode"
        private const val LABELING = "Labeling"
        private const val CLOUD_LABELING = "Cloud Labeling"
        private const val CLOUD_LANDMARK = "Cloud Landmark"
    }
}
