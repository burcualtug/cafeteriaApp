package adapters

import android.content.Context
import android.graphics.Color
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import datamodels.CurrentOrderItem
import com.example.cafeteriaapp.R


class RecyclerCurrentOrderAdapter(
    var context: Context,
    private var currentOrderList: ArrayList<CurrentOrderItem>,
    private val listener: OnItemClickListener
) :
    RecyclerView.Adapter<RecyclerCurrentOrderAdapter.ItemListViewHolder>() {


    interface OnItemClickListener {
        fun showQRCode(orderID: String)
        fun cancelOrder(position: Int)
        fun contactOrder(position: Int)
    }

    class ItemListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val takeAwayTimeTV: TextView = itemView.findViewById(R.id.current_order_item_take_away_time_tv)
        val paymentStatusTV: TextView = itemView.findViewById(R.id.current_order_item_payment_status_tv)
        val orderIDTV: TextView = itemView.findViewById(R.id.current_order_item_order_id_tv)
        val tableLayout: TableLayout = itemView.findViewById(R.id.current_order_item_table_layout)
        val totalItemPriceTV: TextView = itemView.findViewById(R.id.current_order_item_total_price_tv)
        val totalTaxTV: TextView = itemView.findViewById(R.id.current_order_item_total_tax_tv)
        val subTotalTV: TextView = itemView.findViewById(R.id.current_order_item_sub_total_tv)
        val showQRBtn: ExtendedFloatingActionButton = itemView.findViewById(R.id.current_order_item_show_qr_btn)
        val cancelBtn: ExtendedFloatingActionButton = itemView.findViewById(R.id.current_order_item_cancel_btn)
        val contactBtn: ExtendedFloatingActionButton = itemView.findViewById(R.id.current_order_item_chat_btn)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemListViewHolder {
        val itemView = LayoutInflater.from(parent.context).inflate(
            R.layout.current_order_item,
            parent,
            false
        )
        return ItemListViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ItemListViewHolder, position: Int) {

        val currentItem = currentOrderList[position]

        holder.takeAwayTimeTV.text = currentItem.takeAwayTime
        holder.paymentStatusTV.text = currentItem.paymentStatus
        holder.orderIDTV.text = currentItem.orderID
        holder.totalItemPriceTV.text = "\$%.2f".format(currentItem.totalItemPrice.toFloat())
        holder.totalTaxTV.text = ("\$%.2f".format(currentItem.tax.toFloat())).toString()
        holder.subTotalTV.text = ("\$%.2f".format(currentItem.subTotal.toFloat())).toString()
        Log.d("TAG",currentItem.situation)
        addTable(currentItem, holder.tableLayout)
        if(currentItem.situation == "1"){
            holder.cancelBtn.isEnabled=false
            holder.cancelBtn.isClickable=false
            holder.cancelBtn.setBackgroundColor(Color.GRAY)
        }

        holder.showQRBtn.setOnClickListener {
            listener.showQRCode(currentItem.orderID)
        }

        holder.cancelBtn.setOnClickListener {
            listener.cancelOrder(position)
        }

        holder.contactBtn.setOnClickListener {
            listener.contactOrder(position)
        }
    }

    private fun addTable(currentOrderItem: CurrentOrderItem, table: TableLayout) {

        val items = currentOrderItem.orderItemNames.split(";")
        val qtys = currentOrderItem.orderItemQuantities.split(";")

        for(i in items.indices) {
            //adding row in table
            table.addView(
                getTableRow(
                    items[i],
                    qtys[i]
                )
            )
        }
    }

    private fun getTableRow(itemName: String, itemQty: String): TableRow {
        val tbRow = TableRow(context)
        val tbItemName = TextView(context)
        val tbQty = TextView(context)

        val typeface = ResourcesCompat.getFont(context, R.font.montserrat_semi_bold)

        //Setting Text
        tbItemName.text = itemName
        tbQty.text = itemQty

        //Changing Color
        tbItemName.setTextColor(Color.parseColor("#1C213F"))
        tbQty.setTextColor(Color.parseColor("#1C213F"))

        //Changing Font
        tbItemName.typeface = typeface
        tbQty.typeface = typeface

        tbRow.layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        tbQty.textAlignment = ViewGroup.TEXT_ALIGNMENT_TEXT_END

        //adding item name and quantity in a row
        tbRow.addView(tbItemName)
        tbRow.addView(tbQty)

        return tbRow
    }

    override fun getItemCount(): Int = currentOrderList.size

}