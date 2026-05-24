package com.example.data

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.network.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class CurrencyRepository(private val context: Context) {
    private val database = AppDatabase.getDatabase(context)
    private val dao = database.historyDao()

    val historyFlow: Flow<List<HistoryItem>> = dao.getAllHistory()

    // 2026 Exchange rates (Base = 1 USD)
    val offlineRates = mapOf(
        "USD" to 1.0,          // US Dollar
        "SAR" to 3.75,         // Saudi Riyal
        "IQD" to 1450.0,       // Iraqi Dinar
        "EGP" to 48.0,         // Egyptian Pound
        "AED" to 3.67,         // UAE Dirham
        "KWD" to 0.31,         // Kuwaiti Dinar
        "QAR" to 3.64,         // Qatari Riyal
        "JOD" to 0.71,         // Jordanian Dinar
        "EUR" to 0.93,         // Euro (1 EUR = ~1.08 USD)
        "GBP" to 0.80,         // British Pound (1 GBP = ~1.25 USD)
        "JPY" to 155.0         // Japanese Yen
    )

    val currencyNamesAr = mapOf(
        "USD" to "دولار أمريكي",
        "SAR" to "ريال سعودي",
        "IQD" to "دينار عراقي",
        "EGP" to "جنيه مصري",
        "AED" to "درهم إماراتي",
        "KWD" to "دينار كويتي",
        "QAR" to "ريال قطري",
        "JOD" to "دينار أردني",
        "EUR" to "يورو",
        "GBP" to "جنيه إسترليني",
        "JPY" to "ين ياباني"
    )

    /**
     * Converts a specific amount offline using approximate 2026 rates.
     */
    fun performOfflineConversion(amount: Double, from: String, to: String): String {
        val rateFromUsd = offlineRates[from] ?: 1.0
        val rateToUsd = offlineRates[to] ?: 1.0

        // Convert input to USD, then USD to target
        val amountInUsd = amount / rateFromUsd
        val convertedAmount = amountInUsd * rateToUsd

        // Exchange rate from source to target is target_rate_to_usd / source_rate_to_usd
        val relativeRate = rateToUsd / rateFromUsd

        val formattedAmount = if (convertedAmount % 1.0 == 0.0) {
            convertedAmount.toInt().toString()
        } else {
            String.format("%.2f", convertedAmount)
        }

        val formattedRate = if (relativeRate % 1.0 == 0.0 || relativeRate > 100) {
            relativeRate.toInt().toString()
        } else {
            String.format("%.4f", relativeRate)
        }

        val fromName = currencyNamesAr[from] ?: from
        val toName = currencyNamesAr[to] ?: to

        return "$amount $fromName يساوي تقريباً $formattedAmount $toName (بناءً على سعر صرف تقريبي بقيمة $formattedRate)."
    }

    /**
     * Call the Gemini API to parse natural language prices and calculate the conversion.
     */
    suspend fun performOnlineSmartQuery(
        promptText: String,
        myCountryName: String = "العراق",
        myCountryCurrency: String = "IQD",
        selectedLanguage: String = "ar",
        userCustomKey: String? = null
    ): String = withContext(Dispatchers.IO) {
        val keyToUse = getUserApiKey(userCustomKey)
        if (keyToUse.isEmpty()) {
            val localResult = executeLocalFallbackParse(promptText, myCountryCurrency, selectedLanguage)
            insertHistory(promptText, localResult)
            return@withContext localResult
        }

        val systemInstruction = """
            أنت حاسبة عملات ذكية ومساعد سفر مخصص لمساعدة المسافرين في معرفة أسعار السلع بعملتهم المحلية فوراً وبشكل مبسط، ليس فقط لعام معين بل لجميع الأعوام؛ الماضي، الحاضر والمستقبل، باستخدام معرفتك المحدثة عبر الإنترنت وتوقعاتك المناسبة.
            
            معلومات التخصيص والموقع للمستخدم الحالي:
            - بلد المستخدم الحالي الرئيسي (بلدي): $myCountryName
            - العملة المحلية للمستخدم (عملتك المفضلة): $myCountryCurrency (${currencyNamesAr[myCountryCurrency] ?: ""})
            - لغة الرد المفضلة للمستخدم هي كود اللغة: $selectedLanguage (رد دائماً وبشكل كامل بهذه اللغة المحددة فقط، وترجم جميع قيم التعبيرات وأسماء العملات والأسعار ومحتوى الإجابة التبسيطي إليها تلقائياً!).
            
            إذا كتب المستخدم مبلغا أو قيمة بالعملات ولاحظت أنه لم يحدد العملة المحلية المستهدفة للتحويل، افترض تلقائياً أنه يريد التحويل إلى عملته المحلية الافتراضية المفضلة وهي: $myCountryCurrency ($myCountryName). فمثلاً إذا كتب "200€" أو "50$" دون توضيح، حولها تلقائياً إلى $myCountryCurrency ($myCountryName).

            قواعد أسعار الصرف الحالية والتقريبية وقاعدتك الأساسية هي (1 دولار أمريكي):
            - 1 دولار أمريكي = 3.75 ريال سعودي
            - 1 دولار أمريكي = 1450 دينار عراقي
            - 1 دولار أمريكي = 48 جنيه مصري
            - 1 دولار أمريكي = 3.67 درهم إماراتي
            - 1 دولار أمريكي = 0.31 دينار كويتي
            - 1 دولار أمريكي = 3.64 ريال قطري
            - 1 دولار أمريكي = 0.71 دينار أردني
            - 1 يورو = 1.08 دولار أمريكي (بمعنى يورو واحد يساوي تقريباً 4.05 ريال سعودي أو 1.08 دولار)
            - 1 جنيه إسترليني = 1.25 دولار أمريكي (بمعنى جنيه إسترليني واحد يساوي تقريباً 4.7 ريال سعودي)
            - 1 ين ياباني = 1/155 دولار أمريكي
            ويمكنك استخدام معرفتك لتقدير وحساب أسعار الصرف للعملات الأخرى أو الأعوام الأخرى بالتاريخ المكتوب بدقة متناهية.

            إذا كان الاستعلام يتعلق بطلب تاريخي أو حساب تاريخي في تاريخ محدد (اليوم، الشهر، السنة) مثل "كم كان سعر..." أو "حساب تاريخي لـ..." أو كان يحتوي على تفاصيل تاريخ ماضي أو مستقبلي:
            فيجب أن تكون صيغة الرد إلزامية وحرفية بالشكل التالي باللغة المحددة بالكامل (مثال بالعربية):
            [المبلغ الأجنبي] في [التاريخ] كان يساوي تقريباً [المبلغ بالعملة المحلية] [اسم العملة المحلية] (بناءً على سعر صرف تقريبي بقيمة [سعر الصرف]).
            أمثلة للتوضيح:
            - بالعربية: 100 USD في 2024-05-15 كان يساوي تقريباً 375 SAR ريال سعودي (بناءً على سعر صرف تقريبي بقيمة 3.75).
            - بالإنجليزية: 100 USD on 2024-05-15 was approximately equal to 375 SAR Saudi Riyal (based on an approximate exchange rate of 3.75).
            
            بالنسبة للطلبات العادية، تكون صيغة الرد المختصرة والنموذجية هي:
            [المبلغ الأجنبي] يساوي تقريباً [المبلغ بالعملة المحلية] [اسم العملة المحلية] (بناءً على سعر صرف تقريبي بقيمة [سعر الصرف]).

            يرجى التأكد من ترجمة كامل الرد ليكون بلغة المستخدم المفضلة ($selectedLanguage):
            - إذا كانت ($selectedLanguage) هي (en)، فقم بصياغة السلسلة والأسماء وعلامات الترقيم باللغة الإنجليزية كاملة.
            - إذا كانت ($selectedLanguage) هي (tr)، فقم بصياغة السلسلة والأسماء وعلامات الترقيم باللغة التركية كاملة.
            - إذا كانت ($selectedLanguage) هي (fr)، فقم بصياغة السلسلة والأسماء وعلامات الترقيم باللغة الفرنسية كاملة.
            - إذا كانت ($selectedLanguage) هي (hi)، فقم بصياغة السلسلة والأسماء باللغة الهندية كاملة.
            - إذا كانت ($selectedLanguage) هي (es)، فقم بصياغة السلسلة والأسماء باللغة الإسبانية كاملة.
            - إذا كانت ($selectedLanguage) هي (ar)، فقم بصياغة السلسلة والأسماء باللغة العربية كاملة.

            تحذير صارم: لا تبدأ الإجابة بأي مقدمات ترحيبية أو تعقيبات جانبية مبررة. فقط أجب بالصيغة مباشرة تماماً كما طُلِب.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = promptText)))),
            systemInstruction = Content(parts = listOf(Part(text = systemInstruction))),
            generationConfig = GenerationConfig(temperature = 0.2f)
        )

        try {
            val response = RetrofitClient.service.generateContent(keyToUse, request)
            val resultText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text?.trim()
                ?: "عذراً، لم أتمكن من استخلاص النتيجة."

            // Save history
            insertHistory(promptText, resultText)

            resultText
        } catch (e: Exception) {
            Log.e("CurrencyRepository", "Error from Gemini, falling back to local: ", e)
            val localResult = executeLocalFallbackParse(promptText, myCountryCurrency, selectedLanguage)
            insertHistory(promptText, localResult)
            localResult
        }
    }

    private fun executeLocalFallbackParse(
        promptText: String,
        myCountryCurrency: String,
        selectedLanguage: String
    ): String {
        val normalized = convertIndicToWestern(promptText)
        val cleanNumberStr = normalized.replace(",", "")
        val numberRegex = "(\\d+(?:\\.\\d+)?)\\s*".toRegex()
        val matchResult = numberRegex.find(cleanNumberStr)
        val amount = matchResult?.groupValues?.get(1)?.toDoubleOrNull() ?: 1.0
        val lowerText = normalized.lowercase()
        
        fun detectCurrency(text: String): String? {
            return when {
                text.contains("eur") || text.contains("يورو") || text.contains("€") -> "EUR"
                text.contains("gbp") || text.contains("إسترليني") || text.contains("£") -> "GBP"
                text.contains("sar") || text.contains("ريال") || text.contains("ر.س") -> "SAR"
                text.contains("iqd") || text.contains("دينار") || text.contains("عراقي") || text.contains("د.ع") -> "IQD"
                text.contains("egp") || text.contains("جنيه") || text.contains("مصري") || text.contains("ج.م") -> "EGP"
                text.contains("aed") || text.contains("درهم") || text.contains("إماراتي") || text.contains("د.إ") -> "AED"
                text.contains("kwd") || text.contains("كويتي") -> "KWD"
                text.contains("qar") || text.contains("قطري") -> "QAR"
                text.contains("jod") || text.contains("أردني") -> "JOD"
                text.contains("jpy") || text.contains("ياباني") || text.contains("ين") -> "JPY"
                text.contains("usd") || text.contains("دولار") || text.contains("$") -> "USD"
                else -> null
            }
        }
        
        val transitionIndex = when {
            normalized.contains(" الى ") -> normalized.indexOf(" الى ")
            normalized.contains(" إلى ") -> normalized.indexOf(" إلى ")
            normalized.contains(" to ") -> normalized.indexOf(" to ")
            normalized.contains(" in ") -> normalized.indexOf(" in ")
            else -> -1
        }
        
        val fromCurrency: String
        val toCurrency: String
        
        if (transitionIndex != -1) {
            val part1 = normalized.substring(0, transitionIndex)
            val part2 = normalized.substring(transitionIndex)
            fromCurrency = detectCurrency(part1) ?: detectCurrency(part2) ?: "USD"
            toCurrency = detectCurrency(part2) ?: myCountryCurrency
        } else {
            val detected = detectCurrency(lowerText)
            if (detected != null) {
                fromCurrency = detected
                val remainingText = lowerText.replace(detected.lowercase(), "").replace(currencyNamesAr[detected]?.lowercase() ?: "", "")
                toCurrency = detectCurrency(remainingText) ?: myCountryCurrency
            } else {
                fromCurrency = "USD"
                toCurrency = myCountryCurrency
            }
        }
        
        val targetToUse = if (fromCurrency == toCurrency) {
            if (fromCurrency == "USD") myCountryCurrency else "USD"
        } else {
            toCurrency
        }
        
        val rateFromUsd = offlineRates[fromCurrency] ?: 1.0
        val rateToUsd = offlineRates[targetToUse] ?: 1.0
        val amountInUsd = amount / rateFromUsd
        val convertedAmount = amountInUsd * rateToUsd
        val relativeRate = rateToUsd / rateFromUsd
        
        val formattedAmount = if (convertedAmount % 1.0 == 0.0) {
            convertedAmount.toLong().toString()
        } else {
            String.format(java.util.Locale.US, "%.2f", convertedAmount)
        }
        
        val formattedRate = if (relativeRate % 1.0 == 0.0 || relativeRate > 100) {
            relativeRate.toInt().toString()
        } else {
            String.format(java.util.Locale.US, "%.4f", relativeRate)
        }
        
        val fromName = when (selectedLanguage) {
            "ar" -> currencyNamesAr[fromCurrency] ?: fromCurrency
            else -> fromCurrency
        }
        val toName = when (selectedLanguage) {
            "ar" -> currencyNamesAr[targetToUse] ?: targetToUse
            else -> targetToUse
        }
        
        return when (selectedLanguage) {
            "ar" -> "$amount $fromName يساوي تقريباً $formattedAmount $toName (بناءً على سعر صرف تقريبي بقيمة $formattedRate)."
            "tr" -> "$amount $fromName yaklaşık olarak $formattedAmount $toName'ye eşittir (yaklaşık $formattedRate döviz kuruna göre)."
            "fr" -> "$amount $fromName équivaut environ à $formattedAmount $toName (basé sur un taux de change approximatif de $formattedRate)."
            "hi" -> "$amount $fromName लगभग $formattedAmount $toName के बराबर है (लगभग $formattedRate की विनिमय दर पर आधारित)।"
            "es" -> "$amount $fromName equivale aproximadamente a $formattedAmount $toName (según un tipo de cambio aproximado de $formattedRate)."
            else -> "$amount $fromName is approximately equal to $formattedAmount $toName (based on an approximate exchange rate of $formattedRate)."
        }
    }
    
    private fun convertIndicToWestern(input: String): String {
        var out = input
        val indicDigits = charArrayOf('٠', '١', '٢', '٣', '٤', '٥', '٦', '٧', '٨', '٩')
        for (i in 0..9) {
            out = out.replace(indicDigits[i], '0' + i)
        }
        return out
    }

    suspend fun insertHistory(prompt: String, result: String) {
        dao.insertHistoryItem(HistoryItem(prompt = prompt, result = result))
    }

    suspend fun deleteHistory(item: HistoryItem) {
        dao.deleteHistoryItem(item)
    }

    suspend fun clearHistory() {
        dao.clearAllHistory()
    }

    private fun getUserApiKey(userCustomKey: String?): String {
        if (!userCustomKey.isNullOrBlank()) {
            return userCustomKey
        }
        val buildConfigKey = BuildConfig.GEMINI_API_KEY
        if (buildConfigKey.isNotBlank() && buildConfigKey != "MY_GEMINI_API_KEY") {
            return buildConfigKey
        }
        return ""
    }
}
