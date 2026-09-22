package com.example.boibinimoy.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.example.boibinimoy.databinding.ItemHeroSlideBinding

data class HeroSlideItem(
    val title: String,
    val subtitle: String,
    val buttonText: String,
    @param:DrawableRes val bgDrawableRes: Int,
    val onActionClick: () -> Unit
)

class HeroSlideAdapter(
    private val slides: List<HeroSlideItem>
) : RecyclerView.Adapter<HeroSlideAdapter.HeroSlideViewHolder>() {

    inner class HeroSlideViewHolder(val binding: ItemHeroSlideBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeroSlideViewHolder {
        val binding = ItemHeroSlideBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return HeroSlideViewHolder(binding)
    }

    override fun onBindViewHolder(holder: HeroSlideViewHolder, position: Int) {
        val item = slides[position]
        holder.binding.apply {
            tvSlideTitle.text = item.title
            tvSlideSubtitle.text = item.subtitle
            btnSlideAction.text = item.buttonText
            layoutSlideCard.setBackgroundResource(item.bgDrawableRes)
            btnSlideAction.setOnClickListener {
                item.onActionClick()
            }
        }
    }

    override fun getItemCount(): Int = slides.size
}
