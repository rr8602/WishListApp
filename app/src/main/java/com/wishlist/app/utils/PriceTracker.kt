package com.wishlist.app.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.net.URLDecoder

/**
 * 가격 추적 유틸리티
 * 각 쇼핑몰에서 가격 정보를 스크래핑
 */
object PriceTracker {

    private const val TIMEOUT = 10000 // 10초
    private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.120 Mobile Safari/537.36"

    /**
     * URL에서 가격 정보 추출
     */
    suspend fun fetchPrice(url: String): Double? = withContext(Dispatchers.IO) {
        try {
            val siteName = detectShoppingSite(url)
            val doc = Jsoup.connect(url)
                .userAgent(USER_AGENT)
                .timeout(TIMEOUT)
                .get()

            return@withContext when (siteName) {
                "쿠팡" -> parseCoupangPrice(doc)
                "네이버쇼핑" -> parseNaverPrice(doc, url)
                "11번가" -> parse11stPrice(doc)
                "G마켓" -> parseGmarketPrice(doc)
                "옥션" -> parseAuctionPrice(doc)
                "SSG" -> parseSsgPrice(doc)
                "위메프" -> parseWemakePricePrice(doc)
                "티몬" -> parseTmonPrice(doc)
                else -> parseGenericPrice(doc)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * URL에서 쇼핑몰 이름 감지
     */
    fun detectShoppingSite(url: String): String {
        return when {
            url.contains("coupang.com") -> "쿠팡"
            url.contains("shopping.naver.com") || url.contains("smartstore.naver.com") -> "네이버쇼핑"
            url.contains("11st.co.kr") -> "11번가"
            url.contains("gmarket.co.kr") -> "G마켓"
            url.contains("auction.co.kr") -> "옥션"
            url.contains("ssg.com") -> "SSG"
            url.contains("wemakeprice.com") -> "위메프"
            url.contains("tmon.co.kr") -> "티몬"
            else -> "기타"
        }
    }

    /**
     * 쿠팡 가격 파싱
     */
    private fun parseCoupangPrice(doc: Document): Double? {
        return try {
            // 쿠팡의 가격 선택자들 (여러 패턴 시도)
            val selectors = listOf(
                "span.total-price strong",
                "span.total-price",
                "div.prod-price span.total-price",
                "strong.price-value",
                "span.price"
            )

            for (selector in selectors) {
                val priceText = doc.select(selector).first()?.text()
                if (priceText != null) {
                    return extractPrice(priceText)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 네이버쇼핑 가격 파싱
     */
    private fun parseNaverPrice(doc: Document, url: String): Double? {
        return try {
            val selectors = listOf(
                "span.price em",
                "strong.price_total em",
                "span.num",
                "div.price strong",
                "em.price_num"
            )

            for (selector in selectors) {
                val priceText = doc.select(selector).first()?.text()
                if (priceText != null) {
                    return extractPrice(priceText)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 11번가 가격 파싱
     */
    private fun parse11stPrice(doc: Document): Double? {
        return try {
            val selectors = listOf(
                "dd.price strong",
                "strong.sale_price",
                "dd.sale_price",
                "span.price"
            )

            for (selector in selectors) {
                val priceText = doc.select(selector).first()?.text()
                if (priceText != null) {
                    return extractPrice(priceText)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * G마켓 가격 파싱
     */
    private fun parseGmarketPrice(doc: Document): Double? {
        return try {
            val selectors = listOf(
                "div.price strong.price_real",
                "span.price_real",
                "strong.price",
                "div.item_price strong"
            )

            for (selector in selectors) {
                val priceText = doc.select(selector).first()?.text()
                if (priceText != null) {
                    return extractPrice(priceText)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 옥션 가격 파싱
     */
    private fun parseAuctionPrice(doc: Document): Double? {
        return try {
            val selectors = listOf(
                "div.price strong.price_real",
                "span.price_real",
                "strong.price_value"
            )

            for (selector in selectors) {
                val priceText = doc.select(selector).first()?.text()
                if (priceText != null) {
                    return extractPrice(priceText)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * SSG 가격 파싱
     */
    private fun parseSsgPrice(doc: Document): Double? {
        return try {
            val selectors = listOf(
                "em.ssg_price",
                "span.price",
                "em.price"
            )

            for (selector in selectors) {
                val priceText = doc.select(selector).first()?.text()
                if (priceText != null) {
                    return extractPrice(priceText)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 위메프 가격 파싱
     */
    private fun parseWemakePricePrice(doc: Document): Double? {
        return try {
            val selectors = listOf(
                "span.sale_price",
                "strong.price",
                "em.price"
            )

            for (selector in selectors) {
                val priceText = doc.select(selector).first()?.text()
                if (priceText != null) {
                    return extractPrice(priceText)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 티몬 가격 파싱
     */
    private fun parseTmonPrice(doc: Document): Double? {
        return try {
            val selectors = listOf(
                "span.sale_price",
                "strong.sale_price",
                "em.price"
            )

            for (selector in selectors) {
                val priceText = doc.select(selector).first()?.text()
                if (priceText != null) {
                    return extractPrice(priceText)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 일반적인 가격 파싱 (알 수 없는 사이트)
     */
    private fun parseGenericPrice(doc: Document): Double? {
        return try {
            // 일반적인 가격 패턴 찾기
            val selectors = listOf(
                "span.price",
                "div.price",
                "strong.price",
                "em.price",
                "[class*=price]",
                "[id*=price]"
            )

            for (selector in selectors) {
                val elements = doc.select(selector)
                for (element in elements) {
                    val priceText = element.text()
                    val price = extractPrice(priceText)
                    if (price != null && price > 0) {
                        return price
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    /**
     * 텍스트에서 가격 숫자 추출
     * 예: "₩ 50,000원" -> 50000.0
     */
    private fun extractPrice(text: String): Double? {
        return try {
            // 모든 숫자와 쉼표만 추출
            val numbers = text.replace(Regex("[^0-9,]"), "")
                .replace(",", "")

            if (numbers.isEmpty()) return null

            val price = numbers.toDoubleOrNull()

            // 가격이 너무 작거나 크면 무시 (1원 ~ 100억원)
            if (price != null && price in 1.0..10000000000.0) {
                price
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
