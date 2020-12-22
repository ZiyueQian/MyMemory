package com.ziyueqian.mymemory

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.ziyueqian.mymemory.models.BoardSize
import com.ziyueqian.mymemory.utils.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {
    private lateinit var boardSize : BoardSize
    private lateinit var rvImagePicker : RecyclerView
    private lateinit var etGameName : EditText
    private lateinit var btnSave : Button
    private lateinit var adapter : ImagePickerAdapter
    private lateinit var pbUploading : ProgressBar

    private val storage = Firebase.storage
    private val db = Firebase.firestore

    private var numImagesRequired = -1
    private val chosenImageUris = mutableListOf<Uri>() //identifies where the resource lives

    companion object {
        private const val PICK_PHOTOS_REQUEST_CODE = 456
        private const val READ_EXTERNAL_PHOTOS_CODE = 789
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_EXTERNAL_STORAGE
        private const val TAG = "CreateActivity"
        private const val MIN_GAME_NAME_LENGTH = 3
        private const val MAX_GAME_NAME_LENGTH = 14
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btnSave = findViewById(R.id.btnSave)
        pbUploading = findViewById(R.id.pbUploading)

        boardSize = intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        numImagesRequired = boardSize.getNumPairs()
        supportActionBar?.setDisplayHomeAsUpEnabled(true) //back button
        supportActionBar?.title = "Choose pictures (0 / $numImagesRequired)"

        btnSave.setOnClickListener {
            Log.i(TAG,"clicked save button")
            saveDataToFirebase()
        }

        //need to show dialog to inform them of 'dangerous permission'
        adapter = ImagePickerAdapter(this, chosenImageUris,boardSize, object: ImagePickerAdapter.ImageClickListener{
            override fun onPlaceholderClicked() {
                if (isPermissionGranted(this@CreateActivity, READ_PHOTOS_PERMISSION)) {
                    //implicit intent
                    launchIntentForPhotos()
                } else {
                    requestPermission(this@CreateActivity, READ_PHOTOS_PERMISSION, READ_EXTERNAL_PHOTOS_CODE)
                }
            }
        })
        rvImagePicker.adapter = adapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())

        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))  //set max length
        //listener to check whether button should be enabled once text changes
        etGameName.addTextChangedListener(object: TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                btnSave.isEnabled = enableSaveButton()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    //automatic callback after requesting for permissions
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == READ_EXTERNAL_PHOTOS_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                launchIntentForPhotos()
            } else {
                Toast.makeText(this, "In order to create a custom game, you need to provide access to your photos", Toast.LENGTH_LONG).show()
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()    //go home
            return true
        }
        return true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != PICK_PHOTOS_REQUEST_CODE || resultCode != Activity.RESULT_OK || data == null) {
            Log.w(TAG, "Did not get data back from the launched activity, user likely canceled flow")
            return
        }
        Log.i(TAG, "onActivityResult")
        val selectedUri = data.data //some phones for selecting one pic
        val clipData = data.clipData //for selecting multiple images
        if (clipData != null) {
            Log.i(TAG, "clipData numImages ${clipData.itemCount}: $clipData")
            for (i in 0 until clipData.itemCount) {
                val clipItem = clipData.getItemAt(i)
                //check that user didn't select more than needed
                if (chosenImageUris.size < numImagesRequired) {
                    chosenImageUris.add(clipItem.uri)
                }
            }
        } else if (selectedUri != null) {
            Log.i(TAG, "data: $selectedUri")
            chosenImageUris.add(selectedUri)
        }
        adapter.notifyDataSetChanged()
        supportActionBar?.title = "Choose pics (${chosenImageUris.size} / $numImagesRequired)"
        btnSave.isEnabled = enableSaveButton()
    }


    private fun enableSaveButton(): Boolean {
        if (chosenImageUris.size != numImagesRequired) {
            return false
        }
        if (etGameName.text.isBlank() || etGameName.text.length < MIN_GAME_NAME_LENGTH) {
            return false
        }
        return true
    }

    private fun launchIntentForPhotos() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*" //only allow images
        //allow them to select multiple images
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        startActivityForResult(Intent.createChooser(intent, "Choose pics"), PICK_PHOTOS_REQUEST_CODE)
    }

    private fun saveDataToFirebase() {
        Log.i(TAG, "Going to save data to Firebase")
        val customGameName = etGameName.text.toString().trim()
        btnSave.isEnabled = false   //avoid duplicate saves
        //check that this game name is not taken (so to not overwrite data)
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            if (document != null && document.data != null) {
                AlertDialog.Builder(this)
                        .setTitle("Name taken")
                        .setMessage("A game already exists with the name '$customGameName'. Please choose another")
                        .setPositiveButton("OK", null)
                        .show()
                btnSave.isEnabled = true    //can allow them to try again
            } else {
                handleImageUploading(customGameName)
            }
        }.addOnFailureListener {exception ->
            Log.e(TAG, "Encountered error while saving memory game", exception)
            Toast.makeText(this, "Encountered error while saving memory game", Toast.LENGTH_SHORT).show()
            btnSave.isEnabled = true
        }
    }

    private fun handleImageUploading(gameName: String) {
        pbUploading.visibility = View.VISIBLE
        val uploadedImageUrls = mutableListOf<String>()
        var didEncounterError = false
        for ((index, photoUri) in chosenImageUris.withIndex()) {
            val imageByteArray = getImageByteArray(photoUri)
            val filePath = "images/$gameName/${System.currentTimeMillis()}-${index}.jpg"
            val photoReference = storage.reference.child(filePath)
            photoReference.putBytes(imageByteArray)
                    .continueWithTask { photoUploadTask ->
                        Log.i(TAG, "uploaded bytes: ${photoUploadTask.result?.bytesTransferred}")
                        photoReference.downloadUrl
                    }.addOnCompleteListener { downloadUrlTask ->
                        if (!downloadUrlTask.isSuccessful) {
                            Log.e(TAG, "Exception with Firebase storage", downloadUrlTask.exception)
                            Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                            didEncounterError = true
                            return@addOnCompleteListener
                        }
                        if (didEncounterError) {
                            pbUploading.visibility = View.GONE
                            return@addOnCompleteListener
                        }
                        pbUploading.progress = uploadedImageUrls.size * 100 / chosenImageUris.size
                        val downloadUrl = downloadUrlTask.result.toString()
                        uploadedImageUrls.add(downloadUrl)
                        Log.i(TAG, "Finished uploading $photoUri, Num uploaded: ${uploadedImageUrls.size}")
                        if (uploadedImageUrls.size == chosenImageUris.size) {
                            handleAllImagesUploaded(gameName, uploadedImageUrls)
                        }
                    }
        }
    }

    private fun handleAllImagesUploaded(gameName: String, imageUrls: MutableList<String>) {
        db.collection("games").document(gameName)
                .set(mapOf("images" to imageUrls))
                .addOnCompleteListener { gameCreationTask ->
                    pbUploading.visibility = View.GONE
                    if (!gameCreationTask.isSuccessful) {
                        Log.e(TAG, "Exception with game creation", gameCreationTask.exception)
                        Toast.makeText(this, "Failed game creation", Toast.LENGTH_SHORT).show()
                        return@addOnCompleteListener
                    }
                    Log.i(TAG, "Successfully created game $gameName")
                    AlertDialog.Builder(this)
                            .setTitle("Upload complete! Let's play your game '$gameName'")
                            .setPositiveButton("OK") { _, _ ->
                                val resultData = Intent()
                                resultData.putExtra(EXTRA_GAME_NAME, gameName)
                                setResult(Activity.RESULT_OK, resultData)
                                finish()
                            }.show()
                }
    }


    //downgrade photo
    private fun getImageByteArray(photoUri: Uri): ByteArray {
        //if phone running on pie or higher
        val originalBitMap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)
        } else {
            //older phone version
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        Log.i(TAG, "Original width ${originalBitMap.width} and height ${originalBitMap.height}")
        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitMap, 250)
        Log.i(TAG, "Scaled width ${scaledBitmap.width} and height ${scaledBitmap.height}")
        val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputStream)
        return byteOutputStream.toByteArray()
    }
}