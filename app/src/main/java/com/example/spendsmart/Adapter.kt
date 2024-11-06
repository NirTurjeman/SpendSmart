package com.example.spendsmart

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.util.Date
import java.util.Locale

class MyAdapter(private val items: MutableList<Item>) : RecyclerView.Adapter<MyAdapter.ViewHolder>() {

    data class Item(
        val avatar: Int? = null,
        val userName: String = "",
        val category: String = "",
        val description: String? = null,
        val price: String = "",
        val date: Date = Date(),
        val transactionType: TransactionType = TransactionType.INCOME,
    ) {
        constructor() : this(0, "", "", null, "", Date(), TransactionType.INCOME)
    }


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val avatarView: ImageView = view.findViewById(R.id.list_avatar_icon)
        val userNameView: TextView = view.findViewById(R.id.list_username)
        val categoryView: TextView = view.findViewById(R.id.list_category)
        val descriptionView: TextView = view.findViewById(R.id.list_description)
        val priceView: TextView = view.findViewById(R.id.list_price)
        val dateView: TextView = view.findViewById(R.id.list_date)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        if (item.avatar != null)
            holder.avatarView.setImageResource(item.avatar)
         else
            holder.avatarView.setImageDrawable(null)

        holder.userNameView.text = item.userName
        holder.categoryView.text = item.category
        holder.priceView.text = item.price
        holder.dateView.text = item.date.toShortDateString()

        // Set text color for the price based on transaction type
        val color = when (item.transactionType) {
            TransactionType.INCOME -> R.color.green
            TransactionType.EXPENSE -> R.color.red
        }
        when (item.transactionType){
            TransactionType.INCOME -> holder.priceView.text = "+"+item.price+"₪"
            TransactionType.EXPENSE -> holder.priceView.text = "-"+item.price+"₪"
        }
        holder.priceView.setTextColor(ContextCompat.getColor(holder.itemView.context, color))
        // Display description only if it's not null or empty
        if (!item.description.isNullOrEmpty()) {
            holder.descriptionView.text = item.description
            holder.descriptionView.visibility = View.VISIBLE
        } else {
            holder.descriptionView.visibility = View.GONE
        }
    }
    fun Date.toShortDateString(): String {
        val formatter = SimpleDateFormat("yyyy/MM/dd", Locale.ENGLISH)
        return formatter.format(this)
    }
    fun addItem(newItem: Item) {
        items.add(0, newItem)
        notifyItemInserted(0)
    }
    fun setItems(newItems: MutableList<Item>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }
    override fun getItemCount(): Int = items.size
}
