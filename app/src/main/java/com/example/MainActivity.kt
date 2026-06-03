package com.example

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Reminder
import com.example.data.RoutineActivity
import com.example.ui.RhythmViewModel
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class MainActivity : ComponentActivity() {
    private val viewModel: RhythmViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                // Request Permission Launcher on API 33+ (Android 13+)
                val context = LocalContext.current
                var hasNotificationPermission by remember {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        mutableStateOf(
                            ContextCompat.checkSelfPermission(
                                context,
                                android.Manifest.permission.POST_NOTIFICATIONS
                            ) == PackageManager.PERMISSION_GRANTED
                        )
                    } else {
                        mutableStateOf(true)
                    }
                }

                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestPermission()
                ) { isGranted ->
                    hasNotificationPermission = isGranted
                    if (!isGranted) {
                        Toast.makeText(
                            context,
                            "Notification permission denied. Reminders may not show notifications.",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }

                LaunchedEffect(Unit) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        permissionLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    containerColor = PureBlack
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        DailyRhythmApp(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

// Pre-defined modern/pastel colors matching the web specifications
val ColorPalette = listOf(
    "#FFD0BCFF", // Lavender
    "#FF80CBC4", // Soft Teal
    "#FFF48FB1", // Rose Pink
    "#FFFFD54F", // Gold / Amber
    "#FFA5D6A7", // Light Green
    "#FFFFCC80", // Peach / Orange
    "#FF90CAF9", // Sky Blue
    "#FFE6EE9C"  // Soft Lime
)

// Helper standard formatting to show 24 Hour minutes as 12-hour AM/PM
fun formatTime12(h: Int, m: Int): String {
    val period = if (h >= 12) "PM" else "AM"
    val hour12 = if (h % 12 == 0) 12 else h % 12
    val minStr = String.format("%02d", m)
    return "$hour12:$minStr $period"
}

@Composable
fun DailyRhythmApp(viewModel: RhythmViewModel) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()
    val activeActivity by viewModel.activeActivity.collectAsStateWithLifecycle()

    val headerDateFormatter = remember { SimpleDateFormat("EEEE, MMM d • hh:mm a", Locale.US) }
    val formattedHeaderDate = remember(currentTime) { headerDateFormatter.format(currentTime) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(PureBlack)
    ) {
        // Status Bar & Immersive Top Header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, start = 24.dp, end = 24.dp, bottom = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Rhythm",
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.5).sp
                )
                
                // Active status badge
                Box(
                    modifier = Modifier
                        .background(
                            color = if (activeActivity != null) BlueAccent.copy(alpha = 0.15f) else MutedText.copy(alpha = 0.12f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (activeActivity != null) BlueAccent.copy(alpha = 0.35f) else MutedText.copy(alpha = 0.25f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (activeActivity != null) "ACTIVE" else "STANDBY",
                        color = if (activeActivity != null) BlueAccent else MutedText,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = formattedHeaderDate,
                color = MutedText,
                fontSize = 14.sp,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.testTag("date_day_header")
            )
        }
        Spacer(modifier = Modifier.height(10.dp))

        // Main Screen Area based on Selected Tab
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (currentTab) {
                RhythmViewModel.TabState.ROUTINE -> {
                    RoutineTabScreen(viewModel = viewModel)
                }
                RhythmViewModel.TabState.REMINDERS -> {
                    RemindersTabScreen(viewModel = viewModel)
                }
                RhythmViewModel.TabState.ALARMS -> {
                    AlarmsTabScreen(viewModel = viewModel)
                }
            }

            // Floating Custom Pill Tab Bar (PWA Replica)
            PillTabBar(
                currentTab = currentTab,
                onTabSelected = { viewModel.selectTab(it) },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 20.dp)
            )
        }
    }
}

@Composable
fun PillTabBar(
    currentTab: RhythmViewModel.TabState,
    onTabSelected: (RhythmViewModel.TabState) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .width(342.dp)
            .height(54.dp)
            .background(Color(0x33FFFFFF), shape = RoundedCornerShape(27.dp))
            .border(1.dp, Color.White.copy(alpha = 0.12f), shape = RoundedCornerShape(27.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Routine Tab button
        val isRoutineActive = currentTab == RhythmViewModel.TabState.ROUTINE
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(23.dp))
                .background(if (isRoutineActive) Color.White else Color.Transparent)
                .clickable { onTabSelected(RhythmViewModel.TabState.ROUTINE) }
                .testTag("routine_tab_btn"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.PieChart,
                    contentDescription = "Routine",
                    tint = if (isRoutineActive) Color.Black else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Routine",
                    color = if (isRoutineActive) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Reminders Tab button
        val isRemindersActive = currentTab == RhythmViewModel.TabState.REMINDERS
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(23.dp))
                .background(if (isRemindersActive) Color.White else Color.Transparent)
                .clickable { onTabSelected(RhythmViewModel.TabState.REMINDERS) }
                .testTag("reminder_tab_btn"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.Notifications,
                    contentDescription = "Reminder",
                    tint = if (isRemindersActive) Color.Black else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Alerts",
                    color = if (isRemindersActive) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Alarms Tab button
        val isAlarmsActive = currentTab == RhythmViewModel.TabState.ALARMS
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clip(RoundedCornerShape(23.dp))
                .background(if (isAlarmsActive) Color.White else Color.Transparent)
                .clickable { onTabSelected(RhythmViewModel.TabState.ALARMS) }
                .testTag("alarms_tab_btn"),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(
                    imageVector = Icons.Filled.Alarm,
                    contentDescription = "Alarms",
                    tint = if (isAlarmsActive) Color.Black else Color.White,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Alarms",
                    color = if (isAlarmsActive) Color.Black else Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun RoutineTabScreen(viewModel: RhythmViewModel) {
    val activities by viewModel.activities.collectAsStateWithLifecycle()
    val activeActivity by viewModel.activeActivity.collectAsStateWithLifecycle()
    val selectedChartActivity by viewModel.selectedChartActivity.collectAsStateWithLifecycle()
    val currentTime by viewModel.currentTime.collectAsStateWithLifecycle()

    var showActivityDialog by remember { mutableStateOf(false) }
    var editingActivity by remember { mutableStateOf<RoutineActivity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        contentPadding = PaddingValues(bottom = 100.dp) // Cushion to avoid overlapping floating nav
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            // 24 Hour circular chart view
            DailyRhythmChart(
                activities = activities,
                activeActivity = activeActivity,
                currentTime = currentTime,
                onActivitySelected = { viewModel.selectChartActivity(it) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Tapped Segment Details Tooltip Panel
        selectedChartActivity?.let { activity ->
            item {
                val parsedColor = remember(activity.color) {
                    try { Color(android.graphics.Color.parseColor(activity.color)) } catch (e: Exception) { GoldAccent }
                }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .border(
                            1.dp,
                            parsedColor.copy(alpha = 0.4f),
                            RoundedCornerShape(16.dp)
                        ),
                    colors = CardDefaults.cardColors(containerColor = ImmersiveCardActive),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "SELECTED SEGMENT INFO",
                            color = ImmersiveSlate500,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = activity.label,
                            color = parsedColor,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${formatTime12(activity.startH, activity.startM)}  TO  ${formatTime12(activity.endH, activity.endM)}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Timeline Header: Left title, Right dynamic action
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = "TIMELINE",
                    color = ImmersiveSlate400,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                
                Text(
                    text = "+ Add Activity",
                    color = BlueAccent,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            editingActivity = null
                            showActivityDialog = true
                        }
                        .testTag("add_activity_button_timeline_text")
                )
            }
        }

        if (activities.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PieChart,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No activities set yet. Tap button to custom schedule your daily routine segments on the 24h dial!",
                        color = MutedText,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        } else {
            items(activities, key = { it.id }) { activity ->
                val isActive = activeActivity?.id == activity.id
                val parsedColor = remember(activity.color) { 
                    try { Color(android.graphics.Color.parseColor(activity.color)) } catch(e: Exception) { ImmersiveBlue }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .background(
                            if (isActive) ImmersiveCardActive else ImmersiveCard,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isActive) BlueAccent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp)
                        .testTag("task_item_card"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left-side icon container with activity's color
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(parsedColor.copy(alpha = 0.15f), shape = RoundedCornerShape(12.dp))
                            .border(1.dp, parsedColor.copy(alpha = 0.35f), shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isActive) Icons.Filled.PlayArrow else Icons.Default.Schedule,
                            contentDescription = null,
                            tint = parsedColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(14.dp))
                    
                    // Center details: Title and Times
                    Column(
                        modifier = Modifier.weight(1f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = activity.label,
                                color = if (isActive) Color.White else ImmersiveSlate100,
                                fontSize = 15.sp,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${formatTime12(activity.startH, activity.startM)} • ${formatTime12(activity.endH, activity.endM)}",
                                color = if (isActive) BlueAccent else ImmersiveSlate400,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Text(
                            text = if (isActive) "Current active rhythm segment" else "Scheduled daily routine segment",
                            color = if (isActive) ImmersiveSlate400 else ImmersiveSlate500,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(10.dp))
                    
                    // Action Buttons (Edit & Delete)
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                editingActivity = activity
                                showActivityDialog = true
                            },
                            modifier = Modifier.size(32.dp).testTag("edit_activity_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Activity",
                                tint = BlueAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deleteActivity(activity.id) },
                            modifier = Modifier.size(32.dp).testTag("delete_activity_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Activity",
                                tint = DangerRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = {
                    editingActivity = null
                    showActivityDialog = true
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .height(44.dp)
                    .testTag("add_activity_button")
            ) {
                Icon(
                    imageVector = Icons.Default.AddCircle,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "Add New Activity", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }

    // Activity Dialog trigger
    if (showActivityDialog) {
        ActivityFormDialog(
            activity = editingActivity,
            onDismiss = { showActivityDialog = false },
            onSave = { label, sH, sM, eH, eM, color ->
                viewModel.saveActivity(editingActivity?.id ?: 0L, label, sH, sM, eH, eM, color)
                showActivityDialog = false
            }
        )
    }
}

@Composable
fun DailyRhythmChart(
    activities: List<RoutineActivity>,
    activeActivity: RoutineActivity?,
    currentTime: Date,
    onActivitySelected: (RoutineActivity?) -> Unit
) {
    val centerFormatter = remember { SimpleDateFormat("hh:mm a", Locale.US) }
    val formattedCenterTime = remember(currentTime) { centerFormatter.format(currentTime) }

    Box(
        modifier = Modifier
            .size(270.dp)
            .padding(10.dp),
        contentAlignment = Alignment.Center
    ) {
        val baseCircleColor = BaseCircle
        
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(activities) {
                    detectTapGestures { tapOffset ->
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val dx = tapOffset.x - center.x
                        val dy = tapOffset.y - center.y
                        val distance = sqrt(dx * dx + dy * dy)
                        
                        // Check if tap fell inside dial zone (ring size with thickness tolerances)
                        val strokeW = 100f // Matches our thicker Canvas Stroke below
                        val sizeMin = kotlin.math.min(size.width, size.height).toFloat()
                        val dialRad = (sizeMin / 2f) - (strokeW / 2f) - 10f
                        
                        if (distance >= (dialRad - strokeW / 1.5f) && distance <= (dialRad + strokeW / 1.5f)) {
                            // Find tapped Angle relative to top midnight (-90 degrees)
                            val angleRad = atan2(dy, dx)
                            var angleDeg = Math.toDegrees(angleRad.toDouble()).toFloat()
                            var adjustedAngle = (angleDeg + 90f) % 360f
                            if (adjustedAngle < 0f) {
                                adjustedAngle += 360f
                            }
                            
                            // Map degrees (0-360) directly to minutes of the day (0-1440)
                            val tappedMin = (adjustedAngle / 360f) * 1440f
                            
                            val matched = activities.find { activity ->
                                val startM = activity.startH * 60 + activity.startM
                                var endM = activity.endH * 60 + activity.endM
                                if (endM <= startM) { // Overnight routine
                                    val adjustedEnd = endM + (24 * 60)
                                    val adjustedTapped = if (tappedMin < startM) tappedMin + (24 * 60) else tappedMin
                                    adjustedTapped >= startM && adjustedTapped < adjustedEnd
                                } else {
                                    tappedMin >= startM && tappedMin < endM
                                }
                            }
                            onActivitySelected(matched)
                        } else {
                            onActivitySelected(null)
                        }
                    }
                }
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val strokeWidthPx = 80f // High resolution thick stroke for the dial
            val dialRadius = (size.minDimension / 2f) - (strokeWidthPx / 2f) - 10f

            // 1. Draw dial base frame
            drawCircle(
                color = baseCircleColor,
                radius = dialRadius,
                center = center,
                style = Stroke(width = strokeWidthPx)
            )

            // 2. Draw arcs representing scheduled routines
            activities.forEach { activity ->
                val startMin = activity.startH * 60 + activity.startM
                val endMin = activity.endH * 60 + activity.endM
                
                var sweepMin = endMin - startMin
                if (sweepMin < 0) {
                    sweepMin += 24 * 60
                }
                
                // Mapped to Compose layout angle. Midnight matches -90 degrees (top)
                val startAngle = (startMin.toFloat() / (24f * 60f) * 360f) - 90f
                val sweepAngle = sweepMin.toFloat() / (24f * 60f) * 360f
                
                val parsedColor = try {
                    Color(android.graphics.Color.parseColor(activity.color))
                } catch (e: Exception) {
                    GoldAccent
                }

                drawArc(
                    color = parsedColor,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    topLeft = Offset(center.x - dialRadius, center.y - dialRadius),
                    size = Size(dialRadius * 2f, dialRadius * 2f),
                    style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round)
                )
            }

            // 3. Draw clock quadrant text markings (00, 06, 12, 18) for dial legibility
            // They represent Midnight (00), 6 AM, Noon (12) and 6 PM
            // We draw subtle gray indicator dots right inside the ring
            val dotRadius = dialRadius - strokeWidthPx / 2f - 18f
            val quadrants = listOf(
                Pair(0f - 90f, "00"),
                Pair(90f - 90f, "06"),
                Pair(180f - 90f, "12"),
                Pair(270f - 90f, "18")
            )
            
            quadrants.forEach { (angle, label) ->
                val angleRad = Math.toRadians(angle.toDouble())
                val labelX = center.x + (dotRadius * cos(angleRad).toFloat())
                val labelY = center.y + (dotRadius * sin(angleRad).toFloat())
                drawCircle(
                    color = Color.White.copy(alpha = 0.4f),
                    radius = 2.5f,
                    center = Offset(labelX, labelY)
                )
            }

            // 4. Tracker dot mapping where exact current local time falls
            val calendar = Calendar.getInstance().apply { time = currentTime }
            val currentMin = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
            val currentDegrees = (currentMin.toFloat() / (24f * 60f) * 360f) - 90f
            val currentRad = Math.toRadians(currentDegrees.toDouble())
            
            val trackerX = center.x + (dialRadius * cos(currentRad).toFloat())
            val trackerY = center.y + (dialRadius * sin(currentRad).toFloat())

            // Glow backing
            drawCircle(
                color = GoldAccent.copy(alpha = 0.4f),
                radius = 16f,
                center = Offset(trackerX, trackerY)
            )
            // Tracker core dot
            drawCircle(
                color = Color.White,
                radius = 6f,
                center = Offset(trackerX, trackerY)
            )
        }

        // Inner center display card (Active routine title & Clock time)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(130.dp)
                .clip(CircleShape)
                .background(PureBlack.copy(alpha = 0.5f))
                .padding(6.dp)
        ) {
            Text(
                text = activeActivity?.label ?: "No Active",
                color = if (activeActivity != null) GoldAccent else MutedText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = formattedCenterTime,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Light,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 2.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityFormDialog(
    activity: RoutineActivity?,
    onDismiss: () -> Unit,
    onSave: (String, Int, Int, Int, Int, String) -> Unit
) {
    var label by remember { mutableStateOf(activity?.label ?: "") }
    var startH by remember { mutableStateOf(activity?.startH ?: 0) }
    var startM by remember { mutableStateOf(activity?.startM ?: 0) }
    var endH by remember { mutableStateOf(activity?.endH ?: 0) }
    var endM by remember { mutableStateOf(activity?.endM ?: 0) }
    var selectedColor by remember { mutableStateOf(activity?.color ?: ColorPalette.first()) }

    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(
                onClick = {
                    if (label.trim().isEmpty()) {
                        Toast.makeText(context, "Please enter activity label", Toast.LENGTH_SHORT).show()
                    } else {
                        onSave(label.trim(), startH, startM, endH, endM, selectedColor)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.testTag("submit_button")
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MutedText)
            }
        },
        title = {
            Text(
                text = if (activity == null) "Add New Activity" else "Edit Activity",
                color = GoldAccent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Activity Label text field
                TextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text("Activity Label (e.g., Study, Gym, Sleep)") },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard,
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedLabelColor = GoldAccent,
                        unfocusedLabelColor = MutedText,
                        cursorColor = GoldAccent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("username_input")
                )

                // Pick times (Start)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard, shape = RoundedCornerShape(12.dp))
                        .clickable {
                            TimePickerDialog(context, { _, h, m ->
                                startH = h
                                startM = m
                            }, startH, startM, false).show()
                        }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Start Time", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(formatTime12(startH, startM), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = GoldAccent)
                }

                // Pick times (End)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard, shape = RoundedCornerShape(12.dp))
                        .clickable {
                            TimePickerDialog(context, { _, h, m ->
                                endH = h
                                endM = m
                            }, endH, endM, false).show()
                        }
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("End Time", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        Text(formatTime12(endH, endM), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                    }
                    Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = GoldAccent)
                }

                // Style Color Picker Palette Circles
                Column {
                    Text("Routine Map Segment Color", color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(bottom = 6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ColorPalette.take(4).forEach { colorStr ->
                            val color = Color(android.graphics.Color.parseColor(colorStr))
                            val isSelected = selectedColor == colorStr
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(color, shape = CircleShape)
                                    .border(
                                        if (isSelected) 2.5.dp else 1.dp,
                                        if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = colorStr },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = if (colorStr == "#FFFFFF") Color.Black else Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        ColorPalette.takeLast(4).forEach { colorStr ->
                            val color = Color(android.graphics.Color.parseColor(colorStr))
                            val isSelected = selectedColor == colorStr
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(color, shape = CircleShape)
                                    .border(
                                        if (isSelected) 2.5.dp else 1.dp,
                                        if (isSelected) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColor = colorStr },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Selected",
                                        tint = Color.Black,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        containerColor = DarkCard,
        shape = RoundedCornerShape(16.dp)
    )
}

@Composable
fun RemindersTabScreen(viewModel: RhythmViewModel) {
    val reminders by viewModel.reminders.collectAsStateWithLifecycle()
    
    // States for Form layout directly in tab
    var text by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf("") } // Format: YYYY-MM-DD
    var hour by remember { mutableStateOf(8) }
    var minute by remember { mutableStateOf(0) }
    
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }
    val context = LocalContext.current

    // Set default reminder date to today
    LaunchedEffect(editingReminder) {
        if (editingReminder != null) {
            text = editingReminder!!.text
            dateString = editingReminder!!.date
            hour = editingReminder!!.hour
            minute = editingReminder!!.minute
        } else {
            text = ""
            val today = Calendar.getInstance()
            dateString = String.format("%04d-%02d-%02d", today.get(Calendar.YEAR), today.get(Calendar.MONTH) + 1, today.get(Calendar.DAY_OF_MONTH))
            hour = 8
            minute = 0
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 100.dp) // Cushion above bottom tab bar
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "YOUR REMINDERS",
                color = ImmersiveSlate400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (reminders.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Outlined.NotificationsActive,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "You do not have any reminders set yet! Enter a new reminder in the form below.",
                        color = MutedText,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        } else {
            items(reminders, key = { it.id }) { reminder ->
                val isDone = reminder.isCompleted
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .background(
                            if (isDone) ImmersiveCard.copy(alpha = 0.5f) else ImmersiveCard,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isDone) ImmersiveBorder.copy(alpha = 0.4f) else Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Custom aesthetic check mark icon
                    IconButton(
                        onClick = { viewModel.toggleReminderCompleted(reminder) },
                        modifier = Modifier.size(34.dp).testTag("select_button")
                    ) {
                        Icon(
                            imageVector = if (isDone) Icons.Filled.CheckCircle else Icons.Filled.RadioButtonUnchecked,
                            contentDescription = "Status Toggle",
                            tint = if (isDone) BlueAccent else ImmersiveSlate500,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Text Content
                    Column(
                        modifier = Modifier.weight(1.2f)
                    ) {
                        Text(
                            text = reminder.text,
                            color = if (isDone) ImmersiveSlate500 else Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            textDecoration = if (isDone) TextDecoration.LineThrough else TextDecoration.None,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        
                        Text(
                            text = if (isDone) "Completed reminder" else "Active reminder",
                            color = ImmersiveSlate500,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // Date Time
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = reminder.date,
                            color = ImmersiveSlate400,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = formatTime12(reminder.hour, reminder.minute),
                            color = if (isDone) ImmersiveSlate400 else BlueAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Edit / Delete actions
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { editingReminder = reminder },
                            modifier = Modifier.size(32.dp).testTag("edit_reminder_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit Reminder",
                                tint = BlueAccent,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = { viewModel.deleteReminder(reminder.id) },
                            modifier = Modifier.size(32.dp).testTag("delete_reminder_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Reminder",
                                tint = DangerRed,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // "Add New / Edit Reminder" Form Panel (Replicating exact CSS form-card style)
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ImmersiveBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = ImmersiveCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = if (editingReminder == null) "Schedule New Reminder" else "Edit Scheduled Reminder",
                        color = BlueAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Label input text
                    Column {
                        Text("Reminder Text", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp))
                        OutlinedTextField(
                            value = text,
                            onValueChange = { text = it },
                            placeholder = { Text("e.g. Call Mom, Water Plants, Homework") },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("username_input"),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = PureBlack,
                                unfocusedContainerColor = PureBlack,
                                disabledContainerColor = PureBlack,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                cursorColor = BlueAccent,
                                focusedIndicatorColor = ImmersiveBorder,
                                unfocusedIndicatorColor = ImmersiveBorder
                            )
                        )
                    }

                    // Pick Date clicking row
                    Column {
                        Text("Select Date", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PureBlack, shape = RoundedCornerShape(20.dp))
                                .border(1.dp, ImmersiveBorder, shape = RoundedCornerShape(20.dp))
                                .clickable {
                                    val cal = Calendar.getInstance()
                                    if (dateString.isNotEmpty()) {
                                        try {
                                            val parts = dateString.split("-")
                                            if (parts.size == 3) {
                                                cal.set(Calendar.YEAR, parts[0].toInt())
                                                cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                                                cal.set(Calendar.DAY_OF_MONTH, parts[2].toInt())
                                            }
                                        } catch (_: Exception) {}
                                    }
                                    DatePickerDialog(context, { _, y, m, d ->
                                        dateString = String.format("%04d-%02d-%02d", y, m + 1, d)
                                    }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show()
                                }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = if (dateString.isEmpty()) "Tap to select Date" else dateString, color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                            Icon(imageVector = Icons.Default.CalendarToday, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Pick Time clicking row
                    Column {
                        Text("Select Time", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PureBlack, shape = RoundedCornerShape(20.dp))
                                .border(1.dp, ImmersiveBorder, shape = RoundedCornerShape(20.dp))
                                .clickable {
                                    TimePickerDialog(context, { _, h, m ->
                                        hour = h
                                        minute = m
                                    }, hour, minute, false).show()
                                }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = formatTime12(hour, minute), color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace)
                            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = BlueAccent, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Actions Button layout
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                if (text.trim().isEmpty()) {
                                    Toast.makeText(context, "Please enter reminder text", Toast.LENGTH_SHORT).show()
                                } else {
                                    viewModel.saveReminder(editingReminder?.id ?: 0L, text.trim(), dateString, hour, minute)
                                    text = ""
                                    editingReminder = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f).height(42.dp).testTag("save_reminder_button")
                        ) {
                            Text(text = if (editingReminder == null) "Add Reminder" else "Update", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }

                        Button(
                            onClick = {
                                text = ""
                                editingReminder = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBorder, contentColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f).height(42.dp).testTag("cancel_reminder_button")
                        ) {
                            Text(text = "Cancel", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AlarmsTabScreen(viewModel: RhythmViewModel) {
    val alarms by viewModel.alarms.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var label by remember { mutableStateOf("") }
    var hour by remember { mutableStateOf(7) }
    var minute by remember { mutableStateOf(30) }
    var monday by remember { mutableStateOf(false) }
    var tuesday by remember { mutableStateOf(false) }
    var wednesday by remember { mutableStateOf(false) }
    var thursday by remember { mutableStateOf(false) }
    var friday by remember { mutableStateOf(false) }
    var saturday by remember { mutableStateOf(false) }
    var sunday by remember { mutableStateOf(false) }

    var editingAlarm by remember { mutableStateOf<com.example.data.Alarm?>(null) }

    LaunchedEffect(editingAlarm) {
        if (editingAlarm != null) {
            label = editingAlarm!!.label
            hour = editingAlarm!!.hour
            minute = editingAlarm!!.minute
            monday = editingAlarm!!.monday
            tuesday = editingAlarm!!.tuesday
            wednesday = editingAlarm!!.wednesday
            thursday = editingAlarm!!.thursday
            friday = editingAlarm!!.friday
            saturday = editingAlarm!!.saturday
            sunday = editingAlarm!!.sunday
        } else {
            label = ""
            hour = 7
            minute = 30
            monday = true
            tuesday = true
            wednesday = true
            thursday = true
            friday = true
            saturday = false
            sunday = false
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(bottom = 100.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "ALARM SYSTEM",
                color = ImmersiveSlate400,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (alarms.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 30.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.AccessTime,
                        contentDescription = null,
                        tint = MutedText,
                        modifier = Modifier.size(54.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "No alarms set yet. Configure a custom alarm in the panel below.",
                        color = MutedText,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        } else {
            items(alarms, key = { it.id }) { alarm ->
                val isActive = alarm.isActive
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp)
                        .background(
                            if (isActive) ImmersiveCardActive else ImmersiveCard,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .border(
                            width = 1.dp,
                            color = if (isActive) GoldAccent.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.06f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.Bottom) {
                            val displayHour = if (alarm.hour % 12 == 0) 12 else alarm.hour % 12
                            val formatMin = String.format("%02d", alarm.minute)
                            val displayPeriod = if (alarm.hour >= 12) "PM" else "AM"
                            
                            Text(
                                text = "$displayHour:$formatMin",
                                color = Color.White,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = " $displayPeriod",
                                color = ImmersiveSlate400,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }
                        
                        Text(
                            text = if (alarm.label.isNotEmpty()) alarm.label else "Alarm",
                            color = if (isActive) GoldAccent else ImmersiveSlate400,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        
                        // Days check row
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            val days = listOf(
                                Pair("M", alarm.monday),
                                Pair("T", alarm.tuesday),
                                Pair("W", alarm.wednesday),
                                Pair("T", alarm.thursday),
                                Pair("F", alarm.friday),
                                Pair("S", alarm.saturday),
                                Pair("S", alarm.sunday)
                            )
                            days.forEach { (name, enabled) ->
                                Box(
                                    modifier = Modifier
                                        .size(16.dp)
                                        .background(
                                            if (enabled) GoldAccent.copy(alpha = 0.2f) else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (enabled) GoldAccent.copy(alpha = 0.5f) else ImmersiveSlate600.copy(alpha = 0.3f),
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name,
                                        color = if (enabled) GoldAccent else ImmersiveSlate500,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Toggle & delete actions
                    Row(
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Switch(
                            checked = alarm.isActive,
                            onCheckedChange = { viewModel.toggleAlarmActive(alarm) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = GoldAccent,
                                uncheckedThumbColor = ImmersiveSlate500,
                                uncheckedTrackColor = ImmersiveBorder
                            )
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(onClick = { editingAlarm = alarm }) {
                            Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit", tint = BlueAccent, modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { viewModel.deleteAlarm(alarm.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = DangerRed, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        }

        // Add / Edit form card
        item {
            Spacer(modifier = Modifier.height(24.dp))
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, ImmersiveBorder, RoundedCornerShape(16.dp)),
                colors = CardDefaults.cardColors(containerColor = ImmersiveCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (editingAlarm == null) "Configure New Alarm" else "Edit Configured Alarm",
                        color = GoldAccent,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Text label input
                    Column {
                        Text("Alarm Label", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp))
                        OutlinedTextField(
                            value = label,
                            onValueChange = { label = it },
                            placeholder = { Text("e.g. Wake up, Sleep, Meditation") },
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth(),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = PureBlack,
                                unfocusedContainerColor = PureBlack,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedIndicatorColor = ImmersiveBorder,
                                unfocusedIndicatorColor = ImmersiveBorder
                            )
                        )
                    }

                    // Pick Time clicking row
                    Column {
                        Text("Select Time", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PureBlack, shape = RoundedCornerShape(20.dp))
                                .border(1.dp, ImmersiveBorder, shape = RoundedCornerShape(20.dp))
                                .clickable {
                                    TimePickerDialog(context, { _, h, m ->
                                        hour = h
                                        minute = m
                                    }, hour, minute, false).show()
                                }
                                .padding(horizontal = 16.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val tempHour12 = if (hour % 12 == 0) 12 else hour % 12
                            val tempMinStr = String.format("%02d", minute)
                            val tempPeriod = if (hour >= 12) "PM" else "AM"
                            Text(text = "$tempHour12:$tempMinStr $tempPeriod", color = Color.White, fontSize = 14.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                            Icon(imageVector = Icons.Default.Schedule, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Repeating Days selection grid/row
                    Column {
                        Text("Repeat Days", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(bottom = 6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val repeatingDays = listOf(
                                Triple("Mon", monday) { monday = !monday },
                                Triple("Tue", tuesday) { tuesday = !tuesday },
                                Triple("Wed", wednesday) { wednesday = !wednesday },
                                Triple("Thu", thursday) { thursday = !thursday },
                                Triple("Fri", friday) { friday = !friday },
                                Triple("Sat", saturday) { saturday = !saturday },
                                Triple("Sun", sunday) { sunday = !sunday }
                            )
                            repeatingDays.forEach { (name, active, toggle) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 2.dp)
                                        .height(34.dp)
                                        .background(
                                            if (active) GoldAccent.copy(alpha = 0.15f) else Color.Transparent,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .border(
                                            width = 1.dp,
                                            color = if (active) GoldAccent.copy(alpha = 0.4f) else ImmersiveBorder,
                                            shape = RoundedCornerShape(8.dp)
                                        )
                                        .clickable { toggle() },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = name.take(3),
                                        color = if (active) GoldAccent else ImmersiveSlate400,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.saveAlarm(
                                    editingAlarm?.id ?: 0L,
                                    label.trim(),
                                    hour,
                                    minute,
                                    true,
                                    monday,
                                    tuesday,
                                    wednesday,
                                    thursday,
                                    friday,
                                    saturday,
                                    sunday
                                )
                                label = ""
                                editingAlarm = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Black),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = if (editingAlarm == null) "Set Alarm" else "Update", fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                label = ""
                                editingAlarm = null
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = ImmersiveBorder, contentColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = "Cancel", fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}
