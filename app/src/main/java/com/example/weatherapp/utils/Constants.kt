package com.example.weatherapp.utils

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build


// TODO (STEP 3.2: Create the activities package and utils package and add the MainActivity class to it and create the constant object in utils.)\
// START
object Constants {

    // TODO (STEP 4.2: Add the API key and Base URL and Metric unit here from openweathermap.)
    const val APP_ID: String = "2e9c316dbc889d80a1544488afcea51c"
    const val BASE_URL: String = "http://api.openweathermap.org/data/"
    const val METRIC_UNIT: String = "metric"

    // TODO (STEP 8.3: Add the SharedPreferences name and key name for storing the response data in it.)
    // START
    const val PREFERENCE_NAME = "WeatherAppPreference"
    const val WEATHER_RESPONSE_DATA = "weather_response_data"

    // TODO (STEP 3.3: Add a function to check the network connection is available or not.)
    /**
     * This function is used check the weather the device is connected to the Internet or not.
     */

    @SuppressLint("MissingPermission")
    fun isNetworkAvailable(context: Context): Boolean {
        //it answers the queries about the state of network connectivity.
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            val network = connectivityManager.activeNetwork ?:return false
            val activeNetWork = connectivityManager.getNetworkCapabilities(network) ?:return false
            return when{
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ->true
                activeNetWork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ->true
                else ->false
            }
        }else{
            //Returns details about the currently active default data network.
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }

}