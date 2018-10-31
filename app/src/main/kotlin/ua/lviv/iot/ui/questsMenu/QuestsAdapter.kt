package ua.lviv.iot.ui.questsMenu

import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.quests_list_item.view.*
import ua.lviv.iot.ui.quest.QuestActivity
import ua.lviv.iot.R
import ua.lviv.iot.model.map.Quest

class QuestsAdapter(val items: ArrayList<Quest>) :
        RecyclerView.Adapter<ViewHolder>() {
    private lateinit var context: Context

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): ViewHolder {
        context = parent.context
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.quests_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvQuestsTitle.text = items[position].questName

        holder.mainLayout.setOnClickListener {
            Toast.makeText(context, items[position].questName, Toast.LENGTH_SHORT).show()
            context.startActivity(Intent(context, QuestActivity::class.java).putExtra("questName", items[position].questName))
        }
    }
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val tvQuestsTitle = view.tv_quests_item_title!!
    val mainLayout = view.layout_quests_item!!
}
