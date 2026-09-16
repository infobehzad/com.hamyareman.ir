package com.hamyareman.ir.platform.feature.study.books

import com.hamyareman.ir.platform.feature.study.BookModule
import com.hamyareman.ir.platform.feature.study.StudyPack

/**
 * ماژول کتاب «ریاضی پایه نهم» (C905) — معماری مصوب:
 * هر کتاب یک فایل Kotlin و کل محتوای درس‌ها (سکشن/فلش‌کارت/سوال/حل) داخل کد.
 * منبع تألیف: جزوه‌های راهنمای مصور ریپو (پوشه‌های ۰۱ تا ۰۶ هر درس — فقط منبع محلی).
 * تصاویر در assets/lesson-img اپ کپی شده‌اند و به سکشن‌ها وصل‌اند.
 * فعلاً فصل ۱ (مجموعه‌ها) — فصل‌های بعدی همین‌جا اضافه می‌شوند.
 */
object MathC905 {
    private fun l01(): StudyPack = StudyPack(
        packId = "C905_E01-L01", bookCode = "C905", lessonId = "E01-L01",
        title = "درس ۱ — معرفی مجموعه", bookTitle = "ریاضی پایه نهم", pdfFileName = "C905_E01-L01_BOOK.pdf",
        sections = listOf(
            StudyPack.Section(id = "s1", title = "مفهوم اصلی: مجموعه چیست؟", kind = "concept", body = "مجموعه، گردایه‌ای از اعضای متمایز و مشخص است. «مشخص» یعنی برای هر چیزی بتوان گفت عضو است یا نیست؛ «متمایز» یعنی هر عضو یک‌بار در مجموعه می‌آید.\nمثال: مجموعه‌ی روزهای هفته؛ مجموعه‌ی عددهای فرد کوچک‌تر از ۱۰.", images = listOf("lesson-img/C905_E01-L01_RAHNAMA-MOSAVVAR_IMG-01.png")),
            StudyPack.Section(id = "s2", title = "نمایش مجموعه‌ها", kind = "important", body = "۱) نمایش سرعینی (با آکولاد): A = {۱، ۲، ۳}\n۲) نمایش با ویژگی اعضا: A = {x | x عددی طبیعی و x < 4}\n۳) نمایش دایره‌ای (نمودار ون): هر مجموعه را با یک دایره نشان می‌دهیم.", images = listOf("lesson-img/C905_E01-L01_RAHNAMA-MOSAVVAR_IMG-02.png")),
            StudyPack.Section(id = "s3", title = "عضویت: ∈ و ∉", kind = "important", body = "a ∈ A یعنی a عضو مجموعه‌ی A است؛ a ∉ A یعنی عضو نیست.\nدقت: {۱، ۲} ∈ {{{۱،۲}}، ۳} درست است چون خودِ مجموعه‌ی {۱،۲} عضوِ آن مجموعه است.", images = listOf("lesson-img/C905_E01-L01_RAHNAMA-MOSAVVAR_IMG-03.png")),
            StudyPack.Section(id = "s4", title = "مجموعه‌ی تهی و یکتایی اعضا", kind = "note", body = "مجموعه‌ی تهی (∅ یا { }) هیچ عضوی ندارد.\nتکرار اعضا اثر ندارد: {۱، ۲، ۲} همان {۱، ۲} است؛ ترتیب هم مهم نیست: {۱،۲} = {۲،۱}."),
            StudyPack.Section(id = "s5", title = "نکته‌ی امتحانی", kind = "exam", body = "۱) ∅ ∈ {∅} درست است ولی ∅ = {∅} غلط است!\n۲) در تساوی مجموعه‌ها فقط «بودن یا نبودن اعضا» مهم است.\n۳) در نمایش با ویژگی، حتماً «جهان گفتگو» را مشخص کن."),
            StudyPack.Section(id = "s6", title = "خلاصه‌ی امتحانی یک‌نگاهی", kind = "exam", body = "مجموعه = اعضای مشخص و متمایز · ∈ عضویت · ∅ تهی · تکرار و ترتیب بی‌اثر · سه نوع نمایش: سرعینی، ویژگی، ون.", images = listOf("lesson-img/C905_E01-L01_PASOKH-TAMRIN_IMG-01.png")),
        ),
        flashcards = listOf(
            StudyPack.Flashcard("c1", "مجموعه را تعریف کن.", "گردایه‌ای از اعضای متمایز و مشخص؛ برای هر چیز معلوم باشد عضو است یا نه، و هر عضو فقط یک‌بار بیاید.", "تعریف", "دو ویژگی: مشخص و متمایز"),
            StudyPack.Flashcard("c2", "معنی a ∈ A چیست؟", "a عضو مجموعه‌ی A است.", "عضویت", ""),
            StudyPack.Flashcard("c3", "معنی a ∉ A چیست؟", "a عضو مجموعه‌ی A نیست.", "عضویت", ""),
            StudyPack.Flashcard("c4", "مجموعه‌ی تهی چیست و چطور نشان می‌دهیم؟", "مجموعه‌ای بدون هیچ عضو؛ با ∅ یا { } نشان می‌دهیم.", "تهی", "آکولاد خالی یا علامت مخصوص"),
            StudyPack.Flashcard("c5", "آیا {۱، ۲، ۲} با {۱، ۲} برابر است؟", "بله؛ تکرارِ عضو اثری در مجموعه ندارد.", "تساوی", "یکتایی اعضا"),
            StudyPack.Flashcard("c6", "آیا {۱، ۲} و {۲، ۱} برابرند؟", "بله؛ در مجموعه ترتیب مهم نیست.", "تساوی", "ترتیب"),
            StudyPack.Flashcard("c7", "دو مجموعه چه زمانی برابرند؟", "وقتی دقیقاً اعضای یکسانی داشته باشند (هر عضو یکی در دیگری باشد).", "تساوی", "شرط دوسویه"),
            StudyPack.Flashcard("c8", "سه نوع نمایش مجموعه را نام ببر.", "سرعینی (آکولاد)، با ویژگی اعضا، نمودار دایره‌ای (ون).", "نمایش", "یکی کتبی-فهرستی، یکی شرطی، یکی شکل"),
            StudyPack.Flashcard("c9", "A = {x | x عدد طبیعی و x < 4} را سرعینی بنویس.", "A = {۱، ۲، ۳}", "نمایش", "صفر جزو طبیعی نیست"),
            StudyPack.Flashcard("c10", "آیا ∅ ∈ {∅} درست است؟", "بله؛ این مجموعه یک عضو دارد که خودش ∅ است.", "تهی", "عضوِ مجموعه، خودِ ∅ است"),
            StudyPack.Flashcard("c11", "آیا ∅ = {∅} درست است؟", "خیر؛ سمت چپ تهی است ولی سمت راست یک عضو دارد.", "تهی", "تعداد اعضا را بشمار"),
            StudyPack.Flashcard("c12", "در نمایش با ویژگی «x | x فرد و x < 7» با جهان اعداد طبیعی، اعضا کدامند؟", "{۱، ۳، ۵}", "نمایش", "از صفر شروع کن و شرط را چک کن"),
        ),
        questions = listOf(
            StudyPack.Question(id = "q1", type = "mcq", text = "کدام گزینه یک مجموعه است؟",
                options = listOf("عددهای قشنگ", "عددهای طبیعی کوچک‌تر از ۵", "فیلم‌های جالب", "غذاهای خوشمزه"), answer = "عددهای طبیعی کوچک‌تر از ۵", explanation = "فقط در این گزینه برای هر چیز می‌توان دقیق گفت عضو است یا نه؛ «قشنگ» و «جالب» مشخص نیستند.",
                topic = "تعریف", difficulty = 1, refSectionId = "s1"),
            StudyPack.Question(id = "q2", type = "mcq", text = "کدام تساوی درست است؟",
                options = listOf("{۱،۲} = {۲،۱}", "∅ = {∅}", "{۱،۲،۲} = {۱،۱،۲،۲،۳}", "هیچ‌کدام"), answer = "{۱،۲} = {۲،۱}", explanation = "ترتیب و تکرار در مجموعه بی‌اثر است؛ اما ∅ هیچ‌وقت با مجموعه‌ای که یک عضو (∅) دارد برابر نیست.",
                topic = "تساوی", difficulty = 1, refSectionId = "s5"),
            StudyPack.Question(id = "q3", type = "mcq", text = "اگر A = {۱، {۲}، ۳} باشد، کدام گزینه درست است؟",
                options = listOf("{۲} ∈ A", "۲ ∈ A", "∅ ∈ A", "۴ ∈ A"), answer = "{۲} ∈ A", explanation = "عضوهای A عبارت‌اند از: ۱، مجموعه‌ی {۲}، و ۳. خودِ عدد ۲ عضو نیست؛ مجموعه‌ی {۲} عضو است.",
                topic = "عضویت", difficulty = 3, refSectionId = "s3"),
            StudyPack.Question(id = "q4", type = "mcq", text = "مجموعه‌ی تهی را کدام نمایش درست نشان می‌دهد؟",
                options = listOf("{ }", "{∅}", "{0}", "{ } { }"), answer = "{ }", explanation = "{∅} یک عضو دارد (خود ∅)؛ {0} هم یک عضو دارد. تهی فقط { } یا ∅ است.",
                topic = "تهی", difficulty = 1, refSectionId = "s4"),
            StudyPack.Question(id = "q5", type = "mcq", text = "در نمایش با ویژگی، عبارت {x | x² = 4 ، x عدد طبیعی} کدام است؟",
                options = listOf("{۲}", "{۲، -۲}", "{-۲}", "∅"), answer = "{۲}", explanation = "جهان، عددهای طبیعی است؛ فقط x=2 جواب می‌دهد (۲²=۴). عدد ۲- طبیعی نیست.",
                topic = "نمایش", difficulty = 2, refSectionId = "s2"),
            StudyPack.Question(id = "q6", type = "mcq", text = "کدام جمله درباره‌ی نمودار ون درست است؟",
                options = listOf("هر مجموعه با یک دایره نشان داده می‌شود", "فقط برای اعداد کاربرد دارد", "نمی‌تواند چند مجموعه را هم‌زمان نشان دهد", "جایگزین آکولاد است و نمایش سرعینی حذف می‌شود"), answer = "هر مجموعه با یک دایره نشان داده می‌شود", explanation = "نمودار ون نمایش دایره‌ای است و می‌تواند چند مجموعه را هم‌زمان نشان دهد.",
                topic = "نمایش", difficulty = 1, refSectionId = "s2"),
            StudyPack.Question(id = "q7", type = "numeric", text = "مجموعه‌ی B = {عددهای طبیعی زوج، 1 ≤ x ≤ 9} چند عضو دارد؟ فقط عدد بنویس.",
                options = listOf(), answer = "4", explanation = "اعضا: ۲، ۴، ۶، ۸ → تعداد ۴ عضو.",
                topic = "عضویت", difficulty = 2, refSectionId = "s1"),
            StudyPack.Question(id = "q8", type = "numeric", text = "در تساوی {۳، a، ۵} = {۱، ۳، ۵} مقدار a چند است؟",
                options = listOf(), answer = "1", explanation = "دو مجموعه برابرند پس اعضایشان یکسان است؛ a باید ۱ باشد.",
                topic = "تساوی", difficulty = 2, refSectionId = "s5"),
            StudyPack.Question(id = "q9", type = "numeric", text = "مجموعه‌ی C = {x | x فرد ، x عدد طبیعی ، x < 8} چند عضو دارد؟",
                options = listOf(), answer = "4", explanation = "اعضا: ۱، ۳، ۵، ۷ → ۴ عضو.",
                topic = "نمایش", difficulty = 1, refSectionId = "s2"),
            StudyPack.Question(id = "q10", type = "numeric", text = "مجموعه‌ی {۱، ۲}، {۲، ۱} و {۱، ۱، ۲} چند «عضو متمایز» روی‌هم‌رفته دارند؟ (تعداد اعضای متفاوت این نام‌ها)",
                options = listOf(), answer = "2", explanation = "هر سه نام، یک مجموعه‌ی واحد را نشان می‌دهند: {۱، ۲} با دو عضو.",
                topic = "تساوی", difficulty = 2, refSectionId = "s4"),
            StudyPack.Question(id = "q11", type = "short", text = "به مجموعه‌ای که هیچ عضوی ندارد چه می‌گویند و چطور نمایش می‌دهند؟",
                options = listOf(), answer = "مجموعه تهی؛ با ∅ یا { }", explanation = "تعریف تهی.",
                topic = "تهی", difficulty = 1, refSectionId = "s4"),
            StudyPack.Question(id = "q12", type = "short", text = "شرط اصلی یک مجموعه برای «مشخص بودن» چیست؟",
                options = listOf(), answer = "برای هر شیء بتوان گفت عضو است یا عضو نیست", explanation = "مشخص بودن یعنی عضویت برای هر چیز قطعی باشد.",
                topic = "تعریف", difficulty = 2, refSectionId = "s1"),
        ),
        solutions = listOf(
            StudyPack.Solution("sol1", "حل تشریحی q7 (شمارش اعضا)", "شرط: زوج و طبیعی و بین ۱ تا ۹.\nگام ۱: زوج‌های طبیعی: ۲، ۴، ۶، ۸، ۱۰، …\nگام ۲: شرط ≤ x ≤ 9 را اعمال کن → ۲، ۴، ۶، ۸.\nگام ۳: شمارش → ۴ عضو."),
            StudyPack.Solution("sol2", "حل تشریحی q3 (عضویت تودرتو)", "A = {۱، {۲}، ۳} سه عضو دارد: عدد ۱، مجموعه‌ی {۲}، عدد ۳.\nپس «{۲} ∈ A» درست است؛ اما «۲ ∈ A» غلط است چون ۲ خودش (نه در قالب مجموعه) عضو نیست.\nنکته: سطحِ عضویت را همیشه چک کن — عضو است یا داخل عضو؟"),
            StudyPack.Solution("sol3", "حل تشریحی q10 (یکتایی و ترتیب)", "{۱،۲} و {۲،۱} به‌خاطر بی‌اثربودن ترتیب برابرند؛ {۱،۱،۲} هم به‌خاطر بی‌اثربودن تکرار همان است.\nپس هر سه یک مجموعه‌اند با ۲ عضو متمایز."),
        ),
    )
    private fun l02(): StudyPack = StudyPack(
        packId = "C905_E01-L02", bookCode = "C905", lessonId = "E01-L02",
        title = "درس ۲ — مجموعه‌های برابر و نمایش مجموعه‌ها", bookTitle = "ریاضی پایه نهم", pdfFileName = "C905_E01-L02_BOOK.pdf",
        sections = listOf(
            StudyPack.Section(id = "s1", title = "برابری مجموعه‌ها؛ چه زمانی A = B است؟", kind = "concept", body = "دو مجموعه را وقتی «برابر» می‌گوییم که عضوهایشان دقیقاً یکی باشد؛ هر عضو A عضو B هم باشد و برعکس.\nدو نتیجه‌ی مهم: ترتیب نوشتن عضوها بی‌اهمیت است و تکرار مجاز نیست.\nمثال: {8,9,10} و {7,8,9} برابر نیستند؛ ۱۰ در A هست ولی در B نیست.", images = listOf("lesson-img/C905_E01-L01_RAHNAMA-MOSAVVAR_IMG-01.png")),
            StudyPack.Section(id = "s2", title = "زیرمجموعه؛ وقتی یک مجموعه درون دیگری است", kind = "concept", body = "اگر هر عضو D عضو B هم باشد، D زیرمجموعه‌ی B است: D ⊆ B. بررسی عضو به عضو انجام می‌شود.\nسه حقیقت همیشگی: هر مجموعه زیرمجموعه‌ی خودش است (A ⊆ A)؛ مجموعه‌ی تهی زیرمجموعه‌ی هر مجموعه است (∅ ⊆ A)؛ اگر A ⊆ B و B ⊆ A آن‌گاه A = B.\nمثال: B = {1,2,3,4} و D = {2,4} → D ⊆ B.", images = listOf("lesson-img/C905_E01-L01_RAHNAMA-MOSAVVAR_IMG-02.png")),
            StudyPack.Section(id = "s3", title = "قاعده‌ی طلایی 2ⁿ و فهرست کردن منظم", kind = "important", body = "برای نوشتن همه‌ی زیرمجموعه‌ها با نظم پیش برو: اول ∅، بعد تک‌عضوی‌ها، بعد دوعضوی‌ها و در آخر خود مجموعه.\nقاعده‌ی طلایی: مجموعه‌ی n عضوی دقیقاً 2ⁿ زیرمجموعه دارد (هر عضو دو انتخاب دارد: بیا یا نیا).\nزیرمجموعه‌های «دقیق» (به‌جز خود مجموعه): 2ⁿ − ۱. نمونه: {a,b,c} → ۸ زیرمجموعه؛ {a,b,c,d} → ۱۶.", images = listOf("lesson-img/C905_E01-L01_RAHNAMA-MOSAVVAR_IMG-03.png")),
            StudyPack.Section(id = "s4", title = "مجموعه‌های عددی شاخص: N و W و Z و Q", kind = "concept", body = "N عددهای طبیعی {1,2,3,…}؛ W عددهای حسابی {0,1,2,…}؛ Z عددهای صحیح {…,−2,−1,0,1,2,…}؛ Q عددهای گویا (همه‌ی کسرهای a/b با b ≠ 0).\nزنجیره‌ی مهم فصل: N ⊂ W ⊂ Z ⊂ Q؛ برعکسِ هیچ‌کدام درست نیست (مثلاً ½ گویاست ولی حسابی نیست)."),
            StudyPack.Section(id = "s5", title = "نمایش مشخصه‌ای؛ زبان نمادها", kind = "note", body = "نمایش مشخصه‌ای یعنی نوشتن شرط عضویت: A = {n+3 | n∈N} یعنی {4,5,6,7,…}.\nقبل از حل شرط، اول دامنه را ببین (N یا Z یا Q؟)؛ جواب کاملاً عوض می‌شود.\nمثال: B = {x | x∈Z , −5 ≤ x ≤ −1} یعنی {−5,−4,−3,−2,−1}."),
            StudyPack.Section(id = "s6", title = "خطاهای رایج امتحانی این درس", kind = "exam", body = "∅ ⊆ A همیشه درست است ولی ∅ ∈ A معمولاً نادرست (∅ عضوِ مجموعه نیست مگر عمداً گذاشته شده باشد مثل {∅}).\n«۳ ⊆ B» نادرست‌نویسی است؛ درستش «۳ ∈ B» یا «{3} ⊆ B».\nمجموعه‌ی تهی را با {0} اشتباه نگیر؛ {0} یک عضو دارد و تهی نیست.\nدر نمایش مشخصه‌ای، ندیدنِ دامنه = جواب کاملاً متفاوت.", images = listOf("lesson-img/C905_E01-L01_PASOKH-TAMRIN_IMG-01.png")),
            StudyPack.Section(id = "s7", title = "جمع‌بندی امتحانی یک‌نگاهی", kind = "exam", body = "A = B یعنی عضوهای دقیقاً یکسان؛ ترتیب مهم نیست.\nA ⊆ B یعنی هر عضو A عضو B هم هست؛ ∅ و خودِ A همیشه زیرمجموعه‌اند.\nمجموعه‌ی n عضوی: 2ⁿ زیرمجموعه و 2ⁿ−۱ زیرمجموعه‌ی دقیق.\nزنجیره: N ⊂ W ⊂ Z ⊂ Q.\nنمایش مشخصه‌ای = شرط عضویت؛ اول دامنه، بعد شرط."),
        ),
        flashcards = listOf(
            StudyPack.Flashcard("c1", "تعریف برابری دو مجموعه را بگو.", "عضوهایشان دقیقاً یکی باشد؛ هر عضو A عضو B هم باشد و برعکس. ترتیب و تکرار بی‌اهمیت است.", "برابری", "عضو به عضو بسنج"),
            StudyPack.Flashcard("c2", "{1,2,3} و {3,2,1} برابرند؟", "بله؛ فقط ترتیب نوشتن فرق دارد و ترتیب در مجموعه بی‌اهمیت است.", "برابری", "ترتیب؟"),
            StudyPack.Flashcard("c3", "زیرمجموعه یعنی چه؟ (D ⊆ B)", "هر عضو D عضو B هم باشد. بررسی عضو به عضو انجام می‌شود.", "زیرمجموعه", "درون دیگری"),
            StudyPack.Flashcard("c4", "سه حقیقت همیشگی زیرمجموعه‌ها؟", "۱) A ⊆ A  ۲) ∅ ⊆ A  ۳) A ⊆ B و B ⊆ A ⟺ A = B", "زیرمجموعه", "خودش، تهی، دوسویه"),
            StudyPack.Flashcard("c5", "قاعده‌ی طلایی شمارش زیرمجموعه‌ها؟", "مجموعه‌ی n عضوی دقیقاً 2ⁿ زیرمجموعه دارد؛ چون هر عضو دو انتخاب دارد: بیا یا نیا.", "شمارش", "۲ به توان n"),
            StudyPack.Flashcard("c6", "تعداد زیرمجموعه‌های «دقیق» مجموعه‌ی n عضوی؟", "2ⁿ − ۱ (همه به‌جز خود مجموعه). برای n=4 می‌شود ۱۵.", "شمارش", "منهای یکی"),
            StudyPack.Flashcard("c7", "N و W چه هستند؟", "N عددهای طبیعی {1,2,3,…}؛ W عددهای حسابی {0,1,2,…} — فقط صفر با N تفاوت دارد.", "عددهای شاخص", "صفر"),
            StudyPack.Flashcard("c8", "Z و Q چه هستند؟", "Z عددهای صحیح (مثبت، صفر، منفی)؛ Q همه‌ی کسرهای a/b با b ≠ 0.", "عددهای شاخص", "صحیح و گویا"),
            StudyPack.Flashcard("c9", "زنجیره‌ی زیرمجموعه‌ای عددهای شاخص؟", "N ⊂ W ⊂ Z ⊂ Q — و برعکسِ هیچ‌کدام درست نیست.", "عددهای شاخص", "چهار حرف"),
            StudyPack.Flashcard("c10", "نمایش مشخصه‌ای چیست؟", "نوشتن مجموعه با شرط عضویت؛ مثل {n+3 | n∈N} = {4,5,6,7,…}.", "نمایش", "شرط عضویت"),
            StudyPack.Flashcard("c11", "اولین قدم در حل نمایش مشخصه‌ای؟", "دیدن دامنه (N یا Z یا Q؟) — بدون دامنه، شرط را درست نمی‌فهمی.", "نمایش", "دامنه اول"),
            StudyPack.Flashcard("c12", "∅ ∈ A یا ∅ ⊆ A؛ کدام همیشه درست است؟", "∅ ⊆ A همیشه درست است؛ ∅ ∈ A معمولاً نادرست (∅ عضوِ A نیست مگر عمداً داخلش گذاشته باشند).", "تهی", "عضو یا زیرمجموعه؟"),
        ),
        questions = listOf(
            StudyPack.Question(id = "q1", type = "mcq", text = "کدام دو مجموعه برابرند؟",
                options = listOf("{1,2,3} و {3,2,1}", "{1,2,3} و {1,2,3,4}", "{8,9,10} و {7,8,9}", "{1,2} و {2,3}"), answer = "{1,2,3} و {3,2,1}", explanation = "عضوها دقیقاً یکی‌اند؛ فقط ترتیب فرق دارد و ترتیب بی‌اهمیت است.",
                topic = "برابری", difficulty = 1, refSectionId = "s1"),
            StudyPack.Question(id = "q2", type = "mcq", text = "کدام گزینه همیشه درست است؟",
                options = listOf("∅ ⊆ A", "∅ ∈ A", "{0} = ∅", "اگر ۳ عضو B باشد، آن‌گاه {3} ⊄ B"), answer = "∅ ⊆ A", explanation = "مجموعه‌ی تهی زیرمجموعه‌ی هر مجموعه است؛ اما ∅ عضوِ A نیست و {0} هم تهی نیست.",
                topic = "زیرمجموعه", difficulty = 2, refSectionId = "s2"),
            StudyPack.Question(id = "q3", type = "numeric", text = "مجموعه‌ی {a,b,c,d} چند زیرمجموعه دارد؟ فقط عدد بنویس.",
                options = listOf(), answer = "16", explanation = "n=4 → 2⁴ = ۱۶ زیرمجموعه.",
                topic = "شمارش", difficulty = 1, refSectionId = "s3"),
            StudyPack.Question(id = "q4", type = "numeric", text = "مجموعه‌ای با ۴ عضو چند زیرمجموعه‌ی «دقیق» دارد (به‌جز خودش)؟",
                options = listOf(), answer = "15", explanation = "2⁴ − ۱ = ۱۵.",
                topic = "شمارش", difficulty = 2, refSectionId = "s3"),
            StudyPack.Question(id = "q5", type = "mcq", text = "A = {1,3,6,4} و B = {5,1,3} و C = {2,5,1,3,6}. کدام گزینه درست است؟",
                options = listOf("B ⊆ C", "A ⊆ B", "{5,6} ⊄ C", "A = C"), answer = "B ⊆ C", explanation = "۵ و ۱ و ۳ همگی در C هستند؛ ولی ۶ و ۴ در B نیستند پس A ⊆ B نادرست است.",
                topic = "زیرمجموعه", difficulty = 2, refSectionId = "s2"),
            StudyPack.Question(id = "q6", type = "mcq", text = "کدام عدد «حسابی» (W) نیست؟",
                options = listOf("1/2", "0", "−5", "7"), answer = "1/2", explanation = "½ گویاست ولی حسابی نیست؛ حسابی‌ها همان طبیعی‌ها به‌علاوه‌ی صفرند.",
                topic = "عددهای شاخص", difficulty = 1, refSectionId = "s4"),
            StudyPack.Question(id = "q7", type = "numeric", text = "B = {x | x∈Z , −5 ≤ x ≤ −1} چند عضو دارد؟",
                options = listOf(), answer = "5", explanation = "عددهای صحیح از −۵ تا −۱: {−5,−4,−3,−2,−1} → ۵ عضو.",
                topic = "نمایش", difficulty = 2, refSectionId = "s5"),
            StudyPack.Question(id = "q8", type = "short", text = "زنجیره‌ی زیرمجموعه‌ای عددهای شاخص را با نماد بنویس.",
                options = listOf(), answer = "N ⊂ W ⊂ Z ⊂ Q", explanation = "هر عدد طبیعی حسابی است، هر حسابی صحیح است و هر صحیح گویاست.",
                topic = "عددهای شاخص", difficulty = 2, refSectionId = "s4"),
            StudyPack.Question(id = "q9", type = "mcq", text = "A = {n+3 | n∈N}. کدام مجموعه با A برابر است؟",
                options = listOf("{4,5,6,7,…}", "{3,4,5,6,…}", "{1,2,3,…}", "{0,1,2,3,…}"), answer = "{4,5,6,7,…}", explanation = "به‌جای n عددهای طبیعی ۱،۲،۳,… را می‌گذاریم: ۴، ۵، ۶، …",
                topic = "نمایش", difficulty = 2, refSectionId = "s5"),
            StudyPack.Question(id = "q10", type = "numeric", text = "مجموعه‌ای با ۲ عضو چند زیرمجموعه‌ی دقیق دارد؟",
                options = listOf(), answer = "3", explanation = "2² − ۱ = ۳: یعنی ∅ و دو تک‌عضوی.",
                topic = "شمارش", difficulty = 1, refSectionId = "s3"),
            StudyPack.Question(id = "q11", type = "short", text = "تفاوت {0} و ∅ چیست؟",
                options = listOf(), answer = "{0} یک عضو دارد ولی ∅ هیچ عضوی ندارد", explanation = "{0} مجموعه‌ای با یک عضو (صفر) است؛ تهی یعنی بدون هیچ عضو.",
                topic = "تهی", difficulty = 1, refSectionId = "s6"),
            StudyPack.Question(id = "q12", type = "mcq", text = "A = {−2,−1,0,1,2} و B = {x | x∈A , x² ≤ 2}. کدام درست است؟",
                options = listOf("B = {−1,0,1}", "B = A", "B = {−2,2}", "B = ∅"), answer = "B = {−1,0,1}", explanation = "مربعِ −۲ و ۲ می‌شود ۴ که از ۲ بزرگ‌تر است؛ پس فقط −۱ و ۰ و ۱ می‌مانند.",
                topic = "نمایش", difficulty = 3, refSectionId = "s5"),
        ),
        solutions = listOf(
            StudyPack.Solution("sol1", "حل تشریحی q4 (زیرمجموعه‌های دقیق)", "چرا 2ⁿ − ۱؟\nگام ۱: همه‌ی زیرمجموعه‌ها 2ⁿ تاست.\nگام ۲: از آن‌ها دقیقاً یکی «خودِ مجموعه» است.\nگام ۳: پس دقیق‌ها = 2ⁿ − ۱ = ۱۶ − ۱ = ۱۵."),
            StudyPack.Solution("sol2", "حل تشریحی q7 (شمارش اعضای بازه)", "شرط: x صحیح و −5 ≤ x ≤ −1.\nگام ۱: از −۵ شروع کن و یکی‌یکی جلو برو: −۵، −۴، −۳، −۲، −۱.\nگام ۲: بشمار: ۵ عضو.\nنکته: تعداد اعضای بازه‌ی صحیح [a,b] = b − a + 1 است؛ اینجا (−1) − (−5) + 1 = ۵."),
            StudyPack.Solution("sol3", "حل تشریحی q12 (حل شرط مربع)", "شرط: x² ≤ 2 با x ∈ {−2,−1,0,1,2}.\nگام ۱: مربع هر کدام: ۴، ۱، ۰، ۱، ۴.\nگام ۲: کدام‌ها ≤ ۲؟ فقط ۰، ۱، ۱.\nگام ۳: پس B = {−1,0,1}؛ دقت کن −۲ و ۲ حذف می‌شوند."),
        ),
    )
    private fun l03(): StudyPack = StudyPack(
        packId = "C905_E01-L03", bookCode = "C905", lessonId = "E01-L03",
        title = "درس ۳ — اجتماع، اشتراک و تفاضل مجموعه‌ها", bookTitle = "ریاضی پایه نهم", pdfFileName = "C905_E01-L03_BOOK.pdf",
        sections = listOf(
            StudyPack.Section(id = "s1", title = "اشتراک؛ عضوهای مشترک دو مجموعه", kind = "concept", body = "A ∩ B مجموعه‌ی عضوهایی است که هم عضو A و هم عضو B هستند: A∩B = {x | x∈A و x∈B}.\nدر نمودار ون، ناحیه‌ی مشترک دو دایره.\nاگر عضو مشترک نداشته باشند: A∩B = ∅.", images = listOf("lesson-img/C905_E01-L01_RAHNAMA-MOSAVVAR_IMG-01.png")),
            StudyPack.Section(id = "s2", title = "اجتماع؛ عضوهای حداقل یکی از دو مجموعه", kind = "concept", body = "A ∪ B مجموعه‌ی عضوهایی است که حداقل در یکی از دو مجموعه‌اند: A∪B = {x | x∈A یا x∈B}.\nمهم‌ترین نکته: عضو مشترک را فقط یک‌بار می‌نویسیم.\nمثال: {1,2}∪{2,3} = {1,2,3}.", images = listOf("lesson-img/C905_E01-L01_RAHNAMA-MOSAVVAR_IMG-02.png")),
            StudyPack.Section(id = "s3", title = "فرمول طلایی شمارش عضوها", kind = "important", body = "n(A∪B) = n(A) + n(B) − n(A∩B)\nچون عضوهای مشترک در جمع دو بار شمرده می‌شوند، یک‌بار باید کم شوند.\nپرتکرارترین فرمول فصل در آزمون‌ها؛ در مسائل کلامی اول n مشترک را پیدا کن.", images = listOf("lesson-img/C905_E01-L01_RAHNAMA-MOSAVVAR_IMG-03.png")),
            StudyPack.Section(id = "s4", title = "تفاضل؛ عضوهای «فقطِ» یک مجموعه", kind = "concept", body = "A − B = {x | x∈A و x∉B}؛ یعنی عضوهای A که در B نیستند.\nتفاضل جهت دارد: A−B با B−A برابر نیست.\nدر نمودار ون، بخشی از دایره‌ی A که بیرون از B است."),
            StudyPack.Section(id = "s5", title = "ترجمه‌ی صورت سؤال به نماد", kind = "note", body = "«هر دو / هم … هم …» → A ∩ B\n«حداقل یکی / دست‌کم یکی» → A ∪ B\n«فقط» → A − B یا B − A\n«هیچ عضو مشترکی ندارد» → A ∩ B = ∅"),
            StudyPack.Section(id = "s6", title = "خطاهای رایج امتحانی این درس", kind = "exam", body = "عضو مشترک را در اجتماع دو بار ننویس.\nجهت تفاضل را گم نکن: «فقط والیبال» یعنی V−F نه F−V.\nn(A∪B) را با n(A∩B) قاطی نکن؛ اشتراک همیشه ≤ کوچک‌ترین و اجتماع همیشه ≥ بزرگ‌ترین مجموعه است.\nفراموش‌کردن کم‌کردن n(A∩B) در فرمول = جواب بزرگ‌تر از واقعیت؛ با جمع سه ناحیه‌ی جدا کنترل کن.", images = listOf("lesson-img/C905_E01-L01_PASOKH-TAMRIN_IMG-01.png")),
            StudyPack.Section(id = "s7", title = "جمع‌بندی امتحانی یک‌نگاهی", kind = "exam", body = "اشتراک: عضوهای مشترک؛ اجتماع: عضوهای حداقل یکی؛ تفاضل: عضوهای فقط A.\nعضو مشترک در اجتماع فقط یک‌بار نوشته می‌شود.\nفرمول شمارش: n(A∪B) = n(A)+n(B)−n(A∩B).\nاجتماع از سه ناحیه‌ی جدا ساخته می‌شود: n(A−B)+n(B−A)+n(A∩B)."),
        ),
        flashcards = listOf(
            StudyPack.Flashcard("c1", "اشتراک A∩B را تعریف کن.", "مجموعه‌ی عضوهایی که هم عضو A و هم عضو B هستند: {x | x∈A و x∈B}.", "اشتراک", "مشترک‌ها"),
            StudyPack.Flashcard("c2", "اجتماع A∪B را تعریف کن.", "مجموعه‌ی عضوهایی که حداقل در یکی از دو مجموعه‌اند: {x | x∈A یا x∈B}.", "اجتماع", "حداقل یکی"),
            StudyPack.Flashcard("c3", "تفاضل A−B را تعریف کن.", "عضوهایی که در A هستند ولی در B نیستند: {x | x∈A و x∉B}.", "تفاضل", "فقط A"),
            StudyPack.Flashcard("c4", "فرمول طلایی n(A∪B)؟", "n(A∪B) = n(A) + n(B) − n(A∩B) — عضو مشترک دو بار شمرده می‌شود، یک‌بار کم می‌شود.", "شمارش", "منهای مشترک"),
            StudyPack.Flashcard("c5", "عضو مشترک در اجتماع چند بار نوشته می‌شود؟", "فقط یک‌بار؛ چون عضوهای مجموعه متمایزند. {1,2}∪{2,3} = {1,2,3}.", "اجتماع", "بدون تکرار"),
            StudyPack.Flashcard("c6", "«هر دو» در مسئله‌ی کلامی کدام نماد است؟", "A ∩ B (اشتراک)", "ترجمه", "هر دو = مشترک"),
            StudyPack.Flashcard("c7", "«حداقل یکی» کدام نماد است؟", "A ∪ B (اجتماع)", "ترجمه", "دست‌کم یکی"),
            StudyPack.Flashcard("c8", "«فقط» در مسئله‌ی کلامی کدام نماد است؟", "A − B یا B − A (تفاضل جهت‌دار)", "ترجمه", "منهای دیگری"),
            StudyPack.Flashcard("c9", "اگر دو مجموعه عضو مشترک نداشته باشند؟", "اشتراکشان تهی است: A∩B = ∅ (مثل زوج‌ها و فردهای طبیعی).", "اشتراک", "تهی"),
            StudyPack.Flashcard("c10", "A−B و B−A برابرند؟", "نه؛ تفاضل جهت دارد. فقط وقتی دو مجموعه برابر باشند برابر می‌شوند.", "تفاضل", "جهت‌دار"),
            StudyPack.Flashcard("c11", "شمارنده‌های مشترک ۱۲ و ۱۸؟", "A={1,2,3,4,6,12} و B={1,2,3,6,9,18} → A∩B = {1,2,3,6} = شمارنده‌های ب.م.م(۱۲،۱۸).", "کاربرد", "ب.م.م"),
            StudyPack.Flashcard("c12", "رابطه‌ی اجتماع با سه ناحیه‌ی ون؟", "n(A∪B) = n(A−B) + n(B−A) + n(A∩B) — سه ناحیه‌ی جدا بدون هم‌پوشانی.", "شمارش", "سه ناحیه"),
        ),
        questions = listOf(
            StudyPack.Question(id = "q1", type = "mcq", text = "A = {1,2,3,4,5,8} و B = {3,4,5,6,7}. حاصل A∩B کدام است؟",
                options = listOf("{3,4,5}", "{1,2,8}", "{1,2,3,4,5,6,7,8}", "{6,7}"), answer = "{3,4,5}", explanation = "فقط ۳ و ۴ و ۵ در هر دو مجموعه‌اند.",
                topic = "اشتراک", difficulty = 1, refSectionId = "s1"),
            StudyPack.Question(id = "q2", type = "numeric", text = "برای همان A و B، مقدار n(A∪B) چند است؟",
                options = listOf(), answer = "8", explanation = "اجتماع: {1,…,8} بدون تکرار → ۸ عضو. کنترل با فرمول: 6+5−3=۸.",
                topic = "اجتماع", difficulty = 1, refSectionId = "s2"),
            StudyPack.Question(id = "q3", type = "mcq", text = "«دانش‌آموزانی که فقط در تیم والیبال بازی می‌کنند» با کدام نماد نوشته می‌شود؟",
                options = listOf("V − F", "F − V", "V ∩ F", "V ∪ F"), answer = "V − F", explanation = "«فقط» یعنی تفاضل؛ والیبال‌بازهایی که فوتبال‌باز نیستند.",
                topic = "ترجمه", difficulty = 2, refSectionId = "s5"),
            StudyPack.Question(id = "q4", type = "numeric", text = "شمارنده‌های مشترک ۱۲ و ۱۸ چند تا هستند؟",
                options = listOf(), answer = "4", explanation = "A∩B = {1,2,3,6} → ۴ عضو (شمارنده‌های ب.م.م).",
                topic = "کاربرد", difficulty = 2, refSectionId = "s5"),
            StudyPack.Question(id = "q5", type = "numeric", text = "در کلاسی ۲۰ نفر عضو گروه سرود و ۱۵ نفر عضو گروه تئاتر هستند و ۶ نفر در هر دو گروه‌اند. چند نفر دست‌کم در یکی از دو گروه فعالیت می‌کنند؟",
                options = listOf(), answer = "29", explanation = "n(∪) = 20 + 15 − 6 = ۲۹.",
                topic = "شمارش", difficulty = 2, refSectionId = "s3"),
            StudyPack.Question(id = "q6", type = "short", text = "تفاضل A−B را به زبان عضویت (با نماد ∈) بنویس.",
                options = listOf(), answer = "x∈A و x∉B", explanation = "عضو A است و عضو B نیست.",
                topic = "تفاضل", difficulty = 1, refSectionId = "s4"),
            StudyPack.Question(id = "q7", type = "mcq", text = "حاصل {1,2}∪{2,3} کدام است؟",
                options = listOf("{1,2,3}", "{1,2,2,3}", "{2}", "{1,3}"), answer = "{1,2,3}", explanation = "عضو مشترک (۲) فقط یک‌بار نوشته می‌شود.",
                topic = "اجتماع", difficulty = 1, refSectionId = "s2"),
            StudyPack.Question(id = "q8", type = "numeric", text = "A = {a,b,c,d,e,k} و B = {c,d,k,f,s,t}. مقدار n(A−B) چند است؟",
                options = listOf(), answer = "3", explanation = "A−B = {a,b,e} → ۳ عضو؛ حرف‌های مشترک c و d و k نمی‌آیند.",
                topic = "تفاضل", difficulty = 2, refSectionId = "s4"),
            StudyPack.Question(id = "q9", type = "mcq", text = "کدام جمله همیشه درست است؟",
                options = listOf("n(A∩B) هرگز از n(A) بزرگ‌تر نمی‌شود", "n(A∪B) از n(A) کوچک‌تر است", "n(A∩B) = n(A) + n(B)", "n(A−B) = n(B−A)"), answer = "n(A∩B) هرگز از n(A) بزرگ‌تر نمی‌شود", explanation = "اشتراک زیرمجموعه‌ی هر دو مجموعه است؛ پس از کوچک‌ترین هم بزرگ‌تر نمی‌شود.",
                topic = "اشتراک", difficulty = 2, refSectionId = "s6"),
            StudyPack.Question(id = "q10", type = "numeric", text = "اگر n(V) = 7 و n(F) = 9 و n(V∩F) = 5 باشد، n(V∪F) چند است؟",
                options = listOf(), answer = "11", explanation = "7 + 9 − 5 = ۱۱.",
                topic = "شمارش", difficulty = 1, refSectionId = "s3"),
            StudyPack.Question(id = "q11", type = "mcq", text = "C = {1,7,10,11} و B = {3,5,7,9,15}. حاصل C−B کدام است؟",
                options = listOf("{1,10,11}", "{7}", "{1,7,10,11}", "{3,5,9,15}"), answer = "{1,10,11}", explanation = "۷ عضو B هم هست؛ پس از C حذف می‌شود.",
                topic = "تفاضل", difficulty = 2, refSectionId = "s4"),
            StudyPack.Question(id = "q12", type = "numeric", text = "در مسئله‌ی دو تیم، ۵ نفر عضو هر دو تیم و ۲ نفر فقط والیبال‌اند. «فقط فوتبال‌بازها» چند نفرند اگر n(F) = 9 باشد؟",
                options = listOf(), answer = "4", explanation = "فقط فوتبال = n(F) − n(V∩F) = 9 − 5 = ۴.",
                topic = "ترجمه", difficulty = 2, refSectionId = "s5"),
        ),
        solutions = listOf(
            StudyPack.Solution("sol1", "حل تشریحی q5 (فرمول شمارش)", "داده‌ها: n(سرود)=۲۰، n(تئاتر)=۱۵، n(مشترک)=۶.\nگام ۱: فرمول: n(∪) = n(A)+n(B)−n(A∩B).\nگام ۲: جایگذاری: 20+15−6 = ۲۹.\nکنترل: فقط سرود ۱۴ + فقط تئاتر ۹ + مشترک ۶ = ۲۹ ✓"),
            StudyPack.Solution("sol2", "حل تشریحی q4 (شمارنده‌های مشترک)", "گام ۱: شمارنده‌های ۱۲: {1,2,3,4,6,12}.\nگام ۲: شمارنده‌های ۱۸: {1,2,3,6,9,18}.\nگام ۳: مشترک‌ها: {1,2,3,6} → ۴ عضو.\nنکته: مشترک‌ها دقیقاً شمارنده‌های ب.م.م(۱۲،۱۸)=۶ هستند."),
            StudyPack.Solution("sol3", "حل تشریحی q8 (تفاضل مجموعه‌ها)", "A = {a,b,c,d,e,k} و B = {c,d,k,f,s,t}.\nگام ۱: عضوهای A را یکی‌یکی ببین؛ اگر در B بود حذفش کن.\nگام ۲: c و d و k حذف می‌شوند.\nگام ۳: A−B = {a,b,e} → ۳ عضو (و B−A = {f,s,t})."),
        ),
    )
    private fun l04(): StudyPack = StudyPack(
        packId = "C905_E01-L04", bookCode = "C905", lessonId = "E01-L04",
        title = "درس ۴ — مجموعه‌ها و احتمال", bookTitle = "ریاضی پایه نهم", pdfFileName = "C905_E01-L04_BOOK.pdf",
        sections = listOf(
            StudyPack.Section(id = "s1", title = "فضای نمونه و پیشامد؛ با زبان مجموعه‌ها", kind = "concept", body = "فضای نمونه S = مجموعه‌ی همه‌ی نتایج ممکن یک آزمایش تصادفی.\nهر زیرمجموعه‌ی S یک «پیشامد» است؛ پیشامد خودش یک مجموعه است.\nمثال: در تاس، A = {2,4,6} پیشامد «زوج آمدن» است.", images = listOf("lesson-img/C905_E01-L01_RAHNAMA-MOSAVVAR_IMG-01.png")),
            StudyPack.Section(id = "s2", title = "فرمول احتمال", kind = "important", body = "P(A) = n(A) ÷ n(S)\nاحتمال یعنی نسبت اندازه‌ی مجموعه‌ی مطلوب به اندازه‌ی کل فضای نمونه.\nهمیشه 0 ≤ P(A) ≤ 1 است؛ اگر از این بازه بیرون آمدی شمارشت را بازبینی کن.", images = listOf("lesson-img/C905_E01-L01_RAHNAMA-MOSAVVAR_IMG-02.png")),
            StudyPack.Section(id = "s3", title = "پیشامد شانس و پیشامد قطعی", kind = "concept", body = "A = ∅ → P(A) = 0؛ پیشامد «شانس» است و هرگز رخ نمی‌دهد (مثل عدد ۷ در تاس).\nA = S → P(A) = 1؛ پیشامد «قطعی» است (مثل «عدد کوچک‌تر از ۷» در تاس).\nهرچه احتمال به ۱ نزدیک‌تر، پیشامد محتمل‌تر.", images = listOf("lesson-img/C905_E01-L01_RAHNAMA-MOSAVVAR_IMG-03.png")),
            StudyPack.Section(id = "s4", title = "آزمایش دومرحله‌ای؛ دو تاس با هم", kind = "important", body = "هر نتیجه یک جفت مرتب مثل (۲،۵) است؛ عدد اول تاس آبی و دوم تاس قرمز.\nn(S) = 6×6 = ۳۶؛ دقت کن (۱،۲) و (۲،۱) دو نتیجه‌ی متفاوت‌اند.\nابزار طلایی: جدول ۶×۶؛ مثلاً «مجموع ۷» شش حالت دارد → P = 6/36 = 1/6."),
            StudyPack.Section(id = "s5", title = "پیشامدهای کلاسیک تاس و ابزار درس‌های قبل", kind = "note", body = "مضرب ۳: {3,6} → P = 2/6 = 1/3؛ عدد اول: {2,3,5} → P = 3/6 = 1/2.\nپیشامدهای ترکیبی با درس ۳: «زوج یا مضرب ۳» = {2,4,6}∪{3,6} = {2,3,4,6}.\nدر چرخنده‌ی ۱ تا ۵، همه‌ی پیشامدها = همه‌ی زیرمجموعه‌های S = 2⁵ = ۳۲."),
            StudyPack.Section(id = "s6", title = "خطاهای رایج امتحانی این درس", kind = "exam", body = "در دو تاس، n(S) = ۳۶ است نه ۲۱ و نه ۱۲.\nکسر احتمال را ساده کن: 2/6 را بنویس 1/3.\nاحتمال هرگز منفی یا بزرگ‌تر از ۱ نمی‌شود؛ اگر شد، شمارش n(A) یا n(S) را بازبینی کن.\nپیشامد قطعی (P=1) فقط وقتی است که A = S باشد.", images = listOf("lesson-img/C905_E01-L01_PASOKH-TAMRIN_IMG-01.png")),
            StudyPack.Section(id = "s7", title = "جمع‌بندی امتحانی یک‌نگاهی", kind = "exam", body = "S = همه‌ی نتایج ممکن؛ پیشامد = هر زیرمجموعه‌ی S.\nP(A) = n(A) ÷ n(S) و 0 ≤ P(A) ≤ 1.\n∅ → شانس (۰) و S → قطعی (۱).\nدو تاس: n(S) = ۳۶ و جدول ۶×۶ ابزار شمارش.\n«سبز نباشد» در جعبه‌ی مهره = تفاضل کل از آن دسته (درس ۳)."),
        ),
        flashcards = listOf(
            StudyPack.Flashcard("c1", "فضای نمونه چیست؟", "مجموعه‌ی همه‌ی نتایج ممکن یک آزمایش تصادفی؛ با S نشان می‌دهیم.", "فضای نمونه", "همه‌ی حالت‌ها"),
            StudyPack.Flashcard("c2", "پیشامد چیست؟", "هر زیرمجموعه‌ی فضای نمونه S؛ مثلاً {2,4,6} یعنی «زوج آمدن».", "پیشامد", "زیرمجموعه‌ی S"),
            StudyPack.Flashcard("c3", "فرمول احتمال؟", "P(A) = n(A) ÷ n(S) — تعداد حالت‌های مطلوب به همه‌ی حالت‌ها.", "احتمال", "مطلوب بر کل"),
            StudyPack.Flashcard("c4", "پیشامد شانس چیست؟", "پیشامدی با A = ∅ که احتمالش صفر است و هرگز رخ نمی‌دهد؛ مثل عدد ۷ در تاس.", "شانس", "احتمال صفر"),
            StudyPack.Flashcard("c5", "پیشامد قطعی چیست؟", "پیشامدی با A = S که احتمالش ۱ است؛ مثل «عدد کوچک‌تر از ۷» در تاس.", "قطعی", "احتمال یک"),
            StudyPack.Flashcard("c6", "در دو تاس n(S) چند است و چرا؟", "۳۶؛ چون ۶×۶ جفت مرتب (آبی، قرمز) داریم؛ (۱،۲) و (۲،۱) متفاوت‌اند.", "دو تاس", "۶×۶"),
            StudyPack.Flashcard("c7", "احتمال «مجموع ۷» در دو تاس؟", "۶ حالت از ۳۶ → 6/36 = 1/6؛ حالت‌ها روی قطر مقابل جدول ۶×۶.", "دو تاس", "۶ حالت"),
            StudyPack.Flashcard("c8", "ابزار شمارش منظم در دو تاس؟", "جدول ۶×۶ — همه‌ی ۳۶ حالت را منظم نشان می‌دهد.", "دو تاس", "جدول"),
            StudyPack.Flashcard("c9", "چرا احتمال هرگز بزرگ‌تر از ۱ نیست؟", "چون A زیرمجموعه‌ی S است؛ n(A) هرگز از n(S) بیشتر نمی‌شود.", "احتمال", "A ⊆ S"),
            StudyPack.Flashcard("c10", "در چرخنده‌ی ۱..۵ چند پیشامد داریم؟", "همه‌ی پیشامدها = همه‌ی زیرمجموعه‌های S = 2⁵ = ۳۲.", "پیشامد", "2ⁿ"),
            StudyPack.Flashcard("c11", "جعبه: ۳ قرمز، ۴ آبی، ۵ سبز. P(سبز نباشد)؟", "غیرسبزها ۷ از ۱۲ → 7/12 (همان تفاضل: کل منهای سبزها).", "کاربرد", "تفاضل درس ۳"),
            StudyPack.Flashcard("c12", "P(زوج یا مضرب ۳) در تاس؟", "{2,4,6}∪{3,6} = {2,3,4,6} → 4/6 = 2/3 (اجتماع درس ۳).", "کاربرد", "اجتماع"),
        ),
        questions = listOf(
            StudyPack.Question(id = "q1", type = "numeric", text = "در پرتاب یک تاس، احتمال «مضرب ۳ آمدن» چقدر است؟ (به‌صورت کسر بنویس، مثل 1/2)",
                options = listOf(), answer = "1/3", explanation = "A = {3,6} → P = 2/6 = 1/3.",
                topic = "احتمال", difficulty = 1, refSectionId = "s2"),
            StudyPack.Question(id = "q2", type = "mcq", text = "کدام پیشامد در پرتاب تاس «قطعی» است؟",
                options = listOf("عدد کوچک‌تر از ۷ بیاید", "عدد ۷ بیاید", "عدد اول بیاید", "مضرب ۳ بیاید"), answer = "عدد کوچک‌تر از ۷ بیاید", explanation = "A = S = {1,…,6} → P = 1؛ ولی «عدد ۷» شانس است (P=0).",
                topic = "قطعی", difficulty = 1, refSectionId = "s3"),
            StudyPack.Question(id = "q3", type = "numeric", text = "در پرتاب تاس، احتمال «عدد اول آمدن» چقدر است؟ (کسر)",
                options = listOf(), answer = "1/2", explanation = "اول‌ها {2,3,5} → 3/6 = 1/2.",
                topic = "احتمال", difficulty = 1, refSectionId = "s2"),
            StudyPack.Question(id = "q4", type = "numeric", text = "دو تاس را با هم می‌ریزیم. احتمال اینکه هر دو عدد اول باشند؟ (کسر)",
                options = listOf(), answer = "1/4", explanation = "اول‌ها {2,3,5} → حالت‌ها 3×3 = ۹ از ۳۶ → 9/36 = 1/4.",
                topic = "دو تاس", difficulty = 2, refSectionId = "s4"),
            StudyPack.Question(id = "q5", type = "numeric", text = "دو تاس را با هم می‌ریزیم. احتمال «مجموع ۷» چقدر است؟ (کسر)",
                options = listOf(), answer = "1/6", explanation = "۶ حالت: (1,6),(2,5),(3,4),(4,3),(5,2),(6,1) → 6/36 = 1/6.",
                topic = "دو تاس", difficulty = 2, refSectionId = "s4"),
            StudyPack.Question(id = "q6", type = "numeric", text = "دو تاس را با هم می‌ریزیم. احتمال اینکه دو عدد مثل هم باشند؟ (کسر)",
                options = listOf(), answer = "1/6", explanation = "(1,1) تا (6,6) یعنی ۶ حالت → 6/36 = 1/6.",
                topic = "دو تاس", difficulty = 2, refSectionId = "s4"),
            StudyPack.Question(id = "q7", type = "numeric", text = "در جعبه‌ای ۳ مهره‌ی قرمز، ۴ آبی و ۵ سبز است. احتمال آبی آمدن؟ (کسر)",
                options = listOf(), answer = "1/3", explanation = "n(S) = 12 → P = 4/12 = 1/3.",
                topic = "کاربرد", difficulty = 1, refSectionId = "s2"),
            StudyPack.Question(id = "q8", type = "numeric", text = "در همان جعبه، احتمال اینکه مهره «سبز نباشد»؟ (کسر)",
                options = listOf(), answer = "7/12", explanation = "غیرسبزها = 3+4 = ۷ → 7/12؛ همان تفاضل درس ۳.",
                topic = "کاربرد", difficulty = 2, refSectionId = "s5"),
            StudyPack.Question(id = "q9", type = "numeric", text = "خانواده‌ای سه فرزند دارد. احتمال «دقیقاً دو دختر بودن»؟ (کسر)",
                options = listOf(), answer = "3/8", explanation = "n(S) = ۸؛ حالت‌های دقیقاً دو دختر: ۳ تا → 3/8.",
                topic = "شمارش", difficulty = 3, refSectionId = "s4"),
            StudyPack.Question(id = "q10", type = "mcq", text = "در دو تاس، نتیجه‌ی (۱،۲) و (۲،۱)...",
                options = listOf("دو نتیجه‌ی متفاوت‌اند و n(S) = 36 است", "یکی‌اند و n(S) = 21 است", "یکی‌اند و n(S) = 12 است", "دو نتیجه‌ی متفاوت‌اند و n(S) = 12 است"), answer = "دو نتیجه‌ی متفاوت‌اند و n(S) = 36 است", explanation = "جفت مرتب است: اولی تاس آبی، دومی قرمز؛ ۶×۶ = ۳۶.",
                topic = "دو تاس", difficulty = 2, refSectionId = "s6"),
            StudyPack.Question(id = "q11", type = "numeric", text = "چرخنده‌ای ناحیه‌های مساوی ۱ تا ۵ دارد. احتمال پیشامد B = {1,2}؟ (کسر)",
                options = listOf(), answer = "2/5", explanation = "n(S) = 5 و n(B) = 2 → P = 2/5.",
                topic = "احتمال", difficulty = 1, refSectionId = "s2"),
            StudyPack.Question(id = "q12", type = "short", text = "چرا احتمال هیچ پیشامدی نمی‌تواند بزرگ‌تر از ۱ باشد؟",
                options = listOf(), answer = "چون A زیرمجموعه‌ی S است و n(A) هرگز از n(S) بیشتر نمی‌شود", explanation = "بیشترین حالت مطلوب، خودِ کل فضای نمونه است → P حداکثر ۱.",
                topic = "احتمال", difficulty = 2, refSectionId = "s6"),
        ),
        solutions = listOf(
            StudyPack.Solution("sol1", "حل تشریحی q5 (مجموع ۷ در دو تاس)", "گام ۱: n(S) = ۳۶ (جدول ۶×۶).\nگام ۲: حالت‌های مجموع ۷ روی قطر مقابل: (1,6),(2,5),(3,4),(4,3),(5,2),(6,1) → ۶ حالت.\nگام ۳: P = 6/36 = 1/6.\nنکته: مجموع ۷ بیشترین احتمال را در دو تاس دارد."),
            StudyPack.Solution("sol2", "حل تشریحی q9 (خانواده‌ی سه‌فرزندی)", "گام ۱: S = {(پ,پ,پ),(پ,پ,د),(پ,د,پ),(د,پ,پ),(پ,د,د),(د,پ,د),(د,د,پ),(د,د,د)} → n(S) = ۸.\nگام ۲: دقیقاً دو دختر: (پ,د,د),(د,پ,د),(د,د,پ) → ۳ حالت.\nگام ۳: P = 3/8.\nدقت: «دقیقاً» با «حداقل» فرق دارد؛ حداقل دو دختر می‌شد ۴ حالت."),
            StudyPack.Solution("sol3", "حل تشریحی q8 (سبز نباشد)", "گام ۱: n(S) = 3+4+5 = ۱۲.\nگام ۲: «سبز نباشد» یعنی قرمز یا آبی = تفاضل کل از سبزها (درس ۳).\nگام ۳: حالت‌های مطلوب = 3+4 = ۷ → P = 7/12.\nکنترل: P(سبز) = 5/12 و 7/12 + 5/12 = ۱ ✓"),
        ),
    )
    /**
     * آزمون بازه‌ای فصل ۱ (پرامپت ۰۷ — بخش ۱): ترکیب سوال‌های هر ۴ درس.
     * شناسه‌ی سوال‌ها با پیشوند درس یکتا می‌شود؛ بدون فلش‌کارت و حل جداگانه.
     */
    private fun exam(): StudyPack {
        val qs = listOf(l01(), l02(), l03(), l04()).flatMap { p ->
            p.questions.map { q -> q.copy(id = "${p.lessonId}-${q.id}") }
        }
        return StudyPack(
            packId = "C905_E01-EXAM", bookCode = "C905", lessonId = "E01-EXAM",
            title = "آزمون فصل ۱ — مجموعه‌ها (۴ درس)", bookTitle = "ریاضی پایه نهم",
            pdfFileName = "",
            sections = listOf(
                StudyPack.Section("s1", "این آزمون چیست؟", "exam",
                    "ترکیبی از سوال‌های درس ۱ تا ۴ فصل مجموعه‌ها است (تعداد کل: ${qs.size} سوال).\nهر بار ۱۰ سوال نمونه‌گیری می‌شود؛ غلط‌ها ثبت و در تلاش بعدی اولویت می‌گیرند."),
            ),
            flashcards = emptyList(), questions = qs, solutions = emptyList(),
        )
    }

    private fun mathFill(p: StudyPack, exercises: List<StudyPack.Exercise>): StudyPack {
        val teachSecs = p.sections.filter { it.kind != "exam" }
        val examSecs = p.sections.filter { it.kind == "exam" }
        return p.copy(
            teachText = teachSecs.joinToString("\n\n") { "«${it.title}»\n${it.body}" },
            teachSpeech = teachSecs.joinToString("\n") { it.body },
            summary = examSecs.lastOrNull()?.body ?: teachSecs.lastOrNull()?.body.orEmpty(),
            examTips = examSecs.joinToString("\n\n") { "• ${it.title}\n${it.body}" },
            exercises = exercises,
        )
    }

    private fun ex(id: String, prompt: String, answer: String, hint: String, topic: String, vararg alts: String) =
        StudyPack.Exercise(id, prompt, answer, alts.toList(), hint, topic)

    private val l01Ex = listOf(
        ex("e1", "مجموعه گردایه‌ای از اعضای …… و …… است. دو ویژگی را با ویرگول بنویس.", "مشخص، متمایز", "دو شرط تعریف مجموعه", "تعریف", "متمایز، مشخص"),
        ex("e2", "نماد عضویت چیست؟ (a عضو A)", "∈", "علامت عضویت", "عضویت", "a∈A"),
        ex("e3", "مجموعه‌ی تهی را با نماد بنویس.", "∅", "آکولاد خالی هم درست است", "تهی", "{}", "{ }"),
        ex("e4", "{۱، ۲، ۲} را بدون تکرار بنویس.", "{1,2}", "تکرار در مجموعه بی‌اثر است", "تساوی", "{2,1}", "{۱،۲}"),
        ex("e5", "A = {x | x طبیعی و x < 4} را سرعینی بنویس.", "{1,2,3}", "صفر طبیعی نیست", "نمایش", "{۱،۲،۳}"),
        ex("e6", "آیا ∅ = {∅} درست است؟ فقط «بله» یا «خیر».", "خیر", "تعداد اعضا را بشمار", "تهی", "نه"),
        ex("e7", "مجموعه‌ی عددهای طبیعی زوج ۱ تا ۹ چند عضو دارد؟", "4", "۲،۴،۶،۸", "شمارش", "۴"),
    )
    private val l02Ex = listOf(
        ex("e1", "مجموعه‌ی ۴ عضوی چند زیرمجموعه دارد؟", "16", "2⁴", "شمارش", "۱۶", "2^4"),
        ex("e2", "تعداد زیرمجموعه‌های دقیق مجموعه‌ی ۴ عضوی؟", "15", "2ⁿ − ۱", "شمارش", "۱۵"),
        ex("e3", "زنجیره‌ی عددهای شاخص را با ⊂ بنویس.", "N⊂W⊂Z⊂Q", "طبیعی ⊂ حسابی ⊂ صحیح ⊂ گویا", "عددهای شاخص", "N ⊂ W ⊂ Z ⊂ Q"),
        ex("e4", "∅ ⊆ A همیشه درست است؟ بله یا خیر.", "بله", "تهی زیرمجموعه‌ی همه است", "زیرمجموعه", "آری"),
        ex("e5", "B = {x | x∈Z , −5 ≤ x ≤ −1} چند عضو دارد؟", "5", "از −۵ تا −۱", "نمایش", "۵"),
        ex("e6", "مجموعه‌ی ۲ عضوی چند زیرمجموعه‌ی دقیق دارد؟", "3", "4−1", "شمارش", "۳"),
    )
    private val l03Ex = listOf(
        ex("e1", "A={1,2,3,4,5,8} و B={3,4,5,6,7}. A∩B را بنویس.", "{3,4,5}", "فقط مشترک‌ها", "اشتراک", "{۳،۴،۵}"),
        ex("e2", "برای همان A و B، n(A∪B) چند است؟", "8", "6+5−3", "اجتماع", "۸"),
        ex("e3", "فرمول n(A∪B) را بنویس (بدون فاصله).", "n(A)+n(B)-n(A∩B)", "منهای مشترک", "شمارش", "n(A∪B)=n(A)+n(B)-n(A∩B)"),
        ex("e4", "۲۰ سرود، ۱۵ تئاتر، ۶ مشترک. دست‌کم یکی چند نفر؟", "29", "20+15−6", "شمارش", "۲۹"),
        ex("e5", "A={a,b,c,d,e,k} و B={c,d,k,f,s,t}. n(A−B)؟", "3", "{a,b,e}", "تفاضل", "۳"),
        ex("e6", "اگر n(V)=7 و n(F)=9 و n(V∩F)=5 باشد n(V∪F)؟", "11", "7+9−5", "شمارش", "۱۱"),
    )
    private val l04Ex = listOf(
        ex("e1", "احتمال مضرب ۳ در تاس؟ کسر ساده.", "1/3", "2/6", "احتمال", "۲/۶"),
        ex("e2", "در دو تاس n(S) چند است؟", "36", "۶×۶", "دو تاس", "۳۶"),
        ex("e3", "احتمال مجموع ۷ در دو تاس؟ کسر ساده.", "1/6", "6/36", "دو تاس", "۶/۳۶"),
        ex("e4", "جعبه: ۳ قرمز ۴ آبی ۵ سبز. P(آبی)؟ کسر ساده.", "1/3", "4/12", "کاربرد", "۴/۱۲"),
        ex("e5", "در همان جعبه P(سبز نباشد)؟", "7/12", "۷ غیرسبز از ۱۲", "کاربرد", "۷/۱۲"),
        ex("e6", "چرخنده‌ی ۱ تا ۵، P({1,2})؟", "2/5", "۲ از ۵", "احتمال", "۲/۵"),
    )

    val packs: List<StudyPack> = listOf(
        mathFill(l01(), l01Ex),
        mathFill(l02(), l02Ex),
        mathFill(l03(), l03Ex),
        mathFill(l04(), l04Ex),
        exam(),
    )

    val module = BookModule(
        bookCode = "C905", title = "ریاضی پایه نهم", subject = "ریاضی", packs = packs,
    )
}
