package com.wishlist.app

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.textfield.TextInputEditText
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth
import com.wishlist.app.adapter.SharedWishAdapter
import com.wishlist.app.data.FirestoreRepository
import com.wishlist.app.data.WishItem
import kotlinx.coroutines.launch

/**
 * 공유된 위시리스트를 보는 화면
 * Deep Link를 통해 다른 사용자의 위시리스트를 확인하고 선물 완료 표시 가능
 */
class SharedWishlistActivity : AppCompatActivity() {

    private lateinit var toolbar: MaterialToolbar
    private lateinit var imageOwnerProfile: ImageView
    private lateinit var textOwnerName: TextView
    private lateinit var textOwnerEmail: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyStateLayout: LinearLayout

    private lateinit var firestoreRepository: FirestoreRepository
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: SharedWishAdapter

    private var ownerId: String? = null

    companion object {
        private const val TAG = "SharedWishlistActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_shared_wishlist)

        // Firebase 초기화
        auth = FirebaseAuth.getInstance()
        firestoreRepository = FirestoreRepository()

        // 뷰 초기화
        initViews()

        // Deep Link에서 사용자 ID 추출
        handleDeepLink(intent)

        // 툴바 설정
        toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleDeepLink(intent)
    }

    private fun initViews() {
        toolbar = findViewById(R.id.toolbar)
        imageOwnerProfile = findViewById(R.id.imageOwnerProfile)
        textOwnerName = findViewById(R.id.textOwnerName)
        textOwnerEmail = findViewById(R.id.textOwnerEmail)
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.recyclerViewSharedWishes)
        emptyStateLayout = findViewById(R.id.emptyStateLayout)

        // RecyclerView 설정
        recyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun handleDeepLink(intent: Intent?) {
        val data: Uri? = intent?.data

        if (data != null) {
            // URL에서 userId 파라미터 추출
            ownerId = data.getQueryParameter("userId")

            if (ownerId != null) {
                Log.d(TAG, "Received deep link for user: $ownerId")
                loadSharedWishlist(ownerId!!)
            } else {
                Toast.makeText(this, "잘못된 공유 링크입니다", Toast.LENGTH_SHORT).show()
                finish()
            }
        } else {
            Toast.makeText(this, "공유 링크가 없습니다", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun loadSharedWishlist(userId: String) {
        progressBar.visibility = View.VISIBLE
        recyclerView.visibility = View.GONE
        emptyStateLayout.visibility = View.GONE

        lifecycleScope.launch {
            // 사용자 정보 로드
            val userResult = firestoreRepository.getUserInfo(userId)
            userResult.onSuccess { user ->
                if (user != null) {
                    textOwnerName.text = user.displayName ?: "사용자"
                    textOwnerEmail.text = user.email ?: ""
                } else {
                    textOwnerName.text = "사용자"
                    textOwnerEmail.text = ""
                }
            }.onFailure { e ->
                Log.e(TAG, "Error loading user info", e)
                textOwnerName.text = "사용자"
                textOwnerEmail.text = ""
            }

            // 위시리스트 로드
            val wishlistResult = firestoreRepository.getUserWishlist(userId)
            wishlistResult.onSuccess { wishes ->
                progressBar.visibility = View.GONE

                if (wishes.isEmpty()) {
                    emptyStateLayout.visibility = View.VISIBLE
                    recyclerView.visibility = View.GONE
                } else {
                    emptyStateLayout.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE

                    // 어댑터 설정
                    adapter = SharedWishAdapter(
                        wishes = wishes.toMutableList(),
                        ownerId = userId,
                        onMarkAsGifted = { wish ->
                            showGiftDialog(wish)
                        }
                    )
                    recyclerView.adapter = adapter
                }
            }.onFailure { e ->
                Log.e(TAG, "Error loading wishlist", e)
                progressBar.visibility = View.GONE
                Toast.makeText(this@SharedWishlistActivity, "위시리스트를 불러오는데 실패했습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showGiftDialog(wish: WishItem) {
        // 다이얼로그 레이아웃 인플레이트
        val dialogView = layoutInflater.inflate(R.layout.dialog_mark_gifted, null)
        val editTextGifterName: TextInputEditText = dialogView.findViewById(R.id.editTextGifterName)
        val editTextMessage: TextInputEditText = dialogView.findViewById(R.id.editTextMessage)

        // 현재 로그인한 사용자가 있으면 이름 자동 입력
        val currentUser = auth.currentUser
        if (currentUser != null) {
            editTextGifterName.setText(currentUser.displayName ?: currentUser.email?.substringBefore("@") ?: "")
        }

        // 다이얼로그 생성
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // 버튼 리스너 설정
        val buttonCancel: android.widget.Button = dialogView.findViewById(R.id.buttonCancel)
        val buttonConfirm: android.widget.Button = dialogView.findViewById(R.id.buttonConfirm)

        buttonCancel.setOnClickListener {
            dialog.dismiss()
        }

        buttonConfirm.setOnClickListener {
            val gifterName = editTextGifterName.text.toString().trim()
            val message = editTextMessage.text.toString().trim()

            if (gifterName.isEmpty()) {
                Toast.makeText(this, "선물한 사람 이름을 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Firestore에 선물 완료 표시
            markWishAsGifted(wish, gifterName, message.ifEmpty { null })
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun markWishAsGifted(wish: WishItem, gifterName: String, message: String?) {
        val userId = ownerId ?: return

        lifecycleScope.launch {
            val result = firestoreRepository.markAsGifted(userId, wish.id, gifterName, message)

            result.onSuccess {
                Toast.makeText(this@SharedWishlistActivity, "선물 완료 표시되었습니다!", Toast.LENGTH_SHORT).show()
                // 위시리스트 새로고침
                loadSharedWishlist(userId)
            }.onFailure { e ->
                Log.e(TAG, "Error marking as gifted", e)
                Toast.makeText(this@SharedWishlistActivity, "선물 완료 표시 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
