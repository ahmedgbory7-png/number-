package com.example.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CurrencyRepository
import com.example.data.HistoryItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState {
    object Idle : UiState
    object Loading : UiState
    data class Success(val result: String) : UiState
    data class Error(val message: String) : UiState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CurrencyRepository(application)
    private val prefs = application.getSharedPreferences("app_settings", Context.MODE_PRIVATE)

    val offlineRates = repository.offlineRates
    val currencyNamesAr = repository.currencyNamesAr

    // Localization and My Country Preferences (Automatically Loaded and Saved)
    val selectedLanguage = MutableStateFlow(prefs.getString("selected_language", "ar") ?: "ar")
    val myCountryName = MutableStateFlow(prefs.getString("my_country_name", "العراق") ?: "العراق")
    val myCountryCurrency = MutableStateFlow(prefs.getString("my_country_currency", "IQD") ?: "IQD")

    // State for Smart Converter
    val promptText = MutableStateFlow("")
    
    private val _uiState = MutableStateFlow<UiState>(UiState.Idle)
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    // State for Offline Converter
    val offlineAmount = MutableStateFlow("1")
    val offlineFrom = MutableStateFlow("USD")
    val offlineTo = MutableStateFlow("SAR")
    
    private val _offlineResult = MutableStateFlow("")
    val offlineResult: StateFlow<String> = _offlineResult.asStateFlow()

    // State for Historical Rate Calculator
    val histAmount = MutableStateFlow("100")
    val histFromCurrency = MutableStateFlow("USD")
    val histToCurrency = MutableStateFlow("IQD")
    val histDay = MutableStateFlow("24")
    val histMonth = MutableStateFlow("05")
    val histYear = MutableStateFlow("2024")

    private val _historicalUiState = MutableStateFlow<UiState>(UiState.Idle)
    val historicalUiState: StateFlow<UiState> = _historicalUiState.asStateFlow()

    // Saved API key configuration
    private val _customApiKey = MutableStateFlow(prefs.getString("custom_api_key", "") ?: "")
    val customApiKey: StateFlow<String> = _customApiKey.asStateFlow()

    // History Flow from Room DB
    val history: StateFlow<List<HistoryItem>> = repository.historyFlow
         .stateIn(
             scope = viewModelScope,
             started = SharingStarted.WhileSubscribed(5000),
             initialValue = emptyList()
         )

    init {
        // Run initial offline conversion
        calculateOffline()
    }

    /**
     * Executes the smart natural-language conversion with Gemini, passing customization contexts.
     */
    fun startSmartConversion() {
        val currentPrompt = promptText.value.trim()
        if (currentPrompt.isEmpty()) return

        _uiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val apiKey = if (_customApiKey.value.isNotBlank()) _customApiKey.value else null
                val result = repository.performOnlineSmartQuery(
                    promptText = currentPrompt,
                    myCountryName = myCountryName.value,
                    myCountryCurrency = myCountryCurrency.value,
                    selectedLanguage = selectedLanguage.value,
                    userCustomKey = apiKey
                )
                
                if (result.startsWith("خطأ")) {
                    _uiState.value = UiState.Error(result)
                } else {
                    _uiState.value = UiState.Success(result)
                }
            } catch (e: Exception) {
                _uiState.value = UiState.Error("فشل الاتصال: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Executes the historical currency query utilizing selected date parameters.
     */
    fun startHistoricalConversion() {
        val amount = histAmount.value.trim()
        if (amount.isEmpty() || amount.toDoubleOrNull() == null) {
            _historicalUiState.value = UiState.Error("يرجى إدخال مبلغ رقمي صحيح.")
            return
        }

        val prompt = "حساب تاريخي: كم كان سعر الصرف في التاريخ ${histYear.value}-${histMonth.value}-${histDay.value} للمبلغ $amount من العملة ${histFromCurrency.value} إلى العملة ${histToCurrency.value}؟"
        
        _historicalUiState.value = UiState.Loading
        viewModelScope.launch {
            try {
                val apiKey = if (_customApiKey.value.isNotBlank()) _customApiKey.value else null
                val result = repository.performOnlineSmartQuery(
                    promptText = prompt,
                    myCountryName = myCountryName.value,
                    myCountryCurrency = myCountryCurrency.value,
                    selectedLanguage = selectedLanguage.value,
                    userCustomKey = apiKey
                )
                
                if (result.startsWith("خطأ")) {
                    _historicalUiState.value = UiState.Error(result)
                } else {
                    _historicalUiState.value = UiState.Success(result)
                }
            } catch (e: Exception) {
                _historicalUiState.value = UiState.Error("فشل الاتصال: ${e.localizedMessage}")
            }
        }
    }

    /**
     * Change and auto-save the user language preference.
     */
    fun saveLanguage(langCode: String) {
        prefs.edit().putString("selected_language", langCode).apply()
        selectedLanguage.value = langCode
    }

    /**
     * Change and auto-save "My Country" preference setting.
     */
    fun saveMyCountry(countryName: String, currencyCode: String) {
        prefs.edit().putString("my_country_name", countryName).apply()
        prefs.edit().putString("my_country_currency", currencyCode).apply()
        myCountryName.value = countryName
        myCountryCurrency.value = currencyCode
    }

    /**
     * Executes the offline quick conversion.
     */
    fun calculateOffline() {
        val amountStr = offlineAmount.value.trim()
        val amount = amountStr.toDoubleOrNull()
        if (amount == null || amount <= 0.0) {
            _offlineResult.value = "يرجى إدخال مبلغ صحيح."
            return
        }
        val resultText = repository.performOfflineConversion(
            amount = amount,
            from = offlineFrom.value,
            to = offlineTo.value
        )
        _offlineResult.value = resultText
    }

    /**
     * Save/Update API key in SharedPreferences
     */
    fun saveCustomApiKey(key: String) {
        prefs.edit().putString("custom_api_key", key).apply()
        _customApiKey.value = key
    }

    fun deleteHistoryItem(item: HistoryItem) {
        viewModelScope.launch {
            repository.deleteHistory(item)
        }
    }

    fun clearAllHistory() {
        viewModelScope.launch {
            repository.clearHistory()
        }
    }

    fun resetSmartState() {
        _uiState.value = UiState.Idle
    }

    fun resetHistoricalState() {
        _historicalUiState.value = UiState.Idle
    }
}
