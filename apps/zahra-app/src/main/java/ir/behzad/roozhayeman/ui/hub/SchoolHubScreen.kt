package ir.behzad.roozhayeman.ui.hub

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import ir.behzad.platform.feature.study.BookModuleRegistry
import ir.behzad.roozhayeman.ui.navigation.Screen

/**
 * هاب «مدرسه» — ساختار تازه:
 *  ۱) همان اول: کتاب‌ها با کاورِ خودشان؛ با انتخاب هر کتاب، فهرست درس‌ها/فصل‌هایش
 *     باز می‌شود (تدریس + مطالعه/آزمون برای هر درس).
 *  ۲) برنامه‌ی هفتگی.
 *  ۳) آزمون و بازخورد (جزوه، نمودار پیشرفت مخصوص دروس مدرسه).
 * پلیرها، کتابخانه، مطالعات آزاد و آموزش هوش به تب «آموزشگاه» منتقل شدند.
 */
@Composable
fun SchoolHubScreen(nav: NavController) {
    HubBody {
        HubHeader("مدرسه 🎒", "همه‌چیز از کتاب شروع می‌شود — یک کتاب را باز کن")

        HubMenuGroup("📚 کتاب‌ها", "۱۳ کتاب پایه نهم — هر کتاب با درس‌ها، صوت، ویدیو و آزمونش") {
            val ctx = LocalContext.current
            val books = remember { BookModuleRegistry.modules }
            books.chunked(2).forEach { pair ->
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    pair.forEach { book ->
                        val cover = remember(book.bookCode) {
                            runCatching { BitmapFactory.decodeStream(ctx.assets.open("book-covers/${book.bookCode}.jpg")) }.getOrNull()
                        }
                        Card(
                            Modifier.weight(1f).clickable { nav.hubTo(Screen.Book.of(book.bookCode)) },
                        ) {
                            Column {
                                if (cover != null) {
                                    Image(
                                        bitmap = cover.asImageBitmap(),
                                        contentDescription = "کاور ${book.title}",
                                        modifier = Modifier.fillMaxWidth().height(120.dp),
                                        contentScale = ContentScale.Fit,
                                    )
                                }
                                Column(Modifier.padding(10.dp)) {
                                    Text(book.title, style = MaterialTheme.typography.titleSmall, maxLines = 2)
                                    Text(
                                        "${book.packs.size} درس",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    if (pair.size == 1) Spacer(Modifier.weight(1f))
                }
            }
        }

        HubMenuGroup("🗓 برنامه‌ی هفتگی", "چرخش شیفت و درس‌های هر روز") {
            HubCard("⏰", "برنامه‌ی هفتگی من", "شیفت صبح/عصر/شب + جدول درس هر روز (با ورزش)") { nav.hubTo(Screen.WeeklySchedule.route) }
            HubCard("🏫", "برنامه‌ی مدرسه", "زنگ‌ها و برنامه‌ی کلاسی") { nav.hubTo(Screen.School.route) }
        }

        HubMenuGroup("📝 آزمون و بازخورد", "سنجش دروس مدرسه") {
            HubCard("🧪", "جزوه‌ها و آپلود PDF", "جزوه‌ی معلم را بده تا سوال ساخته شود") { nav.hubTo(Screen.Pdf.route) }
            HubCard("📈", "نمودار پیشرفت دروس", "رشدت در هر درس — طبق آزمون‌ها و فلش‌کارت‌ها") { nav.hubTo(Screen.Charts.route) }
        }
    }
}
