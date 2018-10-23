package ua.lviv.iot.ui.quests

import android.content.Context
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import com.google.firebase.database.DatabaseError
import kotlinx.android.synthetic.main.activity_quests.*
import ua.lviv.iot.R
import ua.lviv.iot.model.firebase.FirebaseDataManager
import ua.lviv.iot.model.map.Quest


class QuestsMenuActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_quests)
        FirebaseDataManager().questsRetriever(this ,object: FirebaseDataManager.DataRetrieveListenerForQuest{
            override fun onSuccess(questStructureList: List<Quest>, context: Context) {
                rv_quests_recent.layoutManager = LinearLayoutManager(context)
                rv_quests_all.layoutManager = LinearLayoutManager(context)

                rv_quests_recent.adapter = QuestsAdapter(ArrayList(questStructureList.subList(0, 2)), context)
                rv_quests_all.adapter = QuestsAdapter(ArrayList(questStructureList), context)
            }

            override fun onError(databaseError: DatabaseError) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }
        })

    }
}
