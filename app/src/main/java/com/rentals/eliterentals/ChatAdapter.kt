package com.rentals.eliterentals

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class ChatAdapter(private val items: MutableList<MessageDto>) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_ME = 1
        private const val TYPE_BOT = 2
    }

    override fun getItemViewType(position: Int): Int {
        return if (items[position].isChatbot == true) TYPE_BOT else TYPE_ME
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_ME) {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.row_chat_me, parent, false)
            MeVH(v)
        } else {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.row_chat_bot, parent, false)
            BotVH(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val m = items[position]
        if (holder is MeVH) holder.bind(m)
        if (holder is BotVH) holder.bind(m)
    }

    override fun getItemCount() = items.size

    fun append(m: MessageDto, rv: RecyclerView) {
        items.add(m)
        notifyItemInserted(items.size - 1)
        rv.scrollToPosition(items.size - 1)
    }

    class MeVH(v: View): RecyclerView.ViewHolder(v) {
        private val tv: TextView = v.findViewById(R.id.txt)
        fun bind(m: MessageDto) { tv.text = m.messageText ?: "" }
    }
    class BotVH(v: View): RecyclerView.ViewHolder(v) {
        private val tv: TextView = v.findViewById(R.id.txt)
        fun bind(m: MessageDto) { tv.text = m.messageText ?: "" }
    }
}
