package ua.lviv.iot.utils

import ua.lviv.iot.model.firebase.FirebaseDataManager
import ua.lviv.iot.model.firebase.Repository
import ua.lviv.iot.ui.questmenu.QuestMenuViewModelFactory

object InjectorUtils {
    fun provideQuestMenuViewModelFactory(): QuestMenuViewModelFactory {
        return QuestMenuViewModelFactory(Repository.getInstance(FirebaseDataManager.getInstance()))
    }
}