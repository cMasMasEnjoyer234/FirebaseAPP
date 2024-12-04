package com.example.firebaseapp

import android.annotation.SuppressLint
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.firebaseapp.model.Inventario
import com.google.firebase.firestore.FirebaseFirestore
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.IMqttActionListener
import org.eclipse.paho.client.mqttv3.IMqttToken
import org.eclipse.paho.client.mqttv3.MqttMessage
import android.util.Log
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import java.util.UUID




class MainActivity2 : AppCompatActivity() {
    private lateinit var db: FirebaseFirestore
    private lateinit var inventarioAdapter: InventarioAdapter
    private val inventarioList = mutableListOf<Inventario>()
    private var selectedItemId: String? = null

    //private val ssid = "Galaxy S10111b"   // Cambia a tu SSID
    //private val password = "ncgx1560"     // Cambia a tu contraseña
    private val mqttServer = "tcp://test.mosquitto.org:1883"  // Servidor MQTT

    private lateinit var mqttClient: MqttClient
    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main2)

        // Inicializar la conexión MQTT





        // Inicializar Firebase
        db = FirebaseFirestore.getInstance()

        // Referencias a vistas de la UI
        val rvInventario = findViewById<RecyclerView>(R.id.rvInventario)
        val etNombre = findViewById<EditText>(R.id.etNombre)
        val etDireccion = findViewById<EditText>(R.id.etDireccion)
        val etCantidad = findViewById<EditText>(R.id.etCantidad)
        val btnAddClient = findViewById<Button>(R.id.btnAddClient)
        val btnEditClient = findViewById<Button>(R.id.btnEditClient)
        val btnDisableClient = findViewById<Button>(R.id.btnDisableClient)
        val btnRefrescar = findViewById<Button>(R.id.btnRefrescar)





        // Configuración del RecyclerView
        rvInventario.layoutManager = LinearLayoutManager(this)
        inventarioAdapter = InventarioAdapter(inventarioList)
        rvInventario.adapter = inventarioAdapter

        // Cargar los elementos del inventario
        getAllItems()

        // Configuración de los eventos de los botones
        btnAddClient.setOnClickListener {
            val nombre = etNombre.text.toString()
            val direccion = etDireccion.text.toString()
            val cantidad = etCantidad.text.toString()

            if (nombre.isNotEmpty() && direccion.isNotEmpty() && cantidad.isNotEmpty()) {
                val nuevoInventario = Inventario(
                    id = UUID.randomUUID().toString(),
                    nombre = nombre,
                    direccion = direccion,
                    cantidad = cantidad,
                    habilitado = 1
                )
                addItem(nuevoInventario)
                clearFields(etNombre, etDireccion, etCantidad)
                Toast.makeText(this, "Cliente agregado exitosamente", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Por favor, complete todos los campos", Toast.LENGTH_SHORT).show()
            }
        }

        btnEditClient.setOnClickListener {
            selectedItemId?.let { documentId ->
                val updatedItem = Inventario(
                    id = documentId,
                    nombre = etNombre.text.toString(),
                    direccion = etDireccion.text.toString(),
                    cantidad = etCantidad.text.toString(),
                    habilitado = 1
                )
                editItem(documentId, updatedItem)
                clearFields(etNombre, etDireccion, etCantidad)
                Toast.makeText(this, "Item actualizado exitosamente", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(this, "Seleccione un Item primero", Toast.LENGTH_SHORT).show()
        }

        btnDisableClient.setOnClickListener {
            selectedItemId?.let { documentId ->
                disableItem(documentId)
                clearFields(etNombre, etDireccion, etCantidad)
                Toast.makeText(this, "Item eliminado exitosamente", Toast.LENGTH_SHORT).show()
            } ?: Toast.makeText(this, "Seleccione un Item primero", Toast.LENGTH_SHORT).show()
        }

        btnRefrescar.setOnClickListener {
            getAllItems()
        }

        // Evento de clic en un item del inventario
        inventarioAdapter.setOnItemClickListener { documentId, inventario ->
            selectedItemId = documentId
            etNombre.setText(inventario.nombre)
            etDireccion.setText(inventario.direccion)
            etCantidad.setText(inventario.cantidad)
        }
    }
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Conectar al servidor MQTT y suscribirse a un topic

    fun disconnectMqttClient() {
        try {
            if (mqttClient.isConnected) {
                mqttClient.disconnect() // Disconnect from the broker
                Log.d("MQTT", "Disconnected from the MQTT server")
            } else {
                Log.d("MQTT", "Client is already disconnected")
            }
        } catch (e: Exception) {
            Log.e("MQTT", "Error while disconnecting: ${e.message}")
        }
    }


    // Publicar un mensaje en un topic
    private fun sendMessage(message: String) {
        try {
            val topic = "alerta/inventario"
            val mqttMessage = MqttMessage(message.toByteArray()).apply {
                qos = 1  // QOS 1 para entrega al menos una vez
            }
            mqttClient.publish(topic, mqttMessage)
            Log.d("MQTT", "Mensaje publicado en el topic: $topic")
        } catch (e: Exception) {
            Log.e("MQTT", "Error al enviar el mensaje: ${e.message}")
        }
    }

    // Obtener todos los items del inventario desde Firebase
    private fun getAllItems() {
        db.collection("Inventario")
            .whereEqualTo("habilitado", 1)
            .get()
            .addOnSuccessListener { result ->
                inventarioList.clear()
                val lowStockItems = mutableListOf<String>() // Lista para items con bajo stock

                for (document in result) {
                    val inventario = document.toObject(Inventario::class.java)
                    inventario.id = document.id
                    inventarioList.add(inventario)

                    // Verificar si el stock del item es menor o igual a 5
                    val cantidad = inventario.cantidad.toIntOrNull() ?: 0
                    if (cantidad <= 5) {
                        lowStockItems.add("${inventario.nombre} (Cantidad: $cantidad)")
                    }
                }

                // Enviar mensaje MQTT si hay items con bajo stock
                if (lowStockItems.isNotEmpty()) {
                    Thread {
                        try {
                            // Crear y configurar el cliente MQTT
                            mqttClient = MqttClient(mqttServer, MqttClient.generateClientId(), null)
                            val options = MqttConnectOptions().apply {
                                isCleanSession = true

                            }

                            // Conectar al servidor MQTT
                            mqttClient.connect(options)
                            Log.d("MQTT", "Conexión exitosa al servidor MQTT")

                            // Preparar el mensaje
                            val mensaje = "Items con bajo stock:\n" + lowStockItems.joinToString("\n")
                            sendMessage(mensaje)

                            // Desconectar
                            mqttClient.disconnect()
                            Log.d("MQTT", "Cliente MQTT desconectado")

                        } catch (e: Exception) {
                            Log.e("MQTT", "Error en el cliente MQTT: ${e.message}")
                        }
                    }.start()
                }

                inventarioAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al obtener inventario: $e", Toast.LENGTH_SHORT).show()
            }
    }


    // Añadir un nuevo item al inventario en Firebase
    private fun addItem(inventario: Inventario) {
        db.collection("Inventario")
            .add(inventario)
            .addOnSuccessListener {
                getAllItems()
                //publishMqttMessage() // Publicar mensaje MQTT cuando se añade un nuevo item
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al agregar item: $e", Toast.LENGTH_SHORT).show()
            }
    }

    // Editar un item del inventario en Firebase
    private fun editItem(documentId: String, updatedItem: Inventario) {
        db.collection("Inventario").document(documentId)
            .set(updatedItem)
            .addOnSuccessListener {
                getAllItems()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al editar item: $e", Toast.LENGTH_SHORT).show()
            }
    }

    // Eliminar un item del inventario en Firebase
    private fun disableItem(documentId: String) {
        db.collection("Inventario").document(documentId)
            .delete()
            .addOnSuccessListener {
                getAllItems()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error al eliminar item: $e", Toast.LENGTH_SHORT).show()
            }
    }

    // Limpiar los campos de texto
    private fun clearFields(vararg editTexts: EditText) {
        editTexts.forEach { it.text.clear() }
    }
    override fun onDestroy() {
        super.onDestroy()
        disconnectMqttClient()
    }
}
