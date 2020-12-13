package com.example.mediatemplate

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.example.mediatemplate.databinding.ItemTrackBinding


class MediaAdapter() : RecyclerView.Adapter<MediaAdapter.TracksListViewHolder>() {

    private val TAG: String= Constants.TAG

    private lateinit var listener: OnTrackClickListener

    inner class TracksListViewHolder( itemView: View) : RecyclerView.ViewHolder(itemView) {
        val itemTrackBinding = ItemTrackBinding.bind(itemView)

    }

    // DiffUtil is a list holder callback to manage data into adapter
//   private val diffCallBack = object : DiffUtil.ItemCallback<String>() {
//        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
//            return oldItem == newItem
//        }
//        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
//            return oldItem == newItem
//        }
//    }


    // adapter list placeholder
    val asyncListDiffer = AsyncListDiffer(this, object : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
            return oldItem == newItem
        }
    })


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TracksListViewHolder =
        TracksListViewHolder(
            LayoutInflater.from(parent.context).inflate(R.layout.item_track, parent, false)
        )


    override fun getItemCount() = asyncListDiffer.currentList.size


    override fun onBindViewHolder(holder: TracksListViewHolder, position: Int) {

        asyncListDiffer.currentList.let { userPlayList ->
            val trackName = userPlayList[position]
            val nameView = holder.itemTrackBinding.trackName


            holder.itemView.apply {
                nameView.text = trackName
                setOnClickListener {

                    listener.onTrackClick(position, trackName)
                }
            }
        }

    }


    interface OnTrackClickListener {
        fun onTrackClick(position: Int, trackName: String)

    }

    fun setOnTrackClickListener(onTrackClickListener: OnTrackClickListener) {
        listener = onTrackClickListener
        Log.d(TAG, "setOnTrackClickListener: listener = $listener")

    }


}