package com.wishlist.app.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.wishlist.app.R
import com.wishlist.app.data.WishItem
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

/**
 * 공유된 위시리스트를 표시하는 어댑터
 * 읽기 전용 뷰와 선물 완료 표시 기능 제공
 */
class SharedWishAdapter(
    private val wishes: MutableList<WishItem>,
    private val ownerId: String,
    private val onMarkAsGifted: (WishItem) -> Unit
) : RecyclerView.Adapter<SharedWishAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textWishName: TextView = view.findViewById(R.id.textWishName)
        val textPrice: TextView = view.findViewById(R.id.textPrice)
        val chipCategory: Chip = view.findViewById(R.id.chipCategory)
        val textMemo: TextView = view.findViewById(R.id.textMemo)
        val textUrl: TextView = view.findViewById(R.id.textUrl)
        val textTargetDate: TextView = view.findViewById(R.id.textTargetDate)
        val giftedLayout: LinearLayout = view.findViewById(R.id.giftedLayout)
        val textGiftedBy: TextView = view.findViewById(R.id.textGiftedBy)
        val textGiftedMessage: TextView = view.findViewById(R.id.textGiftedMessage)
        val buttonMarkAsGifted: Button = view.findViewById(R.id.buttonMarkAsGifted)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_shared_wish, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val wish = wishes[position]

        // 제목
        holder.textWishName.text = wish.title

        // 가격
        if (wish.price != null && wish.price > 0) {
            val formattedPrice = NumberFormat.getNumberInstance(Locale.KOREA).format(wish.price)
            holder.textPrice.text = "${formattedPrice}원"
            holder.textPrice.visibility = View.VISIBLE
        } else {
            holder.textPrice.visibility = View.GONE
        }

        // 카테고리
        holder.chipCategory.text = wish.category.displayName

        // 메모 (description)
        if (wish.description.isNotEmpty()) {
            holder.textMemo.text = wish.description
            holder.textMemo.visibility = View.VISIBLE
        } else {
            holder.textMemo.visibility = View.GONE
        }

        // URL
        if (!wish.shoppingUrl.isNullOrEmpty()) {
            holder.textUrl.text = wish.shoppingUrl
            holder.textUrl.visibility = View.VISIBLE
        } else {
            holder.textUrl.visibility = View.GONE
        }

        // 목표일
        if (wish.targetDate != null && wish.targetDate > 0) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            holder.textTargetDate.text = "목표일: ${dateFormat.format(Date(wish.targetDate))}"
            holder.textTargetDate.visibility = View.VISIBLE
        } else {
            holder.textTargetDate.visibility = View.GONE
        }

        // 선물 완료 여부
        if (wish.giftInfo != null) {
            // 이미 선물 완료된 경우
            holder.giftedLayout.visibility = View.VISIBLE
            holder.textGiftedBy.text = "선물한 사람: ${wish.giftInfo.giftedByName}"

            if (wish.giftInfo.message != null) {
                holder.textGiftedMessage.text = wish.giftInfo.message
                holder.textGiftedMessage.visibility = View.VISIBLE
            } else {
                holder.textGiftedMessage.visibility = View.GONE
            }

            holder.buttonMarkAsGifted.visibility = View.GONE
        } else {
            // 아직 선물 전인 경우
            holder.giftedLayout.visibility = View.GONE
            holder.buttonMarkAsGifted.visibility = View.VISIBLE
            holder.buttonMarkAsGifted.setOnClickListener {
                onMarkAsGifted(wish)
            }
        }
    }

    override fun getItemCount(): Int = wishes.size

    fun updateWishes(newWishes: List<WishItem>) {
        wishes.clear()
        wishes.addAll(newWishes)
        notifyDataSetChanged()
    }
}
