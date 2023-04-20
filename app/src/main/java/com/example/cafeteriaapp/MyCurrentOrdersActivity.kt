package com.example.cafeteriaapp

import adapters.RecyclerCurrentOrderAdapter
import android.content.DialogInterface
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.cafeteriaapp.R
import com.google.firebase.database.*
import datamodels.CurrentOrderItem
import services.DatabaseHandler

class MyCurrentOrdersActivity : AppCompatActivity(), RecyclerCurrentOrderAdapter.OnItemClickListener {

    private val currentOrderList = ArrayList<CurrentOrderItem>()
    private lateinit var recyclerView: RecyclerView
    private lateinit var recyclerAdapter: RecyclerCurrentOrderAdapter
    private lateinit var sharedPref: SharedPreferences
    private lateinit var databaseRef: DatabaseReference
    private lateinit var cancelButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_current_orders)

        //cancelButton = findViewById(R.id.current_order_item_cancel_btn)
        sharedPref = getSharedPreferences("user_profile_details", MODE_PRIVATE)
        databaseRef = FirebaseDatabase.getInstance().reference


        recyclerView = findViewById(R.id.current_order_recycler_view)
        recyclerAdapter = RecyclerCurrentOrderAdapter(this, currentOrderList, this)
        recyclerView.adapter = recyclerAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        loadCurrentOrdersFromDatabase()
    }

    private fun loadCurrentOrdersFromDatabase() {

        val db = DatabaseHandler(this)
        val data = db.readCurrentOrdersData()

        val shp = sharedPref.getString("emp_org", "11")
        val ordersDbRef = databaseRef.child(shp!!).child("orders")

        findViewById<LinearLayout>(R.id.current_order_empty_indicator_ll).visibility = ViewGroup.GONE
        ordersDbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (snap in snapshot.children) {
                    val currentOrderItem =  CurrentOrderItem()
                    currentOrderItem.orderID = snap.child("order_id").value.toString()
                    currentOrderItem.takeAwayTime = snap.child("takeAwayTime").value.toString()
                    currentOrderItem.paymentStatus = snap.child("paymentMethod").value.toString()
                    currentOrderItem.orderItemNames = snap.child("itemNames").value.toString()
                    currentOrderItem.orderItemQuantities = snap.child("itemQty").value.toString()
                    currentOrderItem.totalItemPrice = snap.child("totalItemPrice").value.toString()
                    currentOrderItem.tax = snap.child("totalTaxPrice").value.toString()
                    currentOrderItem.subTotal = snap.child("subTotal").value.toString()

                    if(snap.child("situation").value == "1"){
                        //cancelOrder
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

    override fun showQRCode(orderID: String) {
        //User have to just show the QR Code, and canteen staff have to scan, so user don't have to wait more
        val bundle = Bundle()
        bundle.putString("orderID", orderID)

        val dialog = QRCodeFragment()
        dialog.arguments = bundle
        dialog.show(supportFragmentManager, "QR Code Generator")
    }

    override fun cancelOrder(position: Int) {
        AlertDialog.Builder(this)
            .setTitle("Order Cancellation")
            .setMessage("Are you sure you want to cancel this order?")
            .setPositiveButton("Yes, Cancel Order", DialogInterface.OnClickListener {dialogInterface, _ ->
                val result = DatabaseHandler(this).deleteCurrentOrderRecord(currentOrderList[position].orderID)
                currentOrderList.removeAt(position)
                recyclerAdapter.notifyItemRemoved(position)
                val orgID = sharedPref.getString("emp_org", "11")
                val orderIDdb = currentOrderList[position].orderID
                databaseRef.child(orgID!!).child("orders").child(orderIDdb).removeValue()

                recyclerAdapter.notifyItemRangeChanged(position, currentOrderList.size)
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

    fun goBack(view: View) {onBackPressed()}
}