package ua.lviv.iot.ui.questsmenu

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModelProviders
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import kotlinx.android.synthetic.main.activity_questsmenu.*
import ua.lviv.iot.R
import ua.lviv.iot.utils.InjectorUtils


class QuestsMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_questsmenu)
        rv_quests_recent.layoutManager = LinearLayoutManager(this)
        rv_quests_all.layoutManager = LinearLayoutManager(this)
        initUI()
    }

    private fun initUI(){
        val factory = InjectorUtils.provideQuestMenuViewModelFactory()
        val viewModel = ViewModelProviders.of(this, factory)
                .get(QuestMenuViewModel::class.java)
        viewModel.getQuests().observe(this, Observer { quests -> rv_quests_all.adapter = QuestsAdapter(ArrayList(quests)) })
    }
}
