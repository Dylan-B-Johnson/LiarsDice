package com.example.finalproject
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.ArrayAdapter
import android.widget.ImageView

// Taken from Luke Needham's answer here:
// https://stackoverflow.com/questions/3609231/how-is-it-possible-to-create-a-spinner-with-images-instead-of-text
class SpinnerAdapter(context: LiarsDiceActivity, images: Array<Int>) :
    ArrayAdapter<Int>(context, android.R.layout.simple_spinner_item, images) {

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup) =
        getImageForPosition(position)

    override fun getView(position: Int, convertView: View?, parent: ViewGroup) =
        getImageForPosition(position)

    private fun getImageForPosition(position: Int) = ImageView(context).apply {
        setBackgroundResource(getItem(position)!!)
        layoutParams = ViewGroup.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
    }
}