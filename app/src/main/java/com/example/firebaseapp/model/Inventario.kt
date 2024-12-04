package com.example.firebaseapp.model

data class Inventario (
    var id: String = "",
    val nombre: String = "",
    val direccion: String = "",
    val cantidad: String = "",
    val habilitado: Int = 1
){
    constructor() : this("", "", "", "",1)
}