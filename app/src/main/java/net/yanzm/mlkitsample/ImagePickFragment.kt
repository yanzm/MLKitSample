package net.yanzm.mlkitsample

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.core.content.contentValuesOf
import androidx.fragment.app.Fragment

class ImagePickFragment : Fragment() {

    interface ImagePickListener {
        fun onImagePicked(imageUri: Uri)
    }

    private var listener: ImagePickListener? = null
    private var imageUri: Uri? = null

    override fun onAttach(context: Context) {
        super.onAttach(context)
        listener = context as? ImagePickListener
    }

    override fun onDetach() {
        listener = null
        super.onDetach()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState != null) {
            imageUri = savedInstanceState.getParcelable(KEY_IMAGE_URI)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putParcelable(KEY_IMAGE_URI, imageUri)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_image_pick, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        view!!.setOnClickListener {
            AlertDialog.Builder(requireContext())
                .setItems(arrayOf("Camera", "Gallery")) { _, which ->
                    when (which) {
                        0 -> startImageCaptureIntent()
                        1 -> startGetContentIntent()
                    }
                }
                .show()
        }
    }

    private fun startImageCaptureIntent() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_CODE_PERMISSION
            )
            return
        }

        imageUri = null

        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        if (intent.resolveActivity(requireContext().packageManager) != null) {
            val values = contentValuesOf(
                MediaStore.Images.Media.TITLE to "New Picture",
                MediaStore.Images.Media.DESCRIPTION to "From Camera"
            )

            imageUri = requireContext().contentResolver.insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                values
            )

            intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri)
            startActivityForResult(intent, REQUEST_CODE_IMAGE_CAPTURE)
        }
    }

    private fun startGetContentIntent() {
        val intent = Intent().apply {
            type = "image/*"
            action = Intent.ACTION_GET_CONTENT
        }
        startActivityForResult(
            Intent.createChooser(intent, "Select Picture"),
            REQUEST_CODE_CHOOSE_IMAGE
        )
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when (requestCode) {
            REQUEST_CODE_IMAGE_CAPTURE -> {
                if (resultCode == Activity.RESULT_OK) {
                    imageUri?.let {
                        imageUri = null
                        listener?.onImagePicked(it)
                    }
                }
            }
            REQUEST_CODE_CHOOSE_IMAGE -> {
                if (resultCode == Activity.RESULT_OK) {
                    data?.data?.let {
                        listener?.onImagePicked(it)
                    }
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_PERMISSION -> {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    startImageCaptureIntent()
                }
            }
        }
    }

    companion object {
        private const val REQUEST_CODE_IMAGE_CAPTURE = 1
        private const val REQUEST_CODE_CHOOSE_IMAGE = 2
        private const val REQUEST_CODE_PERMISSION = 3

        private const val KEY_IMAGE_URI = "image"
    }
}
