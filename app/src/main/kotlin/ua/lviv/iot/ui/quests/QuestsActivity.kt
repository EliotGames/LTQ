package ua.lviv.iot.ui.quests

import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_quests.*
import ua.lviv.iot.R
import ua.lviv.iot.model.Quest


class QuestsActivity : AppCompatActivity() {
    private var quests: ArrayList<Quest> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quests)

        initQuestArray()
        rv_quests_recent.layoutManager = LinearLayoutManager(this)
        rv_quests_all.layoutManager = LinearLayoutManager(this)

        rv_quests_recent.adapter = QuestsAdapter(ArrayList(quests.subList(0, 3)), this)
        rv_quests_all.adapter = QuestsAdapter(quests, this)
    }

    /**
     * TODO Remove after setting the database
     * Temporary function for simulating quest list
     */
    private fun initQuestArray() {
        val imageUrl = "https://itcluster.lviv.ua/wp-content/uploads/2015/07/lviv6.png"
        val defaultText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Sed venenatis, turpis vel ultricies tincidunt, eros arcu malesuada erat, " +
                "eu venenatis lectus ipsum sed diam. Curabitur pellentesque eu elit sit " +
                "amet interdum. Sed faucibus ante nibh, non tempus turpis rutrum in. " +
                "Aliquam vulputate felis non orci pulvinar, non lacinia ligula semper."

        quests.add(Quest("First Quest", defaultText, imageUrl))
        quests.add(Quest("Second Quest", defaultText, imageUrl))
        quests.add(Quest("Third Quest", defaultText, imageUrl))
        quests.add(Quest("Fourth Quest", defaultText, imageUrl))
        quests.add(Quest("Fifth Quest", defaultText, imageUrl))
        quests.add(Quest("Sixth Quest", defaultText, imageUrl))
        quests.add(Quest("Seventh Quest", defaultText, imageUrl))
        quests.add(Quest("Eighth Quest", defaultText, imageUrl))
    }
}
