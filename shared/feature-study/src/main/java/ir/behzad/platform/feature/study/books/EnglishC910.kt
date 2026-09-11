package ir.behzad.platform.feature.study.books

import ir.behzad.platform.feature.study.BookModule
import ir.behzad.platform.feature.study.StudyPack

/**
 * ماژول کتاب «زبان انگلیسی پایه نهم» (C910) — معماری مصوب: کل محتوای درس داخل کد.
 * منبع تألیف: PDF کتاب درسی ریپو.
 */
object EnglishC910 {
    private fun l01(): StudyPack = StudyPack(
        packId = "C910_L01", bookCode = "C910", lessonId = "L01",
        title = "Lesson 1 — Personality (شخصیت)", bookTitle = "زبان انگلیسی پایه نهم", pdfFileName = "C910_L01_BOOK.pdf",
        audioFileId = "C910_L01_AUDIO.mp3",
        sections = listOf(
            StudyPack.Section(id = "s1", title = "Conversation: دو پسرعمه درباره‌ی بهترین دوست", kind = "important", body = "Ehsan: Who is your best friend at school?\nParham: Reza.\nEhsan: What is he like?\nParham: Oh, he is really great! He's clever and kind.\nEhsan: Is he hard-working too?\nParham: Yes! And he's always very helpful.\nEhsan: How?\nParham: He always helps me with my lessons.\nترجمه: احسان: بهترین دوستت در مدرسه کیست؟ — پرهام: رضا. — او چه‌طور آدمی است؟ — واقعاً عالی است؛ باهوش و مهربان. — سخت‌کوش هم هست؟ — بله! و همیشه خیلی کمک‌گر است. — چطور؟ — همیشه در درس‌هایم کمکم می‌کند."),
            StudyPack.Section(id = "s2", title = "Practice 1: سوال با Am/Is/Are", kind = "important", body = "Are you hard-working? — Yes, I am.\nIs he clever? — Yes, he is.\nIs Zahra talkative? — No, she isn't.\nAre they neat? — Yes, they are.\nAre they upset? — No, they're not.\nالگو: Are/Is + فاعل + صفت؟ جواب کوتاه: Yes/No + ضمیر + (n't) am/is/are."),
            StudyPack.Section(id = "s3", title = "Practice 2: What ... like؟", kind = "important", body = "What's your friend like? — He's very funny.\nWhat's your mother like? — She's very kind and patient.\nWhat's he like? — He is quiet.\nWhat's she like? — She is clever.\nWhat are you like? — I'm a bit serious.\nWhat are they like? — They are very kind.\n«What is he like?» برای پرسیدن شخصیت و اخلاق است."),
            StudyPack.Section(id = "s4", title = "واژه‌های شخصیت (صفت‌ها)", kind = "important", body = "مثبت: brave (شجاع)، neat (مرتب)، quiet (آرام)، funny (بامزه)، clever (باهوش)، kind (مهربان)، helpful (کمک‌گر)، hard-working (سخت‌کوش)، patient (صبور)، generous (بخشنده)، polite (مؤدب).\nمنفی: angry (خشمگین)، careless (بی‌دقت)، cruel (بی‌رحم)، rude (گستاخ)، nervous (عصبی)، selfish (خودخواه)."),
            StudyPack.Section(id = "s5", title = "Language Melody: آهنگ جمله‌های خبری", kind = "note", body = "در جمله‌های خبری (affirmative) صدای آخر جمله پایین می‌آید:\nTeacher: Farzaneh is a clever student. Everybody likes her.\nSamira: Yes. I know. She is also very helpful.\nتمرین: 1. He's very kind. 2. She's very patient. 3. You are very clever. 4. Everybody likes her. 5. I do my homework. 6. She works for a company."),
            StudyPack.Section(id = "s6", title = "Grammar: فعل to be و There is/are", kind = "important", body = "I am — He/She/It is — We/You/They are:\nAli is clever. / It is red. / Zahra and Nadia are generous.\nپرسشی: Am I...? / Is he...? / Are you...?\nمنفی: I'm not talkative. / He isn't shy. / They are not rude.\nکوتاه‌نویسی: I'm, He's, It's, We're, You're, They're, isn't, aren't.\nThere is + مفرد: There is an eraser in the classroom.\nThere are + جمع: There are two computers in the classroom.\nپرسش: Is there an apple on the table? — Yes, there is."),
            StudyPack.Section(id = "s7", title = "Talk to Your Teacher + نکات", kind = "note", body = "«Let me check it in the dictionary.» — بگذار در فرهنگ‌لغت چکش کنم.\nبرای پرسیدن معنی: What's the meaning of ...? / What does ... mean?"),
        ),
        flashcards = listOf(
            StudyPack.Flashcard("c1", "معنی «hard-working» چیست؟", "سخت‌کوش، پرتلاش.", "واژگان", ""),
            StudyPack.Flashcard("c2", "معنی «selfish» و «generous»؟", "selfish: خودخواه؛ generous: بخشنده.", "واژگان", "متضاد یکدیگرند"),
            StudyPack.Flashcard("c3", "What's he like? چه زمانی به‌کار می‌رود؟", "برای پرسیدن شخصیت و اخلاقِ کسی؛ جواب با صفت: He's clever.", "Practice 2", ""),
            StudyPack.Flashcard("c4", "جواب کوتاه Is she neat? چیست؟", "Yes, she is. / No, she isn't.", "Practice 1", ""),
            StudyPack.Flashcard("c5", "برای «they» کدام شکل to be می‌آید؟", "are — They are kind.", "Grammar", ""),
            StudyPack.Flashcard("c6", "شکل کوتاه «He is not» چیست؟", "He isn't. (یا He's not)", "Grammar", ""),
            StudyPack.Flashcard("c7", "There is یا There are: «... two students»", "There are two students — جمع با are.", "Grammar", ""),
            StudyPack.Flashcard("c8", "معنی «careless» و «rude»؟", "careless: بی‌دقت/بی‌احتیاط؛ rude: بی‌ادب/گستاخ.", "واژگان", ""),
            StudyPack.Flashcard("c9", "معنی «brave»؟", "شجاع.", "واژگان", ""),
            StudyPack.Flashcard("c10", "در Language Melody صدای آخر جمله‌ی خبری چه می‌کند؟", "پایین می‌آید.", "Language Melody", ""),
        ),
        questions = listOf(
            StudyPack.Question(id = "qq1", type = "mcq", text = "Choose the correct form: Kate ..... funny.", options = listOf("isn't", "aren't", "am not", "not is"), answer = "isn't", explanation = "Kate مفرد سوم‌شخص است → is + not = isn't.", topic = "Grammar", difficulty = 1, refSectionId = "s1"),
            StudyPack.Question(id = "qq2", type = "mcq", text = "Choose: There ..... fifteen benches in the class.", options = listOf("is", "are", "am", "be"), answer = "are", explanation = "benches جمع است → there are.", topic = "Grammar", difficulty = 1, refSectionId = "s1"),
            StudyPack.Question(id = "qq3", type = "short", text = "جمله را مرتب کن: am/I/nervous/not/.", options = emptyList(), answer = "I am not nervous.", explanation = "ترتیب: فاعل + to be + not + صفت.", topic = "Grammar", difficulty = 2, refSectionId = "s1"),
            StudyPack.Question(id = "qq4", type = "short", text = "جمله را مرتب کن: and/you/your friend/selfish/not/are/.", options = emptyList(), answer = "You and your friend are not selfish.", explanation = "فاعل دوتایی → are.", topic = "Grammar", difficulty = 2, refSectionId = "s1"),
            StudyPack.Question(id = "qq5", type = "short", text = "سوال بساز: Mina/is/careless/؟", options = emptyList(), answer = "Is Mina careless?", explanation = "Is + فاعل + صفت + ? — جواب: Yes, she is.", topic = "Grammar", difficulty = 1, refSectionId = "s1"),
            StudyPack.Question(id = "qq6", type = "short", text = "جمله را مرتب کن: there/an/orange/table/is/on the/؟", options = emptyList(), answer = "Is there an orange on the table?", explanation = "سوال There: Is there + ... ?", topic = "Grammar", difficulty = 2, refSectionId = "s1"),
            StudyPack.Question(id = "qq7", type = "mcq", text = "در مکالمه، Parham درباره‌ی شخصیت رضا چه می‌گوید؟", options = listOf("He's quiet and shy", "He's clever and kind", "He's rude", "He's careless"), answer = "He's clever and kind (and very helpful)", explanation = "کلمه‌به‌کلمه از متن مکالمه.", topic = "Conversation", difficulty = 2, refSectionId = "s1"),
            StudyPack.Question(id = "qq8", type = "mcq", text = "پرسیدن «شخصیت مادرت چه‌طور است؟» به انگلیسی:", options = listOf("Who is your mother?", "What's your mother like?", "How is your mother do?", "What does your mother like?"), answer = "What's your mother like?", explanation = "الگوی What + be + فاعل + like؟", topic = "Practice 2", difficulty = 2, refSectionId = "s1"),
            StudyPack.Question(id = "qq9", type = "short", text = "با کلمه‌ی «patient» یک جمله بنویس.", options = emptyList(), answer = "نمونه: My teacher is very patient.", explanation = "هر جمله‌ی درست با patient پذیرفته است.", topic = "واژگان", difficulty = 1, refSectionId = "s1"),
            StudyPack.Question(id = "qq10", type = "mcq", text = "کدام واژه شخصیت منفی است؟", options = listOf("brave", "neat", "selfish", "polite"), answer = "selfish", explanation = "brave/neat/polite مثبت‌اند؛ selfish یعنی خودخواه.", topic = "واژگان", difficulty = 1, refSectionId = "s1"),
        ),
        solutions = listOf(
            StudyPack.Solution("sol1", "پاسخ کتاب کار تمرین ۱ — چرای هر گزینه", "1. Kate **isn't** funny → چرا؟ Kate مفرد سوم‌شخص است؛ to be برای he/she/it فقط «is» است، پس منفی‌اش «isn't».\n2. There **is** a car → چرا؟ بعد از There، فعل با «اولین اسم بعدی» سازگار است؛ a car مفرد → is.\n3. There **are** fifteen benches → چرا؟ benches جمع → are.\n4. It **is** really beautiful → چرا؟ it همیشه is.\n5. Iranians **are** very brave → چرا؟ Iranians جمع (حرف آخر s) → are.\nقاعده‌ی کلی: I→am؛ he/she/it و هر مفرد→is؛ you/we/they و هر جمع→are."),
            StudyPack.Solution("sol2", "پاسخ کتاب کار تمرین ۲ — چرا این to beها؟", "I **am** 14 years old → چرا؟ «I» همیشه am.\nMy school **is** beautiful → چرا؟ my school یک چیز مفرد = he/she/it خانواده → is.\nThere **are** 30 students → چرا؟ students جمع → are.\nMy classmates **are** clever → چرا؟ جمع → are.\nHe **is** hard-working → he → is.\nhe **isn't** nervous at all → چرا منفی؟ چون «but he ... nervous at all» منطق جمله یعنی «اصلاً عصبی نیست»؛ at all فقط در جمله‌ی منفی می‌آید و معنی «اصلاً» می‌دهد.\nچرا نکته‌ی at all مهم است؟ چون در مثبت نمی‌آید: He is nervous at all ❌."),
            StudyPack.Solution("sol3", "پاسخ تمرین ۳ — چرا جمله این‌طور مرتب می‌شود؟", "I am not nervous → چرا؟ الگوی جمله‌ی انگلیسی ثابت است: فاعل + فعل (to be) + بقیه. «not» همیشه بلافاصله بعد از to be می‌آید.\nYou and your friend are not selfish → چرا are؟ چون فاعلِ دوتایی (شما + دوستت) مثل you جمع است.\nIs Mina careless? → چرا Is اول؟ در جمله‌ی پرسشی، to be جلوی فاعل می‌پرد (Is + فاعل + صفت؟) — سواژینور (inversion).\nIs there an orange on the table? → چرا Is there؟ «there» در سوال هم مثل فاعل رفتار می‌کند: Is there...? / Are there...?\nقاعده‌ی طلایی مرتب‌کردن: ۱) فاعل را پیدا کن ۲) to be را بعدش بگذار ۳) در سوال، to be برود اول جمله."),
        ),
    )

    val packs: List<StudyPack> = listOf(l01())

    val module = BookModule(
        bookCode = "C910", title = "زبان انگلیسی پایه نهم", subject = "", packs = packs,
    )
}
