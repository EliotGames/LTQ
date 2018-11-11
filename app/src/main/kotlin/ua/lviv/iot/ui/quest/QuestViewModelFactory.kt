package ua.lviv.iot.ui.quest

import android.arch.lifecycle.ViewModel
import android.arch.lifecycle.ViewModelProvider

class QuestViewModelFactory: ViewModelProvider.NewInstanceFactory() {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return QuestViewModel() as T
    }
}