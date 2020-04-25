package com.jelndoca.myfragments

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import androidx.fragment.app.Fragment
import com.squareup.picasso.Picasso
import kotlinx.android.synthetic.main.fragment_1.*
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.text.SimpleDateFormat
import java.util.*

class Fragment1 : Fragment() {

    private val PERMISSION_REQUEST_CAMERA = 90
    private val PERMISSION_REQUEST_GALLERY = 98
    private val permission = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.READ_EXTERNAL_STORAGE,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private var photoFile: File? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_1, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        addPhoto.setOnClickListener {
            myRequestPermission(PERMISSION_REQUEST_CAMERA)
        }

        addGallery.setOnClickListener {
            myRequestPermission(PERMISSION_REQUEST_GALLERY)
        }
    }

    private fun myRequestPermission(requestCode: Int) {
        requestPermissions(permission, requestCode)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            PERMISSION_REQUEST_GALLERY -> {
                if (validatePermission(permissions, grantResults)) {
                    startActivityForResult(
                        Intent().apply {
                            type = "image/*"
                            action = Intent.ACTION_GET_CONTENT
                        },
                        PERMISSION_REQUEST_GALLERY
                    )
                } else {
                    showPermissionDenegaded()
                }
            }
            PERMISSION_REQUEST_CAMERA -> {
                if (validatePermission(permissions, grantResults)) {
                    openCamera()
                } else {
                    showPermissionDenegaded()
                }

            }
        }
    }

    private fun showPermissionDenegaded() {
        Toast.makeText(context, "Permisos denegados", Toast.LENGTH_LONG).show()
    }

    private fun validatePermission(
        permissions: Array<out String>,
        grantResults: IntArray
    ): Boolean {
        return grantResults.size == permission.size &&
                grantResults.count { it == PackageManager.PERMISSION_GRANTED } == permissions.size
    }

    private fun openCamera() {
        val photoIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

        if (photoIntent.resolveActivity(activity!!.packageManager) != null) {
            try {
                photoFile = createPhotoFile()
            } catch (e: IOException) {
                Toast.makeText(context, e.message ?: "Error", Toast.LENGTH_LONG).show()
            }
            if (photoFile != null) {
                val photoUri: Uri =
                    FileProvider.getUriForFile(context!!, "com.jelndoca", photoFile!!)
                val resultIntent = activity!!.packageManager.queryIntentActivities(
                    photoIntent, PackageManager.MATCH_DEFAULT_ONLY
                )
                for (i in resultIntent) {
                    val packageName = i.activityInfo.packageName
                    activity!!.grantUriPermission(
                        packageName, photoUri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                                or Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                }
                photoIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                startActivityForResult(photoIntent, PERMISSION_REQUEST_CAMERA)
            }
        }
    }

    private fun createPhotoFile(): File? {
        val imageFileName: String = "myPhoto${SimpleDateFormat("yyyyMMdd_HHmmSS")
            .format(Date())}"
        val storageDir: File? = activity!!.getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return if (storageDir?.exists() == false) {
            val result = storageDir.mkdir()
            null
        } else {
            File.createTempFile(imageFileName, ".jpg", storageDir)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                PERMISSION_REQUEST_GALLERY -> {
                    data?.let {
                        Picasso.get().load(data.data).into(gallery)
                    }
                }
                PERMISSION_REQUEST_CAMERA -> {
                    val thread = Thread(Runnable {
                        val bitmap = BitmapFactory.decodeFile(photoFile!!.absolutePath)
                        val imageRotated = rotateImage(bitmap)
                        val result = resizeBitmap(imageRotated, 400, 400)
                        showBitmap(result)
                    })
                    thread.start()
                }

            }
        }
    }

    private fun showBitmap(bitmap: Bitmap) {
        activity!!.runOnUiThread {
            camera.setImageBitmap(bitmap)
        }

    }

    private fun rotateImage(bitmap: Bitmap): Bitmap {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height
        val matrix = Matrix()
        val rotation = getRotation(photoFile!!.absolutePath)
        matrix.postRotate(rotation)
        val scaledBitmap = Bitmap.createScaledBitmap(
            bitmap,
            bitmapWidth,
            bitmapHeight,
            true
        )
        return Bitmap.createBitmap(
            scaledBitmap,
            0,
            0,
            scaledBitmap.width,
            scaledBitmap.height,
            matrix,
            true
        )
    }

    private fun getRotation(path: String): Float {
        val exifInterface = ExifInterface(path)
        val orientation = exifInterface.getAttributeInt(
            ExifInterface.TAG_ORIENTATION,
            ExifInterface.ORIENTATION_NORMAL
        )
        var rotation = 0F
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotation = 90F
            ExifInterface.ORIENTATION_ROTATE_180 -> rotation = 180F
            ExifInterface.ORIENTATION_ROTATE_270 -> rotation = 270F
        }
        return rotation
    }

    private fun resizeBitmap(bitmap: Bitmap, newWidth: Int, newHeight: Int): Bitmap {
        val scaledBitmap = Bitmap.createBitmap(newWidth, newHeight, Bitmap.Config.ARGB_8888)

        val ratioX = newWidth / (bitmap.getWidth()).toFloat()
        val ratioY = newHeight / (bitmap.getHeight()).toFloat()
        val middleX = newWidth / 2.0f;
        val middleY = newHeight / 2.0f;

        val scaleMatrix = Matrix()
        scaleMatrix.setScale(ratioX, ratioY, middleX, middleY);

        val canvas = Canvas(scaledBitmap);
        canvas.setMatrix(scaleMatrix);
        canvas.drawBitmap(
            bitmap,
            middleX - bitmap.getWidth() / 2,
            middleY - bitmap.getHeight() / 2,
            Paint(Paint.FILTER_BITMAP_FLAG)
        )
        return scaledBitmap
    }
}