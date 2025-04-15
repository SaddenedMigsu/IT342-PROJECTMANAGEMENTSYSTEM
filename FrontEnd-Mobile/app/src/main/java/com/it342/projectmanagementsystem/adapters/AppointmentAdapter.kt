package com.it342.projectmanagementsystem.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.it342.projectmanagementsystem.R
import com.it342.projectmanagementsystem.models.Appointment
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class AppointmentAdapter(private var appointments: List<Appointment>) : 
    RecyclerView.Adapter<AppointmentAdapter.AppointmentViewHolder>() {

    private val dateFormat = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppointmentViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_appointment, parent, false)
        return AppointmentViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppointmentViewHolder, position: Int) {
        val appointment = appointments[position]
        holder.tvTitle.text = appointment.title ?: "No title"
        
        appointment.startTime?.let { startTime ->
            val startTimeMillis = startTime.toMillis()
            holder.tvRemainingTime.text = dateFormat.format(Date(startTimeMillis))
        } ?: run {
            holder.tvRemainingTime.text = "Time not set"
        }
        
        holder.btnViewDetails.setOnClickListener {
            // TODO: Implement view details action
        }
    }

    override fun getItemCount(): Int = appointments.size

    fun updateAppointments(newAppointments: List<Appointment>) {
        appointments = newAppointments
        notifyDataSetChanged()
    }

    class AppointmentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvTitle: TextView = itemView.findViewById(R.id.tvTitle)
        val tvRemainingTime: TextView = itemView.findViewById(R.id.tvRemainingTime)
        val btnViewDetails: MaterialButton = itemView.findViewById(R.id.btnViewDetails)
    }
} 