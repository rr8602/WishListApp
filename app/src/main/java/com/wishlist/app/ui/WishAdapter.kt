package com.wishlist.app.ui

import android.graphics.Paint
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.wishlist.app.R
import com.wishlist.app.data.WishItem
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * 위시리스트 RecyclerView 어댑터
 */
class WishAdapter(
    internal var wishes: List<WishItem>,
    private val onItemClick: (WishItem) -> Unit,
    private val onDeleteClick: (WishItem) -> Unit,
    private val onCheckboxClick: (WishItem) -> Unit,
    private val onShareClick: (WishItem) -> Unit
) : RecyclerView.Adapter<WishAdapter.WishViewHolder>() {

    class WishViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageWish: ImageView = view.findViewById(R.id.imageWish)
        val chipCategory: Chip = view.findViewById(R.id.chipCategory)
        val textPriority: TextView = view.findViewById(R.id.textPriority)
        val textTitle: TextView = view.findViewById(R.id.textTitle)
        val textDescription: TextView = view.findViewById(R.id.textDescription)
        val textPrice: TextView = view.findViewById(R.id.textPrice)
        val textDate: TextView = view.findViewById(R.id.textDate)
        val checkboxCompleted: CheckBox = view.findViewById(R.id.checkboxCompleted)
        val buttonLink: ImageButton = view.findViewById(R.id.buttonLink)
        val buttonShare: ImageButton = view.findViewById(R.id.buttonShare)
        val buttonDelete: ImageButton = view.findViewById(R.id.buttonDelete)
        val giftInfoLayout: LinearLayout = view.findViewById(R.id.giftInfoLayout)
        val textGiftedBy: TextView = view.findViewById(R.id.textGiftedBy)
        val textGiftMessage: TextView = view.findViewById(R.id.textGiftMessage)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WishViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wish, parent, false)
        return WishViewHolder(view)
    }

    override fun onBindViewHolder(holder: WishViewHolder, position: Int) {
        val wish = wishes[position]

        // 이미지 설정
        if (wish.imageUri != null) {
            try {
                val uri = Uri.parse(wish.imageUri)
                holder.imageWish.setImageURI(uri)
                // URI가 유효하지 않으면 null이 설정되므로 체크
                if (holder.imageWish.drawable == null) {
                    holder.imageWish.setImageResource(android.R.drawable.ic_menu_gallery)
                }
            } catch (e: Exception) {
                // URI 파싱 실패 시 기본 이미지 표시
                holder.imageWish.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } else {
            holder.imageWish.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // 카테고리 칩 설정
        holder.chipCategory.text = wish.category.displayName
        holder.chipCategory.setChipBackgroundColorResource(wish.category.colorResId)

        // 우선순위 표시
        when (wish.priority) {
            3 -> { // 높음
                holder.textPriority.text = "⭐⭐⭐"
                holder.textPriority.setTextColor(android.graphics.Color.parseColor("#D32F2F"))
            }
            2 -> { // 보통
                holder.textPriority.text = "⭐⭐"
                holder.textPriority.setTextColor(android.graphics.Color.parseColor("#F57C00"))
            }
            1 -> { // 낮음
                holder.textPriority.text = "⭐"
                holder.textPriority.setTextColor(android.graphics.Color.parseColor("#9E9E9E"))
            }
            else -> {
                holder.textPriority.text = "⭐⭐"
                holder.textPriority.setTextColor(android.graphics.Color.parseColor("#F57C00"))
            }
        }

        // 제목 및 설명
        holder.textTitle.text = wish.title
        holder.textDescription.text = wish.description

        // 가격 표시
        holder.textPrice.text = if (wish.price != null) {
            formatPrice(wish.price)
        } else {
            "가격 미정"
        }

        // 날짜
        holder.textDate.text = formatDate(wish.createdAt)

        // 체크박스
        holder.checkboxCompleted.isChecked = wish.isCompleted

        // 완료된 아이템은 취소선 표시
        if (wish.isCompleted) {
            holder.textTitle.paintFlags = holder.textTitle.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.textDescription.paintFlags = holder.textDescription.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            holder.textTitle.paintFlags = holder.textTitle.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
            holder.textDescription.paintFlags = holder.textDescription.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
        }

        // 설명이 비어있으면 숨김
        holder.textDescription.visibility = if (wish.description.isEmpty()) View.GONE else View.VISIBLE

        // 쇼핑 링크 버튼 표시/숨김
        if (!wish.shoppingUrl.isNullOrEmpty()) {
            holder.buttonLink.visibility = View.VISIBLE
            holder.buttonLink.setOnClickListener {
                val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(wish.shoppingUrl))
                holder.itemView.context.startActivity(intent)
            }
        } else {
            holder.buttonLink.visibility = View.GONE
        }

        // 선물 완료 정보 표시
        if (wish.giftInfo != null) {
            holder.giftInfoLayout.visibility = View.VISIBLE
            holder.textGiftedBy.text = "선물한 사람: ${wish.giftInfo.giftedByName}"

            if (wish.giftInfo.message != null) {
                holder.textGiftMessage.text = wish.giftInfo.message
                holder.textGiftMessage.visibility = View.VISIBLE
            } else {
                holder.textGiftMessage.visibility = View.GONE
            }
        } else {
            holder.giftInfoLayout.visibility = View.GONE
        }

        // 클릭 리스너
        holder.itemView.setOnClickListener { onItemClick(wish) }
        holder.buttonShare.setOnClickListener { onShareClick(wish) }
        holder.buttonDelete.setOnClickListener { onDeleteClick(wish) }
        holder.checkboxCompleted.setOnClickListener { onCheckboxClick(wish) }
    }

    override fun getItemCount() = wishes.size

    fun updateWishes(newWishes: List<WishItem>) {
        wishes = newWishes
        notifyDataSetChanged()
    }

    private fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }

    private fun formatPrice(price: Double): String {
        val formatter = DecimalFormat("#,###")
        return "₩ ${formatter.format(price)}"
    }
}
