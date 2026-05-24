package com.example.ui

import android.content.Intent
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.HistoryItem
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

data class ParsedResult(
    val mainAmount: String,
    val rateText: String,
    val detailsText: String
)

/**
 * Parses response text to extract the calculated value and approximate rate 
 * for the premium high-density display style.
 */
fun parseResultText(text: String): ParsedResult {
    try {
        val equalsIndex = text.indexOf("يساوي تقريباً")
        val parenIndex = text.indexOf("(")
        
        val mainAmount = if (equalsIndex != -1) {
            val end = if (parenIndex != -1 && parenIndex > equalsIndex) parenIndex else text.length
            text.substring(equalsIndex + "يساوي تقريباً".length, end).trim()
        } else {
            text
        }
        
        val rateText = if (parenIndex != -1) {
            val endParen = text.indexOf(")", parenIndex)
            val sub = if (endParen != -1) text.substring(parenIndex + 1, endParen) else text.substring(parenIndex + 1)
            sub.replace("بناءً على سعر صرف تقريبي بقيمة", "").trim()
        } else {
            "١ دولار"
        }
        
        return ParsedResult(
            mainAmount = mainAmount,
            rateText = "سعر الصرف: $rateText",
            detailsText = text
        )
    } catch (e: Exception) {
        return ParsedResult("تحويل ذكي", "سعر الصرف الشامل بدقة", text)
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val clipboardManager = LocalClipboardManager.current

    val promptValue by viewModel.promptText.collectAsState()
    val uiState by viewModel.uiState.collectAsState()
    val customKey by viewModel.customApiKey.collectAsState()

    val offlineAmountValue by viewModel.offlineAmount.collectAsState()
    val offlineFromVal by viewModel.offlineFrom.collectAsState()
    val offlineToVal by viewModel.offlineTo.collectAsState()
    val offlineResultVal by viewModel.offlineResult.collectAsState()

    // Preferences and Smart Country Settings
    val selectedLanguage by viewModel.selectedLanguage.collectAsState()
    val myCountryName by viewModel.myCountryName.collectAsState()
    val myCountryCurrency by viewModel.myCountryCurrency.collectAsState()

    // Historical Conversion Configurations
    val histAmountValue by viewModel.histAmount.collectAsState()
    val histFromCurrencyValue by viewModel.histFromCurrency.collectAsState()
    val histToCurrencyValue by viewModel.histToCurrency.collectAsState()
    val histDayValue by viewModel.histDay.collectAsState()
    val histMonthValue by viewModel.histMonth.collectAsState()
    val histYearValue by viewModel.histYear.collectAsState()
    val historicalUiState by viewModel.historicalUiState.collectAsState()

    val historyItems by viewModel.history.collectAsState()

    var showKeyDialog by remember { mutableStateOf(false) }
    var keyInputText by remember { mutableStateOf(customKey) }
    
    // Country and Language Picker Trigger States
    var showCountryDialog by remember { mutableStateOf(false) }
    var showLangDialog by remember { mutableStateOf(false) }

    // Localization mapping inline helper
    val t = { key: String -> Locales.t(key, selectedLanguage) }

    // Tab integration: 0 for Calculator, 1 for Destinations, 2 for 2026 Rates Guide
    var activeTab by remember { mutableStateOf(0) }

    LaunchedEffect(showKeyDialog) {
        if (showKeyDialog) {
            keyInputText = customKey
        }
    }

    val suggestions = listOf(
        "2.5$ بالدينار العراقي",
        "15 يورو بالريال السعودي",
        "120 دولار بالجنيه المصري",
        "500 درهم إماراتي بالدينار الكويتي"
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(HighDensityBg),
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(modifier = Modifier.background(HighDensitySurface)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // M3 Style Avatar and App Headers (Localized)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(HighDensityContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TravelExplore,
                                contentDescription = null,
                                tint = HighDensityOnContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = t("app_title"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = HighDensityOnContainer
                            )
                            Text(
                                text = t("app_subtitle_full"),
                                fontSize = 11.sp,
                                color = HighDensityMutedText
                            )
                        }
                    }

                    // Gone or empty spacer to preserve perfect Material 3 proportions
                    Spacer(modifier = Modifier.width(40.dp))
                }

                // SECOND ROW: "MY COUNTRY" AND "APP LANGUAGE" SELECTION CHIPS
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val currentLangName = Locales.languages.firstOrNull { it.first == selectedLanguage }?.second?.split(" ")?.firstOrNull() ?: selectedLanguage
                    
                    // My Country Selection Button (Sets target currency automatically)
                    Button(
                        onClick = { showCountryDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HighDensityContainer,
                            contentColor = HighDensityOnContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("my_country_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = HighDensityPrimary
                            )
                            Text(
                                text = "${t("my_country")}: $myCountryName ($myCountryCurrency)",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // App Language Button (Localizes translation output automatically)
                    Button(
                        onClick = { showLangDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = HighDensityContainer,
                            contentColor = HighDensityOnContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("language_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Translate,
                                contentDescription = null,
                                modifier = Modifier.size(15.dp),
                                tint = HighDensityPrimary
                            )
                            Text(
                                text = "${t("language")}: $currentLangName",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                HorizontalDivider(color = HighDensityBorder, thickness = 1.dp)
            }
        },
        bottomBar = {
            // High Density custom Bottom Navigation element complying with the design specs
            NavigationBar(
                containerColor = HighDensityBorder.copy(alpha = 0.4f),
                tonalElevation = 0.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Calculate, contentDescription = t("nav_calc")) },
                    label = { Text(t("nav_calc"), fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = HighDensityOnContainer,
                        selectedTextColor = HighDensityOnContainer,
                        indicatorColor = HighDensityContainer,
                        unselectedIconColor = HighDensityMutedText,
                        unselectedTextColor = HighDensityMutedText
                    )
                )

                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { Icon(Icons.Default.Map, contentDescription = t("nav_dest")) },
                    label = { Text(t("nav_dest"), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = HighDensityOnContainer,
                        selectedTextColor = HighDensityOnContainer,
                        indicatorColor = HighDensityContainer,
                        unselectedIconColor = HighDensityMutedText,
                        unselectedTextColor = HighDensityMutedText
                    )
                )

                NavigationBarItem(
                    selected = activeTab == 2,
                    onClick = { activeTab = 2 },
                    icon = { Icon(Icons.Default.Paid, contentDescription = t("nav_rates")) },
                    label = { Text(t("nav_rates"), fontSize = 11.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = HighDensityOnContainer,
                        selectedTextColor = HighDensityOnContainer,
                        indicatorColor = HighDensityContainer,
                        unselectedIconColor = HighDensityMutedText,
                        unselectedTextColor = HighDensityMutedText
                    )
                )
            }
        }
    ) { paddingValues ->
        Crossfade(
            targetState = activeTab,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) { tabIndex ->
            when (tabIndex) {
                0 -> {
                    // PRIMARY ACTIVE TAB: Interactive Smart Calculator with Full Setup
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                    ) {
                        // Query Input Card Area (Section 1 Card)
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp)),
                                colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = t("enter_foreign_amount"),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = HighDensityPrimary
                                    )

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(HighDensityBg)
                                            .padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Search,
                                            contentDescription = null,
                                            tint = HighDensityMutedText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        TextField(
                                            value = promptValue,
                                            onValueChange = { viewModel.promptText.value = it },
                                            placeholder = {
                                                Text(
                                                    t("prompt_placeholder"),
                                                    fontSize = 13.sp,
                                                    color = HighDensityMutedText.copy(alpha = 0.6f)
                                                )
                                            },
                                            modifier = Modifier
                                                .weight(1f)
                                                .testTag("prompt_input"),
                                            colors = TextFieldDefaults.colors(
                                                focusedContainerColor = Color.Transparent,
                                                unfocusedContainerColor = Color.Transparent,
                                                focusedTextColor = HighDensityOnSurface,
                                                unfocusedTextColor = HighDensityOnSurface,
                                                focusedIndicatorColor = Color.Transparent,
                                                unfocusedIndicatorColor = Color.Transparent,
                                                disabledIndicatorColor = Color.Transparent
                                            ),
                                            textStyle = LocalTextStyle.current.copy(
                                                fontSize = 13.sp,
                                                textDirection = if (selectedLanguage == "ar") TextDirection.Rtl else TextDirection.Ltr,
                                                fontWeight = FontWeight.Medium
                                            ),
                                            singleLine = true,
                                            keyboardOptions = KeyboardOptions(
                                                keyboardType = KeyboardType.Text,
                                                imeAction = ImeAction.Search
                                            ),
                                            keyboardActions = KeyboardActions(
                                                onSearch = {
                                                    focusManager.clearFocus()
                                                    viewModel.startSmartConversion()
                                                }
                                            )
                                        )
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = t("quick_suggestions"),
                                            fontSize = 10.sp,
                                            color = HighDensityMutedText
                                        )

                                        Button(
                                            onClick = {
                                                focusManager.clearFocus()
                                                viewModel.startSmartConversion()
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = HighDensityPrimary,
                                                contentColor = HighDensityOnPrimary
                                            ),
                                            shape = RoundedCornerShape(12.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 4.dp),
                                            modifier = Modifier
                                                .testTag("calculate_button")
                                                .height(34.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.AutoAwesome,
                                                    contentDescription = null,
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Text(t("calculate"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                            }
                                        }
                                    }

                                    // Horizontally scrollable suggestions flow to keep interface neat
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        suggestions.forEachIndexed { idx, txt ->
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(10.dp))
                                                    .background(HighDensityBg)
                                                    .border(BorderStroke(1.dp, HighDensityBorder), RoundedCornerShape(10.dp))
                                                    .clickable {
                                                        viewModel.promptText.value = txt
                                                        viewModel.startSmartConversion()
                                                    }
                                                    .padding(horizontal = 6.dp, vertical = 6.dp)
                                                    .testTag("suggestion_chip_$idx"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = txt,
                                                    fontSize = 10.sp,
                                                    color = HighDensityOnSurface,
                                                    textAlign = TextAlign.Center,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Smart AI Response Section - Custom Capsule formatting
                        item {
                            AnimatedVisibility(
                                visible = (uiState != UiState.Idle),
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                Crossfade(targetState = uiState) { state ->
                                    when (state) {
                                        is UiState.Loading -> {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp)),
                                                colors = CardDefaults.cardColors(containerColor = HighDensitySurface)
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .padding(20.dp),
                                                    horizontalArrangement = Arrangement.Center,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    CircularProgressIndicator(
                                                        color = HighDensityPrimary,
                                                        modifier = Modifier.size(24.dp),
                                                        strokeWidth = 2.5.dp
                                                    )
                                                    Spacer(modifier = Modifier.width(12.dp))
                                                    Text(
                                                        t("calculating"),
                                                        fontSize = 12.sp,
                                                        color = HighDensityOnSurface
                                                    )
                                                }
                                            }
                                        }
                                        is UiState.Success -> {
                                            val parsed = parseResultText(state.result)
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .testTag("result_card")
                                                    .border(1.dp, HighDensityBorder, RoundedCornerShape(32.dp)),
                                                colors = CardDefaults.cardColors(containerColor = HighDensityContainer),
                                                shape = RoundedCornerShape(32.dp)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(24.dp),
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Text(
                                                        text = t("approx_result"),
                                                        fontSize = 12.sp,
                                                        color = HighDensityOnContainer.copy(alpha = 0.7f),
                                                        fontWeight = FontWeight.Medium
                                                    )

                                                    Text(
                                                        text = parsed.mainAmount,
                                                        fontSize = 32.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = HighDensityOnContainer,
                                                        textAlign = TextAlign.Center
                                                    )

                                                    Box(
                                                        modifier = Modifier
                                                            .clip(CircleShape)
                                                            .background(Color.White.copy(alpha = 0.4f))
                                                            .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                                                            .padding(horizontal = 16.dp, vertical = 4.dp)
                                                    ) {
                                                        Text(
                                                            text = parsed.rateText,
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = HighDensityOnContainer
                                                        )
                                                    }

                                                    Text(
                                                        text = parsed.detailsText,
                                                        fontSize = 12.sp,
                                                        color = HighDensityOnContainer.copy(alpha = 0.9f),
                                                        textAlign = TextAlign.Center,
                                                        lineHeight = 18.sp,
                                                        modifier = Modifier.padding(top = 8.dp)
                                                    )

                                                    HorizontalDivider(
                                                        color = HighDensityOnContainer.copy(alpha = 0.15f),
                                                        thickness = 1.dp,
                                                        modifier = Modifier.padding(vertical = 4.dp)
                                                    )

                                                    Row(
                                                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        IconButton(
                                                            onClick = {
                                                                clipboardManager.setText(AnnotatedString(state.result))
                                                                Toast.makeText(context, t("copied"), Toast.LENGTH_SHORT).show()
                                                            },
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(CircleShape)
                                                                .background(Color.White.copy(alpha = 0.3f))
                                                                .testTag("copy_button")
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.ContentCopy,
                                                                contentDescription = t("copy"),
                                                                tint = HighDensityOnContainer,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }

                                                        IconButton(
                                                            onClick = {
                                                                val shareIntent = Intent().apply {
                                                                    action = Intent.ACTION_SEND
                                                                    putExtra(Intent.EXTRA_TEXT, state.result)
                                                                    type = "text/plain"
                                                                }
                                                                context.startActivity(Intent.createChooser(shareIntent, t("approx_result")))
                                                            },
                                                            modifier = Modifier
                                                                .size(36.dp)
                                                                .clip(CircleShape)
                                                                .background(Color.White.copy(alpha = 0.3f))
                                                        ) {
                                                            Icon(
                                                                imageVector = Icons.Default.Share,
                                                                contentDescription = t("share"),
                                                                tint = HighDensityOnContainer,
                                                                modifier = Modifier.size(16.dp)
                                                            )
                                                        }

                                                        TextButton(
                                                            onClick = { viewModel.resetSmartState() }
                                                        ) {
                                                            Text(
                                                                t("close"),
                                                                color = HighDensityOnContainer,
                                                                fontWeight = FontWeight.Bold,
                                                                fontSize = 12.sp
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                        is UiState.Error -> {
                                            Card(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp)),
                                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                            ) {
                                                Column(
                                                    modifier = Modifier.padding(16.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                                        Icon(
                                                            imageVector = Icons.Default.Error,
                                                            contentDescription = null,
                                                            tint = MaterialTheme.colorScheme.error,
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(6.dp))
                                                        Text(
                                                            "خطأ في الاتصال",
                                                            color = MaterialTheme.colorScheme.error,
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 13.sp
                                                        )
                                                    }
                                                    Text(
                                                        text = state.message,
                                                        fontSize = 11.sp,
                                                        color = MaterialTheme.colorScheme.onErrorContainer
                                                    )
                                                    TextButton(onClick = { viewModel.resetSmartState() }) {
                                                        Text("حسناً", color = HighDensityPrimary, fontSize = 12.sp)
                                                    }
                                                }
                                            }
                                        }
                                        else -> {}
                                    }
                                }
                            }
                        }

                        // Beautiful Custom Bento Grid Quick Stats
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(112.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Bento item A: Pink Background, Purple text
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    colors = CardDefaults.cardColors(containerColor = BentoPink),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.TrendingUp,
                                            contentDescription = null,
                                            tint = BentoPinkText,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column {
                                            Text(
                                                text = t("trending_title"),
                                                fontSize = 11.sp,
                                                color = BentoPinkText.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = t("trending_desc"),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BentoPinkText
                                            )
                                        }
                                    }
                                }

                                // Bento item B: Green Background, Green text
                                Card(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight(),
                                    colors = CardDefaults.cardColors(containerColor = BentoGreen),
                                    shape = RoundedCornerShape(24.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .padding(16.dp),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Update,
                                            contentDescription = null,
                                            tint = BentoGreenText,
                                            modifier = Modifier.size(24.dp)
                                        )
                                        Column {
                                            Text(
                                                text = t("update_title"),
                                                fontSize = 11.sp,
                                                color = BentoGreenText.copy(alpha = 0.7f)
                                            )
                                            Text(
                                                text = t("update_desc"),
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BentoGreenText
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Speedy Offline Quick Converter
                        item {
                            Text(
                                text = t("offline_title"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = HighDensityPrimary,
                                modifier = Modifier.padding(start = 2.dp, top = 6.dp)
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp)),
                                colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    OutlinedTextField(
                                        value = offlineAmountValue,
                                        onValueChange = {
                                            viewModel.offlineAmount.value = it
                                            viewModel.calculateOffline()
                                        },
                                        placeholder = { Text(t("offline_amount_placeholder")) },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal,
                                            imeAction = ImeAction.Done
                                        ),
                                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = HighDensityOnSurface,
                                            unfocusedTextColor = HighDensityOnSurface,
                                            focusedBorderColor = HighDensityPrimary,
                                            unfocusedBorderColor = HighDensityBorder
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("offline_amount_input"),
                                        shape = RoundedCornerShape(12.dp),
                                        textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                t("from_currency"),
                                                fontSize = 11.sp,
                                                color = HighDensityMutedText,
                                                modifier = Modifier.padding(bottom = 3.dp)
                                            )
                                            CustomCurrencySpinner(
                                                selected = offlineFromVal,
                                                onSelected = {
                                                    viewModel.offlineFrom.value = it
                                                    viewModel.calculateOffline()
                                                },
                                                currencies = viewModel.offlineRates.keys.toList(),
                                                currencyNames = viewModel.currencyNamesAr
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                t("to_currency"),
                                                fontSize = 11.sp,
                                                color = HighDensityMutedText,
                                                modifier = Modifier.padding(bottom = 3.dp)
                                            )
                                            CustomCurrencySpinner(
                                                selected = offlineToVal,
                                                onSelected = {
                                                    viewModel.offlineTo.value = it
                                                    viewModel.calculateOffline()
                                                },
                                                currencies = viewModel.offlineRates.keys.toList(),
                                                currencyNames = viewModel.currencyNamesAr
                                            )
                                        }
                                    }

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(HighDensityBg)
                                            .border(1.dp, HighDensityBorder, RoundedCornerShape(16.dp))
                                            .padding(14.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(t("offline_result_title"), fontSize = 11.sp, color = HighDensityPrimary, fontWeight = FontWeight.Bold)
                                            Text(
                                                text = offlineResultVal,
                                                fontSize = 13.sp,
                                                color = HighDensityOnSurface,
                                                fontWeight = FontWeight.Medium,
                                                lineHeight = 18.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // Historical Exchange Rate Calculator Section
                        item {
                            Text(
                                text = t("historical_rates_title"),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = HighDensityPrimary,
                                modifier = Modifier.padding(start = 2.dp, top = 8.dp)
                            )
                        }

                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp)),
                                colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    Text(
                                        text = t("historical_rates_desc"),
                                        fontSize = 11.sp,
                                        color = HighDensityMutedText
                                    )

                                    // Amount to Convert input text field
                                    OutlinedTextField(
                                        value = histAmountValue,
                                        onValueChange = { viewModel.histAmount.value = it },
                                        placeholder = { Text(t("offline_amount_placeholder")) },
                                        label = { Text(t("amount"), fontSize = 11.sp) },
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Decimal,
                                            imeAction = ImeAction.Next
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = HighDensityOnSurface,
                                            unfocusedTextColor = HighDensityOnSurface,
                                            focusedBorderColor = HighDensityPrimary,
                                            unfocusedBorderColor = HighDensityBorder
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("historical_amount_input"),
                                        shape = RoundedCornerShape(12.dp)
                                    )

                                    // 3-input Date Selector row: Day, Month, Year
                                    Text(
                                        text = t("historical_date"),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = HighDensityPrimary
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedTextField(
                                            value = histDayValue,
                                            onValueChange = { if (it.length <= 2) viewModel.histDay.value = it },
                                            placeholder = { Text("DD") },
                                            label = { Text(t("day"), fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = HighDensityOnSurface,
                                                unfocusedTextColor = HighDensityOnSurface,
                                                focusedBorderColor = HighDensityPrimary,
                                                unfocusedBorderColor = HighDensityBorder
                                            ),
                                            modifier = Modifier.weight(1f).testTag("hist_day_input"),
                                            shape = RoundedCornerShape(10.dp)
                                        )

                                        OutlinedTextField(
                                            value = histMonthValue,
                                            onValueChange = { if (it.length <= 2) viewModel.histMonth.value = it },
                                            placeholder = { Text("MM") },
                                            label = { Text(t("month"), fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = HighDensityOnSurface,
                                                unfocusedTextColor = HighDensityOnSurface,
                                                focusedBorderColor = HighDensityPrimary,
                                                unfocusedBorderColor = HighDensityBorder
                                            ),
                                            modifier = Modifier.weight(1f).testTag("hist_month_input"),
                                            shape = RoundedCornerShape(10.dp)
                                        )

                                        OutlinedTextField(
                                            value = histYearValue,
                                            onValueChange = { if (it.length <= 4) viewModel.histYear.value = it },
                                            placeholder = { Text("YYYY") },
                                            label = { Text(t("year"), fontSize = 10.sp) },
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedTextColor = HighDensityOnSurface,
                                                unfocusedTextColor = HighDensityOnSurface,
                                                focusedBorderColor = HighDensityPrimary,
                                                unfocusedBorderColor = HighDensityBorder
                                            ),
                                            modifier = Modifier.weight(1.2f).testTag("hist_year_input"),
                                            shape = RoundedCornerShape(10.dp)
                                        )
                                    }

                                    // From and To Currency Row using Spinners
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                t("from_currency"),
                                                fontSize = 11.sp,
                                                color = HighDensityMutedText,
                                                modifier = Modifier.padding(bottom = 3.dp)
                                            )
                                            CustomCurrencySpinner(
                                                selected = histFromCurrencyValue,
                                                onSelected = { viewModel.histFromCurrency.value = it },
                                                currencies = viewModel.offlineRates.keys.toList(),
                                                currencyNames = viewModel.currencyNamesAr
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                t("to_currency"),
                                                fontSize = 11.sp,
                                                color = HighDensityMutedText,
                                                modifier = Modifier.padding(bottom = 3.dp)
                                            )
                                            CustomCurrencySpinner(
                                                selected = histToCurrencyValue,
                                                onSelected = { viewModel.histToCurrency.value = it },
                                                currencies = viewModel.offlineRates.keys.toList(),
                                                currencyNames = viewModel.currencyNamesAr
                                            )
                                        }
                                    }

                                    // Action calculate trigger
                                    Button(
                                        onClick = {
                                            focusManager.clearFocus()
                                            viewModel.startHistoricalConversion()
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = HighDensityPrimary,
                                            contentColor = HighDensityOnPrimary
                                        ),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth().testTag("calculate_historical_button")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Event,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(t("historical_calculate"), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                        }
                                    }

                                    // Historical states visibility
                                    AnimatedVisibility(
                                        visible = (historicalUiState != UiState.Idle),
                                        enter = fadeIn() + expandVertically(),
                                        exit = fadeOut() + shrinkVertically()
                                    ) {
                                        Crossfade(targetState = historicalUiState) { state ->
                                            when (state) {
                                                is UiState.Loading -> {
                                                    Row(
                                                        modifier = Modifier.fillMaxWidth().padding(8.dp),
                                                        horizontalArrangement = Arrangement.Center,
                                                        verticalAlignment = Alignment.CenterVertically
                                                    ) {
                                                        CircularProgressIndicator(
                                                            color = HighDensityPrimary,
                                                            modifier = Modifier.size(20.dp),
                                                            strokeWidth = 2.dp
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(t("calculating"), fontSize = 12.sp, color = HighDensityOnSurface)
                                                    }
                                                }
                                                is UiState.Success -> {
                                                    Card(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .border(1.dp, HighDensityBorder, RoundedCornerShape(16.dp)),
                                                        colors = CardDefaults.cardColors(containerColor = HighDensityContainer),
                                                        shape = RoundedCornerShape(16.dp)
                                                    ) {
                                                        Column(
                                                            modifier = Modifier.padding(12.dp),
                                                            verticalArrangement = Arrangement.spacedBy(6.dp)
                                                        ) {
                                                            Text(
                                                                text = state.result,
                                                                fontSize = 12.sp,
                                                                color = HighDensityOnContainer,
                                                                lineHeight = 16.sp
                                                            )

                                                            Row(
                                                                modifier = Modifier.fillMaxWidth(),
                                                                horizontalArrangement = Arrangement.End,
                                                                verticalAlignment = Alignment.CenterVertically
                                                            ) {
                                                                IconButton(
                                                                    onClick = {
                                                                        clipboardManager.setText(AnnotatedString(state.result))
                                                                        Toast.makeText(context, t("copied"), Toast.LENGTH_SHORT).show()
                                                                    },
                                                                    modifier = Modifier.size(30.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.ContentCopy,
                                                                        contentDescription = t("copy"),
                                                                        tint = HighDensityOnContainer,
                                                                        modifier = Modifier.size(14.dp)
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                IconButton(
                                                                    onClick = {
                                                                        val shareIntent = Intent().apply {
                                                                            action = Intent.ACTION_SEND
                                                                            putExtra(Intent.EXTRA_TEXT, state.result)
                                                                            type = "text/plain"
                                                                        }
                                                                        context.startActivity(Intent.createChooser(shareIntent, t("approx_result")))
                                                                    },
                                                                    modifier = Modifier.size(30.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.Share,
                                                                        contentDescription = t("share"),
                                                                        tint = HighDensityOnContainer,
                                                                        modifier = Modifier.size(14.dp)
                                                                    )
                                                                }
                                                                Spacer(modifier = Modifier.width(6.dp))
                                                                TextButton(
                                                                    onClick = { viewModel.resetHistoricalState() },
                                                                    contentPadding = PaddingValues(0.dp),
                                                                    modifier = Modifier.height(26.dp)
                                                                ) {
                                                                    Text(t("close"), color = HighDensityPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                                                }
                                                            }
                                                        }
                                                    }
                                                }
                                                is UiState.Error -> {
                                                    Card(
                                                        modifier = Modifier.fillMaxWidth(),
                                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                                                    ) {
                                                        Column(modifier = Modifier.padding(10.dp)) {
                                                            Text(state.message, color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                                            TextButton(onClick = { viewModel.resetHistoricalState() }) {
                                                                Text(t("ok"), color = MaterialTheme.colorScheme.error, fontSize = 11.sp)
                                                            }
                                                        }
                                                    }
                                                }
                                                else -> {}
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Conversions History Section
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "📜 سجل العمليات بالذكاء الاصطناعي",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = HighDensityPrimary,
                                    modifier = Modifier.padding(start = 2.dp)
                                )

                                if (historyItems.isNotEmpty()) {
                                    TextButton(
                                        onClick = { viewModel.clearAllHistory() },
                                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.DeleteSweep, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text("مسح السجل", fontSize = 11.sp)
                                        }
                                    }
                                }
                            }
                        }

                        if (historyItems.isEmpty()) {
                            item {
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, HighDensityBorder, RoundedCornerShape(16.dp)),
                                    colors = CardDefaults.cardColors(containerColor = HighDensitySurface)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(24.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.HistoryEdu,
                                            contentDescription = null,
                                            tint = HighDensityMutedText.copy(alpha = 0.3f),
                                            modifier = Modifier.size(40.dp)
                                        )
                                        Text(
                                            text = "لا توجد عمليات سابقة محفوظة لرحلتك حتى الآن.",
                                            fontSize = 12.sp,
                                            color = HighDensityMutedText,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        } else {
                            items(historyItems, key = { it.id }) { item ->
                                HistoryCard(
                                    item = item,
                                    onDelete = { viewModel.deleteHistoryItem(item) },
                                    onCopy = {
                                        clipboardManager.setText(AnnotatedString(item.result))
                                        Toast.makeText(context, "تم نسخ النتيجة بنجاح!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 1: Destinations Bento Card Exploration Panel
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                    ) {
                        item {
                            Text(
                                text = t("destinations_title"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityPrimary
                            )
                        }

                        val dests = listOf(
                            Triple("المملكة العربية السعودية 🇸🇦", "الريال السعودي (SAR)", "الأكثر زيارة لرحلات ومناسك العمرة والحج، سعر الصرف مستقر عند 3.75 ريال لكل دولار."),
                            Triple("جمهورية العراق 🇮🇶", "الدينار العراقي (IQD)", "وجهة تجارية وسياحية تاريخية، سعر الصرف التقريبي هو 1450 دينار لكل دولار."),
                            Triple("جمهورية مصر العربية 🇪🇬", "الجنيه المصري (EGP)", "الحضارة والثقافة والسياحة الترفيهية، بمعدل صرف 48 جنيه لكل دولار."),
                            Triple("دولة الإمارات العربية المتحدة 🇦🇪", "الدرهم الإماراتي (AED)", "العاصمة الاقتصادية والسياحية، بمعدل صرف ثابت قدره 3.67 درهم لكل دولار."),
                            Triple("دولة الكويت 🇰🇼", "الدينار الكويتي (KWD)", "الأعلى قيمة عالمياً، للرحلات العملية والتجارية بنسبة صرف 0.31 دينار لكل دولار."),
                            Triple("الاتحاد الأوروبي 🇪🇺", "اليورو (EUR)", "دليل السعر لرحلات أوروبا الثقافية، بمعدل صرف تقريبي 0.93 يورو لكل دولار.")
                        )

                        items(dests) { (title, cur, desc) ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, HighDensityBorder, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = HighDensitySurface)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(CircleShape)
                                            .background(HighDensityContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Map,
                                            contentDescription = null,
                                            tint = HighDensityOnContainer,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = HighDensityOnSurface)
                                        Text(cur, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = HighDensityPrimary)
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(desc, fontSize = 11.sp, color = HighDensityMutedText, lineHeight = 16.sp)
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: Guide for Rates
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                    ) {
                        item {
                            Text(
                                text = t("rates_guide_title"),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = HighDensityPrimary
                            )
                        }

                        val ratesGuide = listOf(
                            "USD (دولار أمريكي)" to "1.00",
                            "SAR (ريال سعودي)" to "3.75",
                            "IQD (دينار عراقي)" to "1450.00",
                            "EGP (جنيه مصري)" to "48.00",
                            "AED (درهم إماراتي)" to "3.67",
                            "KWD (دينار كويتي)" to "0.31",
                            "QAR (ريال قطري)" to "3.64",
                            "JOD (دينار أردني)" to "0.71",
                            "EUR (يورو)" to "0.93",
                            "GBP (جنيه إسترليني)" to "0.80",
                            "JPY (ين ياباني)" to "155.00"
                        )

                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, HighDensityBorder, RoundedCornerShape(20.dp)),
                                colors = CardDefaults.cardColors(containerColor = HighDensitySurface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(HighDensityBg)
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = if (selectedLanguage == "ar") "رمز العملة والاسم" else if (selectedLanguage == "tr") "Para Birimi ve Simge" else if (selectedLanguage == "es") "Símbolo y nombre" else if (selectedLanguage == "fr") "Symbole et nom" else "Currency Symbol & Name",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = HighDensityOnSurface
                                        )
                                        Text(
                                            text = if (selectedLanguage == "ar") "القيمة مقابل 1 دولار" else if (selectedLanguage == "tr") "1 USD Karşılığı" else if (selectedLanguage == "es") "Valor por 1 USD" else if (selectedLanguage == "fr") "Valeur vs 1 USD" else "Value vs 1 USD",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = HighDensityOnSurface
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    ratesGuide.forEach { (cur, valStr) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(8.dp)
                                                        .clip(CircleShape)
                                                        .background(HighDensityPrimary)
                                                )
                                                Text(cur, fontSize = 13.sp, color = HighDensityOnSurface)
                                            }
                                            Text(valStr, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = HighDensityPrimary)
                                        }
                                        HorizontalDivider(color = HighDensityBg, thickness = 1.dp)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Key configuration dialog
    if (showKeyDialog) {
        Dialog(
            onDismissRequest = { showKeyDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.VpnKey,
                            contentDescription = null,
                            tint = HighDensityPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            text = "إعدادات مفتاح API للمسافر",
                            color = HighDensityOnSurface,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "يتم استخدام مفتاح التشفير هذا لتوصيلك ذكياً بشبكة Gemini API وإتمام عمليات التحويل بدقة.",
                        fontSize = 12.sp,
                        color = HighDensityMutedText,
                        lineHeight = 18.sp
                    )

                    OutlinedTextField(
                        value = keyInputText,
                        onValueChange = { keyInputText = it },
                        placeholder = { Text("أدخل مفتاح الـ API هنا...", fontSize = 12.sp, color = HighDensityMutedText.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = HighDensityOnSurface,
                            unfocusedTextColor = HighDensityOnSurface,
                            focusedBorderColor = HighDensityPrimary,
                            unfocusedBorderColor = HighDensityBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr)
                    )

                    Row(
                        horizontalArrangement = Arrangement.End,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { showKeyDialog = false }) {
                            Text("إلغاء", color = HighDensityMutedText, fontSize = 13.sp)
                        }
                        
                        Button(
                            onClick = {
                                viewModel.saveCustomApiKey(keyInputText)
                                showKeyDialog = false
                                Toast.makeText(context, "تم حفظ المفتاح بنجاح محلياً!", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = HighDensityPrimary,
                                contentColor = HighDensityOnPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("حفظ المفتاح", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (showCountryDialog) {
        Dialog(
            onDismissRequest = { showCountryDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .fillMaxHeight(0.7f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = t("select_my_country"),
                        color = HighDensityOnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    var searchQuery by remember { mutableStateOf("") }
                    
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text(if (selectedLanguage == "ar") "البحث عن بلد أو عملة..." else "Search country or currency...", fontSize = 12.sp, color = HighDensityMutedText.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = HighDensityOnSurface,
                            unfocusedTextColor = HighDensityOnSurface,
                            focusedBorderColor = HighDensityPrimary,
                            unfocusedBorderColor = HighDensityBorder
                        ),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true
                    )

                    val countriesList = listOf(
                        Triple("جمهورية العراق 🇮🇶", "IQD", "الدينار العراقي"),
                        Triple("جمهورية مصر العربية 🇪🇬", "EGP", "الجنيه المصري"),
                        Triple("المملكة العربية السعودية 🇸🇦", "SAR", "الريال السعودي"),
                        Triple("دولة الإمارات العربية المتحدة 🇦🇪", "AED", "الدرهم الإماراتي"),
                        Triple("جمهورية تركيا 🇹🇷", "TRY", "الليرة التركية"),
                        Triple("الاتحاد الأوروبي 🇪🇺", "EUR", "اليورو"),
                        Triple("دولة الكويت 🇰🇼", "KWD", "الدينار الكويتي"),
                        Triple("دولة قطر 🇶🇦", "QAR", "الريال القطري"),
                        Triple("المملكة الأردنية الهاشمية 🇯🇴", "JOD", "الدينار الأردني"),
                        Triple("الولايات المتحدة الأمريكية 🇺🇸", "USD", "الدولار الأمريكي"),
                        Triple("المملكة المتحدة 🇬🇧", "GBP", "الجنيه الإسترليني"),
                        Triple("كندا 🇨🇦", "CAD", "الدولار الكندي"),
                        Triple("أستراليا 🇦🇺", "AUD", "الدولار الأسترالي"),
                        Triple("اليابان 🇯🇵", "JPY", "الين الياباني"),
                        Triple("جمهورية الصين الشعبية 🇨🇳", "CNY", "اليوان الصيني"),
                        Triple("الجمهورية الجزائرية الديمقراطية الشعبية 🇩🇿", "DZD", "الدينار الجزائري"),
                        Triple("المملكة المغربية 🇲🇦", "MAD", "الدرهم المغربي"),
                        Triple("الجمهورية التونسية 🇹🇳", "TND", "الدينار التونسي"),
                        Triple("دولة ليبيا 🇱🇾", "LYD", "الدينار الليبي"),
                        Triple("سلطنة عمان 🇴🇲", "OMR", "الريال العماني"),
                        Triple("مملكة البحرين 🇧🇭", "BHD", "الدينار البحريني"),
                        Triple("الجمهورية اللبنانية 🇱🇧", "LBP", "الليرة اللبنانية"),
                        Triple("الجمهورية العربية السورية 🇸🇾", "SYP", "الليرة السورية"),
                        Triple("الجمهورية اليمنية 🇾🇪", "YER", "الريال اليمني"),
                        Triple("جمهورية السودان 🇸🇩", "SDG", "الجنيه السوداني"),
                        Triple("جمهورية الهند 🇮🇳", "INR", "الروبية الهندية")
                    )

                    val filteredList = countriesList.filter {
                        it.first.contains(searchQuery, ignoreCase = true) ||
                        it.second.contains(searchQuery, ignoreCase = true) ||
                        it.third.contains(searchQuery, ignoreCase = true)
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(filteredList) { (name, currency, description) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.saveMyCountry(name, currency)
                                        showCountryDialog = false
                                        Toast.makeText(context, if (selectedLanguage == "ar") "تم اختيار $name كبلدك الأساسي." else "Selected $name as home country.", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HighDensityOnSurface)
                                    Text(text = description, fontSize = 11.sp, color = HighDensityMutedText)
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(HighDensityBg)
                                        .border(1.dp, HighDensityBorder, CircleShape)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(text = currency, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = HighDensityPrimary)
                                }
                            }
                            HorizontalDivider(color = HighDensityBg, thickness = 1.dp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCountryDialog = false }) {
                            Text(t("close"), color = HighDensityPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }

    if (showLangDialog) {
        Dialog(
            onDismissRequest = { showLangDialog = false }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .clip(RoundedCornerShape(24.dp))
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(24.dp)),
                colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = t("select_language"),
                        color = HighDensityOnSurface,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(Locales.languages) { (code, name) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        viewModel.saveLanguage(code)
                                        showLangDialog = false
                                        Toast.makeText(context, if (code == "ar") "تم تغيير لغة التطبيق!" else "App language updated!", Toast.LENGTH_SHORT).show()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = HighDensityOnSurface)
                                if (selectedLanguage == code) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = HighDensityPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            HorizontalDivider(color = HighDensityBg, thickness = 1.dp)
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showLangDialog = false }) {
                            Text(t("close"), color = HighDensityPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomCurrencySpinner(
    selected: String,
    onSelected: (String) -> Unit,
    currencies: List<String>,
    currencyNames: Map<String, String>,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(HighDensityBg)
            .border(1.dp, HighDensityBorder, RoundedCornerShape(12.dp))
            .clickable { expanded = !expanded }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
        ) {
            val name = currencyNames[selected] ?: selected
            Text(
                text = "$selected ($name)",
                fontSize = 12.sp,
                color = HighDensityOnSurface,
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = HighDensityPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .background(HighDensitySurface)
                .border(1.dp, HighDensityBorder, RoundedCornerShape(12.dp))
                .width(180.dp)
        ) {
            currencies.forEach { code ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "$code (${currencyNames[code] ?: code})",
                            fontSize = 12.sp,
                            color = HighDensityOnSurface
                        )
                    },
                    onClick = {
                        onSelected(code)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun HistoryCard(
    item: HistoryItem,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateString = remember(item.timestamp) {
        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
            sdf.format(Date(item.timestamp))
        } catch (e: Exception) {
            ""
        }
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, HighDensityBorder, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = HighDensitySurface),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(HighDensityBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✈️",
                            fontSize = 12.sp
                        )
                    }
                    Column {
                        Text(
                            text = item.prompt,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = HighDensityOnSurface
                        )
                        Text(
                            text = dateString,
                            fontSize = 10.sp,
                            color = HighDensityMutedText
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onCopy,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "نسخ",
                            tint = HighDensityPrimary,
                            modifier = Modifier.size(15.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "حذف",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(HighDensityBg)
                    .border(1.dp, HighDensityBorder, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = item.result,
                    fontSize = 12.sp,
                    color = HighDensityOnSurface,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
