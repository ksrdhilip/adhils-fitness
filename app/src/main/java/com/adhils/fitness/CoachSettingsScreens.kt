package com.adhils.fitness

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.adhils.fitness.core.*
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

@Composable fun CoachScreen(state:AppState,vm:FitnessViewModel) {
    val busy by vm.busy.collectAsState()
    val suggestion by vm.proposal.collectAsState()
    var message by remember { mutableStateOf("") }
    val week=state.sessions.filter {it.finishedAt!=null && it.finishedAt!!>=System.currentTimeMillis()-7*86400000L}
    SectionTitle("Your coach", "Advice for ${state.profile.name} · ${state.profile.goal}")
    PanelCard {
        SectionTitle("This week", "${week.size} workouts · ${week.sumOf {s->s.results.count {!it.warmup}}} working sets")
        Text("Your profile, restrictions and recent workouts provide the context. Camera video stays on this device.")
        OutlinedButton(onClick={vm.ask("Review my last seven days and explain what to focus on next week.",true)},enabled=!busy) {Text("Review my week")}
    }
    if(state.chats.isEmpty()) EmptyState("Start a conversation", "Ask why an exercise is in your plan, or request a shorter workout. Connect your PC in Settings first.")
    state.chats.takeLast(30).forEach {entry->
        PanelCard { SmallLabel(if(entry.role=="you") state.profile.name else "COACH");Text(entry.text) }
    }
    suggestion?.proposal?.let {proposal->
        PanelCard {
            SectionTitle("Review workout change",proposal.reason)
            proposal.changes.forEach {change->Text("${Catalog.byId[change.exerciseId]?.name ?: change.exerciseId}: ${change.sets} sets")}
            Text("Completed sets are preserved. This applies only to the workout version used by the coach.")
            PrimaryButton("Apply to my workout",!busy) {vm.applyProposal()}
            TextButton(onClick={vm.proposal.value=null}) {Text("Keep my current plan")}
        }
    }
    OutlinedTextField(value=message,onValueChange={message=it.take(4000)},label={Text("Ask your coach")},
        modifier=Modifier.fillMaxWidth(),minLines=2,maxLines=6)
    PrimaryButton(if(busy) "Waiting for your PC…" else "Send",!busy && message.isNotBlank()) {vm.ask(message);message=""}
    SmallLabel("If a movement causes pain, stop that movement. The coach does not diagnose injuries.")
}

@Composable fun SettingsScreen(state:AppState,vm:FitnessViewModel,onProfile:()->Unit,onProfiles:()->Unit) {
    val context=LocalContext.current
    val scope=rememberCoroutineScope()
    val busy by vm.busy.collectAsState()
    val status by vm.connectionStatus.collectAsState()
    var provider by remember {mutableStateOf(vm.companion.provider)}
    var pairing by remember {mutableStateOf(false)}
    var invitation by remember {mutableStateOf("")}
    var backupMode by remember {mutableStateOf<String?>(null)}
    var password by remember {mutableStateOf("")}
    var pendingExport by remember {mutableStateOf<ByteArray?>(null)}
    var pendingImport by remember {mutableStateOf<ByteArray?>(null)}
    var preview by remember {mutableStateOf<ProfileStore?>(null)}
    var working by remember {mutableStateOf(false)}
    var notice by remember {mutableStateOf<String?>(null)}
    var clear by remember {mutableStateOf(false)}
    val exportFile=rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) {uri->
        val bytes=pendingExport;pendingExport=null
        if(uri!=null && bytes!=null) scope.launch {
            working=true
            try {withContext(Dispatchers.IO) {context.contentResolver.openOutputStream(uri,"wt")?.use {it.write(bytes)} ?: error("Could not open destination")};notice="Encrypted backup saved."}
            catch(e:Exception) {vm.error.value="Could not save backup: ${e.message}"} finally {working=false}
        }
    }
    val importFile=rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) {uri->
        if(uri!=null) scope.launch {
            working=true
            try {
                pendingImport=withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use {input->
                        val out=ByteArrayOutputStream();val block=ByteArray(8192)
                        while(true) {val n=input.read(block);if(n<0) break;require(out.size()+n<=16*1024*1024) {"Backup exceeds 16 MB"};out.write(block,0,n)}
                        out.toByteArray()
                    } ?: error("Could not open backup")
                }
                password="";backupMode="import"
            } catch(e:Exception) {vm.error.value=e.message ?: "Could not read backup"} finally {working=false}
        }
    }
    val scanner=rememberLauncherForActivityResult(ScanContract()) {result->result.contents?.let {vm.pair(it)}}
    SectionTitle("Settings", "Personalize your training and connect your devices.")
    PanelCard {
        SectionTitle(state.profile.name,"${state.profile.experience} · ${state.profile.days} days a week")
        Row {TextButton(onClick=onProfile) {Text("Edit profile")};TextButton(onClick=onProfiles) {Text("Switch or add profile")}}
        SmallLabel("Workouts, progress and coach context are kept separately for each profile.")
    }
    PanelCard {
        SectionTitle("Appearance & camera")
        Row(horizontalArrangement=Arrangement.spacedBy(6.dp)) {
            listOf("Dark","Light","System").forEach {theme->FilterChip(selected=state.profile.theme==theme,onClick={vm.edit {it.copy(profile=it.profile.copy(theme=theme))}},label={Text(theme)})}
        }
        SettingSwitch("Spoken movement cues",state.profile.voice) {value->vm.edit {it.copy(profile=it.profile.copy(voice=value))}}
        SettingSwitch("Experimental movement cues",state.profile.experimentalCues) {value->vm.edit {it.copy(profile=it.profile.copy(experimentalCues=value))}}
        SmallLabel("Movement cues are a preview and need real-device validation. Camera counts can always be edited before saving.")
    }
    PanelCard {
        SectionTitle("AI connection",status)
        listOf("codex" to "ChatGPT via your PC", "openai" to "OpenAI API", "claude" to "Claude API").forEach {(id,label)->
            FilterChip(selected=provider==id,onClick={provider=id;vm.companion.provider=id},label={Text(label)})
        }
        Text("ChatGPT mode uses the Codex login on your PC. Optional API keys stay on the PC, with a shared $5/month app budget across profiles and paired devices.")
        Text("Keep the PC awake and connect both devices to the same trusted private network.")
        PrimaryButton("Scan PC pairing QR",!busy) {scanner.launch(ScanOptions().setDesiredBarcodeFormats(ScanOptions.QR_CODE).setPrompt("Scan the invitation on your own PC").setBeepEnabled(false).setOrientationLocked(false))}
        Row {TextButton(onClick={pairing=true},enabled=!busy) {Text("Paste invitation")};TextButton(onClick=vm::status,enabled=!busy) {Text("Check connection")}}
        TextButton(onClick={vm.companion.disconnect();vm.connectionStatus.value="Disconnected"}) {Text("Disconnect this device")}
    }
    PanelCard {
        SectionTitle("Backup & transfer", "Transfer all profiles between your S22 and Galaxy Tab A9+.")
        Text("Export an encrypted file, then import it on the other device. Import replaces all profiles on that device. Videos and PC pairing credentials are not transferred.")
        PrimaryButton("Export encrypted backup",!working) {password="";backupMode="export"}
        OutlinedButton(onClick={importFile.launch(arrayOf("*/*"))},enabled=!working,modifier=Modifier.fillMaxWidth()) {Text("Import backup")}
        notice?.let {Text(it)}
        if(working) LinearProgressIndicator(Modifier.fillMaxWidth())
    }
    TextButton(onClick={clear=true}) {Text("Clear ${state.profile.name}'s training data",color=MaterialTheme.colorScheme.error)}
    SmallLabel("ADhils Fitness 0.1 · Personal preview")
    if(pairing) AlertDialog(onDismissRequest={pairing=false;invitation=""},title={Text("Pair with your PC")},text={
        OutlinedTextField(value=invitation,onValueChange={invitation=it.take(4096)},label={Text("Invitation JSON")},maxLines=5)
    },confirmButton={TextButton(onClick={vm.pair(invitation);invitation="";pairing=false},enabled=invitation.isNotBlank()) {Text("Pair")}},dismissButton={TextButton(onClick={pairing=false;invitation=""}) {Text("Cancel")}})
    backupMode?.let {mode->AlertDialog(onDismissRequest={if(!working) {backupMode=null;password="";pendingImport=null}},
        title={Text(if(mode=="export") "Protect your backup" else "Unlock backup")},text={Column {
            Text(if(mode=="export") "Use a password of at least 10 characters. Keep it somewhere safe; it cannot be recovered." else "Enter the password used to export this file. You will review its profiles before replacing anything.")
            OutlinedTextField(value=password,onValueChange={password=it.take(256)},label={Text("Backup password")},visualTransformation=PasswordVisualTransformation(),singleLine=true,keyboardOptions=KeyboardOptions(autoCorrectEnabled=false))
        }},confirmButton={TextButton(enabled=!working && password.length>=10,onClick={
            val secret=password.toCharArray();password="";working=true
            scope.launch {try {
                if(mode=="export") {pendingExport=vm.export(secret);backupMode=null;exportFile.launch("ADhils-${java.time.LocalDate.now()}.fitness-backup")}
                else {preview=vm.decodeBackup(pendingImport ?: error("Choose a backup first"),secret);pendingImport=null;backupMode=null}
            } catch(e:Exception) {vm.error.value=if(mode=="import") "Could not unlock this backup. Check the password and file." else e.message}
            finally {secret.fill('\u0000');working=false}}
        }) {Text(if(working) "Working…" else if(mode=="export") "Choose destination" else "Preview")}},dismissButton={TextButton(enabled=!working,onClick={backupMode=null;password="";pendingImport=null}) {Text("Cancel")}})}
    preview?.let {value->AlertDialog(onDismissRequest={preview=null},title={Text("Replace all local profiles?")},text={
        Text("This backup contains ${value.profiles.size} profiles: ${value.profiles.joinToString {it.state.profile.name}}. It will replace every profile and workout currently on this device. Export your current data first if you need to keep it.")
    },confirmButton={TextButton(onClick={vm.restore(value);preview=null;notice="Backup restore requested."}) {Text("Replace with backup")}},dismissButton={TextButton(onClick={preview=null}) {Text("Cancel")}})}
    if(clear) AlertDialog(onDismissRequest={clear=false},title={Text("Clear this profile's data?")},text={Text("Remove ${state.profile.name}'s workouts, measurements, routines and chats. Profile settings and other profiles are kept.")},confirmButton={TextButton(onClick={vm.clearProfileData();clear=false}) {Text("Clear data")}},dismissButton={TextButton(onClick={clear=false}) {Text("Cancel")}})
}

@Composable private fun SettingSwitch(label:String,value:Boolean,onChange:(Boolean)->Unit) {
    Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(12.dp)) {Text(label,Modifier.weight(1f));Switch(checked=value,onCheckedChange=onChange)}
}
