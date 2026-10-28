package com.matrixmarket.app.ui.screens.trademeeting

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.matrixmarket.app.R
import com.matrixmarket.app.data.remote.dto.MyTradeMeetingResponse
import com.matrixmarket.app.di.AppContainer
import com.matrixmarket.app.ui.components.EmptyBox
import com.matrixmarket.app.ui.components.ErrorBox
import com.matrixmarket.app.ui.components.LoadingBox
import com.matrixmarket.app.ui.components.Pill
import com.matrixmarket.app.ui.components.ScreenHeader
import com.matrixmarket.app.ui.components.asString
import com.matrixmarket.app.ui.components.displayText
import com.matrixmarket.app.ui.components.roleLabel
import com.matrixmarket.app.ui.theme.CodeBackground
import com.matrixmarket.app.ui.theme.ErrorRed
import com.matrixmarket.app.ui.theme.LavenderLight
import com.matrixmarket.app.ui.theme.MeetingTeal
import com.matrixmarket.app.ui.theme.SuccessGreen
import com.matrixmarket.app.ui.theme.TextMuted
import com.matrixmarket.app.util.DateFormats
import com.matrixmarket.app.util.Resource
import com.matrixmarket.app.util.ViewModelFactory

// The "Meet" tab: every trade meeting the user is part of.
// Buyers see their code; sellers type it in at the meeting to confirm.
@Composable
fun MyMeetingsScreen() {
    val viewModel: TradeMeetingViewModel = viewModel(factory = ViewModelFactory { TradeMeetingViewModel(AppContainer.tradeRepository) })
    val meetingsState by viewModel.myMeetings.collectAsState()
    val confirmState by viewModel.confirmState.collectAsState()

    LaunchedEffect(Unit) { viewModel.loadMyMeetings() }

    Surface(color = LavenderLight, modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(12.dp))
            ScreenHeader(stringResource(R.string.trade_meetings), stringResource(R.string.meetings_subtitle)) {
                IconButton(onClick = { viewModel.loadMyMeetings() }) {
                    Icon(Icons.Filled.Refresh, contentDescription = stringResource(R.string.refresh))
                }
            }
            Spacer(Modifier.height(12.dp))

            when (val state = meetingsState) {
                is Resource.Loading, Resource.Idle -> LoadingBox(color = MeetingTeal)
                is Resource.Error -> ErrorBox(state.displayText(), onRetry = { viewModel.loadMyMeetings() })
                is Resource.Success -> {
                    if (state.data.isEmpty()) {
                        EmptyBox(stringResource(R.string.no_active_trade_meetings), stringResource(R.string.no_meetings_desc))
                    } else {
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(state.data, key = { it.meetingId }) { meeting ->
                                MeetingCard(
                                    meeting = meeting,
                                    confirmState = confirmState,
                                    onConfirm = { code -> viewModel.confirmTrade(meeting.meetingId, code) }
                                )
                            }
                            item { Spacer(Modifier.height(12.dp)) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MeetingCard(meeting: MyTradeMeetingResponse, confirmState: ConfirmUiState, onConfirm: (String) -> Unit) {
    var expanded by rememberSaveable(meeting.meetingId) { mutableStateOf(!meeting.isConfirmed) }
    var codeInput by rememberSaveable(meeting.meetingId) { mutableStateOf("") }
    val isThisMeeting = confirmState.meetingId == meeting.meetingId

    Surface(color = Color.White, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp)) {
            Row(
                Modifier.fillMaxWidth().clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Pill(roleLabel(meeting.role), MeetingTeal, filled = false)
                Spacer(Modifier.width(8.dp))
                Text(meeting.listingTitle, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                if (meeting.isConfirmed) {
                    Icon(Icons.Filled.CheckCircle, contentDescription = stringResource(R.string.confirmed), tint = SuccessGreen)
                }
                Icon(if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore, contentDescription = null, tint = TextMuted)
            }
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.meeting_with, meeting.otherPartyName, DateFormats.formatRand(meeting.price)),
                style = MaterialTheme.typography.bodyMedium, color = TextMuted
            )
            Text(
                "${meeting.campusSpot} · ${DateFormats.meeting(meeting.meetingTime)}",
                style = MaterialTheme.typography.bodyMedium, color = TextMuted
            )

            if (meeting.isConfirmed) {
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.trade_complete), color = SuccessGreen, fontWeight = FontWeight.SemiBold)
                return@Column
            }
            if (!expanded) return@Column

            Spacer(Modifier.height(10.dp))
            if (meeting.role.equals("Buyer", ignoreCase = true)) {
                // Buyer: show the code to the seller at the meeting.
                Surface(color = CodeBackground, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp)) {
                        Text(stringResource(R.string.your_code), style = MaterialTheme.typography.bodySmall, color = TextMuted)
                        Text(meeting.confirmationCode, fontWeight = FontWeight.Bold, fontSize = 26.sp, letterSpacing = 5.sp)
                        Text(stringResource(R.string.show_code_to_seller), style = MaterialTheme.typography.bodySmall)
                    }
                }
            } else if (meeting.canConfirm) {
                // Seller: type in the buyer's code to confirm the hand-over.
                Text(stringResource(R.string.enter_buyer_code), style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = codeInput,
                    onValueChange = { codeInput = it.filter(Char::isDigit).take(6) },
                    label = { Text(stringResource(R.string.six_digit_code)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier.fillMaxWidth()
                )
                if (isThisMeeting) {
                    confirmState.error?.let {
                        Spacer(Modifier.height(6.dp))
                        Text(it.asString(), color = ErrorRed, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = { onConfirm(codeInput) },
                    enabled = !(isThisMeeting && confirmState.isSending),
                    colors = ButtonDefaults.buttonColors(containerColor = MeetingTeal),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isThisMeeting && confirmState.isSending) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text(stringResource(R.string.confirm_trade), color = Color.White)
                    }
                }
            }
        }
    }
}
