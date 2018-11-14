package ua.lviv.iot.utils

import ua.lviv.iot.model.firebase.FirebaseDataManager
import ua.lviv.iot.model.firebase.Repository
import ua.lviv.iot.ui.quest.QuestViewModelFactory
import ua.lviv.iot.ui.questsmenu.QuestMenuViewModelFactory

object InjectorUtils {
    fun provideQuestMenuViewModelFactory(): QuestMenuViewModelFactory {
        return QuestMenuViewModelFactory(Repository.getInstance(FirebaseDataManager.getInstance()))
    }

    fun provideQuestViewModelFactory(): QuestViewModelFactory {
        return QuestViewModelFactory()
    }
}