package com.romaevents.app.ui.events

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.romaevents.app.R
import com.romaevents.app.model.Event
import com.romaevents.app.utils.DateUtils

class EventAdapter(
    private val events: List<Event>,
    private val onClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    class EventViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val titleText: TextView = view.findViewById(R.id.titleText)
        val categoryText: TextView = view.findViewById(R.id.categoryText)
        val dateText: TextView = view.findViewById(R.id.dateText)
        val addressText: TextView = view.findViewById(R.id.addressText)
        val statusText: TextView = view.findViewById(R.id.statusText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_event, parent, false)
        return EventViewHolder(view)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        val event = events[position]

        holder.titleText.text = event.title
        holder.categoryText.text = event.category ?: "Categoria"
        holder.dateText.text = "📅 ${DateUtils.formatDateTime(event.nextOccurrence)}"
        holder.addressText.text = "📍 ${event.address ?: "Indirizzo non disponibile"}"
        holder.statusText.text = event.status ?: "PROSSIMO"

        when (event.status) {
            "IN_CORSO" -> {
                holder.statusText.setTextColor(0xFF2E7D32.toInt())
                holder.statusText.text = "● IN CORSO"
            }
            "PROSSIMO" -> {
                holder.statusText.setTextColor(0xFF1565C0.toInt())
                holder.statusText.text = "● PROSSIMO"
            }
            else -> {
                holder.statusText.setTextColor(0xFF757575.toInt())
                holder.statusText.text = "● ${event.status ?: "STATO NON DISPONIBILE"}"
            }
        }

        holder.itemView.setOnClickListener {
            onClick(event)
        }
    }

    override fun getItemCount(): Int = events.size
}