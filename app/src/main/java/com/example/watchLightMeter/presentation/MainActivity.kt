package com.example.watchLightMeter.presentation;

import android.R
import android.annotation.SuppressLint
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.SensorManager.SENSOR_DELAY_NORMAL
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.PagerSnapDistance
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import kotlinx.coroutines.launch
import kotlin.math.log2

private var exposureValue = 0f
private var selectedMode = "a"
private var iso: Int = 3
private var aperture: Int = 9
private var shutterSpeed: Int = 18
private var compensation: Int = 9
private var isoList = listOf<String>("50", "64", "80", "100", "125", "160", "200", "250", "320", "400", "500", "640", "800", "1000", "1250", "1600", "2000", "2500", "3200", "4000", "5000", "6400")
private var apertureList = listOf<String>("1.4", "1.6", "1.8", "2.0", "2.2", "2.4", "2.8", "3.2", "3.5", "4.0", "4.5", "5.0", "5.6", "6.3", "7.1", "8", "9", "10", "11", "13", "14", "16", "18", "20", "22", "25", "29", "32", "36", "42", "45", "50", "57", "64")
private var shutterSpeedList = listOf<String>("1s", "0.7s", "0.3s", "1/2", "1/2.5", "1/3", "1/4", "1/5", "1/6", "1/8", "1/10", "1/13", "1/15", "1/20", "1/25", "1/30", "1/40", "1/50", "1/60", "1/80", "1/100", "1/125", "1/160", "1/200", "1/250", "1/320", "1/400", "1/500", "1/640", "1/800", "1/1000", "1/1250", "1/1600", "1/2000", "1/2500", "1/3200", "1/4000", "1/5000", "1/6400", "1/8000")
private var compensationList = listOf<String>("-3", "-2.7", "-2.3", "-2", "-1.7", "-1.3", "-1", "-0.7", "-0.3", "0", "+0.3", "+0.7", "+1", "+1.3", "+1.7", "+2", "+2.3", "+2.7", "+3")


var curTime = 0L
var sensorReadDelayms = 2000

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setTheme(R.style.Theme_DeviceDefault)
        setContent {
            MaterialTheme {
                LightSensor()
            }
        }
    }
}

@Composable
fun LightSensor() {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(ComponentActivity.SENSOR_SERVICE) as SensorManager
    }
    val lightSensor = remember {
        sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
    }

    var luxValue by remember { mutableStateOf<Float?>(null) }
    var ev by remember { mutableStateOf<String>("") }
    var gotoAperture by remember { mutableStateOf<Int?>(iso) }
    var gotoShutter by remember { mutableStateOf<Int?>(aperture) }

    DisposableEffect(Unit) {
        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {

                if (System.currentTimeMillis() - curTime > sensorReadDelayms) {
                    curTime = System.currentTimeMillis()
                    luxValue = event.values.firstOrNull()
                    luxValue?.let {
                        exposureValue = log2(luxValue!!.toFloat()/2.5f)
                        ev = "%.2f".format(exposureValue)
                        Log.d("Exposure Value", ev)
                        if(selectedMode == "a") {
                            gotoShutter = calculateShutter()
                            Log.d("gotoshutter", shutterSpeedList[gotoShutter!!])
                        } else if (selectedMode == "s") {
                            gotoAperture = calculateAperture()
                            Log.d("gotoaperture", apertureList[gotoAperture!!])
                        }
                    }
                }
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
                // Not used
            }
        }

        sensorManager.registerListener(listener, lightSensor, SENSOR_DELAY_NORMAL)
        onDispose {
            sensorManager.unregisterListener(listener)
        }
    }

    val scrollState = rememberScrollState()

    Scaffold {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
        ) {
            Row {
                NumberPicker("shutter", gotoShutter, Modifier.padding(top = 40.dp))
            }
            Row {
                NumberPicker(type = "aperture", goToPage = gotoAperture)
            }
            Row {
                NumberPicker(type = "iso", goToPage = null)
            }
            Row {
                NumberPicker("compensation", null, Modifier.padding(bottom = 40.dp))
            }
        }
    }

}

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun NumberPicker(type: String?, goToPage: Int?, modifier: Modifier = Modifier) {

    var infoList: List<String>? = null
    if (type.equals("shutter")) {
        infoList = shutterSpeedList
    } else if (type.equals("aperture")) {
        infoList = apertureList
    } else if (type.equals("iso")) {
        infoList = isoList
    } else if (type.equals("compensation")) {
        infoList = compensationList
    }

    val pagerState = rememberPagerState(
        pageCount = {
            infoList?.count() ?: 0
        }, initialPage = if(type.equals("compensation")) 9 else 0
    )

    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val contentPadding = (maxWidth - 60.dp) / 2
        val offSet = maxWidth / 5
        val itemSpacing = offSet - 40.dp

        val scope = rememberCoroutineScope()

        val mutableInteractionSource = remember {
            MutableInteractionSource()
        }

        HorizontalPager(
            modifier = Modifier.width(maxWidth),
            state = pagerState,
            flingBehavior = PagerDefaults.flingBehavior(
                state = pagerState, pagerSnapDistance = PagerSnapDistance.atMost(15)
            ),
            contentPadding = PaddingValues(horizontal = contentPadding),
            pageSpacing = itemSpacing
        ) { page ->
            Box(
                modifier = Modifier
                    .height(50.dp)
                    .width(60.dp)
                    .clickable(
                        interactionSource = mutableInteractionSource,
                        indication = null,
                        enabled = true,
                    ) {
                        scope.launch {
                            pagerState.animateScrollToPage(page)
                        }
                        if (type == "aperture") {
                            selectedMode = "a"
                        } else if (type == "shutter") {
                            selectedMode = "s"
                        }
                        Log.d("selectedMode", selectedMode)
                    }) {
                Text(
                    text = infoList?.get(page) ?: "",
                    color = Color.White,
                    modifier = Modifier
                        .height(50.dp)
                        .width(60.dp)
                        .wrapContentHeight(),
                    textAlign = TextAlign.Center
                )
            }
        }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { page ->
                if (type.equals("shutter")) {
                    shutterSpeed = page
                } else if (type.equals("aperture")) {
                    aperture = page
                } else if (type.equals("iso")) {
                    iso = page
                } else if (type.equals("compensation")) {
                    compensation = page
                }
            }
        }

        LaunchedEffect(goToPage) {
            goToPage?.let {
                pagerState.animateScrollToPage(page = it)
            }
        }
    }
}

//aligned with EV:10, ISO:100, F:4.0, Shutter Speed:1/60
fun calculateAperture(): Int {
    var exposureDifference = ((exposureValue - 10) * 3).toInt()
    var isoDifference = iso - 3
    var shutterSpeedDifference = shutterSpeed - 18
    var compensationDifference = compensation - 9
    var apertureOffset = 9

    var apertureIndex = (exposureDifference + isoDifference - shutterSpeedDifference - compensationDifference) + apertureOffset

    if (apertureIndex < 0) {
        apertureIndex = 0
    } else if (apertureIndex > 33) {
        apertureIndex = 33
    }

    return apertureIndex
}

fun calculateShutter(): Int {
    var exposureDifference = ((exposureValue - 10) * 3).toInt()
    var isoDifference = iso - 3
    var apertureDifference = aperture - 9
    var compensationDifference = compensation - 9
    var shutterOffset = 16

    var shutterSpeedIndex = (exposureDifference + isoDifference - apertureDifference - compensationDifference) + shutterOffset

    if (shutterSpeedIndex < 0) {
        shutterSpeedIndex = 0
    } else if (shutterSpeedIndex > 39) {
        shutterSpeedIndex = 39
    }

    return shutterSpeedIndex
}