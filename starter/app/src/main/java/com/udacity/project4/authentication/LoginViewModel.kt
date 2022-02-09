package com.udacity.project4.authentication

import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.udacity.project4.authentication.firebase.FirebaseUserLiveData

class LoginViewModel : ViewModel() {

    private val firebaseUserLiveData = FirebaseUserLiveData()

    enum class AuthenticationState {
        AUTHENTICATED, UNAUTHENTICATED, INVALID_AUTHENTICATION
    }

    val authenticationState = firebaseUserLiveData.map { user ->
        if (user != null) {
            AuthenticationState.AUTHENTICATED
        } else {
            AuthenticationState.UNAUTHENTICATED
        }
    }

    fun logOut() {
        firebaseUserLiveData.logOut()
    }
}