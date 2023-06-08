package com.example.cafeteriaapp

import adapters.RecyclerChatAdapter
import adapters.RecyclerCurrentOrderAdapter
import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeteriaapp.databinding.ActivityChat2Binding
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import datamodels.Chat
import datamodels.NotificationData
import datamodels.PushNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import services.FirebaseService
import services.RetrofitInstance

class Chat2Activity : AppCompatActivity() {

    //private var _bindingChat: ActivityChat2Binding? = null
   // private val bindingChat get() = _bindingChat!!
    private lateinit var adapter: RecyclerChatAdapter
    private lateinit var firestore : FirebaseFirestore
    private lateinit var auth : FirebaseAuth
    private lateinit var sharedPref: SharedPreferences
    private var chats = arrayListOf<Chat>()
    private lateinit var sendBtn: Button
    private lateinit var chatTxt: EditText
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerAdapter: RecyclerCurrentOrderAdapter

    private lateinit var databaseRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat2)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        sendBtn=findViewById(R.id.button)
        chatTxt=findViewById(R.id.chatText)
        recyclerView = findViewById(R.id.listRecyclerView)

        sharedPref = getSharedPreferences("user_profile_details", AppCompatActivity.MODE_PRIVATE)
        databaseRef=FirebaseDatabase.getInstance().reference

        adapter = RecyclerChatAdapter()
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)
        //bindingChat.listRecyclerView.layoutManager = LinearLayoutManager(this)

        val shp = sharedPref.getString("emp_org", "11")

        sendBtn.setOnClickListener {

            val chatText = chatTxt.text.toString()
            val user = auth.currentUser!!.email!!
            val userUID = auth.currentUser!!.uid

            val dataMap = HashMap<String, Any>()
            dataMap.put("text",chatText)
            dataMap.put("user",user)
            dataMap.put("date", FieldValue.serverTimestamp())
//cJW1QnwvlgM0nWACxRWtbMvMCtk1
            firestore.collection("Chats").document(shp!!)
                .collection(userUID).add(dataMap).addOnSuccessListener {
                    chatTxt.setText("")
                }.addOnFailureListener {
                    Toast.makeText(this,it.localizedMessage, Toast.LENGTH_LONG).show()
                    chatTxt.setText("")
                }
            pushChatNotification(userUID)
        }



        val userUID = auth.currentUser!!.uid
        firestore.collection("Chats").document(shp!!)
            .collection(userUID).orderBy("date",
            Query.Direction.ASCENDING).addSnapshotListener { value, error ->
            if (value != null) {
                if(value!!.isEmpty) {
                    Toast.makeText(this,"No Chat", Toast.LENGTH_LONG).show()
                } else {
                    val documents = value.documents
                    chats.clear()
                    for (document in documents ) {
                        val text = document.get("text") as String
                        val user = document.get("user") as String
                        val chat = Chat(user,text)
                        chats.add(chat)
                        adapter.chats = chats
                    }
                }
            }
            adapter.notifyDataSetChanged()
        }
    }


    fun pushChatNotification(customerUID:String){
        val orgID = sharedPref.getString("emp_org", "11")
        FirebaseService.sharedPref2 = getSharedPreferences("sharedPref2", Context.MODE_PRIVATE)
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener{ task ->
            if(!task.isSuccessful){
                return@OnCompleteListener
            }
            val token = task.result
            Log.d("FCMTOKEN",token)

            FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)

            databaseRef.child(orgID!!).child("tokens").child(customerUID)
                .get().addOnSuccessListener {
                    if(it.exists()){
                        val orgToken = it.child("token").value.toString()
                        sendNotification(
                            PushNotification(
                                NotificationData("Your have new message!", "Check it"),
                                orgToken)
                        )
                    }
                }
        })
    }

    private fun sendNotification(notification: PushNotification)= CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if(response.isSuccessful) {
                Log.d("TAG", "Response: ${Gson().toJson(response)}")
            } else {
                Log.e("TAG", response.errorBody().toString())
            }
        } catch(e: Exception) {
            Log.e("TAG", e.toString())
        }
    }

    fun goBack(view: View){
        onBackPressed()
    }

}