package ua.lviv.iot.ui.questsmenu

import android.arch.lifecycle.ViewModel
import ua.lviv.iot.model.firebase.Repository

class QuestMenuViewModel(private val repository: Repository): ViewModel() {

    fun getQuests() = repository.getQuestsList()
}