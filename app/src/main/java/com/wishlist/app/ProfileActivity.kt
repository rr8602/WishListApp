package com.wishlist.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.wishlist.app.data.LoginProvider
import com.wishlist.app.data.WishRepository
import java.text.SimpleDateFormat
import java.util.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var repository: WishRepository

    private lateinit var buttonBack: ImageButton
    private lateinit var imageProfile: ImageView
    private lateinit var textDisplayName: TextView
    private lateinit var textEmail: TextView
    private lateinit var chipProvider: Chip
    private lateinit var textUserId: TextView
    private lateinit var textCreatedAt: TextView
    private lateinit var textTotalWishes: TextView
    private lateinit var textCompletedWishes: TextView
    private lateinit var textGiftedWishes: TextView
    private lateinit var buttonShareWishlist: Button
    private lateinit var buttonLogout: Button
    private lateinit var buttonDeleteAccount: Button

    companion object {
        private const val TAG = "ProfileActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        // Firebase 초기화
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        repository = WishRepository(applicationContext)

        // 뷰 초기화
        initViews()

        // 프로필 정보 로드
        loadProfileInfo()

        // 위시리스트 통계 로드
        loadWishlistStats()

        // 버튼 클릭 리스너
        setupClickListeners()
    }

    private fun initViews() {
        buttonBack = findViewById(R.id.buttonBack)
        imageProfile = findViewById(R.id.imageProfile)
        textDisplayName = findViewById(R.id.textDisplayName)
        textEmail = findViewById(R.id.textEmail)
        chipProvider = findViewById(R.id.chipProvider)
        textUserId = findViewById(R.id.textUserId)
        textCreatedAt = findViewById(R.id.textCreatedAt)
        textTotalWishes = findViewById(R.id.textTotalWishes)
        textCompletedWishes = findViewById(R.id.textCompletedWishes)
        textGiftedWishes = findViewById(R.id.textGiftedWishes)
        buttonShareWishlist = findViewById(R.id.buttonShareWishlist)
        buttonLogout = findViewById(R.id.buttonLogout)
        buttonDeleteAccount = findViewById(R.id.buttonDeleteAccount)
    }

    private fun loadProfileInfo() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Firebase 사용자 정보
            textDisplayName.text = currentUser.displayName ?: "사용자"
            textEmail.text = currentUser.email ?: "이메일 없음"
            textUserId.text = currentUser.uid.take(12) + "..."

            // 로그인 제공자 표시
            val providerText = when {
                currentUser.providerData.any { it.providerId.contains("google") } -> "Google 로그인"
                currentUser.providerData.any { it.providerId.contains("password") } -> "이메일 로그인"
                else -> "소셜 로그인"
            }
            chipProvider.text = providerText

            // Firestore에서 사용자 정보 가져오기 (가입일)
            db.collection("users").document(currentUser.uid)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val createdAt = document.getLong("createdAt") ?: System.currentTimeMillis()
                        val dateFormat = SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault())
                        textCreatedAt.text = dateFormat.format(Date(createdAt))
                    }
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error loading user info", e)
                }

            // 프로필 사진 (간단히 기본 아이콘 사용, 나중에 이미지 로딩 라이브러리 추가 가능)
            // TODO: Glide나 Coil로 프로필 사진 로드
        } else {
            // 로그인하지 않은 경우
            textDisplayName.text = "로그인 필요"
            textEmail.text = "로그인하지 않았습니다"
            chipProvider.text = "없음"
            textUserId.text = "N/A"
            textCreatedAt.text = "N/A"
        }
    }

    private fun loadWishlistStats() {
        val wishes = repository.getAllWishes()

        // 전체 위시 수
        textTotalWishes.text = wishes.size.toString()

        // 완료된 위시 수
        val completedCount = wishes.count { it.isCompleted }
        textCompletedWishes.text = completedCount.toString()

        // 선물받은 위시 수
        val giftedCount = wishes.count { it.giftInfo != null }
        textGiftedWishes.text = giftedCount.toString()
    }

    private fun setupClickListeners() {
        // 뒤로 가기
        buttonBack.setOnClickListener {
            finish()
        }

        // 위시리스트 공유
        buttonShareWishlist.setOnClickListener {
            shareWishlist()
        }

        // 로그아웃
        buttonLogout.setOnClickListener {
            performLogout()
        }

        // 계정 삭제
        buttonDeleteAccount.setOnClickListener {
            confirmDeleteAccount()
        }
    }

    private fun shareWishlist() {
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(this, "로그인이 필요합니다", Toast.LENGTH_SHORT).show()
            return
        }

        // Custom URL Scheme 생성
        val shareUrl = "wishlistapp://shared?userId=${currentUser.uid}"

        // 공유 인텐트 생성
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "내 위시리스트를 공유합니다")
            putExtra(Intent.EXTRA_TEXT, """
                🎁 ${currentUser.displayName ?: "나"}의 위시리스트 🎁

                아래 링크를 클릭하면 위시리스트를 확인하고 선물 완료 표시를 할 수 있어요!

                $shareUrl

                ℹ️ 위시리스트 앱 설치가 필요합니다
                앱이 설치되지 않았다면 Play Store에서 "위시리스트 앱"을 검색해주세요!

                📲 Play Store에서 다운로드: https://play.google.com/store/apps/details?id=com.wishlist.app
            """.trimIndent())
        }

        // 공유 다이얼로그 표시
        startActivity(Intent.createChooser(shareIntent, "위시리스트 공유하기"))

        Log.d(TAG, "Wishlist shared with URL: $shareUrl")
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
                        Log.e(TAG, "카카오 로그아웃 실패", error)
                    } else {
                        Log.d(TAG, "카카오 로그아웃 성공")
                    }
                }

                // 네이버 로그아웃
                try {
                    NaverIdLoginSDK.logout()
                    Log.d(TAG, "네이버 로그아웃 성공")
                } catch (e: Exception) {
                    Log.e(TAG, "네이버 로그아웃 실패", e)
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

    private fun confirmDeleteAccount() {
        AlertDialog.Builder(this)
            .setTitle("계정 삭제")
            .setMessage("정말로 계정을 삭제하시겠습니까?\n\n모든 위시리스트 데이터가 삭제되며 복구할 수 없습니다.")
            .setPositiveButton("삭제") { _, _ ->
                deleteAccount()
            }
            .setNegativeButton("취소", null)
            .show()
    }

    private fun deleteAccount() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            // Firestore에서 사용자 데이터 삭제
            db.collection("users").document(currentUser.uid)
                .delete()
                .addOnSuccessListener {
                    Log.d(TAG, "Firestore user data deleted")
                }

            // 모든 위시 삭제
            repository.deleteAllWishes()

            // Firebase 계정 삭제
            currentUser.delete()
                .addOnSuccessListener {
                    Toast.makeText(this, "계정이 삭제되었습니다", Toast.LENGTH_SHORT).show()

                    // 로그인 화면으로 이동
                    val intent = Intent(this, LoginActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e(TAG, "Error deleting account", e)
                    Toast.makeText(
                        this,
                        "계정 삭제 실패: 최근에 로그인하지 않았다면 다시 로그인 후 시도하세요",
                        Toast.LENGTH_LONG
                    ).show()
                }
        }
    }
}
