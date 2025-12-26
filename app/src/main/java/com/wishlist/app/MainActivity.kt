package com.wishlist.app

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.Manifest
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.textfield.TextInputEditText
import com.wishlist.app.data.CompletionFilter
import com.wishlist.app.data.SortOption
import com.wishlist.app.data.WishCategory
import com.wishlist.app.data.WishItem
import com.wishlist.app.data.WishRepository
import com.wishlist.app.ui.WishAdapter
import com.wishlist.app.ui.WishListViewModel
import com.wishlist.app.utils.ThemeManager
import com.wishlist.app.utils.BackupManager
import com.wishlist.app.notification.NotificationHelper
import com.wishlist.app.notification.AlarmScheduler
import com.wishlist.app.worker.PriceCheckScheduler
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import android.util.Log

/**
 * 메인 액티비티 - 위시리스트 화면
 */
class MainActivity : AppCompatActivity() {

    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: View
    private lateinit var wishCountText: TextView
    private lateinit var sortIndicator: TextView
    private lateinit var chipGroupCompletionFilter: ChipGroup
    private lateinit var chipGroupCategoryFilter: ChipGroup
    private lateinit var budgetStatsContent: View
    private lateinit var buttonToggleStats: ImageButton
    private lateinit var textTotalBudget: TextView
    private lateinit var textCompletedBudget: TextView
    private lateinit var textIncompleteBudget: TextView
    private lateinit var categoryStatsContainer: LinearLayout
    private lateinit var fabAddWish: FloatingActionButton

    private lateinit var viewModel: WishListViewModel
    private lateinit var adapter: WishAdapter

    // Firebase
    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var firestoreRepository: com.wishlist.app.data.FirestoreRepository

    // 이미지 선택 관련
    private var selectedImageUri: Uri? = null
    private lateinit var currentImagePreview: ImageView

    // 쇼핑 URL 클립보드 감지
    private var waitingForShoppingUrl = false
    private var lastClipboardText: String? = null
    private var currentUrlEditText: TextInputEditText? = null

    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            // Persistent URI 권한 요청 (앱 재시작 후에도 접근 가능)
            try {
                contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: SecurityException) {
                // 일부 URI는 persistent 권한을 지원하지 않음
                // 이 경우에도 현재 세션에서는 작동
            }
            selectedImageUri = it
            currentImagePreview.setImageURI(it)
        }
    }

    // 알림 권한 요청 런처 (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (!isGranted) {
            Snackbar.make(
                recyclerView,
                "알림 권한이 거부되었습니다. 설정에서 권한을 허용해주세요.",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    // 백업 파일 저장 런처
    private val backupFileLauncher = registerForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri?.let { performBackup(it) }
    }

    // 복원 파일 선택 런처
    private val restoreFileLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { performRestore(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // 테마 적용
        ThemeManager.applyTheme(this)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 알림 채널 생성
        NotificationHelper.createNotificationChannel(this)

        // 알림 권한 요청 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        // Firebase 초기화
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        firestoreRepository = com.wishlist.app.data.FirestoreRepository()

        // ViewModel 초기화
        val repository = WishRepository(applicationContext)
        viewModel = WishListViewModel(repository)

        // View 초기화
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)
        wishCountText = findViewById(R.id.wishCountText)
        sortIndicator = findViewById(R.id.sortIndicator)
        chipGroupCompletionFilter = findViewById(R.id.chipGroupCompletionFilter)
        chipGroupCategoryFilter = findViewById(R.id.chipGroupCategoryFilter)
        budgetStatsContent = findViewById(R.id.statsContent)
        buttonToggleStats = findViewById(R.id.buttonToggleStats)
        textTotalBudget = findViewById(R.id.textTotalBudget)
        textCompletedBudget = findViewById(R.id.textCompletedBudget)
        textIncompleteBudget = findViewById(R.id.textIncompleteBudget)
        categoryStatsContainer = findViewById(R.id.categoryStatsContainer)
        fabAddWish = findViewById(R.id.fabAddWish)

        // Toolbar 설정
        setSupportActionBar(toolbar)

        // 예산 통계 토글 버튼
        buttonToggleStats.setOnClickListener {
            toggleBudgetStats()
        }

        // Adapter 설정
        adapter = WishAdapter(
            wishes = emptyList(),
            onItemClick = { wish -> showEditWishDialog(wish) },
            onDeleteClick = { wish -> showDeleteConfirmDialog(wish) },
            onCheckboxClick = { wish ->
                // 완료로 변경되는 경우 알람 취소
                if (!wish.isCompleted) {
                    AlarmScheduler.cancelReminder(this, wish.id)
                }
                viewModel.toggleWishCompleted(wish)
            },
            onShareClick = { wish -> shareWish(wish) }
        )
        recyclerView.adapter = adapter

        // 스와이프 제스처 설정
        setupSwipeGestures()

        // 필터 설정
        setupFilters()

        // FAB 클릭 리스너
        fabAddWish.setOnClickListener {
            showAddWishDialog()
        }

        // ViewModel 관찰
        viewModel.wishList.observe(this) { wishes ->
            adapter.updateWishes(wishes)
            updateUI(wishes.size)
        }

        viewModel.budgetStats.observe(this) { stats ->
            updateBudgetStats(stats)
        }

        // 초기 정렬 표시
        updateSortIndicator(viewModel.getCurrentSortOption())

        // 테마 변경 메시지 표시
        ThemeManager.getAndClearThemeChangeMessage(this)?.let { themeName ->
            recyclerView.post {
                Snackbar.make(recyclerView, "테마 변경: $themeName", Snackbar.LENGTH_SHORT).show()
            }
        }
    }

    override fun onResume() {
        super.onResume()

        // 쇼핑 URL 대기 중이고 URL 입력 필드가 활성화되어 있으면 클립보드 확인
        if (waitingForShoppingUrl && currentUrlEditText != null) {
            checkClipboardForUrl()
        }
    }

    /**
     * 클립보드에서 URL 확인 및 자동 붙여넣기 제안
     */
    private fun checkClipboardForUrl() {
        try {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = clipboard.primaryClip

            if (clipData != null && clipData.itemCount > 0) {
                val clipText = clipData.getItemAt(0).text?.toString()

                // URL 형식인지 확인하고, 이전과 다른 URL인지 확인
                if (clipText != null &&
                    clipText != lastClipboardText &&
                    (clipText.startsWith("http://") || clipText.startsWith("https://"))) {

                    lastClipboardText = clipText

                    // 자동으로 URL 붙여넣기
                    currentUrlEditText?.let { editText ->
                        editText.setText(clipText)

                        // 사용자에게 알림
                        Snackbar.make(
                            recyclerView,
                            "쇼핑 링크가 자동으로 입력되었습니다",
                            Snackbar.LENGTH_SHORT
                        ).show()

                        // 대기 상태 해제
                        waitingForShoppingUrl = false
                    }
                }
            }
        } catch (e: Exception) {
            // 클립보드 접근 실패 시 무시
        }
    }

    private fun updateUI(wishCount: Int) {
        if (wishCount == 0) {
            recyclerView.visibility = View.GONE
            emptyStateLayout.visibility = View.VISIBLE
            wishCountText.text = getString(R.string.no_wishes)
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyStateLayout.visibility = View.GONE
            wishCountText.text = getString(R.string.wish_count, wishCount)
        }
    }

    private fun showAddWishDialog() {
        selectedImageUri = null
        var selectedTargetDate: Long? = null

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_wish, null)
        val editTextTitle = dialogView.findViewById<TextInputEditText>(R.id.editTextTitle)
        val editTextDescription = dialogView.findViewById<TextInputEditText>(R.id.editTextDescription)
        val editTextPrice = dialogView.findViewById<TextInputEditText>(R.id.editTextPrice)
        val editTextShoppingUrl = dialogView.findViewById<TextInputEditText>(R.id.editTextShoppingUrl)
        val checkboxPriceTracking = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.checkboxPriceTracking)
        val imagePreview = dialogView.findViewById<ImageView>(R.id.imagePreview)
        val buttonSelectImage = dialogView.findViewById<Button>(R.id.buttonSelectImage)
        val buttonSelectDate = dialogView.findViewById<Button>(R.id.buttonSelectDate)
        val buttonOpenShopping = dialogView.findViewById<Button>(R.id.buttonOpenShopping)
        val textSelectedDate = dialogView.findViewById<TextView>(R.id.textSelectedDate)
        val chipGroupPriority = dialogView.findViewById<ChipGroup>(R.id.chipGroupPriority)
        val chipGroupCategory = dialogView.findViewById<ChipGroup>(R.id.chipGroupCategory)
        val buttonSave = dialogView.findViewById<Button>(R.id.buttonSave)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)

        currentImagePreview = imagePreview
        currentUrlEditText = editTextShoppingUrl
        dialogTitle.text = getString(R.string.add_wish)

        // 이미지 선택 버튼
        buttonSelectImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // 날짜 선택 버튼
        buttonSelectDate.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth, 9, 0, 0)  // 오전 9시로 설정
                    selectedTargetDate = calendar.timeInMillis
                    val dateFormat = java.text.SimpleDateFormat("yyyy년 MM월 dd일", java.util.Locale.getDefault())
                    textSelectedDate.text = dateFormat.format(calendar.time)
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        // 쇼핑몰에서 찾기 버튼
        buttonOpenShopping.setOnClickListener {
            waitingForShoppingUrl = true
            showShoppingSiteDialog()
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        buttonSave.setOnClickListener {
            val title = editTextTitle.text.toString().trim()
            val description = editTextDescription.text.toString().trim()
            val priceText = editTextPrice.text.toString().trim()
            val price = priceText.toDoubleOrNull()
            val shoppingUrl = editTextShoppingUrl.text.toString().trim().takeIf { it.isNotEmpty() }
            val priceTrackingEnabled = checkboxPriceTracking.isChecked && !shoppingUrl.isNullOrEmpty()

            // 선택된 우선순위 가져오기
            val selectedPriority = when (chipGroupPriority.checkedChipId) {
                R.id.chipPriorityHigh -> 3
                R.id.chipPriorityMedium -> 2
                R.id.chipPriorityLow -> 1
                else -> 2
            }

            // 선택된 카테고리 가져오기
            val selectedCategory = when (chipGroupCategory.checkedChipId) {
                R.id.chipElectronics -> WishCategory.ELECTRONICS
                R.id.chipFashion -> WishCategory.FASHION
                R.id.chipBooks -> WishCategory.BOOKS
                R.id.chipTravel -> WishCategory.TRAVEL
                R.id.chipFood -> WishCategory.FOOD
                R.id.chipHobby -> WishCategory.HOBBY
                R.id.chipHome -> WishCategory.HOME
                else -> WishCategory.OTHER
            }

            if (title.isNotEmpty()) {
                val newWish = viewModel.addWish(
                    title = title,
                    description = description,
                    imageUri = selectedImageUri?.toString(),
                    price = price,
                    category = selectedCategory,
                    priority = selectedPriority,
                    targetDate = selectedTargetDate,
                    shoppingUrl = shoppingUrl,
                    priceTrackingEnabled = priceTrackingEnabled
                )

                // 목표일이 설정된 경우 알람 스케줄링
                if (selectedTargetDate != null) {
                    AlarmScheduler.scheduleReminder(this, newWish)
                }

                // 가격 추적이 활성화된 경우 백그라운드 작업 시작
                if (priceTrackingEnabled) {
                    PriceCheckScheduler.schedulePriceCheck(this)
                }

                // Firestore에 동기화 (로그인한 경우에만)
                if (auth.currentUser != null) {
                    lifecycleScope.launch {
                        firestoreRepository.saveWishItem(newWish)
                    }
                }

                dialog.dismiss()
            } else {
                editTextTitle.error = getString(R.string.error_title_required)
            }
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        // EditText focusable 설정
        editTextTitle.isFocusableInTouchMode = true
        editTextTitle.isFocusable = true

        // Dialog window 플래그 제거 및 설정
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        dialog.show()

        // 포커스 및 키보드 강제 표시
        editTextTitle.requestFocus()
        editTextTitle.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editTextTitle, InputMethodManager.SHOW_FORCED)
        }, 100)
    }

    private fun showEditWishDialog(wish: WishItem) {
        selectedImageUri = wish.imageUri?.let { Uri.parse(it) }
        var selectedTargetDate: Long? = wish.targetDate

        val dialogView = layoutInflater.inflate(R.layout.dialog_add_wish, null)
        val editTextTitle = dialogView.findViewById<TextInputEditText>(R.id.editTextTitle)
        val editTextDescription = dialogView.findViewById<TextInputEditText>(R.id.editTextDescription)
        val editTextPrice = dialogView.findViewById<TextInputEditText>(R.id.editTextPrice)
        val editTextShoppingUrl = dialogView.findViewById<TextInputEditText>(R.id.editTextShoppingUrl)
        val checkboxPriceTracking = dialogView.findViewById<com.google.android.material.checkbox.MaterialCheckBox>(R.id.checkboxPriceTracking)
        val imagePreview = dialogView.findViewById<ImageView>(R.id.imagePreview)
        val buttonSelectImage = dialogView.findViewById<Button>(R.id.buttonSelectImage)
        val buttonSelectDate = dialogView.findViewById<Button>(R.id.buttonSelectDate)
        val buttonOpenShopping = dialogView.findViewById<Button>(R.id.buttonOpenShopping)
        val textSelectedDate = dialogView.findViewById<TextView>(R.id.textSelectedDate)
        val chipGroupPriority = dialogView.findViewById<ChipGroup>(R.id.chipGroupPriority)
        val chipGroupCategory = dialogView.findViewById<ChipGroup>(R.id.chipGroupCategory)
        val buttonSave = dialogView.findViewById<Button>(R.id.buttonSave)
        val buttonCancel = dialogView.findViewById<Button>(R.id.buttonCancel)
        val dialogTitle = dialogView.findViewById<TextView>(R.id.dialogTitle)

        currentImagePreview = imagePreview
        currentUrlEditText = editTextShoppingUrl

        // 기존 데이터 설정
        dialogTitle.text = getString(R.string.edit_wish)
        editTextTitle.setText(wish.title)
        editTextDescription.setText(wish.description)
        // 가격을 정수로 포맷팅 (소수점 제거)
        editTextPrice.setText(wish.price?.toInt()?.toString() ?: "")
        // 쇼핑 URL 설정
        editTextShoppingUrl.setText(wish.shoppingUrl ?: "")
        // 가격 추적 설정
        checkboxPriceTracking.isChecked = wish.priceTrackingEnabled

        // 이미지 설정
        if (wish.imageUri != null) {
            try {
                imagePreview.setImageURI(Uri.parse(wish.imageUri))
            } catch (e: Exception) {
                // URI가 유효하지 않으면 기본 이미지 유지
                imagePreview.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        }

        // 목표일 설정
        wish.targetDate?.let { targetDate ->
            val dateFormat = java.text.SimpleDateFormat("yyyy년 MM월 dd일", java.util.Locale.getDefault())
            textSelectedDate.text = dateFormat.format(java.util.Date(targetDate))
        }

        // 날짜 선택 버튼
        buttonSelectDate.setOnClickListener {
            val calendar = java.util.Calendar.getInstance()
            val datePickerDialog = DatePickerDialog(
                this,
                { _, year, month, dayOfMonth ->
                    calendar.set(year, month, dayOfMonth, 9, 0, 0)  // 오전 9시로 설정
                    selectedTargetDate = calendar.timeInMillis
                    val dateFormat = java.text.SimpleDateFormat("yyyy년 MM월 dd일", java.util.Locale.getDefault())
                    textSelectedDate.text = dateFormat.format(calendar.time)
                },
                calendar.get(java.util.Calendar.YEAR),
                calendar.get(java.util.Calendar.MONTH),
                calendar.get(java.util.Calendar.DAY_OF_MONTH)
            )
            datePickerDialog.datePicker.minDate = System.currentTimeMillis()
            datePickerDialog.show()
        }

        // 우선순위 선택
        val priorityChipId = when (wish.priority) {
            3 -> R.id.chipPriorityHigh
            2 -> R.id.chipPriorityMedium
            1 -> R.id.chipPriorityLow
            else -> R.id.chipPriorityMedium
        }
        chipGroupPriority.check(priorityChipId)

        // 카테고리 선택
        val categoryChipId = when (wish.category) {
            WishCategory.ELECTRONICS -> R.id.chipElectronics
            WishCategory.FASHION -> R.id.chipFashion
            WishCategory.BOOKS -> R.id.chipBooks
            WishCategory.TRAVEL -> R.id.chipTravel
            WishCategory.FOOD -> R.id.chipFood
            WishCategory.HOBBY -> R.id.chipHobby
            WishCategory.HOME -> R.id.chipHome
            WishCategory.OTHER -> R.id.chipOther
        }
        chipGroupCategory.check(categoryChipId)

        // 이미지 선택 버튼
        buttonSelectImage.setOnClickListener {
            imagePickerLauncher.launch("image/*")
        }

        // 쇼핑몰에서 찾기 버튼
        buttonOpenShopping.setOnClickListener {
            waitingForShoppingUrl = true
            showShoppingSiteDialog()
        }

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        buttonSave.setOnClickListener {
            val title = editTextTitle.text.toString().trim()
            val description = editTextDescription.text.toString().trim()
            val priceText = editTextPrice.text.toString().trim()
            val price = priceText.toDoubleOrNull()
            val shoppingUrl = editTextShoppingUrl.text.toString().trim().takeIf { it.isNotEmpty() }
            val priceTrackingEnabled = checkboxPriceTracking.isChecked && !shoppingUrl.isNullOrEmpty()

            // 선택된 우선순위 가져오기
            val selectedPriority = when (chipGroupPriority.checkedChipId) {
                R.id.chipPriorityHigh -> 3
                R.id.chipPriorityMedium -> 2
                R.id.chipPriorityLow -> 1
                else -> 2
            }

            // 선택된 카테고리 가져오기
            val selectedCategory = when (chipGroupCategory.checkedChipId) {
                R.id.chipElectronics -> WishCategory.ELECTRONICS
                R.id.chipFashion -> WishCategory.FASHION
                R.id.chipBooks -> WishCategory.BOOKS
                R.id.chipTravel -> WishCategory.TRAVEL
                R.id.chipFood -> WishCategory.FOOD
                R.id.chipHobby -> WishCategory.HOBBY
                R.id.chipHome -> WishCategory.HOME
                else -> WishCategory.OTHER
            }

            if (title.isNotEmpty()) {
                val updatedWish = wish.copy(
                    title = title,
                    description = description,
                    imageUri = selectedImageUri?.toString(),
                    price = price,
                    category = selectedCategory,
                    priority = selectedPriority,
                    targetDate = selectedTargetDate,
                    shoppingUrl = shoppingUrl,
                    priceTrackingEnabled = priceTrackingEnabled
                )
                viewModel.updateWish(updatedWish)

                // 기존 알람 취소
                AlarmScheduler.cancelReminder(this, wish.id)

                // 목표일이 설정된 경우 새로운 알람 스케줄링
                if (selectedTargetDate != null) {
                    AlarmScheduler.scheduleReminder(this, updatedWish)
                }

                // 가격 추적이 활성화된 경우 백그라운드 작업 시작
                if (priceTrackingEnabled) {
                    PriceCheckScheduler.schedulePriceCheck(this)
                }

                // Firestore에 동기화 (로그인한 경우에만)
                if (auth.currentUser != null) {
                    lifecycleScope.launch {
                        firestoreRepository.saveWishItem(updatedWish)
                    }
                }

                dialog.dismiss()
            } else {
                editTextTitle.error = getString(R.string.error_title_required)
            }
        }

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        // EditText focusable 설정
        editTextTitle.isFocusableInTouchMode = true
        editTextTitle.isFocusable = true

        // Dialog window 플래그 제거 및 설정
        dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM)
        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

        dialog.show()

        // 포커스 및 키보드 강제 표시
        editTextTitle.requestFocus()
        editTextTitle.postDelayed({
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
            imm.showSoftInput(editTextTitle, InputMethodManager.SHOW_FORCED)
        }, 100)
    }

    /**
     * 쇼핑몰 선택 다이얼로그
     */
    private fun showShoppingSiteDialog() {
        val shoppingSites = arrayOf(
            "쿠팡" to "https://www.coupang.com",
            "네이버쇼핑" to "https://shopping.naver.com",
            "11번가" to "https://www.11st.co.kr",
            "G마켓" to "https://www.gmarket.co.kr",
            "옥션" to "https://www.auction.co.kr",
            "SSG" to "https://www.ssg.com",
            "위메프" to "https://www.wemakeprice.com",
            "티몬" to "https://www.tmon.co.kr"
        )

        val siteNames = shoppingSites.map { it.first }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("쇼핑몰 선택")
            .setItems(siteNames) { dialog, which ->
                val selectedSite = shoppingSites[which]
                openShoppingSite(selectedSite.first, selectedSite.second)
                dialog.dismiss()
            }
            .setNegativeButton("취소") { dialog, _ ->
                waitingForShoppingUrl = false
                dialog.dismiss()
            }
            .show()
    }

    /**
     * 쇼핑몰 사이트/앱 열기
     */
    private fun openShoppingSite(siteName: String, url: String) {
        try {
            // 앱 패키지명 매핑 (앱이 설치되어 있으면 앱으로 열림)
            val packageName = when (siteName) {
                "쿠팡" -> "com.coupang.mobile"
                "네이버쇼핑" -> "com.nhn.android.search"
                "11번가" -> "com.elevenst"
                "G마켓" -> "com.gmarket.mobile"
                "옥션" -> "com.auction.mobile"
                "SSG" -> "kr.co.shinsegae.ssg"
                "위메프" -> "com.wemakeprice"
                "티몬" -> "com.tmon"
                else -> null
            }

            // 앱이 설치되어 있으면 앱으로, 아니면 브라우저로 열기
            val intent = if (packageName != null && isAppInstalled(packageName)) {
                packageManager.getLaunchIntentForPackage(packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            } else {
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            }

            intent?.let {
                startActivity(it)
                Snackbar.make(
                    recyclerView,
                    "$siteName 에서 상품을 찾은 후 URL을 복사하세요",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        } catch (e: Exception) {
            // 앱 실행 실패 시 브라우저로 시도
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                startActivity(browserIntent)
            } catch (e2: Exception) {
                Snackbar.make(recyclerView, "쇼핑몰을 열 수 없습니다", Snackbar.LENGTH_SHORT).show()
                waitingForShoppingUrl = false
            }
        }
    }

    /**
     * 앱이 설치되어 있는지 확인
     */
    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun showDeleteConfirmDialog(wish: WishItem) {
        AlertDialog.Builder(this)
            .setTitle(getString(R.string.delete_wish_title))
            .setMessage(getString(R.string.delete_wish_message, wish.title))
            .setPositiveButton(getString(R.string.delete)) { _, _ ->
                // 알람 취소
                AlarmScheduler.cancelReminder(this, wish.id)
                viewModel.deleteWish(wish.id)

                // Firestore에서도 삭제 (로그인한 경우에만)
                if (auth.currentUser != null) {
                    lifecycleScope.launch {
                        firestoreRepository.deleteWishItem(wish.id)
                    }
                }
            }
            .setNegativeButton(getString(R.string.cancel), null)
            .show()
    }

    /**
     * 위시 공유하기
     */
    private fun shareWish(wish: WishItem) {
        // 공유할 텍스트 포맷
        val shareText = buildString {
            append("📝 ${wish.title}\n\n")

            if (wish.description.isNotEmpty()) {
                append("${wish.description}\n\n")
            }

            append("📂 카테고리: ${wish.category.displayName}\n")

            // 우선순위
            val priorityText = when (wish.priority) {
                3 -> "⭐⭐⭐ 높음"
                2 -> "⭐⭐ 보통"
                1 -> "⭐ 낮음"
                else -> "⭐⭐ 보통"
            }
            append("🎯 우선순위: $priorityText\n")

            // 가격
            if (wish.price != null) {
                val formatter = java.text.DecimalFormat("#,###")
                append("💰 가격: ₩${formatter.format(wish.price)}\n")
            } else {
                append("💰 가격: 미정\n")
            }

            // 목표일
            wish.targetDate?.let { targetDate ->
                val dateFormat = java.text.SimpleDateFormat("yyyy년 MM월 dd일", java.util.Locale.getDefault())
                append("📅 목표일: ${dateFormat.format(java.util.Date(targetDate))}\n")
            }

            // 쇼핑 링크
            if (!wish.shoppingUrl.isNullOrEmpty()) {
                append("🔗 쇼핑 링크: ${wish.shoppingUrl}\n")
            }

            // 최저가 정보
            if (wish.lowestPrice != null && wish.lowestPrice != wish.price) {
                val formatter = java.text.DecimalFormat("#,###")
                append("💎 역대 최저가: ₩${formatter.format(wish.lowestPrice)}\n")
            }

            append("\n✨ 위시리스트 앱으로 공유됨")
        }

        // 이미지가 있으면 이미지와 함께 공유, 없으면 텍스트만 공유
        val shareIntent = if (!wish.imageUri.isNullOrEmpty()) {
            try {
                val imageUri = Uri.parse(wish.imageUri)
                Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, imageUri)
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, "위시: ${wish.title}")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
            } catch (e: Exception) {
                // 이미지 URI 파싱 실패 시 텍스트만 공유
                Intent().apply {
                    action = Intent.ACTION_SEND
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, shareText)
                    putExtra(Intent.EXTRA_SUBJECT, "위시: ${wish.title}")
                }
            }
        } else {
            Intent().apply {
                action = Intent.ACTION_SEND
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, "위시: ${wish.title}")
            }
        }

        // Intent chooser로 앱 선택 화면 표시
        startActivity(Intent.createChooser(shareIntent, "위시 공유하기"))
    }

    /**
     * 전체 위시리스트 공유
     */
    private fun shareAllWishes() {
        val wishes = viewModel.wishList.value ?: emptyList()

        if (wishes.isEmpty()) {
            Snackbar.make(recyclerView, "공유할 위시가 없습니다", Snackbar.LENGTH_SHORT).show()
            return
        }

        // 공유할 텍스트 포맷
        val shareText = buildString {
            append("📋 내 위시리스트 (총 ${wishes.size}개)\n")
            append("=" .repeat(30) + "\n\n")

            wishes.forEachIndexed { index, wish ->
                append("${index + 1}. ")

                // 완료 표시
                if (wish.isCompleted) {
                    append("✅ ")
                }

                append("${wish.title}\n")

                // 가격
                if (wish.price != null) {
                    val formatter = java.text.DecimalFormat("#,###")
                    append("   💰 ₩${formatter.format(wish.price)}")

                    // 최저가 표시
                    if (wish.lowestPrice != null && wish.lowestPrice != wish.price) {
                        append(" (최저가: ₩${formatter.format(wish.lowestPrice)})")
                    }
                    append("\n")
                }

                // 카테고리
                append("   📂 ${wish.category.displayName}")

                // 우선순위
                val priorityText = when (wish.priority) {
                    3 -> " ⭐⭐⭐"
                    2 -> " ⭐⭐"
                    1 -> " ⭐"
                    else -> ""
                }
                append(priorityText)
                append("\n")

                // 쇼핑 링크
                if (!wish.shoppingUrl.isNullOrEmpty()) {
                    append("   🔗 ${wish.shoppingUrl}\n")
                }

                append("\n")
            }

            // 통계
            val totalPrice = wishes.sumOf { it.price ?: 0.0 }
            val completedCount = wishes.count { it.isCompleted }
            val incompleteCount = wishes.size - completedCount

            append("=" .repeat(30) + "\n")
            append("📊 통계\n")
            append("   총 가격: ₩${java.text.DecimalFormat("#,###").format(totalPrice)}\n")
            append("   완료: ${completedCount}개 | 미완료: ${incompleteCount}개\n")

            append("\n✨ 위시리스트 앱으로 공유됨")
        }

        // 공유 Intent 생성
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
            putExtra(Intent.EXTRA_SUBJECT, "내 위시리스트")
        }

        startActivity(Intent.createChooser(shareIntent, "위시리스트 공유하기"))
    }

    /**
     * 백업 시작
     */
    private fun startBackup() {
        val wishes = viewModel.wishList.value ?: emptyList()

        if (wishes.isEmpty()) {
            Snackbar.make(recyclerView, "백업할 위시가 없습니다", Snackbar.LENGTH_SHORT).show()
            return
        }

        // 파일 이름 생성 및 파일 저장 다이얼로그 표시
        val fileName = BackupManager.generateBackupFileName()
        backupFileLauncher.launch(fileName)
    }

    /**
     * 백업 수행
     */
    private fun performBackup(uri: Uri) {
        try {
            val wishes = viewModel.wishList.value ?: emptyList()
            contentResolver.openOutputStream(uri)?.use { outputStream ->
                BackupManager.writeBackupToStream(wishes, outputStream)
            }

            Snackbar.make(
                recyclerView,
                "백업 완료: ${wishes.size}개 아이템",
                Snackbar.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Snackbar.make(
                recyclerView,
                "백업 실패: ${e.message}",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    /**
     * 복원 시작
     */
    private fun startRestore() {
        // 파일 선택 다이얼로그 표시
        restoreFileLauncher.launch(arrayOf("application/json"))
    }

    /**
     * 복원 수행
     */
    private fun performRestore(uri: Uri) {
        try {
            // 1단계: 백업 파일 정보 먼저 읽기
            val jsonString = contentResolver.openInputStream(uri)?.use { inputStream ->
                inputStream.readBytes().toString(Charsets.UTF_8)
            } ?: ""

            if (jsonString.isEmpty()) {
                Snackbar.make(recyclerView, "백업 파일을 읽을 수 없습니다", Snackbar.LENGTH_SHORT).show()
                return
            }

            // 2단계: 백업 정보 가져오기
            val backupInfo = BackupManager.getBackupInfo(jsonString)
            if (backupInfo == null) {
                Snackbar.make(recyclerView, "올바른 백업 파일이 아닙니다", Snackbar.LENGTH_SHORT).show()
                return
            }

            // 3단계: 백업 정보를 보여주고 복원 확인
            val dateFormat = java.text.SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", java.util.Locale.getDefault())
            val backupDateStr = dateFormat.format(java.util.Date(backupInfo.backupDate))

            AlertDialog.Builder(this)
                .setTitle("백업 파일 정보")
                .setMessage(
                    "백업 날짜: $backupDateStr\n" +
                    "위시 개수: ${backupInfo.wishCount}개\n" +
                    "버전: ${backupInfo.version}\n\n" +
                    "이 백업 파일을 복원하시겠습니까?"
                )
                .setPositiveButton("복원") { _, _ ->
                    // 4단계: 실제 복원 수행
                    executeRestore(jsonString)
                }
                .setNegativeButton("취소", null)
                .show()

        } catch (e: Exception) {
            Snackbar.make(
                recyclerView,
                "백업 파일 읽기 실패: ${e.message}",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    /**
     * 실제 복원 실행
     */
    private fun executeRestore(jsonString: String) {
        try {
            val wishes = BackupManager.importFromJson(jsonString)

            if (wishes.isEmpty()) {
                Snackbar.make(recyclerView, "복원할 데이터가 없습니다", Snackbar.LENGTH_SHORT).show()
                return
            }

            // 복원된 위시 아이템 추가
            var addedCount = 0
            wishes.forEach { wish ->
                try {
                    viewModel.addWish(
                        title = wish.title,
                        description = wish.description,
                        imageUri = wish.imageUri,
                        price = wish.price,
                        category = wish.category,
                        priority = wish.priority,
                        targetDate = wish.targetDate
                    )

                    // 목표일이 설정된 경우 알람 스케줄링
                    if (wish.targetDate != null) {
                        AlarmScheduler.scheduleReminder(this, wish)
                    }

                    addedCount++
                } catch (e: Exception) {
                    // 개별 아이템 추가 실패는 무시하고 계속 진행
                }
            }

            Snackbar.make(
                recyclerView,
                "복원 완료: ${addedCount}개 아이템 추가됨",
                Snackbar.LENGTH_LONG
            ).show()
        } catch (e: Exception) {
            Snackbar.make(
                recyclerView,
                "복원 실패: ${e.message}",
                Snackbar.LENGTH_LONG
            ).show()
        }
    }

    /**
     * 전체 위시리스트 공유하기
     */
    private fun setupSwipeGestures() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, // 드래그 비활성화
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT // 좌우 스와이프 활성화
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false // 드래그 비활성화

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.adapterPosition
                val wish = adapter.wishes[position]

                when (direction) {
                    ItemTouchHelper.LEFT -> {
                        // 왼쪽 스와이프: 삭제
                        // 알람 취소
                        AlarmScheduler.cancelReminder(this@MainActivity, wish.id)
                        viewModel.deleteWish(wish.id)

                        // Firestore에서도 삭제 (로그인한 경우에만)
                        if (auth.currentUser != null) {
                            lifecycleScope.launch {
                                firestoreRepository.deleteWishItem(wish.id)
                            }
                        }

                        Snackbar.make(recyclerView, "삭제되었습니다", Snackbar.LENGTH_SHORT)
                            .setAction("취소") {
                                val restoredWish = viewModel.addWish(
                                    wish.title,
                                    wish.description,
                                    wish.imageUri,
                                    wish.price,
                                    wish.category,
                                    wish.priority,
                                    wish.targetDate
                                )
                                // 목표일이 설정되어 있었다면 알람 다시 스케줄링
                                if (wish.targetDate != null) {
                                    AlarmScheduler.scheduleReminder(this@MainActivity, restoredWish)
                                }

                                // Firestore에 복원 (로그인한 경우에만)
                                if (auth.currentUser != null) {
                                    lifecycleScope.launch {
                                        firestoreRepository.saveWishItem(restoredWish)
                                    }
                                }
                            }
                            .show()
                    }
                    ItemTouchHelper.RIGHT -> {
                        // 오른쪽 스와이프: 완료 토글
                        // 완료로 변경되는 경우 알람 취소
                        if (!wish.isCompleted) {
                            AlarmScheduler.cancelReminder(this@MainActivity, wish.id)
                        }
                        viewModel.toggleWishCompleted(wish)

                        // Firestore에 동기화 (로그인한 경우에만)
                        if (auth.currentUser != null) {
                            lifecycleScope.launch {
                                val updatedWish = wish.copy(isCompleted = !wish.isCompleted)
                                firestoreRepository.saveWishItem(updatedWish)
                            }
                        }

                        val message = if (wish.isCompleted) "완료 취소" else "완료!"
                        Snackbar.make(recyclerView, message, Snackbar.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                val itemView = viewHolder.itemView
                val paint = Paint()

                if (dX > 0) {
                    // 오른쪽 스와이프 (완료)
                    val background = ColorDrawable(Color.parseColor("#4CAF50"))
                    background.setBounds(
                        itemView.left,
                        itemView.top,
                        itemView.left + dX.toInt(),
                        itemView.bottom
                    )
                    background.draw(c)
                } else if (dX < 0) {
                    // 왼쪽 스와이프 (삭제)
                    val background = ColorDrawable(Color.parseColor("#F44336"))
                    background.setBounds(
                        itemView.right + dX.toInt(),
                        itemView.top,
                        itemView.right,
                        itemView.bottom
                    )
                    background.draw(c)
                }

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        })

        itemTouchHelper.attachToRecyclerView(recyclerView)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_sort, menu)

        // SearchView 설정
        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem.actionView as SearchView

        searchView.queryHint = "제목 또는 설명 검색"
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.setSearchQuery(newText ?: "")
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_share_all -> {
                shareAllWishes()
                true
            }
            R.id.action_backup -> {
                startBackup()
                true
            }
            R.id.action_restore -> {
                startRestore()
                true
            }
            R.id.action_theme -> {
                val newTheme = ThemeManager.toggleTheme(this)
                val themeName = ThemeManager.getThemeName(newTheme)
                ThemeManager.setThemeChangeMessage(this, themeName)
                true
            }
            R.id.sort_date_newest, R.id.sort_date_oldest, R.id.sort_price_high,
            R.id.sort_price_low, R.id.sort_priority_high, R.id.sort_category,
            R.id.sort_title -> {
                val sortOption = when (item.itemId) {
                    R.id.sort_date_newest -> SortOption.DATE_NEWEST
                    R.id.sort_date_oldest -> SortOption.DATE_OLDEST
                    R.id.sort_price_high -> SortOption.PRICE_HIGH
                    R.id.sort_price_low -> SortOption.PRICE_LOW
                    R.id.sort_priority_high -> SortOption.PRIORITY_HIGH
                    R.id.sort_category -> SortOption.CATEGORY
                    R.id.sort_title -> SortOption.TITLE
                    else -> return super.onOptionsItemSelected(item)
                }
                viewModel.setSortOption(sortOption)
                updateSortIndicator(sortOption)
                true
            }
            R.id.action_profile -> {
                // 프로필 화면으로 이동
                val intent = Intent(this, ProfileActivity::class.java)
                startActivity(intent)
                true
            }
            R.id.action_logout -> {
                performLogout()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun updateSortIndicator(sortOption: SortOption) {
        sortIndicator.text = "정렬: ${sortOption.displayName}"
    }

    private fun setupFilters() {
        // 완료 상태 필터 리스너
        chipGroupCompletionFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val completionFilter = when (checkedIds.firstOrNull()) {
                R.id.chipFilterAll -> CompletionFilter.ALL
                R.id.chipFilterCompleted -> CompletionFilter.COMPLETED
                R.id.chipFilterIncomplete -> CompletionFilter.INCOMPLETE
                else -> CompletionFilter.ALL
            }
            viewModel.setCompletionFilter(completionFilter)
        }

        // 카테고리 필터 리스너
        chipGroupCategoryFilter.setOnCheckedStateChangeListener { _, checkedIds ->
            val category = when (checkedIds.firstOrNull()) {
                R.id.chipFilterAllCategory -> null
                R.id.chipFilterElectronics -> WishCategory.ELECTRONICS
                R.id.chipFilterFashion -> WishCategory.FASHION
                R.id.chipFilterBooks -> WishCategory.BOOKS
                R.id.chipFilterTravel -> WishCategory.TRAVEL
                R.id.chipFilterFood -> WishCategory.FOOD
                R.id.chipFilterHobby -> WishCategory.HOBBY
                R.id.chipFilterHome -> WishCategory.HOME
                R.id.chipFilterOther -> WishCategory.OTHER
                else -> null
            }
            viewModel.setCategoryFilter(category)
        }
    }

    private fun updateBudgetStats(stats: com.wishlist.app.data.BudgetStats) {
        // 금액 포맷팅
        val formatter = java.text.DecimalFormat("#,###")
        textTotalBudget.text = "₩ ${formatter.format(stats.totalBudget)}"
        textCompletedBudget.text = "₩ ${formatter.format(stats.completedBudget)}"
        textIncompleteBudget.text = "₩ ${formatter.format(stats.incompleteBudget)}"

        // 카테고리별 통계 표시
        categoryStatsContainer.removeAllViews()
        stats.categoryBudgets.forEach { (category, amount) ->
            val categoryView = layoutInflater.inflate(
                android.R.layout.simple_list_item_2,
                categoryStatsContainer,
                false
            )
            val text1 = categoryView.findViewById<TextView>(android.R.id.text1)
            val text2 = categoryView.findViewById<TextView>(android.R.id.text2)

            text1.text = category.displayName
            text1.textSize = 13f
            text2.text = "₩ ${formatter.format(amount)}"
            text2.textSize = 13f
            text2.setTextColor(ContextCompat.getColor(this, R.color.primary))

            categoryStatsContainer.addView(categoryView)
        }
    }

    private fun toggleBudgetStats() {
        if (budgetStatsContent.visibility == View.VISIBLE) {
            budgetStatsContent.visibility = View.GONE
            buttonToggleStats.setImageResource(android.R.drawable.arrow_down_float)
        } else {
            budgetStatsContent.visibility = View.VISIBLE
            buttonToggleStats.setImageResource(android.R.drawable.arrow_up_float)
        }
    }

    // ========== 프로필 관리 ==========
    private fun showProfileDialog() {
        val currentUser = auth.currentUser

        val message = if (currentUser != null) {
            """
            이메일: ${currentUser.email ?: "없음"}
            이름: ${currentUser.displayName ?: "없음"}
            UID: ${currentUser.uid}
            로그인 제공자: ${currentUser.providerData.firstOrNull()?.providerId ?: "없음"}
            """.trimIndent()
        } else {
            "로그인되어 있지 않습니다"
        }

        AlertDialog.Builder(this)
            .setTitle("프로필 정보")
            .setMessage(message)
            .setPositiveButton("확인") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun performLogout() {
        AlertDialog.Builder(this)
            .setTitle("로그아웃")
            .setMessage("정말 로그아웃 하시겠습니까?")
            .setPositiveButton("로그아웃") { _, _ ->
                // Firebase 로그아웃
                auth.signOut()

                // 카카오 로그아웃
                UserApiClient.instance.logout { error ->
                    if (error != null) {
                        Log.e("MainActivity", "카카오 로그아웃 실패", error)
                    } else {
                        Log.d("MainActivity", "카카오 로그아웃 성공")
                    }
                }

                // 네이버 로그아웃
                try {
                    NaverIdLoginSDK.logout()
                    Log.d("MainActivity", "네이버 로그아웃 성공")
                } catch (e: Exception) {
                    Log.e("MainActivity", "네이버 로그아웃 실패", e)
                }

                Toast.makeText(this, "로그아웃 되었습니다", Toast.LENGTH_SHORT).show()

                // 로그인 화면으로 이동
                val intent = Intent(this, LoginActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                startActivity(intent)
                finish()
            }
            .setNegativeButton("취소", null)
            .show()
    }
}
