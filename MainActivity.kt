package com.riveradmin.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.room.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow

private val Ink = Color(0xFF09090B)
private val Panel = Color(0xFF17171D)
private val Panel2 = Color(0xFF222229)
private val RiverRed = Color(0xFFC70E1A)
private val Gold = Color(0xFFE0AD4D)
private val Muted = Color(0xFFB5B5BE)
private val Green = Color(0xFF43B581)

@Entity(tableName = "communities")
data class CommunityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val members: Int,
    val status: String = "نشط",
    val notes: String = ""
)

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val community: String,
    val category: String,
    val details: String,
    val status: String = "مفتوح",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "moderators")
data class ModeratorEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val role: String,
    val community: String,
    val status: String = "نشط"
)

@Entity(tableName = "audit_log")
data class AuditEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val action: String,
    val target: String,
    val details: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface RiverDao {
    @Query("SELECT * FROM communities ORDER BY name") fun communities(): Flow<List<CommunityEntity>>
    @Query("SELECT COUNT(*) FROM communities") suspend fun communityCount(): Int
    @Insert suspend fun addCommunity(item: CommunityEntity)
    @Query("SELECT * FROM reports ORDER BY createdAt DESC") fun reports(): Flow<List<ReportEntity>>
    @Query("SELECT COUNT(*) FROM reports WHERE status = 'مفتوح'") fun openReportCount(): Flow<Int>
    @Insert suspend fun addReport(item: ReportEntity)
    @Query("UPDATE reports SET status = :status WHERE id = :id") suspend fun updateReportStatus(id: Long, status: String)
    @Query("SELECT * FROM moderators ORDER BY name") fun moderators(): Flow<List<ModeratorEntity>>
    @Insert suspend fun addModerator(item: ModeratorEntity)
    @Query("SELECT * FROM audit_log ORDER BY createdAt DESC") fun auditLogs(): Flow<List<AuditEntity>>
    @Insert suspend fun addAudit(item: AuditEntity)
}

@Database(
    entities = [CommunityEntity::class, ReportEntity::class, ModeratorEntity::class, AuditEntity::class],
    version = 1,
    exportSchema = false
)
abstract class RiverDatabase : RoomDatabase() {
    abstract fun dao(): RiverDao
    companion object {
        @Volatile private var INSTANCE: RiverDatabase? = null
        fun get(context: android.content.Context): RiverDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext, RiverDatabase::class.java, "river_admin.db"
                ).build().also { INSTANCE = it }
            }
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val db = RiverDatabase.get(this)
        val dao = db.dao()
        kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            if (dao.communityCount() == 0) {
                dao.addCommunity(CommunityEntity(name = "Anime العربي", members = 12480, notes = "بيانات تجريبية محلية"))
                dao.addCommunity(CommunityEntity(name = "مجتمع الألعاب", members = 5830, notes = "بيانات تجريبية محلية"))
                dao.addModerator(ModeratorEntity(name = "مشرف تجريبي", role = "مشرف مجتمع", community = "Anime العربي"))
                dao.addReport(ReportEntity(community = "Anime العربي", category = "إساءة لفظية", details = "بلاغ تجريبي — استبدله ببيانات حقيقية"))
                dao.addAudit(AuditEntity(action = "تهيئة النظام", target = "River Admin", details = "تم إنشاء قاعدة البيانات المحلية"))
            }
        }
        setContent { RiverApp(dao) }
    }
}

private enum class Screen(val label: String, val symbol: String) {
    HOME("الرئيسية", "⌂"),
    COMMUNITIES("المجتمعات", "▦"),
    REPORTS("البلاغات", "⚑"),
    MODERATORS("المشرفون", "♙"),
    AUDIT("السجل", "☷"),
    SETTINGS("الإعدادات", "⚙")
}

@Composable
private fun RiverApp(dao: RiverDao) {
    var current by remember { mutableStateOf(Screen.HOME) }
    val communities by dao.communities().collectAsState(initial = emptyList())
    val reports by dao.reports().collectAsState(initial = emptyList())
    val moderators by dao.moderators().collectAsState(initial = emptyList())
    val audit by dao.auditLogs().collectAsState(initial = emptyList())
    val openCount by dao.openReportCount().collectAsState(initial = 0)
    val scope = rememberCoroutineScope()

    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = RiverRed,
            secondary = Gold,
            background = Ink,
            surface = Panel,
            onPrimary = Color.White,
            onBackground = Color.White,
            onSurface = Color.White
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Scaffold(
                containerColor = Ink,
                topBar = {
                    Column(Modifier.fillMaxWidth().background(Ink).padding(horizontal = 18.dp, vertical = 12.dp)) {
                        Text("RIVER ADMIN", color = Gold, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(5.dp))
                        Text(current.label, color = Color.White, fontSize = 25.sp, fontWeight = FontWeight.Bold)
                        Text("إدارة محلية مستقلة • لا يوجد اتصال بـ KarboAI", color = Muted, fontSize = 11.sp)
                    }
                },
                bottomBar = {
                    NavigationBar(containerColor = Panel) {
                        listOf(Screen.HOME, Screen.COMMUNITIES, Screen.REPORTS, Screen.MODERATORS, Screen.AUDIT, Screen.SETTINGS).forEach { s ->
                            NavigationBarItem(
                                selected = current == s,
                                onClick = { current = s },
                                icon = { Text(s.symbol, fontSize = 18.sp) },
                                label = { Text(s.label, fontSize = 9.sp, maxLines = 1) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Gold,
                                    selectedTextColor = Gold,
                                    indicatorColor = Panel2,
                                    unselectedIconColor = Muted,
                                    unselectedTextColor = Muted
                                )
                            )
                        }
                    }
                }
            ) { padding ->
                Column(
                    Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    when (current) {
                        Screen.HOME -> HomeScreen(communities.size, openCount, moderators.size, audit.size) { current = it }
                        Screen.COMMUNITIES -> CommunitiesScreen(communities, onAdd = { name, members, notes ->
                            scope.launch {
                                dao.addCommunity(CommunityEntity(name = name, members = members.toIntOrNull() ?: 0, notes = notes))
                                dao.addAudit(AuditEntity(action = "إضافة مجتمع", target = name, details = "أضيف محليًا إلى قاعدة البيانات"))
                            }
                        })
                        Screen.REPORTS -> ReportsScreen(reports, onAdd = { community, category, details ->
                            scope.launch {
                                dao.addReport(ReportEntity(community = community, category = category, details = details))
                                dao.addAudit(AuditEntity(action = "إنشاء بلاغ", target = community, details = category))
                            }
                        }, onResolve = { report ->
                            scope.launch {
                                dao.updateReportStatus(report.id, "تمت المراجعة")
                                dao.addAudit(AuditEntity(action = "مراجعة بلاغ", target = "بلاغ #${report.id}", details = "تمت المراجعة محليًا — لا يؤثر على KarboAI"))
                            }
                        })
                        Screen.MODERATORS -> ModeratorsScreen(moderators)
                        Screen.AUDIT -> AuditScreen(audit)
                        Screen.SETTINGS -> SettingsScreen()
                    }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(communityCount: Int, openReports: Int, moderatorCount: Int, auditCount: Int, navigate: (Screen) -> Unit) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(top = 10.dp, bottom = 16.dp)) {
        item { Text("ملخص النظام", color = Muted, fontSize = 13.sp) }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("المجتمعات", communityCount.toString(), Modifier.weight(1f))
                MetricCard("بلاغات مفتوحة", openReports.toString(), Modifier.weight(1f), RiverRed)
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard("المشرفون", moderatorCount.toString(), Modifier.weight(1f))
                MetricCard("سجلات النشاط", auditCount.toString(), Modifier.weight(1f), Gold)
            }
        }
        item { SectionTitle("الوصول السريع") }
        items(listOf(Screen.COMMUNITIES, Screen.REPORTS, Screen.MODERATORS, Screen.AUDIT)) { s ->
            Surface(
                onClick = { navigate(s) },
                color = Panel,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(s.symbol, color = Gold, fontSize = 22.sp)
                    Spacer(Modifier.width(12.dp))
                    Text(s.label, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
                    Text("‹", color = Muted, fontSize = 24.sp)
                }
            }
        }
        item {
            Surface(color = Color(0xFF2B1A1B), shape = RoundedCornerShape(12.dp)) {
                Text(
                    "تنبيه: هذه نسخة مستقلة ببيانات محلية تجريبية. لا ترسل بلاغات ولا تغيّر صلاحيات أو حسابات في KarboAI.",
                    color = Color(0xFFFFD7D7), fontSize = 12.sp, modifier = Modifier.padding(14.dp)
                )
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, modifier: Modifier = Modifier, accent: Color = Gold) {
    Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = modifier) {
        Column(Modifier.padding(15.dp)) {
            Text(title, color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(9.dp))
            Text(value, color = accent, fontSize = 28.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, color = Gold, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp))
}

@Composable
private fun CommunitiesScreen(items: List<CommunityEntity>, onAdd: (String, String, String) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { showDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = RiverRed), modifier = Modifier.fillMaxWidth()) { Text("+ إضافة مجتمع") }
        if (items.isEmpty()) Text("لا توجد مجتمعات بعد", color = Muted)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(items) { item ->
                InfoCard(item.name, "${item.members} عضو • ${item.status}", item.notes.ifBlank { "لا توجد ملاحظات" }, Green)
            }
        }
    }
    if (showDialog) {
        var name by remember { mutableStateOf("") }
        var members by remember { mutableStateOf("0") }
        var notes by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("إضافة مجتمع") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InputField("اسم المجتمع", name) { name = it }
                    InputField("عدد الأعضاء", members) { members = it }
                    InputField("ملاحظات", notes) { notes = it }
                }
            },
            confirmButton = { TextButton(onClick = { if (name.isNotBlank()) { onAdd(name.trim(), members, notes.trim()); showDialog = false } }) { Text("حفظ") } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("إلغاء") } },
            containerColor = Panel
        )
    }
}

@Composable
private fun ReportsScreen(items: List<ReportEntity>, onAdd: (String, String, String) -> Unit, onResolve: (ReportEntity) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(onClick = { showDialog = true }, colors = ButtonDefaults.buttonColors(containerColor = RiverRed), modifier = Modifier.fillMaxWidth()) { Text("+ تسجيل بلاغ محلي") }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(bottom = 16.dp)) {
            items(items, key = { it.id }) { report ->
                Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        Text("بلاغ #${report.id} • ${report.community}", color = Color.White, fontWeight = FontWeight.Bold)
                        Text(report.category, color = Gold, fontSize = 13.sp)
                        Text(report.details, color = Muted, fontSize = 12.sp)
                        Text("الحالة: ${report.status}", color = if (report.status == "مفتوح") Color(0xFFFF7777) else Green, fontSize = 12.sp)
                        if (report.status == "مفتوح") {
                            OutlinedButton(onClick = { onResolve(report) }, modifier = Modifier.fillMaxWidth()) { Text("تسجيل تمت المراجعة") }
                        }
                    }
                }
            }
        }
    }
    if (showDialog) {
        var community by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("") }
        var details by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("تسجيل بلاغ") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    InputField("اسم المجتمع", community) { community = it }
                    InputField("نوع البلاغ", category) { category = it }
                    InputField("تفاصيل البلاغ", details) { details = it }
                }
            },
            confirmButton = { TextButton(onClick = { if (community.isNotBlank() && category.isNotBlank()) { onAdd(community.trim(), category.trim(), details.trim()); showDialog = false } }) { Text("حفظ") } },
            dismissButton = { TextButton(onClick = { showDialog = false }) { Text("إلغاء") } },
            containerColor = Panel
        )
    }
}

@Composable
private fun ModeratorsScreen(items: List<ModeratorEntity>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)) {
        item {
            Surface(color = Panel2, shape = RoundedCornerShape(12.dp)) {
                Text("النسخة الحالية تعرض سجلات المشرفين محليًا. إدارة الصلاحيات الفعلية تحتاج تسجيل دخول وتفويضًا داخل هذا النظام.", color = Muted, fontSize = 12.sp, modifier = Modifier.padding(12.dp))
            }
        }
        if (items.isEmpty()) item { Text("لا يوجد مشرفون. ستضاف إدارة المشرفين في المرحلة التالية.", color = Muted) }
        items(items) { moderator ->
            InfoCard(moderator.name, "${moderator.role} • ${moderator.community}", "الحالة: ${moderator.status}", Gold)
        }
    }
}

@Composable
private fun AuditScreen(items: List<AuditEntity>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), contentPadding = PaddingValues(top = 8.dp, bottom = 16.dp)) {
        item { Text("يسجل هذا السجل الإجراءات التي حدثت داخل التطبيق فقط.", color = Muted, fontSize = 12.sp) }
        if (items.isEmpty()) item { Text("لا توجد سجلات بعد", color = Muted) }
        items(items, key = { it.id }) { entry ->
            InfoCard(entry.action, entry.target, entry.details, Gold)
        }
    }
}

@Composable
private fun SettingsScreen() {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        InfoCard("قاعدة البيانات", "Room / SQLite محلية", "تبقى البيانات على هذا الجهاز ولا تتم مزامنتها.", Green)
        InfoCard("الاتصال الخارجي", "غير مفعّل", "لا توجد واجهة API ولا صلاحية وصول إلى KarboAI.", Gold)
        InfoCard("النسخ الاحتياطي", "غير مفعّل بعد", "أضف تصديرًا مشفرًا قبل تخزين بيانات حساسة.", RiverRed)
        InfoCard("الإصدار", "0.1.0 • MVP", "نسخة أولية للاختبار وليست جاهزة للإنتاج.", Muted)
    }
}

@Composable
private fun InfoCard(title: String, subtitle: String, details: String, accent: Color) {
    Surface(color = Panel, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.Top) {
            Box(Modifier.width(4.dp).height(54.dp).background(accent, RoundedCornerShape(4.dp)))
            Spacer(Modifier.width(11.dp))
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(subtitle, color = accent, fontSize = 12.sp)
                Text(details, color = Muted, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun InputField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        modifier = Modifier.fillMaxWidth(),
        singleLine = label != "تفاصيل البلاغ" && label != "ملاحظات",
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = Gold,
            focusedLabelColor = Gold,
            cursorColor = Gold
        )
    )
}
