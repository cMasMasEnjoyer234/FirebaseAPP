package com.example.firebaseapp

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import kotlin.math.sign

class MainActivity : AppCompatActivity() {

    private lateinit var  firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val correo : TextView = findViewById(R.id.edtEmail)
        val clave : TextView = findViewById(R.id.edtPassword)
        val btnIngresar: Button = findViewById(R.id.btnIngresar)
        firebaseAuth = Firebase.auth

        btnIngresar.setOnClickListener(){
            signIn(correo.text.toString(), clave.text.toString())
        }
    }
    private fun signIn(correo:String, clave:String){
        firebaseAuth.signInWithEmailAndPassword(correo, clave)
            .addOnCompleteListener(this){ task ->
                if(task.isSuccessful){
                    val user = firebaseAuth.currentUser
                    Toast.makeText(baseContext, "Autenticacion Correcta", Toast.LENGTH_SHORT).show()
                    val i = Intent(this, MainActivity2::class.java)
                    startActivity(i)
                }else{
                    Toast.makeText(baseContext, "Autenticacion Fallida", Toast.LENGTH_SHORT).show()
                }
            }
    }

}