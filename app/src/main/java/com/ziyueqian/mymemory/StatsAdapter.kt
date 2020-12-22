package com.ziyueqian.mymemory

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import com.ziyueqian.mymemory.models.BoardSize
import com.ziyueqian.mymemory.models.GameStats
import com.ziyueqian.mymemory.utils.EXTRA_GAME_NAME
import kotlin.math.min

// Create the basic adapter extending from RecyclerView.Adapter
// Note that we specify the custom ViewHolder which gives us access to our views
class StatsAdapter (private val stats: List<GameStats>,
                    private val itemClickListener: StatsAdapter.ItemClickListener
) : RecyclerView.Adapter<StatsAdapter.ViewHolder>() {
    companion object {
        private const val TAG = "StatsAdapter"
    }

    interface ItemClickListener {
        fun onItemClicked() {}
    }

    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    inner class ViewHolder(listItemView: View) : RecyclerView.ViewHolder(listItemView) {
        // Your holder should contain and initialize a member variable
        // for any view that will be set as you render a row
        val tvGameName = itemView.findViewById<TextView>(R.id.tvGameName)
        val tvLevel = itemView.findViewById<TextView>(R.id.tvLevel)
        val tvAvgMoves = itemView.findViewById<TextView>(R.id.tvAvgMoves)
        val statItem = itemView.findViewById<ConstraintLayout>(R.id.rvStatItem)

        fun bind(position : Int ) {
            var stat = stats[position]
            tvGameName.text = stat.gameName
            tvLevel.text = stat.gameLevel
            if (stat.avgMoves != null) {
                tvAvgMoves.text = String.format("%.3f", stat.avgMoves)
            } else {
                tvAvgMoves.text = ""
            }
            statItem.setOnClickListener {
                Log.i(TAG, "Clicked $position")
                if (stat.gameName != null ) {
                    itemClickListener.onItemClicked()
                }
            }
        }
    }

    // ... constructor and member variables
    // Usually involves inflating a layout from XML and returning the holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StatsAdapter.ViewHolder {
        val context = parent.context
        val inflater = LayoutInflater.from(context)
        // Inflate the custom layout
        val statView = inflater.inflate(R.layout.stat_item, parent, false)
        // Return a new holder instance
        return ViewHolder(statView)
    }

    // Involves populating data into the item through holder
    override fun onBindViewHolder(viewHolder: StatsAdapter.ViewHolder, position: Int) {
        // Get the data model based on position
        // Set item views based on your views and data model
        viewHolder.bind(position)
    }

    // Returns the total count of items in the list
    override fun getItemCount(): Int {
        return stats.size
    }

}
