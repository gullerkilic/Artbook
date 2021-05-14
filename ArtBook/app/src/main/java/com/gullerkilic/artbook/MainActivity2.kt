package com.gullerkilic.artbook

import android.Manifest
import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main2.*
import java.io.ByteArrayOutputStream
import java.lang.Exception

class MainActivity2 : AppCompatActivity() {
    var selectedPicture : Uri? = null
    var selectedBitmap : Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        val intent = getIntent()
        val info = intent.getStringExtra("info")
        if (info.equals("new")){
            nameText.setText("")
            artistText.setText("")
            yearText.setText("")
            button.visibility = View.VISIBLE
            val selectedImageBackground = BitmapFactory.decodeResource(applicationContext.resources,R.drawable.selectimage)
            imageView.setImageBitmap(selectedImageBackground)

        }else {
            button.visibility = View.INVISIBLE
            val selectedId = intent.getIntExtra("id",1)

            val database = this.openOrCreateDatabase("Arts",Context.MODE_PRIVATE,null)
            val cursor = database.rawQuery("SELECT*FROM arts WHERE id = ?", arrayOf(selectedId.toString()))
            val artNameIx =cursor.getColumnIndex("artname")
            val artistNmaeIx =cursor.getColumnIndex("artistname")
            val yearIx = cursor.getColumnIndex("year")
            val imageIx = cursor.getColumnIndex("image")

            while (cursor.moveToNext()){
                nameText.setText(cursor.getString(artNameIx))
                artistText.setText(cursor.getString(artistNmaeIx))
                yearText.setText(cursor.getString(yearIx))
                val byteArray =  cursor.getBlob(imageIx)
                val bitmap = BitmapFactory.decodeByteArray(byteArray,0,byteArray.size)
                imageView.setImageBitmap(bitmap)


            }
            cursor.close()

        }



    }

    fun selectImage (view : View){
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            // izin verilmediyse izini sor
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),1)
        }else {
            //izin verildiyse galeriye git
            val intenttoGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intenttoGallery,2)
        }

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
       if ( requestCode == 1){
           if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED){
               val intenttoGallery = Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
               startActivityForResult(intenttoGallery,2)
           }
       }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == 2 && resultCode == Activity.RESULT_OK && data != null){

            selectedPicture = data.data
            try {
                if ( selectedPicture != null){
                    if (Build.VERSION.SDK_INT >= 28){
                        val source = ImageDecoder.createSource(this.contentResolver, selectedPicture!!)
                        selectedBitmap = ImageDecoder.decodeBitmap(source)
                        imageView.setImageBitmap(selectedBitmap)

                    }else{
                        selectedBitmap = MediaStore.Images.Media.getBitmap(this.contentResolver,selectedPicture)
                        imageView.setImageBitmap(selectedBitmap)
                    }
                }
            }catch (e : Exception){

            }
            }


        super.onActivityResult(requestCode, resultCode, data)
    }





    fun save (view : View){
        val artName = nameText.text.toString()
        val artistName = artistText.text.toString()
        val year = yearText.text.toString()
        if (selectedBitmap != null){
            var smallBitmap = makeSmallerBitmap(selectedBitmap!!,300)

            val outputStream = ByteArrayOutputStream()
            smallBitmap?.compress(Bitmap.CompressFormat.PNG,50,outputStream)
            val byteArray = outputStream.toByteArray()

            try {
                val database =  this.openOrCreateDatabase("Arts",Context.MODE_PRIVATE,null)
                database.execSQL("CREATE TABLE IF NOT EXISTS arts (id INTEGER PRIMARY KEY , artname VARCHAR, artistname VARCHAR, year VARCHAR, image BLOB)")

                val sqlString = "INSERT INTO arts (artname, artistname, year, image) VALUES (?,?,?,?)"
                val statement = database.compileStatement(sqlString)
                statement.bindString(1,artName)
                statement.bindString(2,artistName)
                statement.bindString(3,year)
                statement.bindBlob(4,byteArray)
                statement.execute()

            }catch (e : Exception){
                e.printStackTrace()
            }
            // finish()
            val intent = Intent(this,MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            startActivity(intent)
        }

    }

    fun makeSmallerBitmap ( image : Bitmap, maximumSize : Int): Bitmap {
        var width = image.width
        var height = image.height
        val bitmapRatio: Double = width.toDouble() / height.toDouble()
        if (bitmapRatio > 1) {
            width = maximumSize
            val selectedHeight = width / bitmapRatio
            height = selectedHeight.toInt()
        } else {
            height = maximumSize
            val selectedWidth = height * bitmapRatio
            width = selectedWidth.toInt()
        }
        return Bitmap.createScaledBitmap(image,width,height,true)
    }

}