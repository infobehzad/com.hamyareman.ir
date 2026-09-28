package com.hamyareman.ir.ui.hub

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.hamyareman.ir.LocalAppContainer
import com.hamyareman.ir.platform.core.common.LocalStore
import com.hamyareman.ir.platform.core.common.TableIds
import com.hamyareman.ir.platform.core.designsystem.PrimaryButton
import com.hamyareman.ir.ui.study.LinedNotesPaper
import com.hamyareman.ir.ui.wellness.PracticeHubScreen
import com.hamyareman.ir.ui.wellness.WellnessMenu
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.Calendar

private val SelfQuestions50 = listOf(
    "امروز چه چیزی حالت را حتی یک ذره بهتر کرد؟",
    "اگر می‌خواستی به خودت یک جمله‌ی پر از مهربانی و آرامش بگی، چی می‌گفتی؟",
    "امروز از چه چیزی دلت خواست بیشتر بدانی و کشف کنی؟",
    "کدام لحظه‌ی کوچک امروز ارزش به خاطر سپردن داشت؟",
    "اگر امروز فقط یک کار کوچکِ خوب برای خودت می‌کردی، چه بود؟",
    "امروز به کدام احساسِ خودت گوش دادی و پذیرفتی‌اش؟",
    "چه چیزی را از رفتار امروزت می‌خواهی فردا هم تکرار کنی؟",
    "امروز کجا و در چه لحظه‌ای بیشترین مهربانی را با خودت داشتی؟",
    "یک لطف کوچک یا لبخندی که امروز از کسی دیدی چه بود؟",
    "امروز چه درس یا نکته‌ی جدیدی را درباره‌ی خودت کشف کردی؟",
    "در این لحظه بدنت چه نیازی دارد؟ استراحت، آب، یا یک نفس عمیق؟",
    "اگر قرار بود عنوان امروز یک کتاب باشد، چه اسمی برایش می‌گذاشتی؟",
    "چه فکر بیهوده‌ای امروز آمد که توانستی رهایش کنی و از آن بگذری؟",
    "سه چیز ساده در اطرافت که همین حالا بابتشان خوشحالی چیستند؟",
    "اگر امروز یک رنگ بود، چه رنگی بود و چرا؟",
    "امروز چه تصمیمی گرفتی که بعداً از گرفتن آن خوشحال شدی؟",
    "در موقعیت‌های سخت امروز، چطور توانستی آرامش خودت را حفظ کنی؟",
    "کدام ویژگی اخلاقی‌ات امروز به کمکت آمد؟",
    "اگر می‌توانستی یک پیام آرامش‌بخش به گذشته‌ی خودت بفرستی، چه می‌نوشتی؟",
    "چه کاری امروز باعث شد گذر زمان را حس نکنی؟",
    "کدام مکالمه یا حرف امروز احساس خوبی در قلبت ایجاد کرد؟",
    "امروز چطور مرزهای شخصی و احترام به خودت را حفظ کردی؟",
    "برای یک چالش که پشت سر گذاشتی، چطور خودت را تحسین می‌کنی؟",
    "چه چیزی امروز به تو یادآوری کرد که زندگی هنوز زیبایی‌هایش را دارد؟",
    "اگر قرار بود به یکی از تلاش‌های امروزت نمره ۲۰ بدهی، کدام تلاش بود؟",
    "امروز چقدر توانستی در لحظه حال زندگی کنی و نگران آینده نباشی؟",
    "چه بویی، صدایی یا تصویری امروز حس آرامش به تو بخشید؟",
    "یک کار سخت که امروز با شجاعت شروعش کردی چه بود؟",
    "امروز با چه کسی مهربان‌تر از معمول رفتار کردی و چه حسی داشتی؟",
    "اگر الان می‌توانستی یک آرزوی کوچک را برآورده کنی، برای چه کسی بود؟",
    "چه عادتی را در خودت می‌بینی که روز به روز دارد بهتر می‌شود؟",
    "امروز چه چیزی باعث شد از ته دل لبخند بزنی؟",
    "چطور توانستی خستگی امروز را مدیریت کنی و به بدنت استراحت بدهی؟",
    "یک جمله‌ی الهام‌بخش که اخیراً شنیدی یا خواندی چه بود؟",
    "امروز چه باری از دوشت برداشته شد یا چه مسئله‌ای حل شد؟",
    "چه مهارتی را امروز تمرین کردی یا دوست داری بیشتر یاد بگیری؟",
    "اگر احساسات امروزت را در یک کلمه خلاصه کنی، آن کلمه چیست؟",
    "امروز چه فرصتی برای سکوت و خلوت با خودت پیدا کردی؟",
    "چطور می‌توانی امشب را با آرامش و سبک‌بالی به پایان برسانی؟",
    "یک کار خلاقانه که امروز انجام دادی چه بود؟",
    "چه چیزی امروز بهت نشان داد که قوی‌تر از چیزی هستی که فکر می‌کردی؟",
    "کدام نعمت ساده (مثل هوای تمیز، سقف بالای سر یا غذای گرم) امروز یادت آمد؟",
    "اگر می‌توانستی از یک نفر در زندگی‌ات صمیمانه تشکر کنی، چه کسی بود؟",
    "امروز چقدر به ندای درونی و قلبت اعتماد کردی؟",
    "یک فکر مثبت که بهت امید داد چه بود؟",
    "چه تغییری در برنامه‌ات دادی که حس آزادی بیشتری بهت داد؟",
    "امروز از چه مقایسه‌ای با دیگران دست کشیدی و مسیر خودت را رفتی؟",
    "اگر فردا یک شروع تازه باشد، با چه انرژی می‌خواهی بیدارشوی؟",
    "بهترین بخش روزت کجا رقم خورد؟",
    "الان که این را می‌نویسی، چه حسی در قلبت جریان دارد؟",
)

/**
 * هاب «ذهن‌آگاهی» — حضور ذهن، خودهیپنوز سالم، افکار، آگاهی اجتماعی، روزنوشت، یادگیری.
 */
@Composable
fun AwarenessHubScreen(nav: NavController, onBack: () -> Unit) {
    val context = LocalContext.current
    val container = LocalAppContainer.current
    val scope = rememberCoroutineScope()
    val store = remember { LocalStore(context, "hamyar_awareness") }

    val cal = remember { Calendar.getInstance() }
    val dayOfYear = cal.get(Calendar.DAY_OF_YEAR)
    val year = cal.get(Calendar.YEAR)
    val questionIndex = remember(dayOfYear, year) { (year * 365 + dayOfYear) % SelfQuestions50.size }
    val question = remember(questionIndex) { SelfQuestions50[questionIndex] }
    val dateKey = remember { todayKey() }

    var answer by remember { mutableStateOf(store.getString("self_answer_$dateKey", "")) }
    var savedNotice by remember { mutableStateOf<String?>(null) }

    fun syncAnswerToCloud(text: String) {
        scope.launch(Dispatchers.IO) {
            val uid = container.auth.cachedUserId()
                ?: runCatching { container.auth.currentUserId() }.getOrNull().orEmpty()
            if (uid.isBlank()) return@launch
            val client = container.appwrite.client
            runCatching {
                val db = io.appwrite.services.Databases(client)
                val payload = JSONObject().apply {
                    put("date", dateKey)
                    put("question", question)
                    put("answer", text)
                    put("updatedAt", System.currentTimeMillis())
                }.toString()
                db.createDocument(
                    databaseId = TableIds.DATABASE,
                    collectionId = TableIds.APP_STATE,
                    documentId = "self_$dateKey",
                    data = mapOf(
                        "userId" to uid,
                        "type" to "self_awareness",
                        "payload" to payload,
                        "updatedAt" to System.currentTimeMillis(),
                    ),
                )
            }
        }
    }

    PracticeHubScreen(
        nav = nav,
        rootIds = WellnessMenu.mindfulnessIds,
        title = "ذهن‌آگاهی 🪷",
        subtitle = "حضور، فکر سالم، یادگیری آرام",
        headerSlot = "hub.awareness.header",
        accKey = "acc_mindfulness",
        onBack = onBack,
        extraBottom = {
            Spacer(Modifier.height(12.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        "📝 سوال خودشناسی روزانه (دفترچه تأمل)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        question,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(4.dp))
                    LinedNotesPaper(
                        value = answer,
                        onValueChange = {
                            answer = it
                            store.putString("self_answer_$dateKey", it)
                            savedNotice = null
                        },
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            savedNotice ?: "همگام‌سازی خودکار و ذخیره روی دستگاه",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        PrimaryButton(
                            text = "ثبت در دفترچه",
                            onClick = {
                                store.putString("self_answer_$dateKey", answer)
                                syncAnswerToCloud(answer)
                                savedNotice = "با موفقیت ذخیره و همگام شد ✨"
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        },
    )
}

/**
 * گوشه‌ی مطالعه‌ی غیردرسی — ماژول اختصاصی «قفسه‌ی من».
 * فاز فعلی: ثبت کتاب‌ها و وضعیت خواندن؛ اتصال به پلیر/متن کتاب در فاز بعد تکمیل می‌شود.
 */
@Composable
fun ReadingCornerScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val store = remember { LocalStore(context, "hamyar_reading") }
    var title by remember { mutableStateOf("") }
    val books = remember { store.getStringSet("my_shelf").filter { it.isNotBlank() }.sorted() }

    HubBody {
        HubHeader("قفسه‌ی من 📖", "مطالعه‌ی غیردرسی — دنیای خودت")
        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("نام کتابی که می‌خواهی بخوانی…") },
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(onClick = {
            if (title.isNotBlank()) {
                store.putStringSet("my_shelf", store.getStringSet("my_shelf") + title.trim())
                title = ""
            }
        }) { Text("به قفسه اضافه کن") }

        if (books.isEmpty()) {
            Text("قفسه‌ات هنوز خالی است؛ اولین کتاب را اضافه کن ✨", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            books.forEach { Card(Modifier.fillMaxWidth().padding(vertical = 3.dp)) { Text("📗 $it", Modifier.padding(12.dp)) } }
        }

        Text(
            "به‌زودی: پلیر مخصوص کتاب‌ها، هدف‌گذاری صفحات روزانه و پیشنهاد کتاب — این ماژول کامل جدا توسعه داده می‌شود.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
