package ua.lviv.iot.viewmodels

import android.arch.lifecycle.Observer
import android.arch.lifecycle.ViewModel
import ua.lviv.iot.model.firebase.FirebaseDataManager
import ua.lviv.iot.model.firebase.Repository

class QuestMenuViewModel(private val repository: Repository): ViewModel() {

    fun getQuests() = repository.getQuestsList()
}