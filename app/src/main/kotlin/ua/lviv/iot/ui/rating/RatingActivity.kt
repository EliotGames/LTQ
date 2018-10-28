package ua.lviv.iot.ui.rating

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.Toolbar
import kotlinx.android.synthetic.main.activity_rating.*
import ua.lviv.iot.R
import ua.lviv.iot.ui.quests.QuestsAdapter
import ua.lviv.iot.ui.quests.QuestsManager

class RatingActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_rating)

        val toolbar = findViewById<Toolbar>(R.id.user_toolbar)
        toolbar.title = "Rating"
        setSupportActionBar(toolbar)
        supportActionBar!!.setDisplayHomeAsUpEnabled(true)

        val quests = ArrayList(QuestsManager.generateQuestsList().subList(0, 4))

        rv_rating_list.layoutManager = LinearLayoutManager(this)
        rv_rating_list.adapter = QuestsAdapter(quests, this)
    }
}