package ua.lviv.iot.ui.quests

import android.content.Context
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.Toast
import kotlinx.android.synthetic.main.quests_list_item.view.*
import ua.lviv.iot.R
import ua.lviv.iot.model.Quest

class QuestsAdapter(val items: ArrayList<Quest>, val context: Context) :
        RecyclerView.Adapter<ViewHolder>() {

    override fun getItemCount(): Int {
        return items.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, p1: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.quests_list_item, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.tvQuestsTitle.text = items[position].title

        holder.mainLayout.setOnClickListener({
            Toast.makeText(context, items[position].title, Toast.LENGTH_SHORT).show()
        })
    }
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val tvQuestsTitle = view.tv_quests_item_title!!
    val mainLayout = view.layout_quests_item!!
}
