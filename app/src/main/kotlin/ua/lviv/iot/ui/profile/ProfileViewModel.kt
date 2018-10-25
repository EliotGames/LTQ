package ua.lviv.iot.ui.profile

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.google.firebase.database.DatabaseError
import ua.lviv.iot.model.firebase.FirebaseDataManager
import ua.lviv.iot.model.firebase.FirebaseLoginManager
import ua.lviv.iot.model.firebase.User
import ua.lviv.iot.model.firebase.UserSex

class ProfileViewModel : ViewModel() {

    var isUserRegistered: LiveData<Boolean> = FirebaseLoginManager._isUserLoggedIn
    private var currentUser = User("Name Surname", "example@gmail.com")
    var currentUserData = MutableLiveData<User>().default(currentUser)

    fun getCurrentUser() {
        FirebaseDataManager().getCurrentUserData(FirebaseLoginManager().currentUser!!.uid, object : FirebaseDataManager.DataRetrieveListenerForUser {
            override fun onSuccess(user: User) {
                currentUser = user
                currentUserData.value = currentUser
            }

            override fun onError(databaseError: DatabaseError) {
                TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
            }

        })
    }

    fun <T : Any?> MutableLiveData<T>.default(initialValue: T) = apply { setValue(initialValue) }


}