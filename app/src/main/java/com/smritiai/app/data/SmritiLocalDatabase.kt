package com.smritiai.app.data

import kotlin.math.min
import kotlin.random.Random

/**
 * Generated from Smriti_AI_Agent_Prompt.md.
 * Local, offline intent matching with typo tolerance (Levenshtein <= 1)
 * + response rotation (no immediate repeats).
 */
object SmritiLocalDatabase {

    data class IntentData(
        val keywords: List<String>,
        val responseSeeds: List<String>,
    ) {
        val responses: List<String> by lazy { expandResponses(responseSeeds) }
    }

    private val random = Random(System.currentTimeMillis())
    private val lastResponseByIntent = mutableMapOf<String, String>()

    private val fallbackResponses = listOf(
        "I didn’t fully understand that, but I’m here with you. Could you say it a little differently?",
        "I’m not sure I caught that. Can you rephrase it for me?",
        "I want to help. Try asking in a simpler way, and I’ll do my best.",
        "I’m here. Could you tell me that again in other words?",
        "I might have missed it. Say it once more and I’ll help you."
    )

    val intents: Map<String, IntentData> = mapOf(
        "GREETING" to IntentData(
            keywords = listOf("hi", "hello", "hey", "namaste", "hii", "hiya", "good morning", "good evening", "good afternoon", "good night", "namaskar", "pranam", "hy", "hlw", "morning", "evening", "salam"),
            responseSeeds = listOf("Hello! I'm Smriti, your memory companion. How can I help you today?", "Hi there! I'm here whenever you need me.", "Namaste! I'm with you. How can I assist today?", "Good to see you. Tell me what you need.", "Hey! Ready to help. What's on your mind?"),
        ),

        "HOW_ARE_YOU" to IntentData(
            keywords = listOf("how are you", "how r u", "hru", "kaisa hai", "kaise ho", "you okay", "are you fine", "you good", "how do you do"),
            responseSeeds = listOf("I'm doing well and fully focused on helping you! How are you feeling?", "Always ready to help! More importantly — how are YOU doing today?", "I'm great, thank you for asking! How can I make your day better?", "I'm here and attentive. What about you — how's your day going?"),
        ),

        "DAILY_PLANNING" to IntentData(
            keywords = listOf("plan my day", "what should i do", "what to do today", "daily plan", "help me plan", "schedule today", "what now", "kya karu", "plan karo", "suggest me", "what next", "aaj kya karu"),
            responseSeeds = listOf("Start with water and breakfast, take your medicine, then one important task.", "A simple plan: Morning hydrate. Afternoon — one priority. Evening — rest.", "Begin with your most important task first, then take it easy.", "Good plan: Breakfast → medicine → check reminders → one activity → rest well.", "Keep today simple: eat, medicate, do one useful thing, rest."),
        ),

        "MORNING_ROUTINE" to IntentData(
            keywords = listOf("morning routine", "what to do in morning", "morning schedule", "subah kya karu", "morning habits", "wake up routine", "start my day"),
            responseSeeds = listOf("Good morning routine: Wake up → drink water → freshen up → breakfast → medicine → 10 min sunlight.", "Start with a glass of warm water, then freshen up. Breakfast and medicine come next.", "Morning checklist: Hydrate, freshen up, eat, take medicines, do a light stretch.", "A calm morning sets the whole day right. Water first, then everything else follows.", "Rise gently, hydrate, eat a good breakfast, and take your medicines on time."),
        ),

        "NIGHT_ROUTINE" to IntentData(
            keywords = listOf("night routine", "bedtime", "sleeping time", "kya karu raat ko", "goodnight routine", "before sleep", "raat ki routine", "sleep schedule"),
            responseSeeds = listOf("Before sleep: Review tomorrow's plan, charge your phone, dim lights, relax your mind.", "Night routine: Dinner early → light activity → prepare tomorrow → rest by 10pm.", "Calm your mind before bed — avoid screens, drink warm milk, sleep at a fixed time.", "Good night checklist: Medicines done? Phone charging? Doors locked? Now rest well.", "Wind down slowly — tomorrow is a new chance. Sleep peacefully tonight."),
        ),

        "MEMORY_RECALL_GENERAL" to IntentData(
            keywords = listOf("what happened", "what did i do", "remind me", "i forgot", "what was it", "yaad karo", "kya hua tha", "remind kar", "tell me what happened", "recent memories"),
            responseSeeds = listOf("Let me look through your saved memories for that.", "I'll check your recent memory records right now.", "Searching your memory logs — give me just a moment.", "Let's look back together at what you saved recently.", "I'll find it for you. Checking your memory notes now."),
        ),

        "MEMORY_RECALL_YESTERDAY" to IntentData(
            keywords = listOf("yesterday", "what happened yesterday", "kal kya hua", "kal ka din", "yesterday events", "what did i do yesterday"),
            responseSeeds = listOf("Let me pull up your memory entries from yesterday.", "Checking yesterday's logs for you right now.", "I'll find what you saved from yesterday — one moment please.", "Looking at yesterday's records. Here's what I found in your memory.", "Yesterday's notes coming up. Let me fetch those for you."),
        ),

        "LOST_ITEM_KEYS" to IntentData(
            keywords = listOf("where are my keys", "lost keys", "keys kahan hai", "i cant find keys", "missing keys", "keys gum gaye", "found keys", "key kahan rakhi"),
            responseSeeds = listOf("Let me check your last memory note about your keys.", "Think back — did you come in through the front door? Keys are often near the entrance.", "Check near the door, your bag, or where you usually sit. Let's retrace your steps.", "Your last saved location for keys might help. Let me search your notes.", "Common places: door hook, kitchen counter, bedside table, or your bag pocket.", "Don't worry — we'll find them. Where did you go last before coming home?", "I'll search your recent memory logs for any key-related notes.", "Keys are often in the last place you used them. Did you drive recently?"),
        ),

        "LOST_ITEM_PHONE" to IntentData(
            keywords = listOf("where is my phone", "lost phone", "phone kahan hai", "cant find phone", "missing phone", "phone gum gaya", "phone dhundh", "where did i put my phone"),
            responseSeeds = listOf("Your phone is in your hand! Just kidding — let me check your memory logs.", "Check near your charger, on your bed, or in the kitchen. Common spots!", "Retrace your steps — where were you last sitting or resting?", "Let me look at recent memory notes that mention your phone.", "Common locations: beside your bed, on the dining table, or in your bag.", "When did you last use it? That's the best clue to find it."),
        ),

        "LOST_ITEM_GLASSES" to IntentData(
            keywords = listOf("where are my glasses", "lost glasses", "chashma kahan hai", "cant find glasses", "missing glasses", "spectacles", "specs kahan hai"),
            responseSeeds = listOf("Glasses are often left where you last read something. Check near books or the TV.", "Let me search your memory notes for your glasses' last location.", "Common places: bedside table, dining table, bathroom counter, or top of your head!", "When did you last have them on? That's our best starting clue.", "Check near the newspaper, your favorite chair, or the kitchen."),
        ),

        "LOST_ITEM_MEDICINE" to IntentData(
            keywords = listOf("where is my medicine", "medicine kahan hai", "lost medicine", "cant find tablets", "missing tablets", "pills kahan hai", "dawai kahan hai"),
            responseSeeds = listOf("Medicines are usually kept in the kitchen cabinet, bedside drawer, or bathroom.", "Let me check your memory notes for where you store your medicines.", "Check your usual medicine box or the place you last refilled it.", "Common spots: kitchen shelf, medicine box in bedroom, or near your water bottle."),
        ),

        "LOST_ITEM_WALLET" to IntentData(
            keywords = listOf("where is my wallet", "lost wallet", "wallet kahan hai", "cant find wallet", "purse missing", "purse kahan hai"),
            responseSeeds = listOf("Wallet is often left near keys or where you last paid for something.", "Let me check your recent memory entries for wallet location.", "Check your trouser pockets, bag, or near the front door.", "When did you last use it? That helps narrow it down quickly."),
        ),

        "EMOTIONAL_SAD" to IntentData(
            keywords = listOf("i feel sad", "i am sad", "feeling low", "mujhe dukh hai", "sad hu", "dil udaas hai", "i am unhappy", "feeling bad", "bura lag raha hai", "not feeling good"),
            responseSeeds = listOf("I'm here with you. It's okay to feel sad sometimes. You're not alone.", "I hear you. Sadness visits everyone — it doesn't stay forever.", "Would you like to talk about it? Or shall we look at a happy memory together?", "It's okay to have hard days. I'm right here beside you.", "You are loved and valued. This feeling will pass. I'm with you.", "Let's take one small step at a time. Would a favorite memory help right now?", "Feelings are valid. Would you like me to play something calming or remind you of something good?", "I'm listening. You don't have to face this alone — I'm always here for you.", "Sometimes just naming how we feel helps. I'm glad you told me. How can I help?", "You've come through hard times before. You are stronger than you know."),
        ),

        "EMOTIONAL_LONELY" to IntentData(
            keywords = listOf("i feel lonely", "i am alone", "loneliness", "akela feel ho raha", "akela hoon", "no one is here", "missing someone", "bahut akela hoon"),
            responseSeeds = listOf("You are not alone — I'm always right here with you.", "Loneliness is hard. Would you like me to remind you of people who care about you?", "I'm here. Let's talk or look at some saved memories of people you love.", "You have people who love you. Want me to show you a memory of them?", "I'm your constant companion. You're never truly alone when Smriti is here.", "Let's spend some time together right now. What would make you feel better?", "I hear you. Lonely feelings are real and valid. Let me be here with you.", "Would you like to call someone from your contact list? I can help remind you who to call."),
        ),

        "EMOTIONAL_ANXIOUS" to IntentData(
            keywords = listOf("i am anxious", "i feel anxious", "tension ho raha", "nervous", "ghabhrahat", "i am nervous", "anxiety", "feeling uneasy", "restless", "bechain hoon"),
            responseSeeds = listOf("Take a slow, deep breath with me. Breathe in... hold... and breathe out slowly.", "Anxiety is tough but it passes. Let's focus on just this one moment right now.", "You are safe right now. Nothing urgent needs to happen. Just breathe.", "Let's ground you — name 5 things you can see around you right now.", "It's okay to feel this way. One slow breath at a time — that's all we need.", "You've handled hard moments before. This one will pass too.", "Let me help you calm down. Focus on your breathing — in through nose, out through mouth."),
        ),

        "EMOTIONAL_ANGRY" to IntentData(
            keywords = listOf("i am angry", "i feel angry", "gussa aa raha", "gussa hai", "i am frustrated", "irritated", "bohot gussa", "furious", "mad at someone"),
            responseSeeds = listOf("It's okay to feel angry. Take a moment — your feelings are valid.", "Before acting on anger, take 3 deep breaths. Let's pause together.", "Anger often signals something important. Would you like to talk about what happened?", "Let it out safely. Take a walk, breathe deeply, or tell me what's wrong.", "You're allowed to feel this. Let's work through it calmly, one step at a time."),
        ),

        "EMOTIONAL_WORRIED" to IntentData(
            keywords = listOf("i am worried", "i feel worried", "tension hai", "chinta ho rahi", "chinta", "worried about", "scared", "dar lag raha", "i am scared", "fearful"),
            responseSeeds = listOf("I hear your worry. Let's break it into small pieces — what's the biggest concern right now?", "Worry often makes things seem bigger than they are. Let's look at this together.", "You are safe. Let's address what's worrying you one step at a time.", "It's okay to worry. But let's focus on what you can control right now.", "Tell me what's troubling you — sharing it with me often helps lighten the load."),
        ),

        "EMOTIONAL_HAPPY" to IntentData(
            keywords = listOf("i am happy", "i feel great", "feeling good", "khush hoon", "bahut khushi", "wonderful", "excellent", "amazing day", "best day", "great mood"),
            responseSeeds = listOf("That's wonderful! I'm so glad you're feeling happy today!", "Your happiness makes me happy too! What's going on?", "That's the best thing I've heard today! Tell me more — what made you happy?", "Wonderful! Let's save this moment as a happy memory. Shall I?", "Keep this energy going! You deserve every bit of this happiness.", "That's beautiful to hear. Happy moments are precious — let's remember this one."),
        ),

        "MOTIVATION" to IntentData(
            keywords = listOf("motivate me", "encourage me", "i am tired", "i give up", "i cant do this", "motivate kar", "himmat de", "nahi ho raha", "feel like giving up", "i feel weak", "boost me"),
            responseSeeds = listOf("You've already made progress just by being here. Keep going gently.", "One small step today is still progress. You are doing better than you think.", "Even on tired days, showing up matters. You showed up. That counts.", "You are stronger than your hardest moment. I believe in you completely.", "It's okay to rest — but don't quit. Come back when you're ready. I'll be here.", "Every big journey is just small steps taken daily. Today's step counts.", "You've gotten through 100% of your hard days so far. Today is no different.", "Be gentle with yourself. Progress isn't always visible, but it's happening.", "Rest if you must, but never give up. Tomorrow brings new strength.", "I see your effort. It may not feel like much, but it matters enormously.", "Small progress is still progress. Celebrate every tiny win today.", "You don't have to be perfect. You just have to keep going.", "This too shall pass. Right now, just take the next single small step.", "You are not behind. You are exactly where you need to be.", "Hard days test us — and you're passing just by facing it."),
        ),

        "MEDICINE_REMINDER" to IntentData(
            keywords = listOf("medicine reminder", "did i take medicine", "medicine li kya", "tablet lena hai", "medicine time", "dawai yaad dilao", "pill reminder", "medicines", "tablet reminder", "kya maine dawai li"),
            responseSeeds = listOf("Let me check your medicine schedule for today.", "Your medicine reminder is saved. Would you like me to show the schedule?", "Did you take your morning medicines? Let me verify your log.", "It's important not to miss medicines. Let me check what's due now.", "I'll pull up your medicine list. Please always take medicines as prescribed.", "Medicine time! Let me check which ones are due right now.", "Checking your medication log — one moment please.", "Please follow your doctor's prescription. Want me to show today's medicines?", "Would you like to set a daily medicine reminder right now?", "Medicine tracking is important. Let me look at your schedule."),
        ),

        "DOCTOR_APPOINTMENT" to IntentData(
            keywords = listOf("doctor appointment", "doctor se milna hai", "hospital jana hai", "doctor visit", "appointment kab hai", "clinic", "checkup", "medical appointment"),
            responseSeeds = listOf("Let me check your saved doctor appointments.", "Do you have an appointment coming up? Let me look at your calendar.", "I'll search your saved records for doctor visit dates.", "Your next appointment details should be in your memory log. Checking now.", "Always keep your doctor appointments. Want me to set a reminder?"),
        ),

        "HEALTH_NOT_FEELING_WELL" to IntentData(
            keywords = listOf("i feel sick", "not feeling well", "tabiyat kharab", "feeling unwell", "mujhe bura lag raha", "i am ill", "feeling ill", "body ache", "not well", "tasty nahi", "body pain"),
            responseSeeds = listOf("I'm sorry you're not feeling well. Please rest and drink plenty of water.", "If you feel unwell, please inform a family member or caretaker nearby.", "Rest is important. Is there someone around who can help you right now?", "Please don't ignore this — contact your doctor or a trusted person.", "Take it slow. Stay hydrated and rest. If it worsens, seek medical help.", "Your health is the most important thing. Please rest now and let someone know."),
        ),

        "HEALTH_HEADACHE" to IntentData(
            keywords = listOf("headache", "sir dard", "head pain", "migraine", "sir mein dard", "head ache", "headache hai"),
            responseSeeds = listOf("Headaches can be eased by resting in a dark quiet room and staying hydrated.", "Drink a glass of water — dehydration is often the cause of headaches.", "Rest your eyes from screens and lie down for a bit. That often helps.", "If your headache is severe or recurring, please consult your doctor.", "Try a gentle forehead massage and close your eyes for a few minutes."),
        ),

        "HEALTH_TIRED_FATIGUE" to IntentData(
            keywords = listOf("i am tired", "feeling tired", "thaka hoon", "exhausted", "bahut thak gaya", "no energy", "fatigue", "tired all the time", "no strength"),
            responseSeeds = listOf("Rest is not laziness — your body is asking for a break. Listen to it.", "Fatigue is a signal. Drink water, eat something nourishing, and rest.", "Take a short nap or sit quietly for 15 minutes. Your energy will return.", "Are you sleeping enough at night? Good sleep fixes a lot of fatigue.", "Slow down. You don't need to rush. Rest now and return when refreshed."),
        ),

        "SAFETY_GAS_STOVE" to IntentData(
            keywords = listOf("i forgot gas", "gas off kiya", "stove on hai kya", "forgot stove", "gas on hai", "gas check karo", "stove check", "geyser on hai"),
            responseSeeds = listOf("Please check your stove/gas right now — this is a safety priority.", "Go to the kitchen and verify the gas knob is turned to off position.", "Safety first: check the stove, gas, and any hot appliances immediately.", "If unsure, ask someone nearby to check. Gas safety cannot be delayed.", "Please verify this in person right now. Safety cannot wait."),
        ),

        "SAFETY_DOOR_LOCKED" to IntentData(
            keywords = listOf("door locked", "darwaza band hai", "did i lock door", "lock kiya kya", "door check", "main door", "front door", "darwaza lock hai kya"),
            responseSeeds = listOf("Please go check your door physically — it's important to be sure.", "Safety check: verify your front door is locked before resting.", "If unsure about the door, please check it now. Peace of mind is worth it.", "Door security matters. Go verify and come back — I'll be right here.", "Your safety is the priority. Please check the door and any windows too."),
        ),

        "SAFETY_GENERAL" to IntentData(
            keywords = listOf("i feel unsafe", "something is wrong", "help me", "emergency", "danger", "koi hai", "koi aa raha", "bachao", "unsafe", "scared at home"),
            responseSeeds = listOf("Your safety is the top priority. Please contact a trusted family member immediately.", "If you feel unsafe, call emergency services or a family member right now.", "Please move to a safe location and contact someone you trust immediately.", "Don't hesitate — if something feels wrong, reach out to your emergency contact.", "Your instincts matter. If you feel unsafe, act on it — call for help now."),
        ),

        "RELATIONSHIPS_WHO_IS" to IntentData(
            keywords = listOf("who is", "tell me about", "kaun hai", "mujhe batao", "who are they", "relationship with me", "mere baare mein", "family member", "friend kaun hai"),
            responseSeeds = listOf("Let me search your saved contacts and relationship notes for that person.", "I'll look through your memory records for information about them.", "Checking your saved relationship notes — one moment please.", "Your memory log may have details about this person. Let me find it.", "I'll search for saved notes about the people in your life right now."),
        ),

        "RELATIONSHIPS_FAMILY" to IntentData(
            keywords = listOf("my family", "my mother", "my father", "my son", "my daughter", "meri maa", "mere papa", "mera beta", "meri beti", "family ke baare mein", "tell me about family"),
            responseSeeds = listOf("Your family loves you deeply. Let me pull up any saved notes about them.", "Family is your greatest strength. I have some saved notes — shall I show?", "Checking your memory entries about your family members now.", "The people who love you are always with you, even when apart.", "Let me find your saved family memories and information."),
        ),

        "REMEMBER_THIS_SAVE_MEMORY" to IntentData(
            keywords = listOf("remember this", "save this", "note kar", "yaad rakh", "please remember", "save memory", "add to memory", "note down", "record this", "store this", "save it"),
            responseSeeds = listOf("Sure! What would you like me to remember? Tell me and I'll save it right away.", "Of course. Go ahead — tell me what to save and I'll record it for you.", "Ready to save. What's the memory or information you want stored?", "I'm listening. Tell me what to remember and it will be saved safely.", "Saving memories is my specialty. What would you like me to note?", "Go ahead — tell me what to save. I'll keep it safe for you.", "Absolutely. What should I remember? Speak or type and I'll store it.", "Your memory is safe with me. What shall I record?"),
        ),

        "SHOW_MY_MEMORIES" to IntentData(
            keywords = listOf("show memories", "show my notes", "what did i save", "my memories", "recall everything", "show all", "dekho meri memories", "meri yaadein", "memory list", "saved notes"),
            responseSeeds = listOf("Opening your memory vault now. Here's what you've saved recently.", "Let me display your saved memories from the most recent first.", "Your personal memory log is opening now.", "Here are your saved notes and memories — reviewing your records now.", "Fetching all your saved memories. One moment please."),
        ),

        "DELETE_MEMORY" to IntentData(
            keywords = listOf("delete memory", "remove this", "forget this", "delete note", "erase memory", "hatao ye", "remove note", "delete karo", "mita do"),
            responseSeeds = listOf("Are you sure you want to delete that memory? Once deleted it cannot be recovered.", "I can remove that memory for you. Just confirm and it will be erased.", "Deleting a memory is permanent. Shall I proceed, or would you like to review it first?", "Confirm deletion? I want to make sure before removing anything important."),
        ),

        "MORNING_GREETING_SPECIAL" to IntentData(
            keywords = listOf("good morning", "subah", "morning time", "rise and shine", "aaj ki subah", "good morning smriti"),
            responseSeeds = listOf("Good morning! A new day, a fresh start. Let's make it a good one.", "Good morning! Remember to drink water, take your medicine, and smile today.", "Rise and shine! Today is full of possibilities. What's first on your list?", "Good morning! I hope you slept well. Ready to begin the day gently?", "Good morning! Let's start with something simple and beautiful today.", "Morning! The best time to set a positive intention for the day.", "Good morning to you! How are you feeling as this new day begins?", "A brand new morning — fresh start, fresh hope. Good morning!"),
        ),

        "GOOD_NIGHT_SLEEP" to IntentData(
            keywords = listOf("good night", "sone ja raha", "goodnight", "i am going to sleep", "sleep time", "raat ko", "night smriti", "bye night", "sleeping now"),
            responseSeeds = listOf("Good night! Rest well — tomorrow is a new beginning.", "Sleep peacefully. Your memories are safe. I'll be here in the morning.", "Good night! Take your medicines if needed and rest your mind.", "Have a peaceful sleep. You deserve good rest after today.", "Good night! Doors locked, phone charged? Then rest well — you've done enough today.", "Sweet dreams. Tomorrow we continue together.", "Sleep well. Your day mattered. I'll be here when you wake.", "Good night! May you wake up refreshed and ready for a wonderful new day."),
        ),

        "WEATHER" to IntentData(
            keywords = listOf("weather today", "aaj mausam", "what is weather", "will it rain", "temperature today", "mausam kaisa hai", "hot today", "cold today", "barish hogi"),
            responseSeeds = listOf("I don't have live weather access, but you can check the weather app on your phone.", "For current weather, please check your local weather app or look outside!", "I'd suggest checking a weather app for today's forecast. Want me to remind you to check?", "I can't fetch live weather, but if it looks cloudy — carry an umbrella just in case!"),
        ),

        "TIME_AND_DATE" to IntentData(
            keywords = listOf("what time is it", "what is date", "aaj kya date hai", "time kya hua", "current time", "what day is today", "date batao", "day kya hai"),
            responseSeeds = listOf("I'll display the current time and date for you right now.", "Checking the current time — one moment.", "Here's today's date and time for your reference.", "The current time is being fetched for you now."),
        ),

        "BIRTHDAY_REMINDER" to IntentData(
            keywords = listOf("birthday", "birthday kab hai", "whose birthday", "happy birthday", "janmdin", "birthday reminder", "birthday set karo"),
            responseSeeds = listOf("Let me check your saved birthday reminders.", "I'll look through your contact notes for birthday information.", "Shall I set a birthday reminder? Tell me the name and date!", "Checking your calendar for upcoming birthdays now.", "Birthdays are important — let me help you never miss one."),
        ),

        "ANNIVERSARY_REMINDER" to IntentData(
            keywords = listOf("anniversary", "anniversary kab hai", "wedding anniversary", "marriage date", "shaadi ki anniversary", "anniversary reminder"),
            responseSeeds = listOf("Let me check your saved anniversary dates.", "I'll search your memory notes for anniversary information.", "Anniversaries are precious milestones. Want me to set a reminder?", "Checking your saved relationship milestones now."),
        ),

        "PRAYER_SPIRITUAL" to IntentData(
            keywords = listOf("prayer time", "pooja karna", "namaz time", "god", "bhagwan", "prayer reminder", "mandir", "church", "mosque", "spiritual", "aarti time", "prayer kar"),
            responseSeeds = listOf("Prayer brings peace and clarity. It's a wonderful way to start or end the day.", "Your faith is a source of strength. Take this moment for quiet reflection.", "Shall I set a daily prayer or pooja reminder for you?", "A few minutes of prayer or meditation can calm the mind beautifully.", "Your spiritual practice is important. Let me help you keep it regular."),
        ),

        "FOOD_EATING" to IntentData(
            keywords = listOf("i am hungry", "what to eat", "khana kha liya", "food reminder", "meal time", "breakfast", "lunch", "dinner", "khana", "bhook lagi", "eat something", "meal reminder"),
            responseSeeds = listOf("Eating on time is very important. What would you like to have?", "A balanced meal gives you energy and strength. Don't skip meals!", "It's meal time! Let me check if there's anything noted in your routine.", "Breakfast is most important — have you eaten this morning?", "Good nutrition is a key part of staying well. Have a nourishing meal now.", "Don't forget to eat something. Your body needs fuel to feel good."),
        ),

        "WATER_HYDRATION" to IntentData(
            keywords = listOf("drink water", "water pine ka time", "hydrate", "i forgot water", "water reminder", "pani peena", "pani pine ki yaad"),
            responseSeeds = listOf("Please drink a glass of water right now. Hydration is essential!", "Water reminder: 8 glasses a day keeps fatigue and headaches away.", "Drink some water before doing anything else. Your body will thank you.", "Hydration is important for memory and mood. Time to drink up!", "A simple rule: drink water every hour. Want me to set hourly reminders?"),
        ),

        "EXERCISE_WALK" to IntentData(
            keywords = listOf("exercise", "walk karne jao", "morning walk", "stretch", "yoga", "physical activity", "workout", "chalna", "thodi der chalo", "body movement", "gym"),
            responseSeeds = listOf("Even a 10-minute gentle walk does wonders for mood and memory.", "Light stretching in the morning keeps your body flexible and energized.", "Exercise doesn't have to be intense — a slow walk is perfectly wonderful.", "Shall I remind you for your daily walk? Consistency is the key.", "Movement is medicine. A short gentle stroll will lift your spirits."),
        ),

        "MUSIC_ENTERTAINMENT" to IntentData(
            keywords = listOf("play music", "song sunao", "music chalao", "entertainment", "kuch sunao", "play song", "mujhe kuch sunna", "gaana", "favorite song"),
            responseSeeds = listOf("Music is a beautiful companion. I can help you recall your favorite songs.", "Want me to note down your favorite songs so you can find them easily?", "Music has a powerful effect on mood and memory. What kind would you like?", "I'd love to help you find your favorite music. What do you enjoy listening to?"),
        ),

        "NEWS" to IntentData(
            keywords = listOf("news", "what is happening", "aaj ki khabar", "current events", "news sunao", "world news", "local news", "kya ho raha hai duniya mein"),
            responseSeeds = listOf("I don't have live news access, but I'd recommend your preferred news channel or app.", "For the latest news, check your preferred news app or TV channel.", "I can't fetch live news, but I can help you set a daily news-check reminder!"),
        ),

        "CONTACT_SOMEONE" to IntentData(
            keywords = listOf("call someone", "phone karna hai", "call mom", "call family", "contact", "ring someone", "call kaun", "phone lagao", "call karo", "number dhundho"),
            responseSeeds = listOf("Let me check your saved contacts for that person.", "I'll search your contact memory for that name right now.", "Who would you like to call? I'll help you find the right number.", "Your saved contacts are being searched now. Who do you need to reach?", "Family and friends are just a call away. Let me find that number for you."),
        ),

        "TELL_ME_A_STORY" to IntentData(
            keywords = listOf("tell me a story", "kahani sunao", "story", "koi kahani batao", "bedtime story", "tell story", "ek kahani"),
            responseSeeds = listOf("I'd love to! Here's a short story: A wise tortoise once said, 'Slow and steady wins not the race — it wins the peace.' He took one step each day and at the end of the year had traveled the whole world, not by rushing, but by never stopping.", "Once upon a time, a kind old woman planted a small seed every day. People asked why — she couldn't live to see full trees. She smiled and said, 'I plant for those who come after me.' That's the power of love with no expectations.", "Here's a thought: A river doesn't stop when it meets a rock. It flows around it, under it, or waits patiently until it can continue. That's wisdom — keep moving, keep flowing.", "A short tale: A man forgot things often, but every evening his granddaughter would sit with him and say 'Dadu, let me remind you of all the beautiful things from today.' And he would smile — not because he remembered everything, but because he was loved."),
        ),

        "TELL_ME_A_JOKE" to IntentData(
            keywords = listOf("joke sunao", "make me laugh", "funny", "koi joke batao", "haste haste", "entertain me", "i need to laugh", "funny kuch batao"),
            responseSeeds = listOf("Why don't scientists trust atoms? Because they make up everything! 😄", "I told my doctor I broke my arm in two places. He told me to stop going to those places!", "What do you call a fish without eyes? A fsh! 🐟", "Why is a calendar always so popular? Because it has lots of dates!", "I asked my memory why it works only sometimes. It said, 'I forgot!' 😄", "What do you call a sleeping dinosaur? A dino-snore!", "Why did the scarecrow win an award? Because he was outstanding in his field!", "Patient: Doctor I keep thinking I'm a pair of curtains! Doctor: Pull yourself together man!"),
        ),

        "COMPLIMENT_APPRECIATION" to IntentData(
            keywords = listOf("you are great", "thank you smriti", "you are the best", "good job", "well done", "you are amazing", "bahut accha", "shukriya", "dhanyawad", "nice work", "you helped me"),
            responseSeeds = listOf("Thank you so much! Your kind words mean a lot to me.", "I'm so happy I could help! That's exactly what I'm here for.", "You're very welcome! It's my pleasure to be here for you.", "That means the world to me. I'll always do my best for you.", "Thank you! You make my purpose meaningful every single day.", "I'm glad I could assist. You deserve the best support always.", "Your appreciation gives me more energy to help you better. Thank you!"),
        ),

        "CRITICISM_PROBLEM_WITH_APP" to IntentData(
            keywords = listOf("you are wrong", "you made mistake", "bad response", "not helpful", "i dont like", "app problem", "something is wrong", "fix this", "not working", "app issue", "wrong answer"),
            responseSeeds = listOf("I'm sorry if I didn't help as expected. Please tell me what went wrong.", "Thank you for telling me — I'll do better. What was the right answer?", "I apologize for the confusion. Let me try again — what do you need?", "I'm still learning. Your feedback helps me improve. What can I do better?", "I'm sorry! Tell me exactly what you need and I'll give it my best."),
        ),

        "GOODBYE_EXIT" to IntentData(
            keywords = listOf("bye", "goodbye", "alvida", "see you", "later", "take care", "chal raha", "band karo", "exit", "close app", "goodnight smriti", "byebye", "tata"),
            responseSeeds = listOf("Goodbye! I'll be right here whenever you need me. Take care!", "See you soon. Remember — Smriti is always here for you.", "Take care of yourself. Come back anytime — day or night.", "Bye for now! Stay safe, eat well, and rest when needed. I'll be here.", "Until next time! You're never alone — I'm just a tap away.", "Take good care. I'll be waiting. Come back whenever you need a hand.", "Bye! You did great today. Rest well and I'll see you soon.", "Goodbye for now. You are loved and never forgotten."),
        ),

        "WHO_AM_I_IDENTITY" to IntentData(
            keywords = listOf("who am i", "tell me about myself", "mera naam", "mere baare mein batao", "what is my name", "my profile", "my identity"),
            responseSeeds = listOf("Let me fetch your saved profile information right now.", "Your personal profile is stored in your memory. Fetching it now.", "Checking your saved identity and personal notes — one moment."),
        ),

        "WHERE_DO_I_LIVE" to IntentData(
            keywords = listOf("where do i live", "my address", "mera ghar kahan", "my home address", "where is my house", "ghar ka address"),
            responseSeeds = listOf("Your home address is saved in your profile. Let me retrieve it now.", "Checking your saved location information — one moment please.", "Your address is stored safely. Fetching it from your profile now."),
        ),

        "POSITIVE_AFFIRMATIONS" to IntentData(
            keywords = listOf("affirmation", "positive thought", "inspire me", "good thoughts", "motivational quote", "acha vichar", "suvichar", "give me strength", "positive words"),
            responseSeeds = listOf("You are enough. Right now, exactly as you are — you are enough.", "Every day you wake up is a gift. Make today meaningful, one moment at a time.", "Your mind is powerful. Your heart is kind. Your story is not over yet.", "You have survived 100% of your difficult days. You are stronger than you think.", "Progress, not perfection. One gentle step forward is all that's needed today.", "You are worthy of love, care, and peace. Don't forget that.", "The best is not behind you — some of it is still ahead. Keep going.", "You matter deeply to the people around you, even when it doesn't feel that way.", "Be kind to yourself today. You are doing better than you know.", "Small acts of courage every day create a life of extraordinary strength."),
        ),

        "BREATHING_EXERCISE" to IntentData(
            keywords = listOf("breathing exercise", "calm down", "deep breath", "help me breathe", "anxiety relief", "breathing technique", "breathe with me", "relax karo", "shant ho jao"),
            responseSeeds = listOf("Let's breathe together. Breathe IN for 4 counts... hold for 4... OUT for 6. Repeat 3 times.", "Box breathing: Inhale 4 seconds. Hold 4 seconds. Exhale 4 seconds. Hold 4 seconds. Repeat.", "Slow breath: Take a long, slow breath through your nose. Hold briefly. Release slowly through your mouth.", "Try 4-7-8 breathing: Inhale 4 counts, hold 7, exhale 8. This calms the nervous system beautifully.", "Close your eyes. Take one deep breath. Let your shoulders drop. Everything is okay right now."),
        ),

        "GROUNDING_TECHNIQUE" to IntentData(
            keywords = listOf("grounding", "help me focus", "i feel scattered", "5 senses", "anxiety grounding", "panic", "help me stay calm", "feel present", "grounded"),
            responseSeeds = listOf("5-4-3-2-1 grounding: Name 5 things you see, 4 you can touch, 3 you hear, 2 you smell, 1 you taste.", "Feel your feet on the floor. Press them down gently. You are here. You are safe.", "Look around the room and name 5 blue things. It brings you back to the present moment.", "Place both hands on your lap and feel the weight of them. Breathe slowly. You are grounded.", "Say out loud: 'I am safe. I am here. This moment is manageable.'"),
        ),

        "POSITIVE_MEMORY_RECALL" to IntentData(
            keywords = listOf("happy memory", "good memory", "show me something happy", "recall something good", "positive memories", "beautiful memory", "khushi ki yaad", "achhi yaad"),
            responseSeeds = listOf("Let me find your happiest saved memories to brighten this moment.", "Searching for your most positive and cherished memories now.", "Your joy-filled memory notes are being retrieved right now.", "Let's relive a beautiful moment together — fetching your happy memories."),
        ),

        "COGNITIVE_EXERCISE" to IntentData(
            keywords = listOf("brain exercise", "memory game", "brain game", "test my memory", "mind exercise", "cognitive", "quiz me", "mental exercise", "puzzle", "dimag ki kasrat"),
            responseSeeds = listOf("Let's do a quick memory exercise! I'll say 3 words: Apple, Blue, River. Remember them. I'll ask again in a minute!", "Name game: Can you name 5 animals starting with the letter B? Take your time!", "Pattern challenge: What comes next — 2, 4, 6, 8, ___? (Answer: 10!)", "Word recall: Red, Chair, Moon. Repeat those back to me in any order!", "Quick quiz: What's 15 + 27? Take your time — mental math is great brain exercise!", "Name 3 things in your room right now. Then name 3 more. Great for presence and focus!"),
        ),

        "SLEEP_TROUBLE" to IntentData(
            keywords = listOf("cant sleep", "nahi aa rahi neend", "insomnia", "trouble sleeping", "sleepless", "cant fall asleep", "neend nahi aa rahi", "sleep problem", "i am awake at night"),
            responseSeeds = listOf("Avoid screens and bright lights. Try slow breathing or gentle music to help sleep come.", "A warm cup of milk or chamomile tea before bed can help settle the mind.", "Try the breathing technique: breathe in 4 seconds, out 8 seconds. Repeat 10 times.", "Write down any worrying thoughts on paper — it helps empty the mind before sleep.", "Progressive muscle relaxation: tense each muscle group for 5 seconds, then release. Start from toes.", "Keep the room cool and dark. Your body sleeps better in a calm environment.", "Don't watch the clock — it increases anxiety. Trust that sleep will come."),
        ),

        "FALL_DETECTION_ACCIDENT" to IntentData(
            keywords = listOf("i fell", "i slipped", "accident hua", "gir gaya", "injury", "got hurt", "mujhe chot lagi", "pain after fall", "i fell down"),
            responseSeeds = listOf("Are you okay? If you've fallen, please stay still and call for help immediately.", "Don't try to stand up too quickly after a fall. Call out to someone nearby right now.", "Please alert a family member or caretaker immediately. Falls need immediate attention.", "Stay calm and still. Call your emergency contact or shout for help right now.", "Your safety is urgent. Please contact someone near you immediately."),
        ),

        "CONFUSION_DISORIENTED" to IntentData(
            keywords = listOf("i am confused", "where am i", "i dont know where i am", "lost", "confused", "kahan hoon", "samajh nahi aa raha", "disoriented", "i feel lost"),
            responseSeeds = listOf("Take a deep breath. You are safe. Look around slowly — do you recognize the room?", "It's okay. These moments pass. Let me help — what do you see around you right now?", "You are safe. Your name, your home, your people — they are all still there. Breathe slowly.", "Stay where you are. Take slow breaths. Can you see any familiar object nearby?", "Confusion can come and go. Please call a trusted person nearby right now for support.", "I'm here with you. Take one breath. Look at your hands. You are present and safe."),
        )
    )

    fun matchIntentOrNull(input: String): String? {
        val normalized = input.lowercase().trim()
        if (normalized.isBlank()) return null

        val tokens = normalized.split(Regex("\\s+")).filter { it.isNotBlank() }

        for ((intentId, data) in intents) {
            for (keyword in data.keywords) {
                if (containsApprox(normalized, tokens, keyword)) {
                    return pickResponse(intentId, data.responses)
                }
            }
        }
        return null
    }

    fun matchIntent(input: String): String {
        return matchIntentOrNull(input) ?: fallbackResponses.random(random)
    }

    private fun pickResponse(intentId: String, responses: List<String>): String {
        if (responses.isEmpty()) return fallbackResponses.random(random)

        val last = lastResponseByIntent[intentId]
        if (responses.size == 1) {
            val only = responses[0]
            lastResponseByIntent[intentId] = only
            return only
        }

        var choice = responses.random(random)
        var tries = 0
        while (choice == last && tries < 5) {
            choice = responses.random(random)
            tries += 1
        }
        lastResponseByIntent[intentId] = choice
        return choice
    }

    private fun containsApprox(normalized: String, tokens: List<String>, keyword: String): Boolean {
        val kw = keyword.lowercase().trim()
        if (kw.isBlank()) return false

        // Phrase match first.
        if (kw.length >= 3 && normalized.contains(kw)) return true

        // Token-level fuzzy match (Levenshtein <= 1)
        val kwTokens = kw.split(Regex("\\s+")).filter { it.isNotBlank() }
        if (kwTokens.isEmpty()) return false

        // For multi-word keyword, require all tokens to be found approximately.
        return kwTokens.all { k -> tokens.any { t -> levenshteinDistanceAtMostOne(t, k) } }
    }

    private fun levenshteinDistanceAtMostOne(a: String, b: String): Boolean {
        if (a == b) return true
        val la = a.length
        val lb = b.length
        if (kotlin.math.abs(la - lb) > 1) return false
        if (la == 0 || lb == 0) return false

        // Fast path for distance 1 with at most one edit.
        var i = 0
        var j = 0
        var edits = 0
        while (i < la && j < lb) {
            if (a[i] == b[j]) {
                i++; j++; continue
            }
            edits++
            if (edits > 1) return false
            when {
                la > lb -> i++          // deletion in a
                lb > la -> j++          // insertion in a
                else -> { i++; j++ }    // substitution
            }
        }
        // account for trailing char
        if (i < la || j < lb) edits++
        return edits <= 1
    }

    private fun expandResponses(seeds: List<String>): List<String> {
        val base = seeds.map { it.trim() }.filter { it.isNotBlank() }
        if (base.isEmpty()) return emptyList()

        val prefixes = listOf(
            "I’m here with you. ",
            "It’s okay. ",
            "No worries. ",
            "Take a slow breath. ",
            "I understand. ",
            "You’re doing well. ",
        )
        val suffixes = listOf(
            "",
            " I’m right here.",
            " We’ll handle it together.",
            " You’re not alone.",
            " One step at a time.",
        )

        val out = LinkedHashSet<String>()
        out.addAll(base)

        var p = 0
        while (out.size < 20 && p < prefixes.size * suffixes.size * base.size) {
            val seed = base[p % base.size]
            val pref = prefixes[(p / base.size) % prefixes.size]
            val suf = suffixes[(p / (base.size * prefixes.size)) % suffixes.size]
            val candidate = (pref + seed + suf).replace(Regex("\\s+"), " ").trim()
            out.add(candidate)
            p += 1
        }

        return out.toList()
    }
}
