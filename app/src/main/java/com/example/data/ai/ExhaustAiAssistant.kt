package com.example.data.ai

import android.util.Log
import com.example.data.model.CatalogProduct
import com.example.data.model.ChatMessage
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class ExtractedCustomerData(
    val customerName: String,
    val region: String,
    val address: String,
    val phone: String,
    val requestedProduct: String,
    val summaryNote: String,
)

class ExhaustAiAssistant {

    private val chatModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    أنت المساعد الذكي الرسمي لمتجر عوادم التيربو المتخصص في أنظمة العادم الرياضية ودبات التلوث (الكتاليزر) والداون بايب.
                    أجب باللغة العربية بأسلوب عملي وودود ومختصر.
                    ساعد العملاء في اختيار دبة التلوث المناسبة، معالجة كود P0420، اختيار الكتباك أو الداون بايب، وتأكيد بيانات العميل والشحن والأسعار.
                    """.trimIndent()
                )
            }
        )
    }

    private val seoKeywordsSchema = Schema.obj(
        properties = mapOf(
            "keywords" to Schema.array(
                items = Schema.string(),
                description = "قائمة من 6 إلى 8 كلمات مفتاحية عربية وإنجليزية مناسبة للبحث على جوجل"
            ),
            "googleSnippet" to Schema.string(
                description = "وصف تسويقي مختصر يظهر في نتائج بحث جوجل"
            )
        )
    )

    private val seoModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = seoKeywordsSchema
            }
        )
    }

    private val customerExtractSchema = Schema.obj(
        properties = mapOf(
            "customerName" to Schema.string(description = "اسم العميل الكامل إن ورد في المحادثة أو فارغ"),
            "region" to Schema.string(description = "المدينة أو المنطقة مثل الرياض، جدة، الدمام"),
            "address" to Schema.string(description = "العنوان أو الحي أو الشارع"),
            "phone" to Schema.string(description = "رقم جوال العميل إن وجد"),
            "requestedProduct" to Schema.string(description = "المنتج المطلوب من الكتالوج"),
            "summaryNote" to Schema.string(description = "ملخص مختصر بالعربية للبيانات التي تم التقاطها لتأكيدها للعميل")
        )
    )

    private val customerExtractModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = customerExtractSchema
            }
        )
    }

    private val smsDraftModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            systemInstruction = content {
                text(
                    """
                    أنت كاتب رسائل نصية قصيرة (SMS) لمتجر أنظمة العادم ودبات التلوث.
                    اكتب رسالة SMS عربية قصيرة ومباشرة وواضحة أقل من 180 حرفاً، تذكر اسم العميل والمدة المتبقية واسم المتجر ورابط التجديد أو التفعيل بدون علامات تنصيص أو مقدمات.
                    """.trimIndent()
                )
            }
        )
    }

    fun streamStoreReply(
        userPrompt: String,
        catalog: List<CatalogProduct>,
        storeName: String,
        history: List<ChatMessage> = emptyList(),
    ): Flow<String> = flow {
        val catalogSummary = catalog.take(10).joinToString("\n") { p ->
            "- ${p.name} (${p.category}): ${p.priceSar} ر.س | كلمات: ${p.seoKeywords.joinToString("، ")}"
        }
        val fullPrompt = """
            اسم المتجر: $storeName
            منتجات الكتالوج المتوفرة حالياً:
            $catalogSummary

            سؤال العميل أو طلب المالك:
            $userPrompt
        """.trimIndent()

        val priorContents = history
            .filter { it.messageType == "text" && it.text.isNotBlank() }
            .takeLast(12)
            .map { msg ->
                val role = if (msg.senderRole == "ai") "model" else "user"
                content(role = role) { text(msg.text) }
            }

        val accumulated = StringBuilder()
        try {
            withTimeout(60_000L) {
                val chatSession = chatModel.startChat(history = priorContents)
                chatSession.sendMessageStream(fullPrompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(accumulated.toString())
                    }
                }
            }
            if (accumulated.isBlank()) {
                throw IllegalStateException("لم يُرجع الذكاء الاصطناعي رداً، حاول مرة أخرى.")
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "AI stream timed out", e)
            throw IllegalStateException("انتهت مهلة الاتصال بالذكاء الاصطناعي (60 ثانية). يرجى المحاولة مرة أخرى.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Firebase AI stream error", e)
            throw IllegalStateException(e.localizedMessage ?: "تعذر الاتصال بالذكاء الاصطناعي.")
        }
    }

    suspend fun draftSubscriptionExpirySms(
        customerName: String,
        planLabel: String,
        remainingDays: Long,
        storeName: String,
    ): String {
        val statusHint = if (remainingDays <= 0L) "منتهي اليوم" else "متبقي $remainingDays يوم"
        val prompt = """
            اكتب رسالة SMS تذكيرية قصيرة للعميل:
            اسم العميل: $customerName
            المتجر: $storeName
            الباقة: $planLabel
            المدة المتبقية: $statusHint
            أضف دعوة لتجديد الاشتراك الشهري (25 ر.س/شهر) والحفاظ على الخدمة.
        """.trimIndent()
        return try {
            val response = withTimeout(60_000L) {
                smsDraftModel.generateContent(prompt)
            }
            val text = response.text?.trim().orEmpty()
            if (text.isBlank()) {
                throw IllegalStateException("تعذر توليد نص الرسالة النصية.")
            }
            text.take(480)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "SMS draft timed out", e)
            throw IllegalStateException("انتهت مهلة صياغة الرسالة بالذكاء الاصطناعي.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "SMS draft failed", e)
            throw IllegalStateException(e.localizedMessage ?: "تعذر صياغة رسالة SMS بالذكاء الاصطناعي.")
        }
    }

    suspend fun extractCustomerOrderData(
        messages: List<ChatMessage>,
        fallbackCustomerName: String,
        fallbackPhone: String,
        catalog: List<CatalogProduct>,
    ): ExtractedCustomerData {
        val transcript = messages.takeLast(20).joinToString("\n") { m ->
            "[${m.senderRole}]: ${m.text}"
        }
        val productsList = catalog.take(10).joinToString("، ") { "${it.name} (${it.priceSar.toInt()} ر.س)" }
        val prompt = """
            استخرج بيانات العميل (الاسم، المنطقة، العنوان، رقم الجوال، والمنتج المطلوب) من رسائل المحادثة التالية لتعبئة بوليصة الشحن تلقائياً.
            إذا لم يذكر حقل معين استخدم القيمة الاحتياطية أو اتركه فارغاً بدون اختراع معلومات.
            اسم العميل المسجل حالياً: $fallbackCustomerName
            الجوال المسجل حالياً: $fallbackPhone
            المنتجات المتوفرة: $productsList

            المحادثة:
            $transcript
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                customerExtractModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("استجابة فارغة من الذكاء الاصطناعي.")
            val json = JSONObject(rawJson)
            ExtractedCustomerData(
                customerName = json.optString("customerName").ifBlank { fallbackCustomerName },
                region = json.optString("region"),
                address = json.optString("address"),
                phone = json.optString("phone").ifBlank { fallbackPhone },
                requestedProduct = json.optString("requestedProduct"),
                summaryNote = json.optString("summaryNote").ifBlank { "تم استخراج وتعبئة بيانات العميل بنجاح" },
            )
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Customer extraction timed out", e)
            throw IllegalStateException("انتهت مهلة استخراج بيانات العميل.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Customer extraction failed", e)
            throw IllegalStateException(e.localizedMessage ?: "تعذر استخراج بيانات العميل بالذكاء الاصطناعي.")
        }
    }

    suspend fun generateGoogleSeoKeywords(
        productName: String,
        category: String,
        description: String,
    ): Pair<List<String>, String> {
        val prompt = """
            اقترح كلمات مفتاحية للبحث على جوجل (Google SEO) لمنتج سيارات:
            الاسم: $productName
            الفئة: $category
            الوصف: $description
        """.trimIndent()
        return try {
            val response = withTimeout(60_000L) {
                seoModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("استجابة فارغة من النموذج.")
            val json = JSONObject(rawJson)
            val arr = json.getJSONArray("keywords")
            val keywords = (0 until arr.length()).map { arr.getString(it) }
            val snippet = json.optString("googleSnippet", "")
            keywords to snippet
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "SEO generation timed out", e)
            throw IllegalStateException("انتهت مهلة توليد الكلمات المفتاحية.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "SEO keyword generation failed", e)
            throw IllegalStateException(e.localizedMessage ?: "فشل توليد الكلمات المفتاحية.")
        }
    }

    private companion object {
        const val TAG = "ExhaustAiAssistant"
    }
}
