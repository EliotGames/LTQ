package ua.lviv.iot.model.firebase

import android.content.Context
import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import ua.lviv.iot.model.EventResultStatus
import ua.lviv.iot.model.map.Location
import ua.lviv.iot.model.map.Quest
import ua.lviv.iot.model.map.LocationStructure



class FirebaseDataManager private constructor(){

    val firebaseDatabase = FirebaseDatabase.getInstance()

    companion object {
        @Volatile private var instance: FirebaseDataManager?= null

        fun getInstance() = instance?: synchronized(this){
            instance?: FirebaseDataManager().also { instance = it }
        }

    }


    interface DataRetrieveListener {
        fun onSuccess()
    }

    /*public interface DataRetrieveListenerForQuestCategory {
        fun onSuccess(questCategoryList: List<QuestCategory>)
        fun onError(databaseError: DatabaseError)
    }*/

    interface DataRetrieveListenerForQuest {
        fun onSuccess(questStructureList: List<Quest>)
        fun onError(databaseError: DatabaseError)
    }

    interface DataRetrieveListenerForLocationsStructure {
        fun onSuccess(locationStructureList: List<LocationStructure>)
        fun onError(databaseError: DatabaseError)
    }

    interface DataRetrieveListenerForUser {
        fun onSuccess(user: User)
        fun onError(databaseError: DatabaseError)
    }

    interface UserWritingListener {
        fun onSuccess()
        fun onError()
    }

    interface LastLocationByQuestListener {
        fun onSuccess(location: Int)
        fun onError(resultStatus: EventResultStatus)
    }

    interface DataRetrieverListenerForSingleQuestStructure {
        fun onSuccess(questStructure: Quest, locationsIdList: List<Int>)
        fun onError(databaseError: DatabaseError)
    }

    /*fun categoriesNamesListRetriever(listener: DataRetrieveListenerForQuestCategory) {
        firebaseDatabase.getReference("categories").addListenerForSingleValueEvent(object : ValueEventListener() {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val questCategoryList = ArrayList<QuestCategory>()
                for (dataSnapshot1 in dataSnapshot.children) {
                    questCategoryList.add(dataSnapshot1.getValue(QuestCategory::class.java))
                }
                listener.onSuccess(questCategoryList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                listener.onError(databaseError)
            }
        })
    }*/

    fun questsRetriever(listener: DataRetrieveListenerForQuest) {
        firebaseDatabase.getReference("quest").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val questStructureList = ArrayList<Quest>()
                for (dataSpanshot1 in dataSnapshot.children) {
                    questStructureList.add(dataSpanshot1.getValue(Quest::class.java)!!)
                }
                listener.onSuccess(questStructureList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                listener.onError(databaseError)
            }
        })
    }

    fun findQuestsByCategory(fullList: List<Quest>, categoryId: Int): List<Quest> {
        val foundQuests = ArrayList<Quest>()
        for (questStructure in fullList) {
            if (questStructure.parentCategoryID == categoryId) {
                foundQuests.add(questStructure)
            } else {
                Log.d(" Unfined Quest:", questStructure.questName)
            }
        }
        return foundQuests
    }

    fun locationsListRetriever(locationsIdList: List<Int>, listener: DataRetrieveListenerForLocationsStructure) {
        firebaseDatabase.getReference("locations").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val locationsList = ArrayList<LocationStructure>()
                for (dataSnapshot1 in dataSnapshot.children) {
                    for (i in locationsIdList) {
                        if (dataSnapshot1.getValue(LocationStructure::class.java)!!.locationID == i) {
                            locationsList.add(dataSnapshot1.getValue(LocationStructure::class.java)!!)
                        }
                    }
                }
                listener.onSuccess(locationsList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                listener.onError(databaseError)
            }
        })
    }

    fun findLocationsById() {}

    fun writeCurrentUserData(uId: String, userInformation: User, listener: UserWritingListener) {
        firebaseDatabase.reference.child("userData").child(uId).setValue(userInformation)
        checkIfUserIsWritten(uId, object : UserWritingListener {
            override fun onSuccess() {
                listener.onSuccess()
            }

            override fun onError() {
                listener.onError()
            }
        })
    }

    fun checkIfUserIsWritten(uId: String, listener: UserWritingListener) {
        firebaseDatabase.reference.child("userData").child(uId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val userInformation: User
                userInformation = dataSnapshot.getValue(User::class.java)!!
                try {
                    //trying if user information is written on database
                    if (userInformation.name == null) {
                        listener.onError()
                    } else {
                        listener.onSuccess()
                    }
                } catch (e: NullPointerException) {
                    listener.onError()
                }

            }

            override fun onCancelled(databaseError: DatabaseError) {
                Log.e("UserInf problem: ", databaseError.message)
            }
        })
    }

    fun getCurrentUserData(uId: String, listener: DataRetrieveListenerForUser) {
        firebaseDatabase.reference.child("userData").child(uId).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val user: User = dataSnapshot.getValue(User::class.java)!!
                listener.onSuccess(user)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                listener.onError(databaseError)
            }
        })
    }

    fun getLastLocationByQuest(uId: String, questName: String, listener: LastLocationByQuestListener) {
        firebaseDatabase.getReference("userData").child(uId).child("quests").child(questName).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onCancelled(p0: DatabaseError) {
                listener.onError(EventResultStatus.NO_EVENT)
            }
            override fun onDataChange(p0: DataSnapshot) {
                if (p0.getValue(Int::class.java) != null) {
                    listener.onSuccess(p0.getValue(Int::class.java)!!)
                }
                else {listener.onError(EventResultStatus.EVENT_FAILED)}
            }

        })
    }

    fun setLastLocationByQuest(uId: String, questName: String, location: Int) {
        firebaseDatabase.getReference("userData").child(uId).child("quests").child(questName).setValue(location)
    }

    fun writeUserPoints(uId: String, points: Int) {
        firebaseDatabase.reference.child("userData").child(uId).child("points").setValue(points)
    }

    fun questRetrieverByName(questName: String, listener: DataRetrieverListenerForSingleQuestStructure) {
        firebaseDatabase.getReference("quest").child(questName).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(dataSnapshot: DataSnapshot) {

                val locationsIdList = ArrayList<Int>()
                val questStructure = dataSnapshot.getValue(Quest::class.java)
                for (dataSnapshot1 in dataSnapshot.child("locations").children) {
                    locationsIdList.add(dataSnapshot1.getValue(Int::class.java)!!)
                }
                listener.onSuccess(questStructure!!, locationsIdList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                listener.onError(databaseError)
            }
        })
    }


}