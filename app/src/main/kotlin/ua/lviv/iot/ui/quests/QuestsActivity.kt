package ua.lviv.iot.ui.quests

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_quests.*
import ua.lviv.iot.R
import ua.lviv.iot.model.map.Quest


class QuestsActivity : AppCompatActivity() {
    private var quests: ArrayList<Quest> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quests)

        quests = QuestsManager.generateQuestsList()
        rv_quests_recent.layoutManager = LinearLayoutManager(this)
        rv_quests_all.layoutManager = LinearLayoutManager(this)

        rv_quests_recent.adapter = QuestsAdapter(ArrayList(quests.subList(0, 3)), this)
        rv_quests_all.adapter = QuestsAdapter(quests, this)
    }
}
