package diss.testing.runningapp2.adapters

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.textview.MaterialTextView
import diss.testing.runningapp2.R
import diss.testing.runningapp2.db.SessionClass
import diss.testing.runningapp2.other.TrackingUtility
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class SessionAdapter: RecyclerView.Adapter<SessionAdapter.SessionViewHolder>() {

    inner class SessionViewHolder(itemView : View): RecyclerView.ViewHolder(itemView) {
        val imageView : ImageView
        val tvDate : MaterialTextView
        val tvAvgSpeed : MaterialTextView
        val tvDistance : MaterialTextView
        val tvTime : MaterialTextView
        val tvCalories : MaterialTextView

        init {
            // Define click listener for the ViewHolder's View
            imageView = itemView.findViewById(R.id.ivRunImage)
            tvDate = itemView.findViewById(R.id.tvDate)
            tvAvgSpeed = itemView.findViewById(R.id.tvAvgSpeed)
            tvDistance = itemView.findViewById(R.id.tvDistance)
            tvTime = itemView.findViewById(R.id.tvTime)
            tvCalories = itemView.findViewById(R.id.tvCalories)
        }
    }

    private val diffCallBack = object : DiffUtil.ItemCallback<SessionClass>() {
        override fun areItemsTheSame(oldItem: SessionClass, newItem: SessionClass): Boolean {
            return oldItem.sessionId == newItem.sessionId
        }

        @SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: SessionClass, newItem: SessionClass): Boolean {
            return oldItem.hashCode() == newItem.hashCode()
        }
    }

    private val differ = AsyncListDiffer(this, diffCallBack)

    fun submitList(list : List<SessionClass>) = differ.submitList(list)

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): SessionViewHolder {
        return SessionViewHolder(
            LayoutInflater.from(parent.context).inflate(
                R.layout.item_run,
                parent,
                false
            )
        )
    }

    override fun getItemCount(): Int {
        return differ.currentList.size
    }

    override fun onBindViewHolder(holder: SessionViewHolder, position: Int) {

        val session = differ.currentList[position]
        holder.itemView.apply {
            Timber.d("${session.timestamp}")
            Glide.with(this).load(session.img).into(holder.imageView)

            val calendar = Calendar.getInstance().apply {
                timeInMillis = session.timestamp
            }
            val dateFormat = SimpleDateFormat("dd.MM.yy", Locale.getDefault())
            holder.tvDate.text = dateFormat.format(calendar.time)

            val avgSpeed = "${session.avgSpeedInKmp}km/h"
            holder.tvAvgSpeed.text = avgSpeed

            val distanceInKm = "${session.distanceInMeters/1000F}km"
            holder.tvDistance.text = distanceInKm

            holder.tvTime.text = TrackingUtility.getFormattedTime(session.timeInMillis)

            val caloriesBurned = "${session.caloriesBurned}kcal"
            holder.tvCalories.text = caloriesBurned
        }

    }
}