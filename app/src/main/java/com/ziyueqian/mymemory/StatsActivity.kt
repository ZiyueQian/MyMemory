package com.ziyueqian.mymemory

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.ziyueqian.mymemory.models.BoardSize
import com.ziyueqian.mymemory.models.GameStats
import com.ziyueqian.mymemory.models.UserImageList
import com.ziyueqian.mymemory.utils.EXTRA_GAME_NAME

class StatsActivity : AppCompatActivity() {
    private lateinit var rvGames : RecyclerView
    private lateinit var adapter : StatsAdapter
    private lateinit var stats : List<GameStats>
    private var boardSize : BoardSize = BoardSize.EASY
    private val db = Firebase.firestore

    companion object {
        private const val TAG = "StatsActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        stats = ArrayList<GameStats>()
        rvGames = findViewById(R.id.rvGames)
        supportActionBar?.setDisplayHomeAsUpEnabled(true) //back button
        supportActionBar?.title = "Browse all custom games"

        //fetch all data
        db.collection("games")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    //Log.d(TAG, "${document.id} => ${document.data}")
                    val userImageList = document.toObject(UserImageList::class.java)
                    if (userImageList?.images == null) {
                        Log.e(TAG, "Can't find ${document.id}")
                    } else {
                        val avgMoves = userImageList.avgMoves
                        val numCards = userImageList.images?.size * 2
                        //find the board size
                        boardSize = BoardSize.getByValue(numCards)
                        var level = "Easy"
                        //set up the texts
                        when (boardSize) {
                            BoardSize.MEDIUM -> {
                                level = "Medium"
                            }
                            BoardSize.HARD -> {
                                level = "Hard"
                            }
                        }

                        var currentStat = GameStats(document.id.hashCode(), document.id, level, avgMoves)
                        (stats as ArrayList<GameStats>).add(currentStat)
                        Log.i(TAG, document.id + stats.toString())
                        adapter.notifyDataSetChanged()
                    }

                }
            }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }

        adapter = StatsAdapter(stats, object: StatsAdapter.ItemClickListener{
            override fun onItemClicked() {
            }
        })
        rvGames.adapter = adapter
        rvGames.layoutManager = LinearLayoutManager(this)
    }
}