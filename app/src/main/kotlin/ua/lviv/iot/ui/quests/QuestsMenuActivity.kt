package ua.lviv.iot.ui.quests

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import android.arch.lifecycle.ViewModelProviders
import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.util.Log
import com.google.firebase.database.DatabaseError
import kotlinx.android.synthetic.main.activity_quests.*
import ua.lviv.iot.R
import ua.lviv.iot.model.firebase.FirebaseDataManager
import ua.lviv.iot.model.map.Quest
import ua.lviv.iot.utils.InjectorUtils
import ua.lviv.iot.viewmodels.QuestMenuViewModel


class QuestsMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quests)
        rv_quests_recent.layoutManager = LinearLayoutManager(this)
        rv_quests_all.layoutManager = LinearLayoutManager(this)
        initialiseUI()
    }

    fun initialiseUI(){
        val factory = InjectorUtils.provideQuestMenuViewModelFactory()
        val viewModel = ViewModelProviders.of(this, factory)
                .get(QuestMenuViewModel::class.java)
        viewModel.getQuests().observe(this, Observer { quests -> rv_quests_all.adapter = QuestsAdapter(ArrayList(quests), this) })
    }
}
