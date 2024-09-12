package com.example.weatherapp.model

import java.io.Serializable

data class WeatherResponse(
    val coord:Coord,
    val weather: List<Weather>,
    val base:String,
    val main: Main,
    val visibility: Int,
    val wind:Wind,
    val rain:Rain,
    val clouds:Clouds,
    val dt:Int,
    val sys:Sys,
    val timezone:Int,
    val id:Int,
    val name: String,
    val cod:Int
):Serializable    //Serializable is a way to mark a class so that objects of that class can be saved or sent somewhere, like to a file, across a network, or between parts of an app.
