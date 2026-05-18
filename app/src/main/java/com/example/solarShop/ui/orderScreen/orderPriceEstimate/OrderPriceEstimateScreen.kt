package com.example.solarShop.ui.orderScreen.orderPriceEstimate

import android.util.Log
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SecondaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.coerceIn
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.solarShop.CurrencyUnit
import com.example.solarShop.LengthUnit
import com.example.solarShop.data.dataStore.DisplayPreferences
import com.example.solarShop.ui.theme.BambooTheme
import com.example.solarShop.utils.LoadingScreen
import com.example.solarShop.utils.MyCurrencyField
import com.example.solarShop.utils.myFormatNumber
import kotlin.math.abs

//region xxxxxx
//endregion xxxxxx

@Composable
fun PriceEstimateScreen(                               //صفحه سفارش
    modifier: Modifier = Modifier,
    onBack: ()->Unit,
    vm: PriceEstimateViewModel = hiltViewModel()
) {
    val ui by vm.uiState.collectAsStateWithLifecycle()

    val cabinetState by vm.cabinetEstimateState.collectAsStateWithLifecycle()
    val closetState by vm.closetEstimateState.collectAsStateWithLifecycle()
    val initialTab by vm.selectedEstimateTab.collectAsStateWithLifecycle() // ۰ یا ۱

    LaunchedEffect(Unit) { vm.setPriceWindowOpen(true) }


    if (ui.isDataLoaded) {

        PriceEstimateContent(
            cabinetState = cabinetState,
            closetState = closetState,
            initialTab   = initialTab,
            onCabinetEvent = vm::onCabinetEvent,
            onClosetEvent = vm::onClosetEvent,
            onSetSelectedEstimateTab = {vm.setSelectedEstimateTab(it)},
            onClosePriceEstimate = { onBack()  },
            modifier = modifier
                .fillMaxSize()
        )
    } else {
        LoadingScreen()
    }

}

@Composable
fun PriceEstimateContent(
    cabinetState: CabinetEstimateState,
    closetState: ClosetEstimateState,
    initialTab: Int,
    onSetSelectedEstimateTab : (Int) ->Unit,
    onCabinetEvent: (CabinetEstimateEvent) -> Unit,
    onClosetEvent: (ClosetEstimateEvent) -> Unit,
    onClosePriceEstimate: () -> Unit,
    modifier: Modifier = Modifier

) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    LaunchedEffect(initialTab) {
        selectedTab = initialTab
    }





    val tabs = listOf("کابینت","کمد")
    val scroll = rememberScrollState()

    val activeTotal    = if (selectedTab == 1)
        (closetState.breakdown?.totalPrice ?: 0L)
    else
        (cabinetState.breakdown?.totalPrice ?: 0L)


    val activeSettings = if (selectedTab == 1) closetState.settings else cabinetState.settings

    Scaffold(
        bottomBar = {
            PriceEstimateBottomBar(
                total = activeTotal,
                onCabinetEvent = { if (selectedTab == 0) onCabinetEvent(it)  },
                onClosetEvent = { if (selectedTab == 1) onClosetEvent(it)  },
                settings = activeSettings,
                onClosePriceEstimate = onClosePriceEstimate,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
            )
        },

        modifier = modifier
    ) { innerPadding ->

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)

        ) {
            //تبهای انتخاب بین کابینت و کمد و ...
            SecondaryTabRow (
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier.fillMaxWidth(),
            ) {
                tabs.forEachIndexed { i, label ->
                    Tab(
                        selected = i == selectedTab,
                        onClick = {
                            selectedTab = i
                            onSetSelectedEstimateTab(i)
                        },
                        text = { Text(label) },
                        enabled = true
                    )
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                // تب کابینت
                if (selectedTab == 0) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scroll)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = 4.dp, vertical = 8.dp),
//                        .imePadding(),
                        verticalArrangement = Arrangement.Top
                    ) {

                        // قیمت بازار
                        LabeledSectionWithCardLabel(label = "قیمت‌های بازار") {

                            MyTextFieldToMoneyWhitTwoField(
                                label = "قیمت یک متر کابینت:",
                                value = cabinetState.market.pricePerMeter,
                                onValueChange = {
                                    onCabinetEvent(
                                        CabinetEstimateEvent.ChangeCabinetPerMeter(
                                            it.toString()
                                        )
                                    )
                                },
                                settings = cabinetState.settings
                            )

                            MyTextFieldToMoneyWhitTwoField(
                                label = "قیمت صفحه کابینت:",
                                value = cabinetState.market.platePrice,
                                onValueChange = {
                                    onCabinetEvent(
                                        CabinetEstimateEvent.ChangePlateCabinet(
                                            it.toString()
                                        )
                                    )
                                },
                                settings = cabinetState.settings
                            )

                            MyTextFieldToMoneyWhitTwoField(
                                label = "هزینه نصب یک متر:",
                                value = cabinetState.market.installFeePerMeter,
                                onValueChange = {
                                    onCabinetEvent(
                                        CabinetEstimateEvent.ChangeInstallFeePerMeter(
                                            it.toString()
                                        )
                                    )
                                },
                                settings = cabinetState.settings
                            )

                            MyTextFieldToMoneyWhitTwoField(
                                label = "حمل و نقل:",
                                value = cabinetState.market.transportation,
                                onValueChange = {
                                    onCabinetEvent(
                                        CabinetEstimateEvent.ChangeTransportation(
                                            it.toString()
                                        )
                                    )
                                },
                                settings = cabinetState.settings
                            )

                        }

                        // انتخاب رویکرد
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {

                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                FilterChip(
                                    selected = cabinetState.inputs.mode == CabinetMode.SIMPLE,
                                    onClick = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeMode(
                                                CabinetMode.SIMPLE
                                            )
                                        )
                                    },
                                    label = { Text("ساده") }
                                )
                                FilterChip(
                                    selected = cabinetState.inputs.mode == CabinetMode.PRO,
                                    onClick = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeMode(
                                                CabinetMode.PRO
                                            )
                                        )
                                    },
                                    label = { Text("حرفه‌ای") }
                                )
                            }
                        }

                        HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(8.dp))

                        if (cabinetState.inputs.mode == CabinetMode.SIMPLE) {
                            LabeledSectionWithCardLabel(label = "اندازه اصلی") {
                                MyTextFieldThreeField(
                                    label = "متراژ کل:",
                                    meters = cabinetState.inputs.totalMeterSimple,
                                    moneyValue = cabinetState.breakdown?.totalMeterPrice ?: 0,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeTotalMeterSimple(
                                                it
                                            )
                                        )
                                    },
                                    settings = cabinetState.settings,
                                )
                            }
                        }
                        if (cabinetState.inputs.mode == CabinetMode.PRO) {

                            LabeledSectionWithCardLabel(label = "اندازه های اصلی") {
                                Log.i("TEST", "price = ${cabinetState.inputs.baseUnitMeter}")
                                MyTextFieldThreeField(
                                    label = "متراژ زمینی",
                                    meters = cabinetState.inputs.baseUnitMeter,
                                    moneyValue = cabinetState.breakdown?.baseUnitMeterPrice ?: 0,
                                    onValueChange = { v ->
                                        // فقط اگر readOnly=false اجازهٔ تغییر بده

                                        onCabinetEvent(CabinetEstimateEvent.ChangeBaseUnitMeter(v))

                                    },
                                    settings = cabinetState.settings,
                                    modifier = Modifier,
                                )
                                MyTextFieldThreeField(
                                    label = "متراژ هوایی",
                                    meters = cabinetState.inputs.wallUnitMeter,
                                    moneyValue = cabinetState.breakdown?.wallUnitMeterPrice ?: 0,
                                    onValueChange = { v ->
                                        // فقط اگر readOnly=false اجازهٔ تغییر بده

                                        onCabinetEvent(CabinetEstimateEvent.ChangeWallUnitMeter(v))

                                    },
                                    settings = cabinetState.settings,
                                )
                                MyTextFieldThreeField(
                                    label = "متراژ ایستاده",
                                    meters = cabinetState.inputs.tallUnitMeter,
                                    moneyValue = cabinetState.breakdown?.tallUnitMeterPrice ?: 0,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeTallUnitMeter(
                                                it
                                            )
                                        )
                                    },
                                    settings = cabinetState.settings,
                                )
                                MyTextFieldThreeField(
                                    label = "متراژ دکوری",
                                    meters = cabinetState.inputs.decorateUnitMeter,
                                    moneyValue = cabinetState.breakdown?.decorateUnitMeterPrice
                                        ?: 0,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeDecorateUnitMeter(
                                                it
                                            )
                                        )
                                    },
                                    settings = cabinetState.settings,
                                )
                                MyTextFieldThreeField(
                                    label = "متراژ جزیره",
                                    meters = cabinetState.inputs.islandUnitMeter,
                                    moneyValue = cabinetState.breakdown?.islandUnitMeterPrice ?: 0,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeIslandUnitMeter(
                                                it
                                            )
                                        )
                                    },
                                    settings = cabinetState.settings,
                                )
                                MyTextFieldThreeField(
                                    label = "متراژ یخچال",
                                    meters = cabinetState.inputs.refrigeratorUnitMeter,
                                    moneyValue = cabinetState.breakdown?.refrigeratorUnitMeterPrice
                                        ?: 0,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeRefrigeratorUnitMeter(
                                                it
                                            )
                                        )
                                    },
                                    settings = cabinetState.settings,
                                )
                                MyTextFieldThreeField(
                                    label = "متراژ لباسشویی",
                                    meters = cabinetState.inputs.laundryUnitMeter,
                                    moneyValue = cabinetState.breakdown?.laundryUnitMeterPrice ?: 0,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeLaundryUnitMeter(
                                                it
                                            )
                                        )
                                    },
                                    settings = cabinetState.settings,
                                )
                                MyTextFieldThreeField(
                                    label = "متراژ ظرفشویی",
                                    meters = cabinetState.inputs.dishwasherUnitMeter,
                                    moneyValue = cabinetState.breakdown?.dishwasherUnitMeterPrice
                                        ?: 0,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeDishwasherUnitMeter(
                                                it
                                            )
                                        )
                                    },
                                    settings = cabinetState.settings,
                                )


                            }

                            HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(8.dp))

                            LabeledSectionWithCardLabel(label = "نسبت قیمتی یونیتها") {
                                MyTextFieldToPercent(
                                    label = "نسبت زمینی",
                                    value = cabinetState.inputs.ratioBasePct,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeRatioBasePct(
                                                it
                                            )
                                        )
                                    }
                                )
                                MyTextFieldToPercent(
                                    label = "نسبت هوایی",
                                    value = cabinetState.inputs.ratioWallPct,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeRatioWallPct(
                                                it
                                            )
                                        )
                                    }

                                )
                                MyTextFieldToPercent(
                                    label = "نسبت ایستاده",
                                    value = cabinetState.inputs.ratioTallPct,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeRatioTallPct(
                                                it
                                            )
                                        )
                                    }
                                )
                                MyTextFieldToPercent(
                                    label = "نسبت دکوری",
                                    value = cabinetState.inputs.ratioDecoratePct,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeRatioDecoratePct(
                                                it
                                            )
                                        )
                                    }
                                )
                                MyTextFieldToPercent(
                                    label = "نسبت جزیره",
                                    value = cabinetState.inputs.ratioIslandPct,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeRatioIslandPct(
                                                it
                                            )
                                        )
                                    }
                                )
                                MyTextFieldToPercent(
                                    label = "نسبت یخچال",
                                    value = cabinetState.inputs.ratioRefrigeratorPct,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeRatioRefrigeratorPct(
                                                it
                                            )
                                        )
                                    }
                                )


                            }

                            HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(8.dp))

                            LabeledSectionWithCardLabel(label = "عمق یونیتها") {
                                MyTextFieldThreeField(
                                    label = "عمق زمینی",
                                    meters = cabinetState.inputs.baseUnitDepth,
                                    moneyValue = cabinetState.breakdown?.baseUnitDepthPrice ?: 0,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeBaseUnitDepth(
                                                it
                                            )
                                        )
                                    },
                                    settings = cabinetState.settings,
                                )
                                MyTextFieldThreeField(
                                    label = "عمق هوایی",
                                    meters = cabinetState.inputs.wallUnitDepth,
                                    moneyValue = cabinetState.breakdown?.wallUnitDepthPrice ?: 0,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeWallUnitDepth(
                                                it
                                            )
                                        )
                                    },
                                    settings = cabinetState.settings,
                                )
                                MyTextFieldThreeField(
                                    label = "عمق ایستاده",
                                    meters = cabinetState.inputs.tallUnitDepth,
                                    moneyValue = cabinetState.breakdown?.tallUnitDepthPrice ?: 0,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeTallUnitDepth(
                                                it
                                            )
                                        )
                                    },
                                    settings = cabinetState.settings,
                                )
                                MyTextFieldThreeField(
                                    label = "عمق دکوری",
                                    meters = cabinetState.inputs.decorateUnitDepth,
                                    moneyValue = cabinetState.breakdown?.decorateUnitDepthPrice
                                        ?: 0,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeDecorateUnitDepth(
                                                it
                                            )
                                        )
                                    },
                                    settings = cabinetState.settings,
                                )
                                MyTextFieldThreeField(
                                    label = "عمق جزیره",
                                    meters = cabinetState.inputs.islandUnitDepth,
                                    moneyValue = cabinetState.breakdown?.islandUnitDepthPrice ?: 0,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeIslandUnitDepth(
                                                it
                                            )
                                        )
                                    },
                                    settings = cabinetState.settings,
                                )


                            }

                            HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(8.dp))

                            LabeledSectionWithCardLabel(label = "ارتفاع یونیتها") {
                                MyTextFieldThreeField(
                                    label = "ارتفاع زمینی",
                                    meters = cabinetState.inputs.baseUnitHeight,
                                    moneyValue = cabinetState.breakdown?.baseUnitHeightPrice ?: 0,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeBaseUnitHeight(
                                                it
                                            )
                                        )
                                    },
                                    settings = cabinetState.settings,
                                )
                                MyTextFieldThreeField(
                                    label = "ارتفاع هوایی",
                                    meters = cabinetState.inputs.wallUnitHeight,
                                    moneyValue = cabinetState.breakdown?.wallUnitHeightPrice ?: 0,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeWallUnitHeight(
                                                it
                                            )
                                        )
                                    },
                                    settings = cabinetState.settings,
                                )
                                MyTextFieldThreeField(
                                    label = "ارتفاع ایستاده",
                                    meters = cabinetState.inputs.tallUnitHeight,
                                    moneyValue = cabinetState.breakdown?.tallUnitHeightPrice ?: 0,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeTallUnitHeight(
                                                it
                                            )
                                        )
                                    },
                                    settings = cabinetState.settings,
                                )
                                MyTextFieldThreeField(
                                    label = "ارتفاع دکوری",
                                    meters = cabinetState.inputs.decorateUnitHeight,
                                    moneyValue = cabinetState.breakdown?.decorateUnitHeightPrice
                                        ?: 0,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeDecorateUnitHeight(
                                                it
                                            )
                                        )
                                    },
                                    settings = cabinetState.settings,
                                )
                                MyTextFieldThreeField(
                                    label = "ارتفاع جزیره",
                                    meters = cabinetState.inputs.islandUnitHeight,
                                    moneyValue = cabinetState.breakdown?.islandUnitHeightPrice ?: 0,
                                    onValueChange = {
                                        onCabinetEvent(
                                            CabinetEstimateEvent.ChangeIslandUnitHeight(
                                                it
                                            )
                                        )
                                    },
                                    settings = cabinetState.settings,
                                )


                            }

                            Spacer(modifier = Modifier.height(200.dp))
                        }

                    }
                }

                // تب کمد
                if (selectedTab == 1) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(scroll)
                            .background(MaterialTheme.colorScheme.surfaceContainerLow)
                            .padding(horizontal = 4.dp, vertical = 8.dp),
//                        .imePadding(),
                        verticalArrangement = Arrangement.Top
                    ) {
                        // قیمت بازار
                        LabeledSectionWithCardLabel(label = "قیمت‌های بازار") {

                            MyTextFieldToMoneyWhitTwoField(
                                label = "قیمت یک متر مربع کمد:",
                                value = closetState.market.pricePerM2,
                                onValueChange = {
                                    onClosetEvent(
                                        ClosetEstimateEvent.ChangePricePerM2(
                                            it.toString()
                                        )
                                    )
                                },
                                settings = closetState.settings
                            )

                            MyTextFieldToMoneyWhitTwoField(
                                label = "هزینه نصب یک متر مربع:",
                                value = closetState.market.installFeePerM2,
                                onValueChange = {
                                    onClosetEvent(
                                        ClosetEstimateEvent.ChangeInstallFeePerM2(
                                            it.toString()
                                        )
                                    )
                                },
                                settings = closetState.settings
                            )

                            MyTextFieldToMoneyWhitTwoField(
                                label = "حمل و نقل:",
                                value = closetState.market.transportation,
                                onValueChange = {
                                    onClosetEvent(
                                        ClosetEstimateEvent.ChangeTransportation(
                                            it.toString()
                                        )
                                    )
                                },
                                settings = closetState.settings
                            )

                        }

                        // انتخاب رویکرد
//                        Column(
//                            modifier = Modifier
//                                .fillMaxWidth()
//                                .padding(8.dp),
//                            horizontalAlignment = Alignment.CenterHorizontally
//                        ) {
//
//                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
//                                FilterChip(
//                                    selected = closetState.inputs.mode == ClosetMode.SIMPLE,
//                                    onClick = {
//                                        onClosetEvent(
//                                            ClosetEstimateEvent.ChangeMode(
//                                                ClosetMode.SIMPLE
//                                            )
//                                        )
//                                    },
//                                    label = { Text("ساده") }
//                                )
//                                FilterChip(
//                                    selected = closetState.inputs.mode == ClosetMode.PRO,
//                                    onClick = {
//                                        onClosetEvent(
//                                            ClosetEstimateEvent.ChangeMode(
//                                                ClosetMode.PRO
//                                            )
//                                        )
//                                    },
//                                    label = { Text("حرفه‌ای") }
//                                )
//                            }
//                        }
//
//                        HorizontalDivider(thickness = 1.dp, modifier = Modifier.padding(8.dp))
//
//                        if (closetState.inputs.mode == ClosetMode.SIMPLE) {
//
//                        }
                        LabeledSectionWithCardLabel(label = "اندازه های اصلی") {
                            MyTextFieldTwoField(
                                label = "عرض کمد",
                                meters = closetState.inputs.width,
                                onValueChange = {
                                    onClosetEvent(
                                        ClosetEstimateEvent.ChangeWidth(
                                            it
                                        )
                                    )
                                },
                                settings = closetState.settings,
                            )
                            MyTextFieldTwoField(
                                label = "ارتفاع کمد",
                                meters = closetState.inputs.height,
                                onValueChange = {
                                    onClosetEvent(
                                        ClosetEstimateEvent.ChangeHeight(
                                            it
                                        )
                                    )
                                },
                                settings = closetState.settings,
                            )
                            MoneyDeltaText(
                                value = closetState.breakdown?.baseAreaPrice ?: 0,
                                settings = closetState.settings,
                                modifier = Modifier.widthIn(min = 100.dp)
                            )

                            HorizontalDivider(
                                thickness = 1.dp,
                                modifier = Modifier.padding(8.dp)
                            )

                            MyTextFieldThreeField(
                                label = "عمق کمد",
                                meters = closetState.inputs.depth,
                                moneyValue = closetState.breakdown?.depthAdjustment ?: 0,
                                onValueChange = {
                                    onClosetEvent(
                                        ClosetEstimateEvent.ChangeDepth(
                                            it
                                        )
                                    )
                                },
                                settings = cabinetState.settings,
                            )


                        }

                    }
                }

            }
        }

    }

}

//region BottomBar
@Composable
private fun PriceEstimateBottomBar(
    total: Long,
    onCabinetEvent: (CabinetEstimateEvent) -> Unit,
    onClosetEvent: (ClosetEstimateEvent) -> Unit,
    settings: DisplayPreferences,
    onClosePriceEstimate: () -> Unit,
    modifier: Modifier = Modifier,
) {


    Surface(
        tonalElevation = 2.dp,
        color = BambooTheme.sections.bottomBarContainer,
        contentColor = BambooTheme.sections.bottomBarContent,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {


            // قیمت کل و فیلد آن
            MyReadOnlyTwoField(
                value = total ?: 0,
                label = "قیمت کل: ",
                settings = settings,
                modifier = Modifier.weight(2.4f)
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                IconButton(
                    onClick = {
                        onCabinetEvent(CabinetEstimateEvent.SaveEstimateToDb)
                        onClosetEvent(ClosetEstimateEvent.SaveEstimateToDb)
                        onClosePriceEstimate()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Check,
                        contentDescription = "تأیید",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(
                    onClick = {
                        onClosePriceEstimate()
                    },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "انصراف",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            }


        }
    }
}
//endregion BottomBar





/* --------------- Pieces --------------- */
@Composable
private fun SimpleUnitRow(
    label: String,
    field: SimpleUnitField,
    meters: Double,
    settings: DisplayPreferences,
    onChange: (CabinetEstimateEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current

    // نمایش/ورود بر اساس تنظیمات طول
    val displayText = when (settings.lengthUnit) {
        LengthUnit.METER -> if (meters == 0.0) "" else meters.toString()
        LengthUnit.CENTIMETER -> if (meters == 0.0) "" else ((meters * 100.0).toInt()).toString()
    }

    var tfv by rememberSaveable(displayText, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(displayText))
    }

    // وقتی meters بیرونی تغییر کند، TextField را هم همگام کن
    LaunchedEffect(meters, settings.lengthUnit) {
        val newDisplay = when (settings.lengthUnit) {
            LengthUnit.METER -> if (meters == 0.0) "" else meters.toString()
            LengthUnit.CENTIMETER -> if (meters == 0.0) "" else ((meters * 100.0).toInt()).toString()
        }
        if (tfv.text != newDisplay) {
            tfv = tfv.copy(text = newDisplay, selection = TextRange(newDisplay.length))
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // − نیم متر
        OutlinedButton(
            onClick = { onChange(CabinetEstimateEvent.StepSimpleUnit(field, -0.5)) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.width(48.dp)
        ) { Text("−") }

        // TextField
        OutlinedTextField(
            value = tfv,
            onValueChange = { new ->
                // فقط رقم (و در حالت متر اجازه نقطه)
                val filtered = when (settings.lengthUnit) {
                    LengthUnit.METER -> new.text.filter { it.isDigit() || it == '.' }
                    LengthUnit.CENTIMETER -> new.text.filter { it.isDigit() }
                }
                tfv =
                    TextFieldValue(filtered, selection = new.selection.coerceIn(0, filtered.length))

                val metersValue: Double? =
                    if (filtered.isBlank()) 0.0
                    else when (settings.lengthUnit) {
                        LengthUnit.METER -> filtered.toDoubleOrNull()
                        LengthUnit.CENTIMETER -> filtered.toLongOrNull()?.div(100.0)
                    }

                onChange(CabinetEstimateEvent.ChangeSimpleUnit(field, metersValue))
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            shape = RoundedCornerShape(12.dp),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
            suffix = {
                Text(if (settings.lengthUnit == LengthUnit.CENTIMETER) "cm" else "m")
            },
            modifier = Modifier.widthIn(min = 110.dp, max = 140.dp)
        )

        // + نیم متر
        OutlinedButton(
            onClick = { onChange(CabinetEstimateEvent.StepSimpleUnit(field, +0.5)) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.width(48.dp)
        ) { Text("+") }

        // لیبل راست‌به‌چپ
//        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
//        }
    }
}

@Composable
fun SimpleModeSection(
    state: CabinetEstimateState,
    onEvent: (CabinetEstimateEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        SimpleUnitRow(
            label = "یونیت زمینی",
            field = SimpleUnitField.BASE,
            meters = state.inputs.baseUnitMeter,
            settings = state.settings,
            onChange = onEvent
        )
        SimpleUnitRow(
            label = "یونیت هوایی",
            field = SimpleUnitField.WALL,
            meters = state.inputs.wallUnitMeter,
            settings = state.settings,
            onChange = onEvent
        )
        SimpleUnitRow(
            label = "یونیت ایستاده",
            field = SimpleUnitField.TALL,
            meters = state.inputs.tallUnitMeter,
            settings = state.settings,
            onChange = onEvent
        )
        SimpleUnitRow(
            label = "یونیت دکوری",
            field = SimpleUnitField.DECOR,
            meters = state.inputs.decorateUnitMeter,
            settings = state.settings,
            onChange = onEvent
        )
        SimpleUnitRow(
            label = "جزیره",
            field = SimpleUnitField.ISLAND,
            meters = state.inputs.islandUnitMeter,
            settings = state.settings,
            onChange = onEvent
        )
    }
}


@Composable
private fun MyTextFieldToPercent(
    label: String,
    value: Double,                              // نسبت: 1.0 = 100%
    onValueChange: (Double?) -> Unit,           // نسبت را برمی‌گردانیم (مثلاً 2.0 = 200%)
    modifier: Modifier = Modifier,
    minPercent: Int = 0,                        // حداقل درصدِ نمایش/ورود (مثلاً 0)
    maxPercent: Int = 300,                      // حداکثر درصد (مثلاً 300 برای 3 برابر)
    allowNegative: Boolean = false              // اگر لازم داری زیر 0 نیز مجاز باشد
) {
    val focusManager = LocalFocusManager.current

    fun clampPercent(p: Int): Int {
        val lo = if (allowNegative) minPercent else maxOf(0, minPercent)
        return p.coerceIn(lo, maxPercent)
    }

    // نسبت بیرونی → متن درصد
    fun valueToPercentText(v: Double): String {
        // گرد کردن دوستانه
        val pct = (v * 100.0).toInt()
        return clampPercent(pct).toString()
    }

    var tfv by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(text = valueToPercentText(value)))
    }

    var selectAllNextFocus by rememberSaveable { mutableStateOf(true) }
    var isFocused by remember { mutableStateOf(false) }

    // سینک با مقدار بیرونی
    LaunchedEffect(value, minPercent, maxPercent, allowNegative) {
        val newText = valueToPercentText(value)
        if (tfv.text != newText) {
            tfv = tfv.copy(
                text = newText,
                selection = tfv.selection.coerceIn(0, newText.length)
            )
        }
    }

    // Select-All بعد از یک فریم
    LaunchedEffect(isFocused, selectAllNextFocus, tfv.text) {
        if (isFocused && selectAllNextFocus) {
            kotlinx.coroutines.android.awaitFrame()
            tfv = tfv.copy(selection = TextRange(0, tfv.text.length))
            selectAllNextFocus = false
        }
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
//        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            )
//        }

        OutlinedTextField(
            value = tfv,
            onValueChange = { new ->
                // اجازه‌ی منفی فقط اگر allowNegative=true
                val raw = new.text
                val cleaned = buildString {
                    raw.forEachIndexed { i, ch ->
                        when {
                            ch.isDigit() -> append(ch)
                            allowNegative && i == 0 && ch == '-' -> append(ch)
                        }
                    }
                }
                // اگر فقط "-" بود، موقتاً صفر فرض می‌کنیم
                val pctInt = cleaned.toIntOrNull() ?: 0
                val pct = clampPercent(pctInt)

                val txt = pct.toString()
                val sel = new.selection.coerceIn(0, txt.length)

                tfv = TextFieldValue(text = txt, selection = sel)

                // تبدیل درصد → نسبت (0..∞) و ارسال
                onValueChange(pct / 100.0)
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
            suffix = { Text("%") },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedSuffixColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedSuffixColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            placeholder = { Text("0") },
            modifier = Modifier
                .widthIn(min = 100.dp, max = 100.dp)
                .onFocusChanged { st ->
                    isFocused = st.isFocused
                    if (!st.isFocused) selectAllNextFocus = true
                }
        )


    }
}

@Composable
fun LabeledSectionWithCardLabel(
    label: String,
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(16.dp),
    sectionBorderColor: Color = MaterialTheme.colorScheme.primary,
    sectionContainerColor: Color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
    labelCardColor: Color = MaterialTheme.colorScheme.surface,         // پس‌زمینه کارت لیبل
    labelBorderColor: Color = MaterialTheme.colorScheme.outline,       // بوردر کارت لیبل
    content: @Composable ColumnScope.() -> Unit
) {
    Box(modifier = modifier.padding(top = 16.dp)) {  // فضا برای نشستن کارت لیبل
        // بدنه‌ی بخش با بوردر
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.dp, sectionBorderColor), shape)
                .background(sectionContainerColor, shape)
                .padding(12.dp)                         // فاصله‌ی داخلی
                .zIndex(0f)
        ) {
            Spacer(Modifier.height(2.dp))
            content()
        }

        // کارت لیبل که روی مرز می‌نشیند و مرز زیرش را می‌پوشاند
        Card(
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, labelBorderColor),
            colors = CardDefaults.cardColors(containerColor = labelCardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = (6).dp, y = (-18).dp)       // کمی بالاتر از خط مرز
                .zIndex(1f)                             // بالاتر از ستون
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}


@Composable
private fun MyReadOnlyTwoField(
    label: String,
    value: Long,                 // کانُن: همیشه تومان
    settings: DisplayPreferences,
    modifier: Modifier = Modifier
) {
    // مبلغِ نمایشی بر اساس تنظیمات
    val displayAmount = if (settings.currencyUnit == CurrencyUnit.RIAL) {
        value * 10L           // تومان → ریال
    } else value

    val currencyLabel = if (settings.currencyUnit == CurrencyUnit.RIAL) "ریال" else "تومان"

    // متن قالب‌بندی‌شده؛ با تغییر value یا واحد، دوباره محاسبه شود
    val text = remember(value, settings.currencyUnit) {
        myFormatNumber(displayAmount)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
//        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp)
            )
//        }

        OutlinedTextField(
            value = text,
            onValueChange = {},                 // فقط-خواندنی
            readOnly = true,
            enabled = true,
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
            suffix = { Text(currencyLabel) },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant,
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedSuffixColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedSuffixColor = MaterialTheme.colorScheme.onSurfaceVariant,
                cursorColor = Color.Transparent
            ),
            modifier = Modifier
                .widthIn(max = 170.dp)
//                .heightIn(min = 56.dp)
        )


    }
}


@Composable
private fun MyTextFieldToMoneyWhitTwoField(
    label: String,
    value: Long,                       // کانُن: همیشه به تومان
    onValueChange: (Long?) -> Unit,    // کانُن: همیشه تومان
    settings: DisplayPreferences,
    modifier: Modifier = Modifier
) {

    val isToman = settings.currencyUnit == CurrencyUnit.TOMAN

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {



        // لیبل سمت راست (همان رفتاری که قبلاً داشتی)
//        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                modifier = Modifier.padding(8.dp)
            )
//        }

        Spacer(modifier = Modifier.weight(1f))

        key(isToman) {
            MyCurrencyField(
                label = "",                  // لیبل را بیرون فیلد نشان می‌دهیم
                value = value,               // همیشه تومان
                toman = isToman,             // نمایش تومان/ریال
                onValueChange = onValueChange,
                modifier = Modifier.widthIn(max = 150.dp)
            )
        }



    }
}



@Composable
private fun MoneyDeltaText(
    value: Long,                      // همیشه تومان
    modifier: Modifier = Modifier,
    settings: DisplayPreferences
) {
    val displayAmount: Long =
        if (settings.currencyUnit == CurrencyUnit.RIAL) value * 10L else value

    val currencyLabel = if (settings.currencyUnit == CurrencyUnit.RIAL) "ریال" else "تومان"

    val targetColor = when {
        displayAmount > 0L -> Color.Blue
        displayAmount == 0L -> Color.Gray
        else -> Color.Red
    }
    val color by animateColorAsState(targetValue = targetColor, label = "moneyColor")

    // LRM برای اینکه منفی سمت چپ عدد بماند
    val LRM = "\u200E"

    val amountText = if (displayAmount < 0) {
        // منفی را دستی می‌سازیم که مطمئن باشیم اول می‌آید
        "$LRM-${myFormatNumber(abs(displayAmount))} $currencyLabel"
    } else {
        "$LRM${myFormatNumber(displayAmount)} $currencyLabel"
    }

    Text(
        text = amountText,
        color = color,
        style = MaterialTheme.typography.bodyMedium.copy(textDirection = TextDirection.Ltr),
        textAlign = TextAlign.Start,
        maxLines = 1,
        modifier = modifier
    )
}



@Composable
private fun MyTextFieldThreeField(
    label: String,
    meters: Double,                 // منبع حقیقت (متر)
    moneyValue: Long,
    onValueChange: (Double?) -> Unit, // همیشه "متر" بده
    settings: DisplayPreferences,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val focusManager = LocalFocusManager.current

    fun toDisplayText(m: Double, unit: LengthUnit): String =
        if (m == 0.0) "" else when (unit) {
            LengthUnit.METER -> m.toString()
            LengthUnit.CENTIMETER -> ((m * 100.0).toInt()).toString()
        }

    // --------- state ----------
    var isFocused by remember { mutableStateOf(false) }
    var selectAllNextFocus by rememberSaveable { mutableStateOf(true) }

    // متن ورودی فعلیِ کاربر (Raw، بدون فرمت هزارگان)
    var tfv by rememberSaveable(settings.lengthUnit, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(toDisplayText(meters, settings.lengthUnit)))
    }

    // فقط وقتی فیلد فوکوس ندارد، از مقدار بیرونی سینک کن تا در حین تایپ کرسر نپرد
    LaunchedEffect(meters, settings.lengthUnit, isFocused) {
        if (!isFocused) {
            val newText = toDisplayText(meters, settings.lengthUnit)
            if (tfv.text != newText) {
                tfv = tfv.copy(
                    text = newText,
                    selection = TextRange(newText.length)
                )
            }
        }
    }

    // Select-All در هر فوکوس
    LaunchedEffect(isFocused, selectAllNextFocus, tfv.text) {
        if (isFocused && selectAllNextFocus) {
            kotlinx.coroutines.android.awaitFrame()
            tfv = tfv.copy(selection = TextRange(0, tfv.text.length))
            selectAllNextFocus = false
        }
    }

    // فیلتر + نگاشت دقیق کرسر
    fun filterAndMapCursor(input: String, cursor: Int, unit: LengthUnit): Pair<String, Int> {
        val allowDot = unit == LengthUnit.METER
        var dotSeen = false
        val sb = StringBuilder()
        var newCursor = 0
        input.forEachIndexed { i, ch ->
            val keep = when {
                ch.isDigit() -> true
                allowDot && ch == '.' && !dotSeen -> {
                    dotSeen = true; true
                }

                else -> false
            }
            if (keep) {
                sb.append(ch)
                if (i < cursor) newCursor++
            }
        }
        return sb.toString() to newCursor.coerceIn(0, sb.length)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        // لیبل RTL (سمت راست)
//        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = label,
            fontSize = 16.sp,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f, fill = true)
                .padding(8.dp)
        )

//        }


        // ورودی طول
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.widthIn(min = 20.dp, max = 105.dp) // کمی بزرگ‌تر چون unit جدا شد
        ) {
            // واحد سمت راست
            Text(
                text = if (settings.lengthUnit == LengthUnit.CENTIMETER) "cm" else "m",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 6.dp) // چون این Row LTR است، end یعنی راست
            )

            OutlinedTextField(
                value = tfv,
                onValueChange = { new ->
                    val (filtered, mappedCursor) =
                        filterAndMapCursor(new.text, new.selection.end, settings.lengthUnit)

                    tfv = TextFieldValue(filtered, selection = TextRange(mappedCursor))

                    val metersValue: Double =
                        if (filtered.isBlank()) 0.0
                        else if (settings.lengthUnit == LengthUnit.METER) filtered.toDoubleOrNull() ?: 0.0
                        else (filtered.toLongOrNull()?.div(100.0)) ?: 0.0

                    onValueChange(metersValue)
                },
                singleLine = true,
                enabled = enabled,
                readOnly = !enabled,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (settings.lengthUnit == LengthUnit.METER) KeyboardType.Decimal else KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.outline,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                ),
                modifier = Modifier.weight(1f)
                    .onFocusChanged { st ->
                        isFocused = st.isFocused
                        if (!st.isFocused) selectAllNextFocus = true
                    }
            )
        }



        // مبلغ
        Box(
            modifier = Modifier.widthIn(min = 120.dp, max = 120.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            MoneyDeltaText(
                value = moneyValue,
                settings = settings,
                modifier = Modifier.fillMaxWidth() // برای اینکه textAlign اثر کنه
            )
        }



    }
}



@Composable
private fun MyTextFieldTwoField(
    label: String,
    meters: Double,
    onValueChange: (Double?) -> Unit, // همیشه "متر" بده
    settings: DisplayPreferences,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val focusManager = LocalFocusManager.current

    fun toDisplayText(m: Double, unit: LengthUnit): String =
        if (m == 0.0) "" else when (unit) {
            LengthUnit.METER -> m.toString()
            LengthUnit.CENTIMETER -> ((m * 100.0).toInt()).toString()
        }

    // --------- state ----------
    var isFocused by remember { mutableStateOf(false) }
    var selectAllNextFocus by rememberSaveable { mutableStateOf(true) }

    // متن ورودی فعلیِ کاربر (Raw، بدون فرمت هزارگان)
    var tfv by rememberSaveable(settings.lengthUnit, stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(toDisplayText(meters, settings.lengthUnit)))
    }

    // فقط وقتی فیلد فوکوس ندارد، از مقدار بیرونی سینک کن تا در حین تایپ کرسر نپرد
    LaunchedEffect(meters, settings.lengthUnit, isFocused) {
        if (!isFocused) {
            val newText = toDisplayText(meters, settings.lengthUnit)
            if (tfv.text != newText) {
                tfv = tfv.copy(
                    text = newText,
                    selection = TextRange(newText.length)
                )
            }
        }
    }

    // Select-All در هر فوکوس
    LaunchedEffect(isFocused, selectAllNextFocus, tfv.text) {
        if (isFocused && selectAllNextFocus) {
            kotlinx.coroutines.android.awaitFrame()
            tfv = tfv.copy(selection = TextRange(0, tfv.text.length))
            selectAllNextFocus = false
        }
    }

    // فیلتر + نگاشت دقیق کرسر
    fun filterAndMapCursor(input: String, cursor: Int, unit: LengthUnit): Pair<String, Int> {
        val allowDot = unit == LengthUnit.METER
        var dotSeen = false
        val sb = StringBuilder()
        var newCursor = 0
        input.forEachIndexed { i, ch ->
            val keep = when {
                ch.isDigit() -> true
                allowDot && ch == '.' && !dotSeen -> {
                    dotSeen = true; true
                }

                else -> false
            }
            if (keep) {
                sb.append(ch)
                if (i < cursor) newCursor++
            }
        }
        return sb.toString() to newCursor.coerceIn(0, sb.length)
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {

        // لیبل RTL (سمت راست)
//        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Text(
            text = label,
            fontSize = 16.sp,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .padding(8.dp)
        )
//        }

        // ورودی طول
        OutlinedTextField(
            value = tfv,
            onValueChange = { new ->
                // فیلتر + نگاشت کرسر
                val (filtered, mappedCursor) =
                    filterAndMapCursor(new.text, new.selection.end, settings.lengthUnit)

                tfv = TextFieldValue(filtered, selection = TextRange(mappedCursor))

                // تبدیل به متر
                val metersValue: Double =
                    if (filtered.isBlank()) 0.0
                    else if (settings.lengthUnit == LengthUnit.METER) filtered.toDoubleOrNull()
                        ?: 0.0
                    else (filtered.toLongOrNull()?.div(100.0)) ?: 0.0

                onValueChange(metersValue)

            },
            singleLine = true,
            enabled = enabled,
            readOnly = !enabled,
            keyboardOptions = KeyboardOptions(
                keyboardType = if (settings.lengthUnit == LengthUnit.METER)
                    KeyboardType.Decimal else KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.End),
            suffix = { Text(if (settings.lengthUnit == LengthUnit.CENTIMETER) "cm" else "m") },
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.outline,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f),
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                focusedSuffixColor = MaterialTheme.colorScheme.onSurfaceVariant,
                unfocusedSuffixColor = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier
                .widthIn(min = 20.dp, max = 110.dp)
                .onFocusChanged { st ->
                    isFocused = st.isFocused
                    if (!st.isFocused) selectAllNextFocus = true
                }
        )


    }
}
