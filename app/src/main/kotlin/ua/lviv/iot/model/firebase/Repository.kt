package ua.lviv.iot.model.firebase

import android.arch.lifecycle.LiveData
import ua.lviv.iot.model.map.Quest
import android.arch.lifecycle.MutableLiveData
import android.content.Context
import com.google.firebase.database.DatabaseError
import ua.lviv.iot.model.EventResultStatus


class Repository private  constructor(private val firebaseDataManager: FirebaseDataManager){
    private val questsList = mutableListOf<Quest>()
    private val quests = MutableLiveData<List<Quest>>()

    companion object {
        @Volatile private var instance: Repository? = null

        fun getInstance(firebaseDataManager: FirebaseDataManager) = instance?: synchronized(this){
            instance?: Repository(firebaseDataManager).also { instance = it }
        }
    }

    init{
        quests.value = questsList
    }

    fun addToQuestList(quests: List<Quest>){

    }

    fun getQuestsList(): LiveData<List<Quest>>{
        firebaseDataManager.questsRetriever(object: FirebaseDataManager.DataRetrieveListenerForQuest {
            override fun onSuccess(questStructureList: List<Quest>) {
                questsList.clear()
                questsList.addAll(questStructureList)
                quests.postValue(questsList)
            }

            override fun onError(databaseError: DatabaseError) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        })
        return quests
    }

    fun getLastLocationByQuest(uId: String, questID: Int, listener: FirebaseDataManager.LastLocationByQuestListener) {
        firebaseDataManager.getLastLocationByQuest(uId, questID, object: FirebaseDataManager.LastLocationByQuestListener {
            override fun onSuccess(location: Int) {
                listener.onSuccess(location)
            }
            override fun onError(resultStatus: EventResultStatus) {
                listener.onError(resultStatus)
            }
        })
    }

    fun setLastLocationByQuest(uId: String, questID: Int, location: Int) {
        firebaseDataManager.setLastLocationByQuest(uId, questID, location)
    }

}