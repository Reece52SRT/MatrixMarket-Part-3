package com.matrixmarket.app.ui.screens.trademeeting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matrixmarket.app.R
import com.matrixmarket.app.di.AppContainer
import com.matrixmarket.app.ui.components.asString
import com.matrixmarket.app.ui.theme.CodeBackground
import com.matrixmarket.app.ui.theme.ErrorRed
import com.matrixmarket.app.ui.theme.LavenderLight
import com.matrixmarket.app.ui.theme.MeetingTeal
import com.matrixmarket.app.ui.theme.SuccessGreen
import com.matrixmarket.app.ui.theme.TextMuted
import com.matrixmarket.app.util.DateFormats
import com.matrixmarket.app.util.Resource
import com.matrixmarket.app.util.ViewModelFactory
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Verified on-campus meeting spots.
val SafeSpots = listOf("Student Center North", "Library Foyer", "Campus Security Lobby")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleMeetingScreen(offerId: Int, onBack: () -> Unit, onDone: () -> Unit) {
    val viewModel: TradeMeetingViewModel = viewModel(factory = ViewModelFactory { TradeMeetingViewModel(AppContainer.tradeRepository) })
    val scheduleState by viewModel.scheduleState.collectAsState()
    val scheduleError by viewModel.scheduleError.collectAsState()

    var selectedSpot by rememberSaveable { mutableStateOf("") }
    var selectedDate by rememberSaveable { mutableStateOf(LocalDate.now().plusDays(1).toString()) }
    var selectedTime by rememberSaveable { mutableStateOf("14:00") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val date = LocalDate.parse(selectedDate)
    val time = LocalTime.parse(selectedTime)

    Surface(color = LavenderLight, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                }
                Text(stringResource(R.string.trade_meeting), style = MaterialTheme.typography.headlineSmall)
            }
            Text(stringResource(R.string.schedule_subtitle), color = TextMuted)

            val current = scheduleState
            if (current is Resource.Success) {
                val meeting = current.data
                Spacer(Modifier.height(20.dp))
                Surface(color = CodeBackground, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = SuccessGreen)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.meeting_scheduled), fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(meeting.listingTitle, style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.meeting_location, meeting.campusSpot))
                        Text(stringResource(R.string.meeting_time, DateFormats.meeting(meeting.meetingTime)))
                        if (meeting.confirmationCode.isNotBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(stringResource(R.string.your_code), color = TextMuted, style = MaterialTheme.typography.bodySmall)
                            Text(meeting.confirmationCode, fontWeight = FontWeight.Bold, fontSize = 32.sp, letterSpacing = 6.sp)
                            Spacer(Modifier.height(6.dp))
                            Text(stringResource(R.string.show_code_to_seller), style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = onDone,
                    colors = ButtonDefaults.buttonColors(containerColor = MeetingTeal),
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) { Text(stringResource(R.string.go_to_meetings), color = Color.White) }
                return@Column
            }

            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.choose_safe_spot), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            SafeSpots.forEach { spot ->
                val selected = selectedSpot == spot
                Surface(
                    color = if (selected) MeetingTeal.copy(alpha = 0.15f) else Color.White,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp).clickable { selectedSpot = spot }
                ) {
                    Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocationOn, contentDescription = null, tint = if (selected) MeetingTeal else TextMuted)
                        Spacer(Modifier.width(10.dp))
                        Text(spot, modifier = Modifier.weight(1f), fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal)
                        if (selected) Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = MeetingTeal)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(stringResource(R.string.select_date_time), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(date.format(DateTimeFormatter.ofPattern("d MMM yyyy")))
                }
                OutlinedButton(onClick = { showTimePicker = true }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp)) {
                    Icon(Icons.Filled.Schedule, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(time.format(DateTimeFormatter.ofPattern("HH:mm")))
                }
            }

            scheduleError?.let {
                Spacer(Modifier.height(12.dp))
                Text(it.asString(), color = ErrorRed)
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = { viewModel.scheduleMeeting(offerId, selectedSpot, LocalDateTime.of(date, time)) },
                enabled = scheduleState !is Resource.Loading,
                colors = ButtonDefaults.buttonColors(containerColor = MeetingTeal),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (scheduleState is Resource.Loading) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text(stringResource(R.string.confirm_meeting), color = Color.White)
                }
            }
        }
    }

    if (showDatePicker) {
        val todayUtcMillis = LocalDate.now().atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli(),
            selectableDates = object : SelectableDates {
                override fun isSelectableDate(utcTimeMillis: Long): Boolean = utcTimeMillis >= todayUtcMillis
            }
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        // DatePicker works in UTC midnight - convert with UTC to avoid off-by-one days.
                        selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate().toString()
                    }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) } }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(initialHour = time.hour, initialMinute = time.minute, is24Hour = true)
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute).toString()
                    showTimePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = { TextButton(onClick = { showTimePicker = false }) { Text(stringResource(R.string.cancel)) } },
            text = {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TimePicker(state = timePickerState) }
            }
        )
    }
}
