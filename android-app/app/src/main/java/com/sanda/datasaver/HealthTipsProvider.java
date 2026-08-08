package com.sanda.datasaver;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * HealthTipsProvider — 20 health tips to distract from excessive gaming/social media
 * and focus on Scripture, prayer, constructive beneficial activities.
 * By Bishop Dr. David Sanda — Free for Jesus — Matthew 10:8
 */
public class HealthTipsProvider {

    private static final List<HealthTip> tips = new ArrayList<>();

    static {
        tips.add(new HealthTip(
                "Gaming Detox",
                "🎮",
                "Gaming Break — Rest Your Mind",
                "You've been gaming a while. Excessive gaming drains focus and steals time from God and family. Take a break!",
                "Psalm 46:10",
                "Be still, and know that I am God.",
                "Close game, stand up, stretch, pray 5 min, drink water"
        ));

        tips.add(new HealthTip(
                "Social Media Detox",
                "📱",
                "Social Media Pause — Reclaim Your Time",
                "Endless scrolling on TikTok, Facebook, Instagram wastes hours. Your mind needs rest from comparison and noise.",
                "Philippians 4:8",
                "Whatever is true, noble, pure — think about such things.",
                "Put phone face-down 20 min, read Bible chapter, call a loved one"
        ));

        tips.add(new HealthTip(
                "Prayer Focus",
                "🙏",
                "Prayer Time — Talk to Your Father",
                "Instead of another game level or reel, talk to God who loves you. Prayer restores peace.",
                "Matthew 6:6",
                "When you pray, go into your room and pray to your Father.",
                "Go quiet place, pray 5 min for family, Nigeria, your future"
        ));

        tips.add(new HealthTip(
                "Scripture Reading",
                "📖",
                "Read the Word — Feed Your Spirit",
                "Screen time feeds eyes, but Word feeds soul. 5 min Scripture changes your day.",
                "Joshua 1:8",
                "Meditate on it day and night, so you may be careful to do everything written.",
                "Open Bible to Proverbs 3, read 10 verses slowly"
        ));

        tips.add(new HealthTip(
                "Constructive Action",
                "🌱",
                "Do Something Beneficial — Build, Don't Scroll",
                "Time is gift. Use it to build, help, learn — not just consume.",
                "Colossians 3:23",
                "Whatever you do, work heartily, as for the Lord.",
                "Clean room 10 min, help someone, learn one useful skill"
        ));

        tips.add(new HealthTip(
                "Eye Care",
                "👁️",
                "Eye Rest — 20-20-20 Rule",
                "Long screen time hurts eyes. Every 20 min, look 20 feet away for 20 sec.",
                "Psalm 121:1",
                "I lift up my eyes to the hills — where does my help come from?",
                "Look out window 20 sec, blink, close eyes and thank God for sight"
        ));

        tips.add(new HealthTip(
                "Gaming Wisdom",
                "🎯",
                "Wise Time — Not Wasted Time",
                "Games are fun but don't let them control you. Be master of your time.",
                "Ephesians 5:15-16",
                "Be very careful how you live — making the most of every opportunity.",
                "Set timer 30 min for games, then switch to constructive task"
        ));

        tips.add(new HealthTip(
                "Social Comparison",
                "💭",
                "You Are Enough — No Comparison",
                "Social media makes you compare. God made you unique with purpose.",
                "Psalm 139:14",
                "I am fearfully and wonderfully made.",
                "Write 3 things you are grateful for, pray thanks"
        ));

        tips.add(new HealthTip(
                "Prayer Walk",
                "🚶",
                "Prayer Walk — Move Your Body",
                "Sitting long with phone harms body. Move and pray.",
                "1 Corinthians 6:19-20",
                "Your body is a temple of the Holy Spirit. Honor God with your body.",
                "Walk 5 min outside, pray, breathe fresh air, no phone"
        ));

        tips.add(new HealthTip(
                "Scripture Memory",
                "🧠",
                "Memory Verse — Renew Your Mind",
                "Replace game tricks memorized with Bible verses that give life.",
                "Romans 12:2",
                "Be transformed by the renewing of your mind.",
                "Memorize Philippians 4:13 today"
        ));

        tips.add(new HealthTip(
                "Family Time",
                "👨‍👩‍👧",
                "Family First — People Over Pixels",
                "Games and reels will wait, but family moments won't. Be present.",
                "Deuteronomy 6:7",
                "Talk about God's word when you sit at home and when you walk along the road.",
                "Put phone away, talk with family 15 min, no screen"
        ));

        tips.add(new HealthTip(
                "Night Rest",
                "🌙",
                "Night Rest — No Screen Before Sleep",
                "Late night gaming/social harms sleep. God gives sleep to His beloved.",
                "Psalm 127:2",
                "He gives sleep to His beloved.",
                "No screen 30 min before bed, read Psalm 4, pray, sleep early"
        ));

        tips.add(new HealthTip(
                "Addiction Break",
                "⛓️‍💥",
                "Break the Chain — You Are Free",
                "If you can't stop scrolling/gaming, Jesus can break the chain.",
                "John 8:36",
                "If the Son sets you free, you will be free indeed.",
                "Pray: 'Jesus help me control screen time', delete one distracting app for 1 day"
        ));

        tips.add(new HealthTip(
                "Constructive Learning",
                "📚",
                "Learn Something Useful — Grow",
                "Instead of another reel, learn something that helps your future.",
                "Proverbs 1:5",
                "Let the wise listen and add to their learning.",
                "Watch 10 min educational video or read help article davidsanda.com/apps/help.html"
        ));

        tips.add(new HealthTip(
                "Serve Others",
                "🤝",
                "Serve — Be a Blessing",
                "Excessive self-focused screen time makes us isolated. Serving brings joy.",
                "Acts 20:35",
                "It is more blessed to give than to receive.",
                "Do one kind act: help neighbor, message encouragement, give"
        ));

        tips.add(new HealthTip(
                "Focus Reset",
                "🧘",
                "Focus Reset — Quiet Your Mind",
                "Games and social overstimulate mind. Quietness restores focus.",
                "Isaiah 30:15",
                "In returning and rest you shall be saved; in quietness and trust shall be your strength.",
                "Sit quietly 3 min, breathe deeply, no phone, listen to God"
        ));

        tips.add(new HealthTip(
                "Purpose Check",
                "🎯",
                "Purpose — Why Are You Here?",
                "God gave you time for purpose, not just entertainment.",
                "Jeremiah 29:11",
                "For I know the plans I have for you, plans to prosper you.",
                "Write your top 3 goals for today, do most important first"
        ));

        tips.add(new HealthTip(
                "Gratitude",
                "🙌",
                "Gratitude Break — Thank God",
                "Scrolling makes us complain. Gratitude makes us joyful.",
                "1 Thessalonians 5:18",
                "Give thanks in all circumstances.",
                "List 5 blessings, thank God aloud"
        ));

        tips.add(new HealthTip(
                "Water & Health",
                "💧",
                "Drink Water — Care for Temple",
                "Long gaming/social sessions, you forget water. Your body needs water.",
                "3 John 1:2",
                "I pray that you may enjoy good health and that all may go well with you.",
                "Drink full glass water now, stretch neck and back"
        ));

        tips.add(new HealthTip(
                "Final Encouragement",
                "✝️",
                "Freely Given — Use Time for Jesus",
                "Sanda Data Saver is free, like God's grace is free. Use your saved data and time for good.",
                "Matthew 10:8",
                "Freely you have received; freely give.",
                "Share this app with friend who needs screen-time balance"
        ));
    }

    public static List<HealthTip> getAllTips() {
        return new ArrayList<>(tips);
    }

    public static HealthTip getRandomTip() {
        Random rand = new Random();
        return tips.get(rand.nextInt(tips.size()));
    }

    public static HealthTip getTip(int index) {
        return tips.get(index % tips.size());
    }

    public static int getCount() {
        return tips.size();
    }
}
