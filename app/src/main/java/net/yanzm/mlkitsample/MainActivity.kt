package net.yanzm.mlkitsample

import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.cloud.FirebaseVisionCloudDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import com.google.firebase.ml.vision.face.FirebaseVisionFaceDetectorOptions
import com.google.firebase.ml.vision.label.FirebaseVisionLabelDetectorOptions
import com.google.firebase.ml.vision.text.FirebaseVisionCloudTextRecognizerOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.math.max

class MainActivity : AppCompatActivity(), ImagePickFragment.ImagePickListener, CoroutineScope {

    private var bitmap: Bitmap? = null

    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Main + job

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        job = Job()

        val detectors = listOf(
            TEXT_DETECTION,
            CLOUD_TEXT_DETECTION,
            FACE_DETECTION,
            BARCODE_DETECTION,
            LABELING,
            CLOUD_LABELING,
            CLOUD_LANDMARK
        )
        detectorSpinner.adapter =
            ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, detectors).apply {
                setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            }

        detectButton.setOnClickListener {
            bitmap?.let { detect(it) }
        }
    }

    override fun onDestroy() {
        job.cancel()
        super.onDestroy()
    }

    override fun onImagePicked(imageUri: Uri) {
        launch {
            bitmap = withContext(Dispatchers.Default) {
                val imageBitmap = Glide.with(this@MainActivity)
                    .asBitmap()
                    .load(imageUri)
                    .submit(imageView.width, imageView.height)
                    .get()

                val scaleFactor = max(
                    imageBitmap.width.toFloat() / imageView.width.toFloat(),
                    imageBitmap.height.toFloat() / imageView.height.toFloat()
                )

                val targetWidth = (imageBitmap.width / scaleFactor).toInt()
                val targetHeight = (imageBitmap.height / scaleFactor).toInt()

                Bitmap.createScaledBitmap(
                    imageBitmap,
                    targetWidth,
                    targetHeight,
                    true
                )
            }

            println("${bitmap!!.width}, ${bitmap!!.height}")

            imageView.setImageBitmap(bitmap)

            overlay.clear()
            overlay.targetWidth = bitmap!!.width
            overlay.targetHeight = bitmap!!.height

            detectButton.isEnabled = true
        }
    }

    private fun detect(bitmap: Bitmap) {
        overlay.clear()

        val detectorName = detectorSpinner.selectedItem as String
        when (detectorName) {
            TEXT_DETECTION -> {
                // on-device テキスト認識
                // https://firebase.google.com/docs/ml-kit/android/recognize-text#on-device

                detectButton.isEnabled = false
                progressBar.visibility = View.VISIBLE

                val image = FirebaseVisionImage.fromBitmap(bitmap)

                FirebaseVision.getInstance()
                    .onDeviceTextRecognizer
                    .processImage(image)
                    .addOnSuccessListener { texts ->
                        detectButton.isEnabled = true
                        progressBar.visibility = View.GONE

                        for (block in texts.textBlocks) {
                            for (line in block.lines) {
                                for (element in line.elements) {
                                    element.boundingBox?.let {
                                        overlay.add(BoxData(element.text, it))
                                    }
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        detectButton.isEnabled = true
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    }
            }
            FACE_DETECTION -> {
                // on-device 顔検出
                // https://firebase.google.com/docs/ml-kit/android/detect-faces#on-device

                detectButton.isEnabled = false
                progressBar.visibility = View.VISIBLE

                val image = FirebaseVisionImage.fromBitmap(bitmap)

                val options = FirebaseVisionFaceDetectorOptions.Builder()
                    .setPerformanceMode(FirebaseVisionFaceDetectorOptions.ACCURATE) // or FAST
                    .setLandmarkMode(FirebaseVisionFaceDetectorOptions.ALL_LANDMARKS) // or NO_LANDMARKS
                    .setClassificationMode(FirebaseVisionFaceDetectorOptions.ALL_CLASSIFICATIONS) // or NO_CLASSIFICATIONS
                    .setContourMode(FirebaseVisionFaceDetectorOptions.NO_CONTOURS) // or ALL_CONTOURS
                    .setMinFaceSize(0.15f)
                    .enableTracking()
                    .build()

                FirebaseVision.getInstance()
//                    .visionFaceDetector
                    .getVisionFaceDetector(options)
                    .detectInImage(image)
                    .addOnSuccessListener { faces ->
                        detectButton.isEnabled = true
                        progressBar.visibility = View.GONE

                        for (face in faces) {
                            face.boundingBox?.let {
                                overlay.add(BoxData(face.smilingProbability.toString(), it))
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        detectButton.isEnabled = true
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    }
            }
            BARCODE_DETECTION -> {
                // on-device バーコードスキャン
                // https://firebase.google.com/docs/ml-kit/android/read-barcodes#configure-the-barcode-detector

                detectButton.isEnabled = false
                progressBar.visibility = View.VISIBLE

                val image = FirebaseVisionImage.fromBitmap(bitmap)

                val options = FirebaseVisionBarcodeDetectorOptions.Builder()
                    .setBarcodeFormats(
                        FirebaseVisionBarcode.FORMAT_EAN_8,
                        FirebaseVisionBarcode.FORMAT_EAN_13
                    )
                    .build()

                FirebaseVision.getInstance()
//                    .visionBarcodeDetector
                    .getVisionBarcodeDetector(options)
                    .detectInImage(image)
                    .addOnSuccessListener { barcodes ->
                        detectButton.isEnabled = true
                        progressBar.visibility = View.GONE

                        for (barcode in barcodes) {
                            barcode.boundingBox?.let {
                                overlay.add(BoxData(barcode.rawValue ?: "", it))
                            }

                            println("format : ${barcode.format}")
                            println("valueType : ${barcode.valueType}")
                            println("rawValue : ${barcode.rawValue}")
                            println("displayValue : ${barcode.displayValue}")
                            println("boundingBox : ${barcode.boundingBox}")
                            println("cornerPoints : ${barcode.cornerPoints}")

                            barcode.calendarEvent?.let {
                                println("description : ${it.description}")
                                println("start : ${it.start?.rawValue}")
                                println("end : ${it.end?.rawValue}")
                                println("status : ${it.status}")
                                println("summary : ${it.summary}")
                                println("location : ${it.location}")
                                println("organizer : ${it.organizer}")
                            }
                            barcode.contactInfo?.let {
                                println("name : ${it.name?.formattedName}")
                                println("organization : ${it.organization}")
                                println("title : ${it.title}")
                                println("addresses : ${it.addresses}")
                                println("emails : ${it.emails}")
                                println("phones : ${it.phones}")
                                println("urls : ${it.urls}")
                            }
                            barcode.email?.let {
                                println("email : ${it.address}")
                            }
                            barcode.geoPoint?.let {
                                println("geoPoint : ${it.lat}, ${it.lng}")
                            }
                            barcode.phone?.let {
                                println("phone : ${it.number}")
                            }
                            barcode.sms?.let {
                                println("sms : ${it.message}, ${it.phoneNumber}")
                            }
                            barcode.url?.let {
                                println("url : ${it.title}, ${it.url}")
                            }
                            barcode.wifi?.let {
                                println("encryptionType : ${it.encryptionType}")
                                println("ssid : ${it.ssid}")
                                println("password : ${it.password}")
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        detectButton.isEnabled = true
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    }
            }
            LABELING -> {
                // on-device ラベルづけ
                // https://firebase.google.com/docs/ml-kit/android/label-images#on-device

                detectButton.isEnabled = false
                progressBar.visibility = View.VISIBLE

                val image = FirebaseVisionImage.fromBitmap(bitmap)

                val options = FirebaseVisionLabelDetectorOptions.Builder()
                    .setConfidenceThreshold(0.8f)
                    .build()

                FirebaseVision.getInstance()
//                    .visionLabelDetector
                    .getVisionLabelDetector(options)
                    .detectInImage(image)
                    .addOnSuccessListener { labels ->
                        detectButton.isEnabled = true
                        progressBar.visibility = View.GONE

                        labels.forEach {
                            println("label : ${it.label}")
                            println("confidence : ${it.confidence}")
                            println("entityId : ${it.entityId}")
                        }

                        overlay.add(TextsData(labels.map { "${it.label}, ${it.confidence}" }))
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        detectButton.isEnabled = true
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    }
            }
            CLOUD_TEXT_DETECTION -> {
                // cloud テキスト認識
                // https://firebase.google.com/docs/ml-kit/android/recognize-text#cloud-based

                detectButton.isEnabled = false
                progressBar.visibility = View.VISIBLE

                val image = FirebaseVisionImage.fromBitmap(bitmap)

                val options = FirebaseVisionCloudTextRecognizerOptions.Builder()
                    .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
                    .setModelType(FirebaseVisionCloudTextRecognizerOptions.DENSE_MODEL)
                    .setLanguageHints(listOf("jp"))
                    .build()

                FirebaseVision.getInstance()
//                    .cloudTextRecognizer
                    .getCloudTextRecognizer(options)
                    .processImage(image)
                    .addOnSuccessListener { cloudText ->
                        detectButton.isEnabled = true
                        progressBar.visibility = View.GONE

                        for (block in cloudText.textBlocks) {
                            for (line in block.lines) {
                                for (element in line.elements) {
                                    element.boundingBox?.let {
                                        overlay.add(BoxData(element.text, it))
                                    }
                                }
                            }
                        }
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        detectButton.isEnabled = true
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    }
            }
            CLOUD_LABELING -> {
                // cloud ラベルづけ
                // https://firebase.google.com/docs/ml-kit/android/label-images#cloud-based

                detectButton.isEnabled = false
                progressBar.visibility = View.VISIBLE

                val image = FirebaseVisionImage.fromBitmap(bitmap)

                val options = FirebaseVisionCloudDetectorOptions.Builder()
                    .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
                    .setMaxResults(15)
                    .build()

                FirebaseVision.getInstance()
//                    .visionCloudLabelDetector
                    .getVisionCloudLabelDetector(options)
                    .detectInImage(image)
                    .addOnSuccessListener { labels ->
                        detectButton.isEnabled = true
                        progressBar.visibility = View.GONE

                        labels.forEach {
                            println("label : ${it.label}")
                            println("confidence : ${it.confidence}")
                            println("entityId : ${it.entityId}")
                        }

                        overlay.add(TextsData(labels.map { "${it.label}, ${it.confidence}" }))
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        detectButton.isEnabled = true
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    }
            }
            CLOUD_LANDMARK -> {
                // cloud ランドマーク認識
                // https://firebase.google.com/docs/ml-kit/android/recognize-landmarks#configure-the-landmark-detector

                detectButton.isEnabled = false
                progressBar.visibility = View.VISIBLE

                val image = FirebaseVisionImage.fromBitmap(bitmap)

                val options = FirebaseVisionCloudDetectorOptions.Builder()
                    .setModelType(FirebaseVisionCloudDetectorOptions.LATEST_MODEL)
                    .setMaxResults(15)
                    .build()

                FirebaseVision.getInstance()
//                    .visionCloudLandmarkDetector
                    .getVisionCloudLandmarkDetector(options)
                    .detectInImage(image)
                    .addOnSuccessListener { labels ->
                        detectButton.isEnabled = true
                        progressBar.visibility = View.GONE

                        labels.forEach {
                            if (it.boundingBox != null) {
                                overlay.add(
                                    BoxData(
                                        "${it.landmark}, ${it.confidence}",
                                        it.boundingBox!!
                                    )
                                )
                            }

                            println("boundingBox : ${it.boundingBox}")
                            println("confidence : ${it.confidence}")
                            println("entityId : ${it.entityId}")
                            println("landmark : ${it.landmark}")
                            println("locations : ${it.locations[0].latitude}, ${it.locations[0].longitude}")
                        }
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        detectButton.isEnabled = true
                        progressBar.visibility = View.GONE
                        Toast.makeText(this, e.message, Toast.LENGTH_SHORT).show()
                    }
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
