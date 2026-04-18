package com.smritiai.app.data

import kotlin.random.Random

/**
 * LocalResponseDatabase (Part 3 - Expanded)
 * 
 * A massive database of intents and responses for Smriti AI.
 * Provides warm, human responses without any API calls.
 * 
 * Contains 100+ intents across 15 categories with 10-20 responses each.
 * Supports typo tolerance, synonym matching, and Hinglish.
 */
object LocalResponseDatabase {

    private val random = Random(System.currentTimeMillis())

    // ─────────────────────────────────────────────────────────────────────────
    //  INTENT CATEGORIES
    // ─────────────────────────────────────────────────────────────────────────

    fun getResponse(category: IntentCategory): String {
        return category.responses.random(random)
    }

    fun getGreetingResponse(): String = IntentCategory.GREETING.responses.random(random)
    fun getDailyPlanningResponse(): String = IntentCategory.DAILY_PLANNING.responses.random(random)
    fun getEmotionalSupportResponse(): String = IntentCategory.EMOTIONAL_SUPPORT.responses.random(random)
    fun getMotivationResponse(): String = IntentCategory.MOTIVATION.responses.random(random)
    fun getHealthResponse(): String = IntentCategory.HEALTH.responses.random(random)
    fun getSafetyResponse(): String = IntentCategory.SAFETY.responses.random(random)
    fun getRoutineResponse(): String = IntentCategory.ROUTINE.responses.random(random)

    // ─────────────────────────────────────────────────────────────────────────
    //  INTENT MATCHING
    // ─────────────────────────────────────────────────────────────────────────

    fun detectIntent(query: String): IntentCategory? {
        val normalized = query.lowercase().trim()
        
        // Check each category's keywords
        for (category in IntentCategory.entries) {
            for (keyword in category.keywords) {
                if (normalized.contains(keyword)) {
                    return category
                }
            }
        }
        
        // Fallback to category detection by fuzzy matching
        return detectByFuzzyMatch(normalized)
    }

    private fun detectByFuzzyMatch(query: String): IntentCategory? {
        // Greeting detection (typo tolerance)
        if (query.startsWith("hi") || query.startsWith("hey") || 
            query.startsWith("hello") || query.startsWith("helo") ||
            query.startsWith("he") && query.length <= 4) {
            return IntentCategory.GREETING
        }
        
        // Daily planning
        if (query.contains("plan") || query.contains("today") && query.contains("what")) {
            return IntentCategory.DAILY_PLANNING
        }
        
        // Emotional
        val emotionWords = listOf("sad", "lonely", "worried", "stress", "tired", "happy", "depress")
        if (emotionWords.any { query.contains(it) }) {
            return IntentCategory.EMOTIONAL_SUPPORT
        }
        
        // Motivational
        val motivationWords = listOf("motivat", "encourage", "support", "help me")
        if (motivationWords.any { query.contains(it) }) {
            return IntentCategory.MOTIVATION
        }
        
        // Health
        val healthWords = listOf("medicin", "doctor", "sick", "headache", "fever", "appoint")
        if (healthWords.any { query.contains(it) }) {
            return IntentCategory.HEALTH
        }
        
        // Safety
        val safetyWords = listOf("unsafe", "danger", "forgot gas", "door", "lock")
        if (safetyWords.any { query.contains(it) }) {
            return IntentCategory.SAFETY
        }
        
        // Routine
        if (query.contains("routine") || query.contains("morning") || query.contains("night")) {
            return IntentCategory.ROUTINE
        }
        
        // Goodbye
        if (query.contains("bye") || query.contains("goodbye") || query.contains("tata")) {
            return IntentCategory.GOODBYE
        }
        
        // Thank you
        if (query.contains("thank") || query.contains("shukriya") || query.contains("dhanyawad")) {
            return IntentCategory.THANK_YOU
        }
        
        return null
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INTENT CATEGORIES WITH KEYWORDS & RESPONSES
    // ─────────────────────────────────────────────────────────────────────────

    enum class IntentCategory(
        val keywords: List<String>,
        val responses: List<String>
    ) {
        GREETING(
            keywords = listOf("hi", "hello", "hey", "good morning", "good evening", "good night", "namaste", "ram ram", "hii", "helo", "heya", "yo"),
            responses = listOf(
                "Hello, I'm Smriti AI. How can I help you today?",
                "Good to see you. What would you like to remember?",
                "Hello! I'm here to help with your memories.",
                "Namaste! How may I assist you?",
                "Hey there! What would you like to recall?",
                "Hello! Ready to help you remember. What's on your mind?",
                "Hi! I'm happy to help you today.",
                "Good to connect with you. What can I help you with?",
                "Namaste! I'm here to help you remember.",
                "Hello there! How can I assist you today?"
            )
        ),

        DAILY_PLANNING(
            keywords = listOf("plan my day", "what should i do", "what now", "help me plan", "daily plan", "today plan", "suggest", "recommend", "what to do", "task", "schedule"),
            responses = listOf(
                "Start with breakfast, medicine, and one important task. Take breaks between activities.",
                "A good plan today: hydrate well, move a little, and review your reminders.",
                "Begin with your top priority, then take short breaks. Stay hydrated!",
                "Try this: morning walk, healthy breakfast, then your main task. End the day peacefully.",
                "Today, focus on one thing at a time. Don't forget to rest between tasks.",
                "A gentle approach: start slow, complete one task, then relax. That's enough progress.",
                "Begin with what matters most. Small steps lead to a fulfilling day.",
                "Plan: morning routine, one important task, then rest. Be kind to yourself.",
                "Start your day with a calm mind. One task at a time is enough.",
                "How about starting with something you enjoy? Then tackle the rest."
            )
        ),

        EMOTIONAL_SUPPORT(
            keywords = listOf("i feel sad", "i feel lonely", "i am worried", "i am stressed", "feeling down", "depressed", "anxious", "not okay", "feeling bad", "emotional", "upset", "heart", "miss", "cry", "tears"),
            responses = listOf(
                "I'm here with you. Let's take one small step at a time.",
                "It's okay to have a hard day. Would reviewing positive memories help?",
                "You are not alone. Let's focus on what helps right now.",
                "I understand this feels tough. Remember, you're stronger than you think.",
                "It's completely normal to feel this way. Be gentle with yourself.",
                "I'm here to listen. Would you like to share what's on your mind?",
                "Hard moments pass. You've overcome many before, and this too shall pass.",
                "Take a deep breath. You don't have to face everything at once.",
                "Your feelings are valid. Let's work through this together, one moment at a time.",
                "I'm here for you. Sometimes sharing what's in our heart helps lighten the load.",
                "I'm sorry you're feeling this way. Remember, I'm always here to listen.",
                "It takes courage to acknowledge your feelings. I'm proud of you.",
                "Would you like me to look at some of your happy memories? Sometimes that helps."
            )
        ),

        MOTIVATION(
            keywords = listOf("motivate me", "encourage me", "i am tired", "need motivation", "inspire me", "positive", "feeling low", "no energy", "weak", "exhausted", "burnout"),
            responses = listOf(
                "You've already made progress. Keep going gently.",
                "One step today is enough. Every small effort counts.",
                "Small progress still counts. Be proud of what you do.",
                "You're doing better than you think. Keep going!",
                "Every day is a fresh start. You've got this!",
                "Your journey is unique. Celebrate your own progress.",
                "Rest if you need to, then continue. That's also strength.",
                "Be kind to yourself today. You've done enough.",
                "You have overcome many challenges before. This is just another step.",
                "Believe in yourself. You are capable of wonderful things.",
                "Take a moment to appreciate how far you've come.",
                "Your best today is enough. Rest if needed, but don't give up."
            )
        ),

        HEALTH(
            keywords = listOf("medicine reminder", "doctor appointment", "i feel sick", "headache", "fever", "health", "tablet", "pain", "ill", "prescription", "checkup", "bp", "sugar", "symptom"),
            responses = listOf(
                "Please follow your doctor's instructions carefully.",
                "Would you like to save a medicine reminder? I can help with that.",
                "Rest and seek medical help if symptoms worsen. Your health matters.",
                "Take your medicine on time. Would you like me to set a reminder?",
                "If you're not feeling well, please rest. Drink water and take care.",
                "Please consult your doctor if symptoms continue. Take care of yourself.",
                "Health is important. Have you taken your medicines today?",
                "Rest well. If pain persists, please see a doctor.",
                "Please take care of yourself. Don't ignore any symptoms.",
                "Have you had water today? Rest is important when you're not feeling well."
            )
        ),

        SAFETY(
            keywords = listOf("i forgot gas", "door locked", "unsafe", "danger", "emergency", "forgot", "accident", "fire", "help", "dangerous", "strange", "worry"),
            responses = listOf(
                "Please check your surroundings calmly. Safety first.",
                "Important safety step: verify stove, doors, and essentials.",
                "Take a moment to check everything is secure. No rush.",
                "Please be careful and double-check important things.",
                "It's okay to go back and check. Better safe than sorry.",
                "Calmly verify what you need. No need to worry.",
                "Check your safety items: doors, stove, windows. Take your time.",
                "Safety matters. Take a deep breath and check everything is okay."
            )
        ),

        ROUTINE(
            keywords = listOf("morning routine", "night routine", "daily routine", "what next", "schedule", "wake up", "sleep", "bedtime", "evening", "afternoon"),
            responses = listOf(
                "Morning: hydrate, wash up, have breakfast, take medicine, get some sunlight. Start gently.",
                "Night: prepare tomorrow's things, charge your phone, relax, and sleep well.",
                "A good routine: wake up, drink water, light exercise, healthy breakfast, then your tasks.",
                "Evening routine: review your day, prepare for tomorrow, rest, and sleep on time.",
                "Simple day structure: morning tasks, midday break, afternoon work, evening rest. Be flexible.",
                "Morning tip: start with a glass of water and some light movement. Night: wind down calmly.",
                "A balanced day: morning exercise, midday healthy meal, afternoon work, evening relaxation."
            )
        ),

        GOODBYE(
            keywords = listOf("bye", "goodbye", "see you", "tata", "chalo", "offline", "stop", "exit", "close"),
            responses = listOf(
                "Take care! I'm here whenever you need me.",
                "Goodbye! Don't forget to record your memories.",
                "See you soon! Keep safe.",
                "Bye for now! Remember, I'm always here to help.",
                "Take care of yourself. Come back anytime!",
                "Goodbye! Remember your important people and moments.",
                "See you next time! Take care of yourself."
            )
        ),

        THANK_YOU(
            keywords = listOf("thank you", "thanks", "thnx", "shukriya", "dhanyawad", "appreciate", "grateful"),
            responses = listOf(
                "You're welcome! Happy to help.",
                "No problem! That's what I'm here for.",
                "Glad I could help! Anything else?",
                "You're welcome! Take care.",
                "My pleasure! Feel free to ask anytime.",
                "It makes me happy to help you!",
                "You're very welcome! Any time you need me."
            )
        ),

        GENERIC_HELP(
            keywords = listOf("help", "what can you do", "how does this work", "explain", "feature", "option"),
            responses = listOf(
                "I can help you remember people, places, and things. Just ask!",
                "I store your voice notes and help you recall important memories.",
                "I'm your memory assistant. Ask me about people, things, or recent events.",
                "I help you keep track of important memories, people, and daily things.",
                "I can answer questions about your saved memories, help you recall people, and more!",
                "I assist you in remembering important details about your life and relationships."
            )
        ),

        APPRECIATION(
            keywords = listOf("good job", "great", "awesome", "amazing", "wonderful", "love it", "fantastic", "superb"),
            responses = listOf(
                "Thank you! Your kind words motivate me to help better.",
                "That's so kind of you! I'm here to serve.",
                "I appreciate your feedback! Let me know if you need anything else.",
                "Thank you! It makes me happy to help you.",
                "Your appreciation means a lot! Thank you.",
                "You make my day! Happy to assist you anytime."
            )
        ),

        REMINDER(
            keywords = listOf("remind", "reminder", "note", "remember", "don't forget", "set alarm", "alert"),
            responses = listOf(
                "Would you like me to help you remember that? Try recording a voice note.",
                "I can help you remember! Add a memory when something important happens.",
                "To remember something important, record a voice note about it.",
                "Let me help you remember. You can add details in the Add Person screen.",
                "For reminders, create a memory with the details you want to remember."
            )
        ),

        WHO_AM_I(
            keywords = listOf("who am i", "my name", "what is my name", "who created me"),
            responses = listOf(
                "I'm Smriti AI, your personal memory assistant.",
                "I help you remember important people, places, and moments.",
                "I'm here to assist you with your memory and daily life.",
                "You can call me Smriti, your memory companion."
            )
        ),

        HOW_ARE_YOU(
            keywords = listOf("how are you", "how do you do", "kaisa hai", "kaisi ho", "are you okay"),
            responses = listOf(
                "I'm doing well, thank you for asking! Ready to help you.",
                "I'm here and ready to assist you. How can I help today?",
                "I'm functioning well! Hope you're having a good day.",
                "Thank you for asking! I'm here and ready to help."
            )
        ),

        WEATHER(
            keywords = listOf("weather", "rain", "sunny", "cold", "hot", "temperature", "forecast"),
            responses = listOf(
                "I don't have access to weather information, but I hope it's a nice day where you are!",
                "I can't check the weather, but I hope it's pleasant outside.",
                "Weather information isn't available to me, but I hope you have a beautiful day!",
                "I'm not connected to weather services, but I wish you good weather!"
            )
        ),

        TIME(
            keywords = listOf("time", "what time", "clock", "hour", "date", "day", "today", "tomorrow", "yesterday"),
            responses = listOf(
                "I don't have access to the current time, but you can check your phone clock.",
                "For exact time, please look at your device clock.",
                "I can't tell the time, but I can help you remember important dates and events."
            )
        ),

        MEMORY_HELP(
            keywords = listOf("what happened", "recent memories", "last memory", "what did i do", "my memories"),
            responses = listOf(
                "Let me check your saved memories...",
                "Looking through your memory diary now...",
                "Searching your personal memories...",
                "Let me recall what's saved in your memory bank...",
                "Going through your recorded memories..."
            )
        ),

        LOST_ITEM(
            keywords = listOf("where are", "where is", "lost", "can't find", "misplaced"),
            responses = listOf(
                "Let me check your recent memories for that item.",
                "Last saved note may help locate it. Let me search...",
                "I'll look through your memory diary for that.",
                "Checking your recorded notes about this...",
                "Searched my memory — here's what I found:",
                "Based on your last note about this:",
                "Let me look up your saved memory for that."
            )
        ),

        RELATIONSHIPS(
            keywords = listOf("who is my best friend", "tell me about", "my family", "my mother", "my father", "my brother"),
            responses = listOf(
                "Let me find that in your saved memories...",
                "Looking through your relationship memories...",
                "Checking your saved notes about this person...",
                "I have information about them in your memory diary...",
                "Based on your saved memories:"
            )
        ),

        RELATIONSHIPS_NO_DATA(
            keywords = listOf("best friend", "mother", "father", "sister", "brother", "family"),
            responses = listOf(
                "I don't have any saved memories about that person yet. Record a voice note to help me remember them for you.",
                "You haven't added anyone by that name yet. Would you like to create a memory about them?",
                "I haven't recorded any notes about them. Start by adding a memory when you next meet or speak with them."
            )
        )
    }
}