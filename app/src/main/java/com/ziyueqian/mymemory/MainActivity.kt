package com.ziyueqian.mymemory

import android.animation.ArgbEvaluator
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.coordinatorlayout.widget.CoordinatorLayout
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.jinatonic.confetti.CommonConfetti
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.squareup.picasso.Picasso
import com.ziyueqian.mymemory.models.BoardSize
import com.ziyueqian.mymemory.models.MemoryGame
import com.ziyueqian.mymemory.models.UserImageList
import com.ziyueqian.mymemory.utils.EXTRA_BOARD_SIZE
import com.ziyueqian.mymemory.utils.EXTRA_GAME_NAME

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val CREATE_REQUEST_CODE = 123
    }

    private lateinit var memoryGame : MemoryGame
    private lateinit var adapter : MemoryBoardAdapter
    private lateinit var clRoot: CoordinatorLayout
    private lateinit var rvBoard: RecyclerView
    private lateinit var tvNumMoves: TextView
    private lateinit var tvNumPairs: TextView

    private var boardSize : BoardSize = BoardSize.EASY
    private val db = Firebase.firestore
    private var customGameImages : List<String>? = null
    private var gameName : String? = null //only set when playing custom game
    private var avgMoves : Double? = null //only in custom game
    private var timesPlayed : Double? = null //only in custom game

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        clRoot = findViewById(R.id.clRoot)
        rvBoard = findViewById(R.id.rvBoard)
        tvNumMoves = findViewById(R.id.tvNumMoves)
        tvNumPairs = findViewById(R.id.tvNumPairs)

        setUpBoard()
    }

    private fun setUpBoard() {
        //if custom game, then use that as title
        supportActionBar?.title = gameName ?: getString(R.string.app_name)

        //set up the texts
        when (boardSize) {
            BoardSize.EASY -> {
                tvNumMoves.text = "Easy 4 x 2"
                tvNumPairs.text = "Pairs: 0 / 4"
            }
            BoardSize.MEDIUM -> {
                tvNumMoves.text = "Medium 6 x 3"
                tvNumPairs.text = "Pairs: 0 / 9"
            }
            BoardSize.HARD -> {
                tvNumMoves.text = "Hard 6 x 4"
                tvNumPairs.text = "Pairs: 0 / 12"
            }
        }
        tvNumPairs.setTextColor(ContextCompat.getColor(this, R.color.color_progress_none))

        //construct memory game
        memoryGame = MemoryGame(boardSize, customGameImages)

        adapter = MemoryBoardAdapter(this, boardSize, memoryGame.cards, object: MemoryBoardAdapter.CardClickListener {
            override fun onCardClicked(position: Int) {
                updateGameWithFlip(position)
            }
        })
        rvBoard.adapter = adapter
        rvBoard.setHasFixedSize(true) //size of RV won't change. app more efficient
        rvBoard.layoutManager = GridLayoutManager(this, boardSize.getWidth())
    }

    //inflate menu
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.mi_refresh -> {
                //setup game again
                if (memoryGame.getNumMoves() >0 && !memoryGame.haveWonGame()) {
                    showAlertDialog("Quit your current game?",null,View.OnClickListener { setUpBoard() })
                }
                else {
                    setUpBoard()
                }
                return true
            }
            R.id.mi_new_size -> {
                showNewSizeDialog()
                return true
            }
            R.id.mi_custom -> {
                showCreationDialog()
                return true
            }
            R.id.mi_download -> {
                showDownloadDialog()
                return true
            }
            R.id.mi_stats -> {
                val intent = Intent(this, StatsActivity::class.java)
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun showDownloadDialog() {
        val boardDownloadView = LayoutInflater.from(this).inflate(R.layout.dialog_download_board,null)
        showAlertDialog("Fetch memory game", boardDownloadView, View.OnClickListener {
            //grab text of game name user wants to play
            val etDownloadGame = boardDownloadView.findViewById<EditText>(R.id.etDownloadGame)
            val gameToDownload = etDownloadGame.text.toString().trim()
            downloadGame(gameToDownload)
        })
    }

    private fun showCreationDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        showAlertDialog("Create your own memory board", boardSizeView, View.OnClickListener {
            //set a new value for the board size
            val desiredBoardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            //navigate to a new activity to choose photos
            val intent = Intent(this, CreateActivity::class.java)
            //defined the shared constant in Constants.kt file
            intent.putExtra(EXTRA_BOARD_SIZE,desiredBoardSize)
            //want data back from activity, so need result
            startActivityForResult(intent, CREATE_REQUEST_CODE)
        })
    }

    //callback to get the result data
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (resultCode == CREATE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val customGameName = data?.getStringExtra(EXTRA_GAME_NAME)
            if (customGameName == null) {
                Log.e(TAG, "Got null custom game from CreateActivity")
                return
            }
            downloadGame(customGameName)
        }
        super.onActivityResult(requestCode, resultCode, data)
    }

    private fun downloadGame(customGameName: String) {
        //query firestore to play the game
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            val userImageList = document.toObject(UserImageList::class.java)
            if (userImageList?.images == null) {
                Log.e(TAG, "Invalid custom game data from firestore")
                Snackbar.make(clRoot, "Sorry, couldn't find the game, $customGameName",Snackbar.LENGTH_LONG).show()
                return@addOnSuccessListener
            }
            //fetch other fields
            avgMoves = userImageList.avgMoves
            timesPlayed = userImageList.timesPlayed
            Log.i(TAG,"Average moves $avgMoves and timesPlayed $timesPlayed")

            //found the game successfully, reset RV with this custom game
            val numCards = userImageList.images.size * 2
            //find the board size
            boardSize = BoardSize.getByValue(numCards)
            customGameImages = userImageList.images
            gameName = customGameName
            //pre-fetch images to reduce delay
            for (imageUrl in userImageList.images) {
                Picasso.get().load(imageUrl).fetch()
            }
            Snackbar.make(clRoot,"You're now playing $customGameName",Snackbar.LENGTH_LONG).show()
            Log.i(TAG, "Downloaded game $customGameName")
            setUpBoard()
        }.addOnFailureListener{exception ->
            Log.e(TAG, "Exception when retrieving game", exception)
        }
    }

    private fun showNewSizeDialog() {
        val boardSizeView = LayoutInflater.from(this).inflate(R.layout.dialog_board_size, null)
        val radioGroupSize = boardSizeView.findViewById<RadioGroup>(R.id.radioGroup)
        //want current size board to be automatically selected
        when (boardSize) {
            BoardSize.EASY -> radioGroupSize.check(R.id.rbEasy)
            BoardSize.MEDIUM -> radioGroupSize.check(R.id.rbMedium)
            BoardSize.HARD -> radioGroupSize.check(R.id.rbHard)
        }
        showAlertDialog("Choose new size", boardSizeView, View.OnClickListener {
            //set a new value for the board size
            boardSize = when (radioGroupSize.checkedRadioButtonId) {
                R.id.rbEasy -> BoardSize.EASY
                R.id.rbMedium -> BoardSize.MEDIUM
                else -> BoardSize.HARD
            }
            gameName = null  //remove cache for custom game
            customGameImages = null //remove cache for custom game
            setUpBoard()
        })
    }

    private fun showAlertDialog(title:String, view: View?, positiveClickListener: View.OnClickListener) {
        AlertDialog.Builder(this)
                .setTitle(title)
                .setView(view)
                .setNegativeButton("Cancel",null)   //dismiss
                .setPositiveButton("Ok")
                { _, _ -> positiveClickListener.onClick(null)}.show()  //managing click listener
    }

    private fun updateGameWithFlip(position: Int) {
        //error checking
        if (memoryGame.haveWonGame()) {
            Snackbar.make(clRoot, "You already won!",Snackbar.LENGTH_LONG).show()
            return
        }
        if (memoryGame.isCardFaceUp(position)) {
            Snackbar.make(clRoot, "Invalid move",Snackbar.LENGTH_SHORT).show()
            return
        }

        //flip card
        if (memoryGame.flipCard(position)) {
            Log.i(TAG,"Found a match")

            //show progress through colors
            val color = ArgbEvaluator().evaluate(
                    memoryGame.numPairsFound.toFloat() / boardSize.getNumPairs(),
                    ContextCompat.getColor(this, R.color.color_progress_none),
                    ContextCompat.getColor(this, R.color.color_progress_full)
            ) as Int
            tvNumPairs.setTextColor(color)

            tvNumPairs.text = "Pairs: ${memoryGame.numPairsFound} / ${boardSize.getNumPairs()}"
            if (memoryGame.haveWonGame()) {
                CommonConfetti.rainingConfetti(clRoot, intArrayOf(Color.BLUE,Color.GREEN,Color.MAGENTA,Color.YELLOW)).oneShot()
                //add to average moves
                saveAvgMoves(memoryGame.getNumMoves(), gameName!!)
                if (avgMoves == null) {
                    Snackbar.make(clRoot, "You won! Congratulations",Snackbar.LENGTH_LONG).show()
                } else {
                    val avgMovesString = String.format("%.3f", avgMoves)
                    Snackbar.make(clRoot, "You won! Congratulations. Average move was $avgMovesString",Snackbar.LENGTH_LONG).show()
                }

            }
        }
        tvNumMoves.text = "Moves: ${memoryGame.getNumMoves().toString()}"
        adapter.notifyDataSetChanged()
    }

    private fun saveAvgMoves(currentMoves: Int, gameName: String) {
        Log.i(TAG, "avgMoves $avgMoves")

        var newAvg = currentMoves.toDouble()
        if (avgMoves != null && timesPlayed != null) {
            //have been played played before, need to calculate new average
            newAvg = (avgMoves!! * timesPlayed!! + currentMoves.toDouble()) / (timesPlayed!! + 1)
        }
        // Update two fields, creating the document if it does not already exist.
        val data = db.collection("games").document(gameName)
        data
            .update("avgMoves", newAvg)
            .addOnSuccessListener {
                Log.d(TAG, "New average successfully updated from $avgMoves to $newAvg!")
                avgMoves = newAvg
            }
            .addOnFailureListener { e -> Log.w(TAG, "Error updating new average", e) }

        data.update("timesPlayed", FieldValue.increment(1))
            .addOnSuccessListener {
                if (timesPlayed == null) {
                    timesPlayed = 1.0
                } else {
                    timesPlayed = timesPlayed!! + 1
                }
                Log.d(TAG, "timesPlayed successfully updated!") }
            .addOnFailureListener { e -> Log.w(TAG, "Error updating timesPlayed", e) }
    }
}

