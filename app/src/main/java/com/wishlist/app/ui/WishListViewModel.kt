package com.wishlist.app.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.wishlist.app.data.BudgetStats
import com.wishlist.app.data.CompletionFilter
import com.wishlist.app.data.FilterOptions
import com.wishlist.app.data.SortOption
import com.wishlist.app.data.WishCategory
import com.wishlist.app.data.WishItem
import com.wishlist.app.data.WishRepository

/**
 * 위시리스트 화면의 ViewModel
 * UI 상태를 관리하고 Repository와 통신
 */
class WishListViewModel(private val repository: WishRepository) : ViewModel() {

    private val _wishList = MutableLiveData<List<WishItem>>()
    val wishList: LiveData<List<WishItem>> = _wishList

    private val _budgetStats = MutableLiveData<BudgetStats>()
    val budgetStats: LiveData<BudgetStats> = _budgetStats

    private var currentSortOption = SortOption.DATE_NEWEST
    private var currentFilterOptions = FilterOptions()
    private var searchQuery: String = ""

    init {
        loadWishes()
    }
    
    /**
     * 위시리스트 불러오기
     */
    fun loadWishes() {
        val wishes = repository.getAllWishes()
        val filtered = filterWishes(wishes)
        _wishList.value = sortWishes(filtered)
        _budgetStats.value = calculateBudgetStats(wishes)
    }

    /**
     * 정렬 옵션 변경
     */
    fun setSortOption(sortOption: SortOption) {
        currentSortOption = sortOption
        loadWishes()
    }

    /**
     * 현재 정렬 옵션 가져오기
     */
    fun getCurrentSortOption(): SortOption = currentSortOption

    /**
     * 필터 옵션 변경
     */
    fun setFilterOptions(filterOptions: FilterOptions) {
        currentFilterOptions = filterOptions
        loadWishes()
    }

    /**
     * 카테고리 필터 변경
     */
    fun setCategoryFilter(category: WishCategory?) {
        currentFilterOptions = currentFilterOptions.copy(category = category)
        loadWishes()
    }

    /**
     * 완료 상태 필터 변경
     */
    fun setCompletionFilter(completionFilter: CompletionFilter) {
        currentFilterOptions = currentFilterOptions.copy(completionStatus = completionFilter)
        loadWishes()
    }

    /**
     * 현재 필터 옵션 가져오기
     */
    fun getCurrentFilterOptions(): FilterOptions = currentFilterOptions

    /**
     * 검색어 설정
     */
    fun setSearchQuery(query: String) {
        searchQuery = query
        loadWishes()
    }

    /**
     * 위시리스트 필터링
     */
    private fun filterWishes(wishes: List<WishItem>): List<WishItem> {
        var filtered = wishes

        // 검색어 필터
        if (searchQuery.isNotEmpty()) {
            filtered = filtered.filter { wish ->
                wish.title.contains(searchQuery, ignoreCase = true) ||
                wish.description.contains(searchQuery, ignoreCase = true)
            }
        }

        // 카테고리 필터
        currentFilterOptions.category?.let { category ->
            filtered = filtered.filter { it.category == category }
        }

        // 완료 상태 필터
        filtered = when (currentFilterOptions.completionStatus) {
            CompletionFilter.ALL -> filtered
            CompletionFilter.COMPLETED -> filtered.filter { it.isCompleted }
            CompletionFilter.INCOMPLETE -> filtered.filter { !it.isCompleted }
        }

        return filtered
    }

    /**
     * 위시리스트 정렬
     */
    private fun sortWishes(wishes: List<WishItem>): List<WishItem> {
        return when (currentSortOption) {
            SortOption.DATE_NEWEST -> wishes.sortedByDescending { it.createdAt }
            SortOption.DATE_OLDEST -> wishes.sortedBy { it.createdAt }
            SortOption.PRICE_HIGH -> wishes.sortedByDescending { it.price ?: 0.0 }
            SortOption.PRICE_LOW -> wishes.sortedBy { it.price ?: Double.MAX_VALUE }
            SortOption.PRIORITY_HIGH -> wishes.sortedByDescending { it.priority }
            SortOption.CATEGORY -> wishes.sortedBy { it.category.displayName }
            SortOption.TITLE -> wishes.sortedBy { it.title }
        }
    }

    /**
     * 예산 통계 계산
     */
    private fun calculateBudgetStats(wishes: List<WishItem>): BudgetStats {
        val totalBudget = wishes.sumOf { it.price ?: 0.0 }
        val completedBudget = wishes.filter { it.isCompleted }.sumOf { it.price ?: 0.0 }
        val incompleteBudget = wishes.filter { !it.isCompleted }.sumOf { it.price ?: 0.0 }

        val categoryBudgets = mutableMapOf<WishCategory, Double>()
        WishCategory.values().forEach { category ->
            val categoryTotal = wishes.filter { it.category == category }.sumOf { it.price ?: 0.0 }
            if (categoryTotal > 0) {
                categoryBudgets[category] = categoryTotal
            }
        }

        return BudgetStats(
            totalBudget = totalBudget,
            completedBudget = completedBudget,
            incompleteBudget = incompleteBudget,
            categoryBudgets = categoryBudgets
        )
    }
    
    /**
     * 위시 추가
     * @return 생성된 WishItem
     */
    fun addWish(
        title: String,
        description: String,
        imageUri: String? = null,
        price: Double? = null,
        category: WishCategory = WishCategory.OTHER,
        priority: Int = 2,
        targetDate: Long? = null,
        shoppingUrl: String? = null,
        priceTrackingEnabled: Boolean = false
    ): WishItem {
        val wish = WishItem(
            title = title,
            description = description,
            imageUri = imageUri,
            price = price,
            category = category,
            priority = priority,
            targetDate = targetDate,
            shoppingUrl = shoppingUrl,
            priceTrackingEnabled = priceTrackingEnabled,
            lowestPrice = price // 초기 최저가는 현재 가격으로 설정
        )
        repository.addWish(wish)
        loadWishes()
        return wish
    }
    
    /**
     * 위시 업데이트
     */
    fun updateWish(wish: WishItem) {
        repository.updateWish(wish)
        loadWishes()
    }
    
    /**
     * 위시 삭제
     */
    fun deleteWish(wishId: String) {
        repository.deleteWish(wishId)
        loadWishes()
    }
    
    /**
     * 위시 완료 토글
     */
    fun toggleWishCompleted(wish: WishItem) {
        val updatedWish = wish.copy(isCompleted = !wish.isCompleted)
        updateWish(updatedWish)
    }
}
