package com.udacity.project4.locationreminders.geofence

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Settings.Secure.getString
import android.util.Log
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofenceStatusCodes
import com.google.android.gms.location.GeofencingEvent
import com.udacity.project4.R
import com.udacity.project4.utils.sendNotification
import timber.log.Timber
import java.io.Serializable

/**
 * Triggered by the Geofence.  Since we can have many Geofences at once, we pull the request
 * ID from the first Geofence, and locate it within the cached data in our Room DB
 *
 * Or users can add the reminders and then close the app, So our app has to run in the background
 * and handle the geofencing in the background.
 * To do that you can use https://developer.android.com/reference/android/support/v4/app/JobIntentService to do that.
 *
 */

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        //get the intent from geofencing event
        val geofencingEvent = GeofencingEvent.fromIntent(intent)
        if (geofencingEvent.hasError()) {
            val errorMessage = GeofenceStatusCodes
                .getStatusCodeString(geofencingEvent.errorCode)
            Timber.e(errorMessage)
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition

        // Test that the reported transition was of interest.
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER ||
        geofenceTransition == Geofence.GEOFENCE_TRANSITION_EXIT) {
            Timber.i("geofence SIZE = ${(geofenceTransition as ArrayList<Geofence>).size}")

            val triggeringGeofences = geofencingEvent.triggeringGeofences

            val intentGeofence = Intent()
            intentGeofence.putExtra(
                GeofenceTransitionsJobIntentService.INTENT_EXTRA_REQUEST_ID,
                triggeringGeofences as Serializable
            )
            GeofenceTransitionsJobIntentService.enqueueWork(context, intentGeofence)

            Timber.i("geofence NO PROBLEM $geofenceTransition")
        } else {
            // Log the error.
            Timber.e("geofence PROBLEM $geofenceTransition")
        }

    }
}