package com.example.weatherapp

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.example.weatherapp.model.WeatherResponse
import com.example.weatherapp.network.WeatherService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import retrofit2.*
import retrofit2.converter.gson.GsonConverterFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class MainActivity : AppCompatActivity() {

    private lateinit var mFusedLocationClient:FusedLocationProviderClient

    private var customProgressDialog:Dialog?=null

    private var tv_main:TextView?=null
    private var tv_main_description:TextView?=null
    private var tv_temp:TextView?=null
    private var tv_sunrise_time:TextView?=null
    private var tv_sunset_time:TextView?=null
    private var tv_humidity:TextView?=null
    private var tv_min:TextView?=null
    private var tv_speed:TextView?=null
    private var tv_name:TextView?=null
    private var tv_country:TextView?=null
    private var tv_max:TextView?=null
    private var iv_main:ImageView?=null
    private var ll_parent_layout:LinearLayout?=null

    private lateinit var mSharedPrefernces : SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tv_main=findViewById<TextView>(R.id.tv_main)
        tv_main_description=findViewById<TextView>(R.id.tv_main_description)
        tv_temp=findViewById<TextView>(R.id.tv_temp)
        tv_sunrise_time=findViewById<TextView>(R.id.tv_sunrise_time)
        tv_sunset_time=findViewById<TextView>(R.id.tv_sunset_time)
        tv_humidity=findViewById<TextView>(R.id.tv_humidity)
        tv_min=findViewById<TextView>(R.id.tv_min)
        tv_max=findViewById<TextView>(R.id.tv_max)
        tv_speed=findViewById<TextView>(R.id.tv_speed)
        tv_name=findViewById<TextView>(R.id.tv_name)
        tv_country=findViewById<TextView>(R.id.tv_country)
        iv_main=findViewById<ImageView>(R.id.iv_main)
        ll_parent_layout=findViewById<LinearLayout>(R.id.ll_parent_layout)



        //Constants.PREFERENCE_NAME is a constant that specifies the name of the preferences file.
        //Context.MODE_PRIVATE is the operating mode for the preferences, which ensures that the preferences file can only be accessed by the application.
        mSharedPrefernces=getSharedPreferences(Constants.PREFERENCE_NAME,Context.MODE_PRIVATE) //MODE_PRIVATE means that the shared preference info which we store in our phone will only be available for this particular application

        mFusedLocationClient=LocationServices.getFusedLocationProviderClient(this)  // returns the client that will be used to request location updates.

        setupUI()

        if(!isLocationEnabled()){
            Toast.makeText(this,"Your location provider is turned off. Please turn it on",Toast.LENGTH_LONG).show()

            val intent=Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)  //creates an Intent that directs the user to the device's location settings screen.
            startActivity(intent)
        }else{
            //location provider is already turned on so ask for permission to access fine and course location using dexter
            Dexter.withActivity(this) //This initializes the Dexter library for the current activity (this)
                .withPermissions(  //This specifies the permissions that need to be requested
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener{ //This sets up a listener that handles the user's response to the permission requests.
                    //MultiplePermissionsListener interface is implemented to manage the results of multiple permission requests.

                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        //This method is called after the permissions have been checked. It receives a MultiplePermissionsReport object that contains details about the permissions' status.

                        if(report!!.areAllPermissionsGranted()){ //This checks if all requested permissions have been granted by the user.
                            requestLocationData()
                        }
                        if(report.isAnyPermissionPermanentlyDenied){  //This checks if any of the requested permissions were permanently denied
                            Toast.makeText(this@MainActivity,
                                "You have denied the location permission. Please allow it for the app to work!",
                                Toast.LENGTH_LONG).show()

                        }

                    }

                    override fun onPermissionRationaleShouldBeShown(
                        //This method is called if the user needs to be shown a rationale for the permission request.
                        permissions: MutableList<PermissionRequest>?,  //The permissions parameter is a list of the requested permissions
                        token: PermissionToken?  //token allows the request to be continued or cancelled.
                    ) {
                        showRationalDialogForPermissions()
                    }
                })
                .onSameThread()  //Ensures that the permission request and the callback run on the same thread, typically the main (UI) thread.
                .check()  //This finalizes the permission request process, triggering the Dexter workflow to check and request the specified permissions.
        }
    }

    @SuppressLint("MissingPermission")
    private fun requestLocationData(){

        // Create a new LocationRequest object
        val mLocationRequest = LocationRequest.create().apply {  //Creates a new LocationRequest object using the create() method.
                                                                                //apply is used to configure the LocationRequest object within the block.
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY  //Sets the priority of the location request to high accuracy, meaning it will use GPS as well as Wi-Fi and cellular networks to determine the location.
        }

        // Request location updates
        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest,  //The LocationRequest object is passed as the first parameter to specify the request parameters.
            mLocationCallback,  //This callback will be triggered when the location is available.
            Looper.myLooper()  //This specifies the thread on which the callback should be executed. Looper.myLooper() returns the current thread's Looper object.
        )
    }

    private val mLocationCallback=object:LocationCallback(){
        override fun onLocationResult(locationResult: LocationResult) { //It receives a LocationResult object containing the location data.
            val mLastLocation: Location? =locationResult.lastLocation
            val latitude= mLastLocation?.latitude
            Log.i("Current latitude", "$latitude")

            val longitude=mLastLocation?.longitude
            Log.i("Current longitude", "$longitude")

            getLocationWeatherDetails(latitude,longitude)

        }

    }

    private fun getLocationWeatherDetails(latitude: Double?,longitude:Double?){
        if(Constants.isNetworkAvailable(this)){

            val retrofit:Retrofit=Retrofit.Builder()//building a Retrofit instance.
                .baseUrl(Constants.BASE_URL)  //Sets the base URL for API requests
                .addConverterFactory(GsonConverterFactory.create())  //This converts JSON responses into Kotlin objects.
                .build()  //Finalizes and creates the Retrofit instance.

            val service: WeatherService=retrofit  //using the retrofit we create  an instance of WeatherService (links the base url to "2.5/weather" end point)
                .create<WeatherService>(WeatherService::class.java)   // Creates an implementation of the WeatherService interface, which will be used to make network requests to the weather API.


            //Declares a Call object that will manage the API request and response.
            val listCall:Call<WeatherResponse> = service.getWeather(
                latitude!!,longitude!!,Constants.METRIC_UNIT,Constants.APP_ID
            )

            //just before the background task starts
            showProgressDialog()

            //listCall.enqueue(...): Sends the listCall request asynchronously and wait for the callback of the WeatherResponse.
            listCall.enqueue(object:Callback<WeatherResponse>{
            //enqueue is a method that schedules the request to be run in the background.

                //The response will be handled in the callback methods (onResponse and onFailure).

                override fun onResponse(
                    call: Call<WeatherResponse>,
                    response: Response<WeatherResponse>
                ) {
                    //we recieve the response and the response is successfull
                    if(response.isSuccessful){

                        cancelProgressDialog()

                        val weatherList: WeatherResponse? =response.body()

                        //****for shared preferences*****//
                        val weatherResponseJsonString= Gson().toJson(weatherList) //This line converts an object weatherList (presumably a list of weather data) into a JSON string.
                        val editor = mSharedPrefernces.edit()  //This line creates an editor object, which is used to make changes to the SharedPreferences


                        editor.putString(Constants.WEATHER_RESPONSE_DATA,weatherResponseJsonString)
                        //This line stores the weatherResponseJsonString (the JSON string representation of the weather data) into the SharedPreferences.
                        //Constants.WEATHER_RESPONSE_DATA is a key under which the JSON string is stored, so it can be retrieved later

                        editor.apply()  //to ensure that the changes are saved. 


                        //****for shared preferences*****//
                        setupUI()
                        
                        Log.i("Response Result ","$weatherList")
                    }
                    else{
                        //we recieve the response but the response is not successfull
                        val rc=response.code()
                        when(rc){
                            400->{
                                Log.e("Error 400","Bad Connection")
                            }
                            404->{
                                Log.e("Error 404","Not Found")
                            }
                            else -> {
                                Log.e("Error","Generic Error")
                            }
                        }

                    }
                }

                override fun onFailure(call: Call<WeatherResponse>, t: Throwable) {
                   Log.e("Errrorrrrrrr",t.message.toString())
                    cancelProgressDialog()
                }

            })

        }
        else{
            Toast.makeText(this,"No internet connection!",Toast.LENGTH_LONG).show()

        }
    }

    private fun showRationalDialogForPermissions(){
        AlertDialog.Builder(this) //Initializes an AlertDialog.Builder to create a dialog.
            .setMessage("It looks like you have turned off permissions for location. Please enable it for the app to work!")
            .setPositiveButton(
                "GO TO SETTINGS"
            ){
                _,_ ->
                try{
                    val intent=Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)  //Creates an Intent that will open the app's details settings page, where the user can manually enable the necessary permissions.
                    val uri = Uri.fromParts("package", packageName,null) //Constructs a Uri with the app's package name. This Uri is used to direct the settings page to the specific app's details.
                    intent.data=uri  //Sets the Uri data on the Intent, specifying that the Intent should open the settings page for this specific app.
                    startActivity(intent)
                }catch (e:ActivityNotFoundException){
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel"){
                dialog,_->
                dialog.dismiss()

            }.show()
    }



    //this function checks whether location services(Location/GPS) is enabled on an Android device.
    private fun isLocationEnabled():Boolean{

        //this provides access to the system location services
        val locationManager:LocationManager=getSystemService(Context.LOCATION_SERVICE) as LocationManager  //accessing the system's location services through LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)  //checks if the GPS provider is enabled for location service
                ||locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)  ////checks if the network provider is enabled for location service

    }

    private fun showProgressDialog() {
        customProgressDialog = Dialog(this@MainActivity)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top-level views to the screen.*/
        customProgressDialog?.setContentView(R.layout.dialog_custom_progress)

        //Start the dialog and display it on screen.
        customProgressDialog?.show()
    }

    /*
    * This function is used to dismiss the progress dialog if it is visible to user.
    */
    private fun cancelProgressDialog() {
        if (customProgressDialog != null) {
            customProgressDialog?.dismiss()
                customProgressDialog = null
        }

    }

    //this function will be called when Options  menu will be created
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when(item.itemId){
            R.id.action_refresh->{
                requestLocationData()
                true
            }
            else ->super.onOptionsItemSelected(item)
        }

    }

    private fun setupUI(){


        val weatherResponseJsonString=mSharedPrefernces.getString(Constants.WEATHER_RESPONSE_DATA,"") //This line retrieves the JSON string from SharedPreferences using the key Constants.WEATHER_RESPONSE_DATA

        Log.d("WeatherApp", "JSON String: $weatherResponseJsonString")


        if(!weatherResponseJsonString.isNullOrEmpty()){
            val weatherList=Gson().fromJson(weatherResponseJsonString,WeatherResponse::class.java)  //If the JSON string is valid, this line converts it back into a WeatherResponse object.
            Log.d("WeatherApp", "Parsed WeatherList: ${weatherList.weather}")
            for(i in weatherList!!.weather.indices){
                Log.i("Weather name",weatherList.weather[i].toString())


                tv_main!!.text=weatherList.weather[i].main
                tv_main_description!!.text=weatherList.weather[i].description
                tv_temp!!.text=weatherList.main.temp.toString()+getUnit(application.resources.configuration.locales.toString())

                tv_sunrise_time!!.text=unixTime(weatherList.sys.sunrise)
                tv_sunset_time!!.text=unixTime(weatherList.sys.sunset)

                tv_humidity!!.text=weatherList.main.humidity.toString()+" percent"
                tv_min!!.text=weatherList.main.temp_min.toString()+" min"
                tv_max!!.text=weatherList.main.temp_max.toString()+" max"
                tv_speed!!.text=weatherList.wind.speed.toString()
                tv_name!!.text=weatherList.name
                tv_country!!.text=weatherList.sys.country

                when(weatherList.weather[i].icon){
                    "01d" -> {iv_main!!.setImageResource(R.drawable.sunny)
                                ll_parent_layout!!.setBackgroundResource(R.color.sky_blue)}
                    "02d" -> {iv_main!!.setImageResource(R.drawable.cloud)
                        ll_parent_layout!!.setBackgroundResource(R.color.light_grey)}
                    "03d" -> {iv_main!!.setImageResource(R.drawable.cloud)
                        ll_parent_layout!!.setBackgroundResource(R.color.light_grey)}
                    "04d" -> {iv_main!!.setImageResource(R.drawable.cloud)
                        ll_parent_layout!!.setBackgroundResource(R.color.light_grey)}
                    "04n" -> {iv_main!!.setImageResource(R.drawable.cloud)
                        ll_parent_layout!!.setBackgroundResource(R.color.light_grey)}
                    "10d" -> {iv_main!!.setImageResource(R.drawable.rain)
                        ll_parent_layout!!.setBackgroundResource(R.color.dark_grey)}
                    "11d" -> iv_main!!.setImageResource(R.drawable.storm)
                    "13d" -> iv_main!!.setImageResource(R.drawable.snowflake)
                    "01n" -> {iv_main!!.setImageResource(R.drawable.cloud)
                        ll_parent_layout!!.setBackgroundResource(R.color.light_grey)}
                    "02n" ->{iv_main!!.setImageResource(R.drawable.cloud)
                        ll_parent_layout!!.setBackgroundResource(R.color.light_grey)}
                    "03n" -> {iv_main!!.setImageResource(R.drawable.cloud)
                        ll_parent_layout!!.setBackgroundResource(R.color.light_grey)}
                    "10n" -> {iv_main!!.setImageResource(R.drawable.cloud)
                        ll_parent_layout!!.setBackgroundResource(R.color.light_grey)}
                    "11n" -> {iv_main!!.setImageResource(R.drawable.rain)
                        ll_parent_layout!!.setBackgroundResource(R.color.dark_grey)}
                    "13n" -> iv_main!!.setImageResource(R.drawable.snowflake)
                }




            }
        }



    }

    //sets the unit of the temperature according to the location
    private fun getUnit(value:String):String?{

        if(value=="US" || value=="LR"||value=="MM"){
            return "°F"
        }
        return "°C"
    }

    //to convert the time from long to a standard understandable format
    private fun unixTime(timeX:Long):String?{
        val date= Date(timeX*1000L)  //timeX * 1000L converts the Unix timestamp from seconds to milliseconds, which is the expected format for the Date class.
        val sdf=SimpleDateFormat("HH:mm", Locale.UK)  //Locale.UK to ensure consistency in formatting according to UK locale conventions.
        sdf.timeZone= TimeZone.getDefault()  // SimpleDateFormat object (sdf) is set to use the default time zone of the device running the code
        return sdf.format(date)  //The function formats the Date object (date) into a string based on the specified pattern ("HH:mm") and returns it.
    }
}