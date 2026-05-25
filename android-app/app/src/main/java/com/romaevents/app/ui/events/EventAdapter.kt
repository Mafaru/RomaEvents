package com.romaevents.app.ui.events

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.romaevents.app.R
import com.romaevents.app.model.Event
import com.romaevents.app.utils.DateUtils
import java.util.Locale

class EventAdapter(
    private var events: List<Event>,
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
        val context = holder.itemView.context

        val orange = ContextCompat.getColor(context, R.color.roma_orange)
        val inCorso = ContextCompat.getColor(context, R.color.status_in_corso)
        val passato = ContextCompat.getColor(context, R.color.status_passato)

        holder.titleText.text = event.title
        holder.categoryText.text = (event.category ?: "EVENTO").uppercase(Locale.ROOT)
        
        // Formattazione data Premium
        val formattedDate = DateUtils.formatDateTime(event.nextOccurrence)
        holder.dateText.text = "📅  $formattedDate"
        
        holder.addressText.text = "📍  ${event.address ?: "Roma, Italia"}"

        when (event.status) {
            "IN_CORSO" -> {
                holder.statusText.setTextColor(inCorso)
                holder.statusText.text = "●  IN CORSO"
            }
            "PROSSIMO" -> {
                holder.statusText.setTextColor(orange)
                holder.statusText.text = "●  PROSSIMO"
            }
            else -> {
                holder.statusText.setTextColor(passato)
                holder.statusText.text = "●  CONCLUSO"
            }
        }

        holder.itemView.setOnClickListener {
            onClick(event)
        }
    }

    override fun getItemCount(): Int = events.size

    fun updateList(newList: List<Event>) {
        this.events = newList
        notifyDataSetChanged()
    }
}
