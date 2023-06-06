package com.example.cafeteriaapp

import adapters.RecyclerChatAdapter
import adapters.RecyclerCurrentOrderAdapter
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeteriaapp.databinding.ActivityChat2Binding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import datamodels.Chat

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat2)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()
        sendBtn=findViewById(R.id.button)
        chatTxt=findViewById(R.id.chatText)
        recyclerView = findViewById(R.id.listRecyclerView)

        sharedPref = getSharedPreferences("user_profile_details", AppCompatActivity.MODE_PRIVATE)

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

    fun goBack(view: View){
        onBackPressed()
    }

}