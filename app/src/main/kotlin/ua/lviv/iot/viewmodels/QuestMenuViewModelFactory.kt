package ua.lviv.iot.viewmodels

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider
import ua.lviv.iot.model.firebase.FirebaseDataManager
import ua.lviv.iot.model.firebase.Repository

class QuestMenuViewModelFactory(private val repository: Repository): ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return QuestMenuViewModel(repository) as T
    }
}