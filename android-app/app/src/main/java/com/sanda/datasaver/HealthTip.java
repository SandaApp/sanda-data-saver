package com.sanda.datasaver;

/**
 * HealthTip — Model for health reminders focused on reducing excessive screen time
 * on games and social media, and encouraging scripture, prayer, constructive activities.
 */
public class HealthTip {
    public String category;      // 🎮 Gaming, 📱 Social, 🙏 Prayer, 📖 Scripture, 🌱 Constructive
    public String title;
    public String message;
    public String scriptureRef;
    public String scriptureText;
    public String action;        // Practical constructive action
    public String emoji;

    public HealthTip(String category, String emoji, String title, String message, String scriptureRef, String scriptureText, String action) {
        this.category = category;
        this.emoji = emoji;
        this.title = title;
        this.message = message;
        this.scriptureRef = scriptureRef;
        this.scriptureText = scriptureText;
        this.action = action;
    }

    public String getFullMessage() {
        return emoji + " " + title + "\n\n" + message + "\n\n📖 " + scriptureRef + " — \"" + scriptureText + "\"\n\n✅ Action: " + action;
    }

    public String getShortMessage() {
        return emoji + " " + title + ": " + action;
    }
}
