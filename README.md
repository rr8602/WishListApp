# 위시리스트 앱 (WishList App)

안드로이드용 간단한 위시리스트 관리 앱입니다.

## 기능

- ✅ 위시리스트 추가
- ✅ 위시리스트 수정
- ✅ 위시리스트 삭제
- ✅ 위시 완료 체크
- ✅ 데이터 영구 저장 (SharedPreferences)

## 기술 스택

- **언어**: Kotlin
- **빌드 시스템**: Gradle (Kotlin DSL)
- **아키텍처**: MVVM (Model-View-ViewModel)
- **UI**: XML Layouts + RecyclerView
- **데이터 저장**: SharedPreferences + Gson

## 프로젝트 구조

```
wishlist_app/
├── app/
│   ├── src/
│   │   └── main/
│   │       ├── java/com/wishlist/app/
│   │       │   ├── data/
│   │       │   │   ├── WishItem.kt          # 데이터 모델
│   │       │   │   └── WishRepository.kt    # 데이터 저장소
│   │       │   ├── ui/
│   │       │   │   ├── WishListViewModel.kt # ViewModel
│   │       │   │   └── WishAdapter.kt       # RecyclerView 어댑터
│   │       │   └── MainActivity.kt          # 메인 액티비티
│   │       ├── res/
│   │       │   ├── layout/
│   │       │   │   ├── activity_main.xml    # 메인 화면
│   │       │   │   ├── item_wish.xml        # 위시 아이템
│   │       │   │   └── dialog_add_wish.xml  # 추가/수정 다이얼로그
│   │       │   └── values/
│   │       │       ├── strings.xml          # 문자열 리소스
│   │       │       ├── colors.xml           # 색상 정의
│   │       │       └── themes.xml           # 테마 설정
│   │       └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle.kts
├── settings.gradle.kts
└── gradle.properties
```

## 시작하기

### 필수 요구사항

1. **Android Studio** (최신 버전 권장)
2. **Android SDK** (API Level 24 이상)
3. **JDK 8** 이상

### 설치 및 실행

1. **Android SDK 경로 설정**
   
   `local.properties` 파일을 열고 시스템의 Android SDK 경로를 확인/수정하세요:
   ```properties
   sdk.dir=C\:\\Users\\YourUsername\\AppData\\Local\\Android\\Sdk
   ```

2. **Android Studio에서 프로젝트 열기**
   
   - Android Studio 실행
   - "Open an Existing Project" 선택
   - `wishlist_app` 폴더 선택

3. **Gradle 동기화**
   
   프로젝트를 열면 자동으로 Gradle 동기화가 시작됩니다.
   수동으로 동기화하려면: `File > Sync Project with Gradle Files`

4. **앱 실행**
   
   - 에뮬레이터를 실행하거나 실제 기기를 연결
   - `Run > Run 'app'` 클릭 (또는 Shift+F10)

### 명령줄에서 빌드

```bash
# Windows
gradlew.bat build

# Linux/macOS
./gradlew build
```

## 사용 방법

1. **위시 추가**: 우측 하단의 `+` 버튼을 클릭하여 새로운 위시를 추가합니다.
2. **위시 수정**: 위시 아이템을 클릭하면 수정할 수 있습니다.
3. **위시 삭제**: 각 아이템의 삭제 버튼을 클릭합니다.
4. **위시 완료**: 체크박스를 클릭하여 완료 상태를 토글합니다.

## 향후 개선 계획

- [ ] Room Database로 데이터 저장 방식 업그레이드
- [ ] Jetpack Compose로 UI 마이그레이션
- [ ] 카테고리 기능 추가
- [ ] 우선순위 설정 기능
- [ ] 이미지 첨부 기능
- [ ] 검색 및 필터링 기능
- [ ] 다크 모드 지원

## 라이선스

이 프로젝트는 학습 목적으로 만들어졌습니다.
