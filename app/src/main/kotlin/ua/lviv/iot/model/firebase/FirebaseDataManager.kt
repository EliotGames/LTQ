package ua.lviv.iot.model.firebase

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import ua.lviv.iot.model.map.Location
import ua.lviv.iot.model.map.Quest

class FirebaseDataManager constructor(val firebaseDatabase: FirebaseDatabase = FirebaseDatabase.getInstance()){
    interface DataRetrieveListener {
        fun onSuccess()
    }

    /*public interface DataRetrieveListenerForQuestCategory {
        fun onSuccess(questCategoryList: List<QuestCategory>)
        fun onError(databaseError: DatabaseError)
    }*/

    public interface DataRetrieveListenerForQuest {
        fun onSuccess(questStructureList: List<Quest>)
        fun onError(databaseError: DatabaseError)
    }

    public interface DataRetrieveListenerForLocationsStructure {
        fun onSuccess(locationStructureList: List<Location>)
        fun onError(databaseError: DatabaseError)
    }

    public interface DataRetrieveListenerForUser {
        fun onSuccess(userInformation: User)
        fun onError(databaseError: DatabaseError)
    }

    public interface UserWritingListener {
        fun onSuccess()
        fun onError()
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
        firebaseDatabase.getReference("quest").addListenerForSingleValueEvent(object : ValueEventListener {
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

    /*fun locationsListRetriever(listener: DataRetrieveListenerForLocationsStructure) {
        firebaseDatabase.getReference("locations").addListenerForSingleValueEvent(object : ValueEventListener() {
            override fun onDataChange(dataSnapshot: DataSnapshot) {
                val locationsList = ArrayList<Location>()
                for (dataSnapshot1 in dataSnapshot.children) {
                    locationsList.add(dataSnapshot1.getValue(Location::class.java)!!)
                }
                listener.onSuccess(locationsList)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                listener.onError(databaseError)
            }
        })
    }*/

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
                val userInformation: User = dataSnapshot.getValue(User::class.java)!!
                listener.onSuccess(userInformation)
            }

            override fun onCancelled(databaseError: DatabaseError) {
                listener.onError(databaseError)
            }
        })
    }

    fun writeUserPoints(uId: String, points: Int) {
        firebaseDatabase.reference.child("userData").child(uId).child("points").setValue(points)
    }


}