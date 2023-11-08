package com.example.weatherapp.activities

import android.Manifest
import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

import com.example.weatherapp.R
import com.example.weatherapp.models.WeatherResponse
import com.example.weatherapp.network.WeatherService
import com.example.weatherapp.utils.Constants
import com.google.android.gms.location.*
import com.google.gson.Gson
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener

import retrofit.*
import java.text.SimpleDateFormat
import java.util.*

//import com.karumi.dexter.listener.single.PermissionListener

//import com.karumi.dexter.listener.single.PermissionListener

// TODO (STEP 1.1 : After creating a project as we are developing a weather app kindly visit the link below.)
// OpenWeather Link : https://openweathermap.org/api

class MainActivity : AppCompatActivity() {

    // TODO (STEP 2.3: Add a variable for FusedLocationProviderClient.)
    // START
    // A fused location client variable which is further used to get the user's current location

    private lateinit var mFusedLocationClient: FusedLocationProviderClient

    // TODO (STEP 5.2: Create a global variable for ProgressDialog.)
    private var mProgressDialog: Dialog? = null     // global variable for progress dialog

    // TODO (STEP 7.5: Make the latitude and longitude variables global to use it in the menu item selection to refresh the data.)
    // START
    // A global variable for Current Latitude
    private var mLatitude: Double = 0.0
    // A global variable for Current Longitude
    private var mLongitude: Double = 0.0
    // END

    // TODO (STEP 8.1: Add a variable for SharedPreferences)
    // START
    // A global variable for the SharedPreferences
    private lateinit var mSharedPreferences: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        // TODO (STEP 2.4: Initialize the fusedLocationProviderClient variable.)
        // START
        // Initialize the Fused location variable
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        // TODO (STEP 8.2: Initialize the SharedPreferences variable.)
        // START
        // Initialize the SharedPreferences variable
        mSharedPreferences = getSharedPreferences(Constants.PREFERENCE_NAME, Context.MODE_PRIVATE)
        // TODO (STEP 1.8: Check here whether GPS is ON or OFF using the method which we have created)
        // START
        if(!isLocationEnabled()){
            Toast.makeText(
                this,
                "Your location provider is turned off. Please turn it on.",
                Toast.LENGTH_SHORT
            ).show()

            //This will redirect you to settings where you need to turn on the location provider.
            val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
            startActivity(intent)
        }else{
//            Toast.makeText(
//                this,
//                "Your location provider is already ON",
//                Toast.LENGTH_SHORT
//            ).show()

            // TODO (STEP 2.1: Asking the location permission on runtime.)
            // START
            Dexter.withActivity(this)
                .withPermissions(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
                .withListener(object : MultiplePermissionsListener {
                    override fun onPermissionsChecked(report: MultiplePermissionsReport?) {
                        if (report!!.areAllPermissionsGranted()) {
                            // TODO (STEP 2.7: Call the location request function here.)
                            // START
                            requestLocationData()
                            // END
                        }

                        if (report.isAnyPermissionPermanentlyDenied) {
                            Toast.makeText(
                                this@MainActivity,
                                "You have denied location permission. Please allow it is mandatory.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }

                    override fun onPermissionRationaleShouldBeShown(
                        permissions: MutableList<PermissionRequest>?,
                        token: PermissionToken?
                    ) {
                        showRationalDialogForPermissions()
                    }
                }).onSameThread()
                .check()
            // END
        }
    }

    // TODO (STEP 1.7: Check whether the GPS is ON or OFF)
    // START

    fun isLocationEnabled(): Boolean {

        //This provides access to system location services.
        val locationManager: LocationManager =
            getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // TODO (STEP 2.2: A alert dialog for denied permissions and if needed to allow it from the settings app info.)
    // START
    /**
     * A function used to show the alert dialog when the permissions are denied and need to allow it from settings app info.
     */
    private fun showRationalDialogForPermissions() {
        AlertDialog.Builder(this)
            .setMessage("It looks like you have turned off permissions required for this feature. It can be enabled under application settings")
            .setPositiveButton(
                "GO TO SETTINGS"
            ) {_, _ ->
                try {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    val uri = Uri.fromParts("package",packageName, null)
                    intent.data = uri
                    startActivity(intent)
                }catch (e: ActivityNotFoundException){
                    e.printStackTrace()
                }
            }
            .setNegativeButton("Cancel"){ dialog,
                                          _ ->
                dialog.dismiss()
            }.show()
    }


    //TODO(2.5 - Add a function to get the location of device using the fusedLocationProviderClient.)
    /**
     * A function to request the current location. Using the fused location provider client.
     */
    @SuppressLint("MissingPermission")
    private fun requestLocationData() {

        val mLocationRequest = LocationRequest()
        mLocationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        mFusedLocationClient.requestLocationUpdates(
            mLocationRequest, mLocationCallback,
            Looper.myLooper()
        )
    }

    // TODO (STEP 2.6: Register a request location callback to get the location.)
    // START
    /**
     * A location callback object of fused location provider client where we will get the current location details.
     */

    private val mLocationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {

            val mLastLocation: Location = locationResult.lastLocation

            // TODO (STEP 7.6: Assign the values to the global variables here
            //  to use that for api calling. And remove the latitude and
            //  longitude from the parameter as we can directly use it while
            //  API calling.)
            mLatitude = mLastLocation.latitude
            Log.i("Current Latitude", "$mLatitude")

            mLongitude = mLastLocation.longitude
            Log.i("Current Longitude", "$mLongitude")

            // TODO (STEP 4.6: Call the api calling function here.)
            getLocationWeatherDetails(mLatitude, mLongitude)
        }
    }

    // TODO (STEP 3.5: Create a function to make an api call using Retrofit Network Library.)
    // START
    /**
     * Function is used to get the weather details of the current location based on the latitude longitude
     */

    private fun getLocationWeatherDetails(latitude: Double, longitude: Double){
        //TODO(STEP 3.6 - Here we will check weather the internet connection
        // is available or not using the method which we have created in the Contant object.

        if(Constants.isNetworkAvailable(this)){

            // TODO (STEP 4.1: Make an api call using retrofit.)
            // START
            /**
             * Add the built-in converter factory first. This prevents overriding its
             * behavior but also ensures correct behavior when using converters that consume all types.
             */
            val retrofit : Retrofit = Retrofit.Builder()
                .baseUrl(Constants.BASE_URL)
                /** Add converter factory for serialization and deserialization of objects. */
                /**
                 * Create an instance using a default {@link Gson} instance for conversion. Encoding to JSON and
                 * decoding from JSON (when no charset is specified by a header) will use UTF-8.
                 */
                .addConverterFactory(GsonConverterFactory.create())
                /** Create the Retrofit instances. */
                .build()

            // TODO (STEP 4.5: Further step for API call)
            // START
            /**
             * Here we map the service interface in which we declares the end point and the API type
             *i.e GET, POST and so on along with the request parameter which are required.
             */
            val service: WeatherService = retrofit.create<WeatherService>(WeatherService::class.java)

            val listCall: Call<WeatherResponse> = service.getWeather(
                latitude, longitude, Constants.METRIC_UNIT, Constants.APP_ID
            )

            //TODO (5.4 - Show the progress dialog.)
            showCustomProgressDialog()      //used to show the progress dialog

            /** An invocation of a Retrofit method that sends a request to a web-server and returns a response.
             * Here we pass the required param in the service
             */
            listCall.enqueue(object : Callback<WeatherResponse>{
                override fun onResponse(response: Response<WeatherResponse>?, retrofit: Retrofit?) {
                    // Check weather the response is success or not.
                    if(response!!.isSuccess){

                        //TODO(5.5 - Hide the progress dialog)
                        hideProgressDialog()

                        val weatherList: WeatherResponse = response.body()
                        // TODO (STEP 6.6: Call the setup UI method here and pass the response object as a parameter to it to get the individual values.)
                        // START

                        // TODO (STEP 8.4: Here we convert the response object to string and store the string in the SharedPreference.)
                        // START
                        // Here we have converted the model class in to Json String to store it in the SharedPreferences.
                        val weatherResponseJsonString = Gson().toJson(weatherList)
                        // Save the converted string to shared preferences
                        val editor = mSharedPreferences.edit()
                        editor.putString(Constants.WEATHER_RESPONSE_DATA, weatherResponseJsonString)
                        editor.apply()

                        // TODO (STEP 8.5: Remove the weather detail object as we will be getting
                        //  the object in form of a string in the setup UI method.)

                        setupUI()
                        //setupUI(weatherList)

                        Log.i("Response Result", "$weatherList")
                    } else {
                        // If the response is not success then we check the response code.
                        val rc = response.code()
                        when (rc) {
                            400 -> {
                                Log.e("Error 400", "Bad Request")
                            }
                            404 -> {
                                Log.e("Error 404", "Not Found")
                            }
                            else -> {
                                Log.e("Error", "Generic Error")
                            }
                        }

                    }
                }

                override fun onFailure(t: Throwable?) {
                    //TODO(5.6 - Hide the progress dialog)
                    hideProgressDialog()
                    if (t != null) {
                        Log.e("Errorrrrr", t.message.toString())
                    }
                }

            }
            )

//            Toast.makeText(
//                this@MainActivity,
//                "You have connected to the internet. Now you can make an api call.",
//                Toast.LENGTH_SHORT
//            ).show()
        }else {
            Toast.makeText(
                this@MainActivity,
                "No internet connection available.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    //TODO(5.3 - Create a function for SHOW and HIDE progress dialog.)
    /**
     * Method is used to show the Custom progress dialog.
     */

    private fun showCustomProgressDialog(){
        mProgressDialog = Dialog(this)

        /*Set the screen content from a layout resource.
        The resource will be inflated, adding all top lvl views to the screen.
         */
        mProgressDialog!!.setContentView(R.layout.dialog_custom_progress)

        //Start the dialog and display it on screen.
        mProgressDialog!!.show()
    }

    // TODO (STEP 7.3: Now add the override methods to load the menu file and perform the selection on item click.)
    // START
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // TODO (STEP 7.7: Now finally, make an api call on item selection.)
        // START
        return when(item.itemId){
            R.id.action_refresh->{
                requestLocationData()
                true
            }else ->return super.onOptionsItemSelected(item)
        }
        //END

    }

    /**
     * This function is used to dismiss the progress dialog if it is visible to user.
     */

    private fun hideProgressDialog(){
        if(mProgressDialog != null){
            mProgressDialog!!.dismiss()
        }
    }

    // TODO (STEP 6.5: We have set the values to the UI and also added some required methods for Unit and Time below.)
    /**
     * Function is used to set the result in the UI elements.
     */

    private fun setupUI(){

        // TODO (STEP 8.6: Here we get the stored response from
        //  SharedPreferences and again convert back to data object
        //  to populate the data in the UI.)
        // START
        // Here we have got the latest stored response from the SharedPreference and converted back to the data model object.


        val weatherResponseJsonString = mSharedPreferences.getString(Constants.WEATHER_RESPONSE_DATA, "")
        if(!weatherResponseJsonString.isNullOrEmpty()){
            val weatherList = Gson().fromJson(weatherResponseJsonString, WeatherResponse::class.java)

            for (z in weatherList.weather.indices){

                var tv_main = findViewById<TextView>(R.id.tv_main)
                var tv_main_description = findViewById<TextView>(R.id.tv_main_description)
                var tv_temp = findViewById<TextView>(R.id.tv_temp)
                var tv_humidity = findViewById<TextView>(R.id.tv_humidity)
                var tv_min = findViewById<TextView>(R.id.tv_min)
                var tv_max = findViewById<TextView>(R.id.tv_max)
                var tv_speed = findViewById<TextView>(R.id.tv_speed)
                var tv_name = findViewById<TextView>(R.id.tv_name)
                var tv_country = findViewById<TextView>(R.id.tv_country)
                var tv_sunrise_time = findViewById<TextView>(R.id.tv_sunrise_time)
                var tv_sunset_time = findViewById<TextView>(R.id.tv_sunset_time)


                tv_main.text = weatherList.weather[z].main
                tv_main_description.text = weatherList.weather[z].description
                tv_temp.text =
                    weatherList.main.temp.toString() + getUnit(application.resources.configuration.locales.toString())
                tv_humidity.text = weatherList.main.humidity.toString() + " per cent"
                tv_min.text = weatherList.main.tempMin.toString() + " min"
                tv_max.text = weatherList.main.tempMax.toString() + " max"
                tv_speed.text = weatherList.wind.speed.toString()
                tv_name.text = weatherList.name
                tv_country.text = weatherList.sys.country
                tv_sunrise_time.text = unixTime(weatherList.sys.sunrise.toLong())
                tv_sunset_time.text = unixTime(weatherList.sys.sunset.toLong())


                var iv_main = findViewById<ImageView>(R.id.iv_main)
                when (weatherList.weather[z].icon) {
                    "01d" -> iv_main.setImageResource(R.drawable.sunny)
                    "02d" -> iv_main.setImageResource(R.drawable.cloud)
                    "03d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04d" -> iv_main.setImageResource(R.drawable.cloud)
                    "04n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10d" -> iv_main.setImageResource(R.drawable.rain)
                    "11d" -> iv_main.setImageResource(R.drawable.storm)
                    "13d" -> iv_main.setImageResource(R.drawable.snowflake)
                    "01n" -> iv_main.setImageResource(R.drawable.cloud)
                    "02n" -> iv_main.setImageResource(R.drawable.cloud)
                    "03n" -> iv_main.setImageResource(R.drawable.cloud)
                    "10n" -> iv_main.setImageResource(R.drawable.cloud)
                    "11n" -> iv_main.setImageResource(R.drawable.rain)
                    "13n" -> iv_main.setImageResource(R.drawable.snowflake)
                }
            }
        }


    }

    private fun unixTime(timex: Long): CharSequence? {
        val date = Date(timex * 1000L)
        @SuppressLint("SimpleDateFormat") val sdf =
            SimpleDateFormat("HH:mm:ss")
        sdf.timeZone = TimeZone.getDefault()
        return sdf.format(date)
    }

    private fun getUnit(value: String): String? {
        Log.i("unitttttt", value)
        var value = "°C"
        if ("US" == value || "LR" == value || "MM" == value) {
            value = "°F"
        }
        return value
    }

}

