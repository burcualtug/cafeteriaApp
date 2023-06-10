package com.example.cafeteriaapp

import adapters.RecyclerCurrentOrderAdapter
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.messaging.FirebaseMessaging
import com.google.gson.Gson
import datamodels.CurrentOrderItem
import datamodels.NotificationData
import datamodels.PushNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import services.DatabaseHandler
import services.FirebaseService
import services.RetrofitInstance

class MyCurrentOrdersActivity : AppCompatActivity(), RecyclerCurrentOrderAdapter.OnItemClickListener {

    private val currentOrderList = ArrayList<CurrentOrderItem>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerAdapter: RecyclerCurrentOrderAdapter
    private lateinit var sharedPref: SharedPreferences
    private lateinit var databaseRef: DatabaseReference
    private lateinit var cancelButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_current_orders)

        //cancelButton = findViewById(R.id.current_order_item_cancel_btn)
        sharedPref = getSharedPreferences("user_profile_details", MODE_PRIVATE)
        databaseRef = FirebaseDatabase.getInstance().reference
        auth= FirebaseAuth.getInstance()


        recyclerView = findViewById(R.id.current_order_recycler_view)
        recyclerAdapter = RecyclerCurrentOrderAdapter(this, currentOrderList, this)
        recyclerView.adapter = recyclerAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadCurrentOrdersFromDatabase()
    }

    private fun loadCurrentOrdersFromDatabase() {

        val db = DatabaseHandler(this)
        val data = db.readCurrentOrdersData()
        val user=auth.currentUser!!
        val shp = sharedPref.getString("emp_org", "11")
        val ordersDbRef = databaseRef.child(shp!!).child("orders").child(user.uid)

        findViewById<LinearLayout>(R.id.current_order_empty_indicator_ll).visibility = ViewGroup.GONE
        ordersDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (snap in snapshot.children) {
                    val currentOrderItem =  CurrentOrderItem()
                    currentOrderItem.userUID=snap.child("userUID").value.toString()
                    currentOrderItem.orderID = snap.child("order_id").value.toString()
                    currentOrderItem.takeAwayTime = snap.child("takeAwayTime").value.toString()
                    currentOrderItem.paymentStatus = snap.child("paymentMethod").value.toString()
                    currentOrderItem.orderItemNames = snap.child("itemNames").value.toString()
                    currentOrderItem.orderItemQuantities = snap.child("itemQty").value.toString()
                    currentOrderItem.totalItemPrice = snap.child("totalItemPrice").value.toString()
                    currentOrderItem.tax = snap.child("totalTaxPrice").value.toString()
                    currentOrderItem.subTotal = snap.child("subTotalPrice").value.toString()
                    currentOrderItem.situation = snap.child("situation").value.toString()

                    if(snap.child("situation").value.toString() == "1"){
                        //Toast.makeText(applicationContext,"HAZIR VARRR",Toast.LENGTH_SHORT).show()
                    }
                    currentOrderList.add(currentOrderItem)
                    currentOrderList.reverse()
                    recyclerAdapter.notifyItemRangeInserted(0, 2)
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // HANDLE ERROR
            }
        })
/*
        val db = DatabaseHandler(this)
        val data = db.readCurrentOrdersData()

        if(data.isEmpty()) {
            return
        }

        findViewById<LinearLayout>(R.id.current_order_empty_indicator_ll).visibility = ViewGroup.GONE
        for(i in 0 until data.size) {
            val currentOrderItem =  CurrentOrderItem()

            currentOrderItem.orderID = data[i].orderID
            currentOrderItem.takeAwayTime = data[i].takeAwayTime
            currentOrderItem.paymentStatus = data[i].paymentStatus
            currentOrderItem.orderItemNames = data[i].orderItemNames
            currentOrderItem.orderItemQuantities = data[i].orderItemQuantities
            currentOrderItem.totalItemPrice = data[i].totalItemPrice
            currentOrderItem.tax = data[i].tax
            currentOrderItem.subTotal = data[i].subTotal
            currentOrderList.add(currentOrderItem)
            currentOrderList.reverse()
            recyclerAdapter.notifyItemRangeInserted(0, data.size)
        } */
    }

    override fun contactOrder(position: Int){
        startActivity(
            Intent(
                this,
                Chat2Activity::class.java
            )
        )
    }

    override fun showQRCode(orderID: String) {
        //User have to just show the QR Code, and canteen staff have to scan, so user don't have to wait more
        val bundle = Bundle()
        bundle.putString("orderID", orderID)

        val dialog = QRCodeFragment()
        dialog.arguments = bundle
        dialog.show(supportFragmentManager, "QR Code Generator")
    }

    override fun cancelOrder(position: Int) {
        getOrgID(position)
        AlertDialog.Builder(this)
            .setTitle("Order Cancellation")
            .setMessage("Are you sure you want to cancel this order?")
            .setPositiveButton("Yes, Cancel Order", DialogInterface.OnClickListener {dialogInterface, _ ->
                val result = DatabaseHandler(this).deleteCurrentOrderRecord(currentOrderList[position].orderID)

                val orgID = sharedPref.getString("emp_org", "11")
                val orderIDdb = currentOrderList[position].orderID
                val orderUserUID = currentOrderList[position].userUID
                databaseRef.child(orgID!!).child("orders").child(orderUserUID).child(orderIDdb).removeValue()

                currentOrderList.removeAt(position)
                recyclerAdapter.notifyItemRemoved(position)
                recyclerAdapter.notifyItemRangeChanged(position, currentOrderList.size)

                //pushCancelOrderNotification(position)
                Toast.makeText(this, result, Toast.LENGTH_SHORT).show()

                if(currentOrderList.isEmpty()) {
                    findViewById<LinearLayout>(R.id.current_order_empty_indicator_ll).visibility = ViewGroup.VISIBLE
                }

                dialogInterface.dismiss()
            })
            .setNegativeButton("No", DialogInterface.OnClickListener {dialogInterface, _ ->
                dialogInterface.dismiss()
            })
            .create().show()
    }

    private fun getOrgID(position:Int){

        val user = FirebaseAuth.getInstance().currentUser!!
        val databaseRef: DatabaseReference = FirebaseDatabase.getInstance().reference

        databaseRef.child("matches").child(user.uid)
            .addListenerForSingleValueEvent(object: ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val globalOrgID = snapshot.child("organizationID").value.toString()

                    pushCancelOrderNotification(globalOrgID,position)
                }

                override fun onCancelled(error: DatabaseError) {
                    TODO("Not yet implemented")
                }
            })
    }

    fun pushCancelOrderNotification(orgID:String,position: Int){
        //val orgID = sharedPref.getString("emp_org", "11")
        FirebaseService.sharedPref2 = getSharedPreferences("sharedPref2", Context.MODE_PRIVATE)
        FirebaseMessaging.getInstance().token.addOnCompleteListener(OnCompleteListener{ task ->
            if(!task.isSuccessful){
                return@OnCompleteListener
            }
            val token = task.result
            Log.d("FCMTOKEN",token)

            FirebaseMessaging.getInstance().subscribeToTopic(TOPIC)

            val orderID=currentOrderList[position].orderID

            databaseRef.child(orgID!!).child("tokens").child("company")
                .get().addOnSuccessListener {
                    if(it.exists()){
                        val orgToken = it.child("token").value.toString()
                        sendNotification(
                            PushNotification(
                            NotificationData("Your order cancelled!", "Order ID: $orderID"),
                            orgToken)
                        )
                    }
                }
        })
    }

    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
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

    fun goBack(view: View) {onBackPressed()}
}