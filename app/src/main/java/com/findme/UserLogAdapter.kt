package com.findme

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import kotlinx.android.synthetic.main.user_log_list_item.view.*

class UserLogAdapter(private val items: ArrayList<Location>, private val clickListener: (Location) -> Unit) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        val view = inflater.inflate(R.layout.user_log_list_item, parent, false)
        return PartViewHolder(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as PartViewHolder).bind(items[position], clickListener)
    }

    override fun getItemCount() = items.size

    class PartViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        fun bind(location: Location, clickListener: (Location) -> Unit) {
            itemView.userLocationTextView.text = location.name
            itemView.userLatTextView.text = location.lat
            itemView.userLngTextView.text = location.lng
            itemView.userTimeTextView.text = location.time
            itemView.setOnClickListener { clickListener(location) }
        }
    }
}