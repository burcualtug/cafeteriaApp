package com.example.cafeteriaapp

import adapters.RecyclerChatAdapter
import android.content.SharedPreferences
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.cafeteriaapp.databinding.FragmentChatBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import datamodels.Chat
/*
*
//import com.google.firebase.iid.FirebaseInstanceId
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import datamodels.NotificationData
import datamodels.PushNotification
import kotlinx.android.synthetic.main.activity_notification.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import services.FirebaseDBService
import services.FirebaseService
import services.RetrofitInstance
*
* FirebaseMessaging.getInstance().token.addOnSuccessListener { result ->
            FirebaseService.token = result
            etToken.setText(result)
        }

        * <activity
            android:name=".NotificationActivity"
            android:exported="false">
            <meta-data
                android:name="android.app.lib_name"
                android:value="" />
        </activity>

        <service
            android:name="services.FirebaseService"
            android:exported="true"
            android:permission="com.google.android.c2dm.permission.SEND">
            <intent-filter>
                <action android:name="com.google.firebase.MESSAGING_EVENT" />
                <action android:name="com.google.android.c2dm.intent.RECEIVE" />
            </intent-filter>
        </service>
*
* */
class ChatFragment : Fragment() {
    private var _binding: FragmentChatBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: RecyclerChatAdapter
    private lateinit var firestore : FirebaseFirestore
    private lateinit var auth : FirebaseAuth
    private lateinit var sharedPref: SharedPreferences
    private var chats = arrayListOf<Chat>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        sharedPref = this.requireActivity().getSharedPreferences("user_profile_details", AppCompatActivity.MODE_PRIVATE)


    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentChatBinding.inflate(inflater,container,false)
        val view = binding.root
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = RecyclerChatAdapter()
        binding.listRecyclerView.adapter = adapter
        binding.listRecyclerView.layoutManager = LinearLayoutManager(requireContext())

        val shp = sharedPref.getString("emp_org", "11")

        binding.button.setOnClickListener {

            val chatText = binding.chatText.text.toString()
            val user = auth.currentUser!!.email!!
            val userUID = auth.currentUser!!.uid

            val dataMap = HashMap<String, Any>()
            dataMap.put("text",chatText)
            dataMap.put("user",user)
            dataMap.put("date", FieldValue.serverTimestamp())

            firestore.collection("Chats").document(shp!!)
                .collection(userUID).add(dataMap).addOnSuccessListener {
                binding.chatText.setText("")
            }.addOnFailureListener {
//                Toast.makeText(requireContext(),it.localizedMessage, Toast.LENGTH_LONG).show()
                binding.chatText.setText("")
            }
        }

        firestore.collection("Chats").orderBy("date",
            Query.Direction.ASCENDING).addSnapshotListener { value, error ->
            if (value != null) {
                if(value!!.isEmpty) {
                    Toast.makeText(requireContext(),"No Chat", Toast.LENGTH_LONG).show()
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


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}