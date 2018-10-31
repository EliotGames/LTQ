package ua.lviv.iot.ui.rating

import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import ua.lviv.iot.R
import ua.lviv.iot.ui.quests.QuestsAdapter
import ua.lviv.iot.ui.quests.QuestsManager

class RatingFragment : Fragment() {
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_rating, container, false)

        val quests = ArrayList(QuestsManager.generateQuestsList().subList(0, 4))

        val rvRatingList = view.findViewById<RecyclerView>(R.id.rv_rating_list)
        rvRatingList.layoutManager = LinearLayoutManager(activity)
        rvRatingList.adapter = QuestsAdapter(quests)

        return view
    }
}