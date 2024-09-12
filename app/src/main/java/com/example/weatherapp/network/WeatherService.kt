package com.example.weatherapp.network

//import retrofit.http.GET
import com.example.weatherapp.model.WeatherResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query


interface WeatherService {
    @GET("2.5/weather")  //This is an annotation provided by Retrofit. It tells Retrofit that when this method is called, it should make a GET HTTP request to the URL endpoint "2.5/weather".

    //This function is meant to fetch weather data.
    fun getWeather(

        //The @Query annotation is used to specify query parameters that will be appended to the URL.
        @Query("lat")lat:Double,
        @Query("lon")lon:Double,
        @Query("units")units:String?,
        @Query("appid")appid:String?
    ) : Call<WeatherResponse>  //the return type of the fun is Call<WeatherResponse>
                                    //Call is a Retrofit type that represents the HTTP request and its response(response of type WeatherResponse).

}