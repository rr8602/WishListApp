package com.wishlist.app

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.kakao.sdk.auth.model.OAuthToken
import com.kakao.sdk.common.model.ClientError
import com.kakao.sdk.common.model.ClientErrorCause
import com.kakao.sdk.user.UserApiClient
import com.navercorp.nid.NaverIdLoginSDK
import com.navercorp.nid.oauth.NidOAuthLogin
import com.navercorp.nid.oauth.OAuthLoginCallback
import com.navercorp.nid.profile.NidProfileCallback
import com.navercorp.nid.profile.data.NidProfileResponse
import com.wishlist.app.data.LoginProvider
import com.wishlist.app.data.User

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var googleSignInClient: GoogleSignInClient

    private lateinit var editTextEmail: TextInputEditText
    private lateinit var editTextPassword: TextInputEditText
    private lateinit var buttonEmailLogin: Button
    private lateinit var buttonEmailSignup: Button
    private lateinit var buttonGoogleLogin: Button
    private lateinit var buttonKakaoLogin: Button
    private lateinit var buttonNaverLogin: Button
    private lateinit var buttonSkipLogin: Button

    companion object {
        private const val TAG = "LoginActivity"
        private const val RC_GOOGLE_SIGN_IN = 9001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Firebase 초기화
        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // 이미 로그인된 경우 바로 메인으로
        if (auth.currentUser != null) {
            navigateToMain()
            return
        }

        // 뷰 초기화
        initViews()

        // 구글 로그인 설정
        setupGoogleSignIn()

        // 네이버 SDK 초기화
        NaverIdLoginSDK.initialize(this, "5NClb6EiRCBxJts9K_TF", "cmGqQIThkl", "WishListApp")

        // 버튼 클릭 리스너 설정
        setupClickListeners()
    }

    private fun initViews() {
        editTextEmail = findViewById(R.id.editTextEmail)
        editTextPassword = findViewById(R.id.editTextPassword)
        buttonEmailLogin = findViewById(R.id.buttonEmailLogin)
        buttonEmailSignup = findViewById(R.id.buttonEmailSignup)
        buttonGoogleLogin = findViewById(R.id.buttonGoogleLogin)
        buttonKakaoLogin = findViewById(R.id.buttonKakaoLogin)
        buttonNaverLogin = findViewById(R.id.buttonNaverLogin)
        buttonSkipLogin = findViewById(R.id.buttonSkipLogin)
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {
        // 이메일 로그인
        buttonEmailLogin.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            signInWithEmail(email, password)
        }

        // 이메일 회원가입
        buttonEmailSignup.setOnClickListener {
            val email = editTextEmail.text.toString().trim()
            val password = editTextPassword.text.toString().trim()

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "이메일과 비밀번호를 입력하세요", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            signUpWithEmail(email, password)
        }

        // 구글 로그인
        buttonGoogleLogin.setOnClickListener {
            signInWithGoogle()
        }

        // 카카오 로그인
        buttonKakaoLogin.setOnClickListener {
            signInWithKakao()
        }

        // 네이버 로그인
        buttonNaverLogin.setOnClickListener {
            signInWithNaver()
        }

        // 로그인 없이 계속
        buttonSkipLogin.setOnClickListener {
            navigateToMain()
        }
    }

    // ========== 이메일 로그인 ==========
    private fun signInWithEmail(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    user?.let {
                        saveUserToFirestore(it, LoginProvider.EMAIL)
                    }
                    navigateToMain()
                } else {
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(
                        this,
                        "로그인 실패: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun signUpWithEmail(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    user?.let {
                        saveUserToFirestore(it, LoginProvider.EMAIL)
                    }
                    Toast.makeText(this, "회원가입 성공!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(
                        this,
                        "회원가입 실패: ${task.exception?.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    // ========== 구글 로그인 ==========
    private fun signInWithGoogle() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN)
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "signInWithCredential:success")
                    val user = auth.currentUser
                    user?.let {
                        saveUserToFirestore(it, LoginProvider.GOOGLE)
                    }
                    Toast.makeText(this, "구글 로그인 성공!", Toast.LENGTH_SHORT).show()
                    navigateToMain()
                } else {
                    Log.w(TAG, "signInWithCredential:failure", task.exception)
                    Toast.makeText(this, "구글 로그인 실패", Toast.LENGTH_SHORT).show()
                }
            }
    }

    // ========== 카카오 로그인 ==========
    private fun signInWithKakao() {
        val callback: (OAuthToken?, Throwable?) -> Unit = { token, error ->
            if (error != null) {
                Log.e(TAG, "카카오 로그인 실패", error)
                Toast.makeText(this, "카카오 로그인 실패", Toast.LENGTH_SHORT).show()
            } else if (token != null) {
                Log.i(TAG, "카카오 로그인 성공 ${token.accessToken}")
                getKakaoUserInfo()
            }
        }

        // 카카오톡이 설치되어 있으면 카카오톡으로 로그인, 아니면 카카오계정으로 로그인
        if (UserApiClient.instance.isKakaoTalkLoginAvailable(this)) {
            UserApiClient.instance.loginWithKakaoTalk(this) { token, error ->
                if (error != null) {
                    Log.e(TAG, "카카오톡으로 로그인 실패", error)

                    // 사용자가 카카오톡 설치 후 디바이스 권한 요청 화면에서 로그인을 취소한 경우,
                    // 의도적인 로그인 취소로 보고 카카오계정으로 로그인 시도 없이 로그인 취소로 처리 (예: 뒤로 가기)
                    if (error is ClientError && error.reason == ClientErrorCause.Cancelled) {
                        return@loginWithKakaoTalk
                    }

                    // 카카오톡에 연결된 카카오계정이 없는 경우, 카카오계정으로 로그인 시도
                    UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
                } else if (token != null) {
                    Log.i(TAG, "카카오톡으로 로그인 성공 ${token.accessToken}")
                    getKakaoUserInfo()
                }
            }
        } else {
            UserApiClient.instance.loginWithKakaoAccount(this, callback = callback)
        }
    }

    private fun getKakaoUserInfo() {
        UserApiClient.instance.me { user, error ->
            if (error != null) {
                Log.e(TAG, "사용자 정보 요청 실패", error)
            } else if (user != null) {
                Log.i(TAG, "사용자 정보 요청 성공: ${user.kakaoAccount?.email}")

                // 카카오 로그인은 Firebase Custom Token을 사용하거나
                // Firestore에 직접 저장 (여기서는 간단히 Firestore 저장)
                val wishlistUser = User(
                    uid = user.id.toString(),
                    email = user.kakaoAccount?.email,
                    displayName = user.kakaoAccount?.profile?.nickname,
                    profileImageUrl = user.kakaoAccount?.profile?.profileImageUrl,
                    provider = LoginProvider.KAKAO
                )

                saveKakaoUserToFirestore(wishlistUser)
                Toast.makeText(this, "카카오 로그인 성공!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }
        }
    }

    // ========== 네이버 로그인 ==========
    private fun signInWithNaver() {
        val oauthLoginCallback = object : OAuthLoginCallback {
            override fun onSuccess() {
                // 네이버 로그인 성공
                Log.d(TAG, "네이버 로그인 성공")
                getNaverUserInfo()
            }

            override fun onFailure(httpStatus: Int, message: String) {
                val errorCode = NaverIdLoginSDK.getLastErrorCode().code
                val errorDescription = NaverIdLoginSDK.getLastErrorDescription()
                Log.e(TAG, "네이버 로그인 실패: $errorCode, $errorDescription")
                Toast.makeText(this@LoginActivity, "네이버 로그인 실패", Toast.LENGTH_SHORT).show()
            }

            override fun onError(errorCode: Int, message: String) {
                onFailure(errorCode, message)
            }
        }

        NaverIdLoginSDK.authenticate(this, oauthLoginCallback)
    }

    private fun getNaverUserInfo() {
        NidOAuthLogin().callProfileApi(object : NidProfileCallback<NidProfileResponse> {
            override fun onSuccess(response: NidProfileResponse) {
                val profile = response.profile
                Log.d(TAG, "네이버 사용자 정보: ${profile?.email}")

                val wishlistUser = User(
                    uid = profile?.id ?: "",
                    email = profile?.email,
                    displayName = profile?.name,
                    profileImageUrl = profile?.profileImage,
                    provider = LoginProvider.NAVER
                )

                saveNaverUserToFirestore(wishlistUser)
                Toast.makeText(this@LoginActivity, "네이버 로그인 성공!", Toast.LENGTH_SHORT).show()
                navigateToMain()
            }

            override fun onFailure(httpStatus: Int, message: String) {
                Log.e(TAG, "네이버 프로필 조회 실패: $message")
                Toast.makeText(this@LoginActivity, "사용자 정보 조회 실패", Toast.LENGTH_SHORT).show()
            }

            override fun onError(errorCode: Int, message: String) {
                onFailure(errorCode, message)
            }
        })
    }

    // ========== Firestore에 사용자 정보 저장 ==========
    private fun saveUserToFirestore(firebaseUser: FirebaseUser, provider: LoginProvider) {
        val user = User(
            uid = firebaseUser.uid,
            email = firebaseUser.email,
            displayName = firebaseUser.displayName,
            profileImageUrl = firebaseUser.photoUrl?.toString(),
            provider = provider
        )

        db.collection("users")
            .document(firebaseUser.uid)
            .set(user)
            .addOnSuccessListener {
                Log.d(TAG, "User profile saved to Firestore")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error saving user profile", e)
            }
    }

    // 카카오 사용자 저장 (Firebase Auth 없이)
    private fun saveKakaoUserToFirestore(user: User) {
        db.collection("users")
            .document(user.uid)
            .set(user)
            .addOnSuccessListener {
                Log.d(TAG, "Kakao user saved to Firestore")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error saving Kakao user", e)
            }
    }

    // 네이버 사용자 저장 (Firebase Auth 없이)
    private fun saveNaverUserToFirestore(user: User) {
        db.collection("users")
            .document(user.uid)
            .set(user)
            .addOnSuccessListener {
                Log.d(TAG, "Naver user saved to Firestore")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error saving Naver user", e)
            }
    }

    // ========== 메인 화면으로 이동 ==========
    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    // ========== Activity Result 처리 ==========
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        // 구글 로그인 결과 처리
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                Log.d(TAG, "firebaseAuthWithGoogle:" + account.id)
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Log.w(TAG, "Google sign in failed", e)
                Toast.makeText(this, "구글 로그인 실패", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
