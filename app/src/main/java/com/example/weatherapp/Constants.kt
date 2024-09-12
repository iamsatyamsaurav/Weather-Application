package com.example.weatherapp

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build

object Constants {

    const val APP_ID:String="c6133ad4d02edc42c122269a3fcad579"  //api key
    const val BASE_URL:String="http://api.openweathermap.org/data/"
    const val METRIC_UNIT:String="metric"

    //for shared preferences
    const val PREFERENCE_NAME="WeatherAppPreference"
    const val WEATHER_RESPONSE_DATA="weather_response_data"


    //This function, isNetworkAvailable(), checks if the device has an active internet connection
    fun isNetworkAvailable(context: Context):Boolean{
        val connectivityManager=context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager //This line initializes the connectivityManager, which is used to check the state of network connectivity (like whether Wi-Fi or mobile data is connected).

        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            //for newer versions

            //Gets the active network (if there is one). If there’s no active network, the function returns false, meaning no internet connection is available.
            val network =connectivityManager.activeNetwork?: return false

            //Retrieves the capabilities of the active network (e.g., whether it's Wi-Fi, cellular, etc.). If no capabilities are found, it returns false.
            val activeNetwork=connectivityManager.getNetworkCapabilities(network)?: return false

            return when{
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)->true  //Checks if the active network is a Wi-Fi connection.
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)->true //Checks if the active network is a cellular (mobile data) connection
                activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)->true //Checks if the active network is an Ethernet connection
                else->false
            }

        }
        else{
            //for older versions
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo!=null && networkInfo.isConnectedOrConnecting  //- Checks if the device is connected to a network or in the process of connecting. If so, it returns `true`; otherwise, it returns `false`.

        }
    }
}