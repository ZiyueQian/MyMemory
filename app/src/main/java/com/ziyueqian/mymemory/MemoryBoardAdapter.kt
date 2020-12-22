package com.ziyueqian.mymemory

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.recyclerview.widget.RecyclerView
import com.squareup.picasso.Picasso
import com.ziyueqian.mymemory.models.BoardSize
import com.ziyueqian.mymemory.models.MemoryCard
import kotlin.math.min

class MemoryBoardAdapter(private val context: Context,
                         private val boardSize: BoardSize,
                         private val cards: List<MemoryCard>,
                         private val cardClickListener : CardClickListener)
    : RecyclerView.Adapter<MemoryBoardAdapter.ViewHolder>() {

    //similar to static variables in java. define constants
    companion object {
        private const val MARGIN_SIZE = 10
        private const val TAG = "MemoryBoardAdapter"
    }

    interface CardClickListener {
        fun onCardClicked(position: Int)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        //how to create one view
        val cardWidth = parent.width / boardSize.getWidth() - (2 * MARGIN_SIZE)
        val cardHeight = parent.height / boardSize.getHeight() - (2 * MARGIN_SIZE)
        val cardSizeLength = min(cardWidth, cardHeight)
        val view = LayoutInflater.from(context).inflate(R.layout.memory_card, parent, false)
        val layoutParams = view.findViewById<CardView>(R.id.cardView).layoutParams as ViewGroup.MarginLayoutParams
        //used to take up the full height in linear layout
        layoutParams.width = cardSizeLength
        layoutParams.height = cardSizeLength
        layoutParams.setMargins(MARGIN_SIZE,MARGIN_SIZE,MARGIN_SIZE,MARGIN_SIZE)
        return ViewHolder(view)
    }

    override fun getItemCount(): Int {
        return boardSize.numCards
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(position)
    }

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val imageButton = itemView.findViewById<ImageButton>(R.id.imageButton)

        fun bind(position: Int) {
            val memoryCard = cards[position]

            if (memoryCard.isFaceUp) {
                if (memoryCard.imageUrl != null) {
                    //valid custom image, download with the library
                    Picasso.get().load(memoryCard.imageUrl).placeholder(R.drawable.ic_image).into(imageButton)
                } else {
                    imageButton.setImageResource(memoryCard.identifier)
                }
            } else {
                imageButton.setImageResource(R.drawable.waves)
            }

            //change color to fade if matched
            imageButton.alpha = if (memoryCard.isMatched) .4f else 1.0f
            val colorStateList = if (memoryCard.isMatched) ContextCompat.getColorStateList(context, R.color.color_gray) else null
            ViewCompat.setBackgroundTintList(imageButton,colorStateList)

            imageButton.setOnClickListener{
                Log.i(TAG, "Clicked on position $position")
                cardClickListener.onCardClicked(position)
            }
        }
    }


}

