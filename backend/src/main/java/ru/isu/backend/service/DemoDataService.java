package ru.isu.backend.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.isu.backend.model.AnswerQuality;
import ru.isu.backend.model.DailyUserStats;
import ru.isu.backend.model.Deck;
import ru.isu.backend.model.DeckRating;
import ru.isu.backend.model.Difficulty;
import ru.isu.backend.model.Flashcard;
import ru.isu.backend.model.FlashcardProgress;
import ru.isu.backend.model.LearningStatus;
import ru.isu.backend.model.PhraseType;
import ru.isu.backend.model.User;
import ru.isu.backend.model.UserRole;
import ru.isu.backend.repository.DailyUserStatsRepository;
import ru.isu.backend.repository.DeckRatingRepository;
import ru.isu.backend.repository.DeckRepository;
import ru.isu.backend.repository.FlashcardProgressRepository;
import ru.isu.backend.repository.FlashcardRepository;
import ru.isu.backend.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DemoDataService {

    private static final String PASSWORD = "demo12345";

    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final FlashcardRepository flashcardRepository;
    private final FlashcardProgressRepository progressRepository;
    private final DeckRatingRepository ratingRepository;
    private final DailyUserStatsRepository statsRepository;
    private final PasswordEncoder passwordEncoder;

    @PersistenceContext
    private EntityManager entityManager;

    @Transactional
    public void rebuild() {
        clearDatabase();

        LocalDate today = LocalDate.now();
        LocalDateTime now = LocalDateTime.now();
        String passwordHash = passwordEncoder.encode(PASSWORD);

        User admin = user("Администратор", "admin@flashlex.test", UserRole.ADMIN, 8, 25, now.minusDays(60), passwordHash);
        User demo = user("Алексей Демин", "student@flashlex.test", UserRole.STUDENT, 8, 24, now.minusDays(42), passwordHash);
        User elena = user("Елена Волкова", "elena@flashlex.test", UserRole.STUDENT, 10, 28, now.minusDays(38), passwordHash);
        User mark = user("Марк Соколов", "mark@flashlex.test", UserRole.STUDENT, 7, 20, now.minusDays(31), passwordHash);
        User sofia = user("София Орлова", "sofia@flashlex.test", UserRole.STUDENT, 12, 30, now.minusDays(24), passwordHash);
        User nikita = user("Никита Романов", "nikita@flashlex.test", UserRole.STUDENT, 6, 18, now.minusDays(18), passwordHash);
        User daria = user("Дарья Ким", "daria@flashlex.test", UserRole.STUDENT, 9, 22, now.minusDays(14), passwordHash);
        daria.setPublicationBanned(true);

        List<User> users = userRepository.saveAll(List.of(admin, demo, elena, mark, sofia, nikita, daria));
        admin = users.get(0);
        demo = users.get(1);
        elena = users.get(2);
        mark = users.get(3);
        sofia = users.get(4);
        nikita = users.get(5);
        daria = users.get(6);

        Map<String, DeckCards> decks = new HashMap<>();
        decks.put("Core Review Queue", deck(demo, "Core Review Queue", "Личный набор для демонстрации очереди: новые слова, learning и повторения 3-4 итерации.", "B1", List.of("review", "demo", "spaced-repetition"), false, now.minusDays(9), coreReviewCards()));
        decks.put("New Words for Today", deck(demo, "New Words for Today", "Новые слова на сегодня без прогресса, чтобы показать лимит дневной цели.", "A2", List.of("daily-goal", "starter"), false, now.minusDays(5), newWordsCards()));
        decks.put("Travel Survival English", deck(elena, "Travel Survival English", "Аэропорт, отель, билеты и базовые фразы в поездке.", "A2", List.of("travel", "airport", "hotel"), true, now.minusDays(30), travelCards()));
        decks.put("Business Meetings Toolkit", deck(mark, "Business Meetings Toolkit", "Лексика для созвонов, задач, сроков и договоренностей.", "B1", List.of("business", "meetings", "work"), true, now.minusDays(27), businessCards()));
        decks.put("Academic Collocations B2", deck(sofia, "Academic Collocations B2", "Устойчивые академические сочетания для эссе и докладов.", "B2", List.of("academic", "collocations", "writing"), true, now.minusDays(25), academicCards()));
        decks.put("Tech Product English", deck(nikita, "Tech Product English", "Слова и фразы для разработки, релизов и поддержки продукта.", "B2", List.of("tech", "product", "software"), true, now.minusDays(22), techCards()));
        decks.put("Everyday Conversations", deck(daria, "Everyday Conversations", "Повседневные диалоги, просьбы и короткие фразы.", "A2", List.of("conversation", "everyday"), true, now.minusDays(20), everydayCards()));
        decks.put("Food and Cooking", deck(elena, "Food and Cooking", "Покупки, рецепты, способы приготовления и ресторан.", "B1", List.of("food", "cooking", "restaurant"), true, now.minusDays(18), foodCards()));
        decks.put("Health and Fitness", deck(mark, "Health and Fitness", "Запись к врачу, симптомы, тренировки и самочувствие.", "B1", List.of("health", "fitness"), true, now.minusDays(16), healthCards()));
        decks.put("Phrasal Verbs in Context", deck(sofia, "Phrasal Verbs in Context", "Фразовые глаголы с примерами в бытовых и рабочих ситуациях.", "B1", List.of("phrasal-verbs", "context"), true, now.minusDays(13), phrasalCards()));
        decks.put("Idioms for Real Speech", deck(nikita, "Idioms for Real Speech", "Идиомы, которые часто встречаются в живой речи.", "B2", List.of("idioms", "speaking"), true, now.minusDays(11), idiomCards()));
        decks.put("Finance Basics", deck(daria, "Finance Basics", "Бюджет, счета, возвраты, подписки и личные финансы.", "B1", List.of("finance", "money"), true, now.minusDays(8), financeCards()));

        DeckCards travelClone = cloneDeck(demo, decks.get("Travel Survival English"), now.minusDays(4));
        DeckCards businessClone = cloneDeck(demo, decks.get("Business Meetings Toolkit"), now.minusDays(3));
        addReviewProgress(demo, decks.get("Core Review Queue").cards(), today, now);
        addStarterProgress(demo, travelClone.cards(), today, now);
        addStarterProgress(demo, businessClone.cards(), today, now);

        addRatings(List.of(demo, mark, sofia, nikita, daria), decks.get("Travel Survival English").deck(), List.of(5, 4, 5, 4, 5));
        addRatings(List.of(demo, elena, sofia, nikita, daria), decks.get("Business Meetings Toolkit").deck(), List.of(4, 5, 4, 4, 5));
        addRatings(List.of(demo, elena, mark, nikita), decks.get("Academic Collocations B2").deck(), List.of(5, 5, 4, 5));
        addRatings(List.of(demo, elena, mark, sofia, daria), decks.get("Tech Product English").deck(), List.of(4, 4, 5, 4, 5));
        addRatings(List.of(demo, elena, mark), decks.get("Everyday Conversations").deck(), List.of(4, 4, 5));
        addRatings(List.of(demo, mark, sofia), decks.get("Food and Cooking").deck(), List.of(5, 4, 4));
        addRatings(List.of(demo, elena, sofia, daria), decks.get("Health and Fitness").deck(), List.of(4, 4, 5, 4));
        addRatings(List.of(demo, elena, mark, nikita, daria), decks.get("Phrasal Verbs in Context").deck(), List.of(5, 5, 4, 4, 5));
        addRatings(List.of(demo, elena, sofia), decks.get("Idioms for Real Speech").deck(), List.of(4, 5, 4));
        addRatings(List.of(demo, mark, nikita), decks.get("Finance Basics").deck(), List.of(4, 4, 5));

        addWeeklyStats(admin, today, new int[][]{{0, 0, 0}, {0, 0, 0}, {1, 4, 5}, {0, 0, 0}, {1, 5, 6}, {0, 0, 0}, {0, 0, 0}});
        addWeeklyStats(demo, today, new int[][]{{5, 16, 19}, {6, 18, 22}, {4, 20, 22}, {7, 22, 27}, {5, 19, 21}, {6, 21, 25}, {8, 24, 30}});
        addWeeklyStats(elena, today, new int[][]{{8, 24, 29}, {9, 27, 33}, {7, 26, 30}, {10, 30, 36}, {8, 28, 33}, {9, 31, 37}, {10, 34, 41}});
        addWeeklyStats(mark, today, new int[][]{{3, 10, 11}, {5, 14, 17}, {4, 12, 14}, {5, 16, 19}, {4, 15, 17}, {6, 17, 20}, {5, 18, 21}});
        addWeeklyStats(sofia, today, new int[][]{{7, 20, 24}, {6, 22, 25}, {8, 25, 30}, {7, 24, 29}, {9, 27, 33}, {10, 29, 35}, {9, 31, 37}});
        addWeeklyStats(nikita, today, new int[][]{{2, 8, 8}, {3, 12, 14}, {4, 14, 16}, {3, 13, 15}, {5, 16, 19}, {4, 17, 19}, {6, 20, 24}});
        addWeeklyStats(daria, today, new int[][]{{4, 14, 16}, {5, 15, 18}, {6, 18, 22}, {4, 16, 19}, {6, 20, 24}, {7, 22, 27}, {7, 23, 28}});
    }

    private void clearDatabase() {
        entityManager.createNativeQuery("set foreign_key_checks = 0").executeUpdate();
        List.of(
                "flashcard_progress",
                "daily_user_stats",
                "deck_ratings",
                "flashcards",
                "deck_tags",
                "decks",
                "users"
        ).forEach(table -> entityManager.createNativeQuery("truncate table " + table).executeUpdate());
        entityManager.createNativeQuery("set foreign_key_checks = 1").executeUpdate();
    }

    private User user(
            String name,
            String email,
            UserRole role,
            int dailyNewLimit,
            int dailyReviewLimit,
            LocalDateTime registeredAt,
            String passwordHash
    ) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setRole(role);
        user.setDailyNewLimit(dailyNewLimit);
        user.setDailyReviewLimit(dailyReviewLimit);
        user.setPublicationBanned(false);
        user.setRegisteredAt(registeredAt);
        user.setPasswordHash(passwordHash);
        return user;
    }

    private DeckCards deck(
            User author,
            String name,
            String description,
            String level,
            List<String> tags,
            boolean published,
            LocalDateTime createdAt,
            List<CardSeed> cardSeeds
    ) {
        Deck deck = new Deck();
        deck.setAuthor(author);
        deck.setName(name);
        deck.setDescription(description);
        deck.setLevel(level);
        deck.setPublished(published);
        deck.setTags(new ArrayList<>(tags));
        deck.setCreatedAt(createdAt);
        Deck savedDeck = deckRepository.save(deck);
        List<Flashcard> cards = cardSeeds.stream()
                .map(seed -> card(savedDeck, seed))
                .toList();
        List<Flashcard> savedCards = flashcardRepository.saveAll(cards);
        return new DeckCards(savedDeck, savedCards);
    }

    private DeckCards cloneDeck(User user, DeckCards source, LocalDateTime createdAt) {
        Deck sourceDeck = source.deck();
        Deck clone = new Deck();
        clone.setAuthor(user);
        clone.setSourceDeck(sourceDeck);
        clone.setName(sourceDeck.getName());
        clone.setDescription(sourceDeck.getDescription());
        clone.setLevel(sourceDeck.getLevel());
        clone.setTags(new ArrayList<>(sourceDeck.getTags()));
        clone.setPublished(false);
        clone.setCreatedAt(createdAt);
        sourceDeck.setClonesCount(sourceDeck.getClonesCount() + 1);
        Deck savedClone = deckRepository.save(clone);
        List<Flashcard> savedCards = flashcardRepository.saveAll(source.cards().stream()
                .map(card -> copyCard(card, savedClone))
                .toList());
        return new DeckCards(savedClone, savedCards);
    }

    private Flashcard card(Deck deck, CardSeed seed) {
        Flashcard card = new Flashcard();
        card.setDeck(deck);
        card.setEnglishWord(seed.englishWord());
        card.setTranslation(seed.translation());
        card.setTranscription(seed.transcription());
        card.setExampleSentence(seed.exampleSentence());
        card.setPhraseType(seed.phraseType());
        card.setDifficulty(seed.difficulty());
        return card;
    }

    private Flashcard copyCard(Flashcard source, Deck targetDeck) {
        Flashcard clone = new Flashcard();
        clone.setDeck(targetDeck);
        clone.setEnglishWord(source.getEnglishWord());
        clone.setTranslation(source.getTranslation());
        clone.setTranscription(source.getTranscription());
        clone.setExampleSentence(source.getExampleSentence());
        clone.setPhraseType(source.getPhraseType());
        clone.setDifficulty(source.getDifficulty());
        return clone;
    }

    private void addReviewProgress(User user, List<Flashcard> cards, LocalDate today, LocalDateTime now) {
        List<FlashcardProgress> progress = new ArrayList<>();
        for (int i = 0; i < cards.size(); i++) {
            if (i < 9) {
                FlashcardProgress item = progress(user, cards.get(i), LearningStatus.REVIEW);
                item.setCorrectAnswers(i % 2 == 0 ? 4 : 3);
                item.setWrongAnswers(i % 3 == 0 ? 1 : 0);
                item.setIntervalDays(i < 4 ? 4 : 7);
                item.setIntervalSeconds(item.getIntervalDays() * 86_400L);
                item.setEaseFactor(i % 3 == 0 ? 2.25 : 2.55);
                item.setNextReviewDate(today);
                item.setNextReviewAt(now.minusMinutes(90L - i * 5L));
                item.setLastReviewedAt(now.minusDays(item.getIntervalDays()));
                item.setLastAnswerQuality(i % 3 == 0 ? AnswerQuality.HARD_3 : AnswerQuality.GOOD_4);
                progress.add(item);
            } else if (i < 12) {
                FlashcardProgress item = progress(user, cards.get(i), LearningStatus.LEARNING);
                item.setCorrectAnswers(1);
                item.setWrongAnswers(i == 10 ? 1 : 0);
                item.setRemainingSteps(1);
                item.setIntervalMinutes(10);
                item.setIntervalSeconds(600L);
                item.setNextReviewDate(today);
                item.setNextReviewAt(now.minusMinutes(15L - i));
                item.setLastReviewedAt(now.minusMinutes(35L + i));
                item.setLastAnswerQuality(AnswerQuality.GOOD_4);
                progress.add(item);
            }
        }
        progressRepository.saveAll(progress);
    }

    private void addStarterProgress(User user, List<Flashcard> cards, LocalDate today, LocalDateTime now) {
        List<FlashcardProgress> progress = new ArrayList<>();
        for (int i = 0; i < Math.min(5, cards.size()); i++) {
            FlashcardProgress item = progress(user, cards.get(i), i < 3 ? LearningStatus.REVIEW : LearningStatus.LEARNING);
            item.setCorrectAnswers(i < 3 ? 3 : 1);
            item.setWrongAnswers(0);
            item.setIntervalDays(i < 3 ? 3 : 0);
            item.setIntervalSeconds(i < 3 ? 259_200L : 900L);
            item.setIntervalMinutes(i < 3 ? 0 : 15);
            item.setNextReviewDate(today);
            item.setNextReviewAt(now.minusMinutes(20L + i));
            item.setLastReviewedAt(now.minusDays(i < 3 ? 3 : 0).minusMinutes(30L));
            item.setLastAnswerQuality(AnswerQuality.GOOD_4);
            progress.add(item);
        }
        progressRepository.saveAll(progress);
    }

    private FlashcardProgress progress(User user, Flashcard card, LearningStatus status) {
        FlashcardProgress progress = new FlashcardProgress();
        progress.setUser(user);
        progress.setFlashcard(card);
        progress.setStatus(status);
        progress.setEaseFactor(2.5);
        return progress;
    }

    private void addRatings(List<User> users, Deck deck, List<Integer> values) {
        List<DeckRating> ratings = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            DeckRating rating = new DeckRating();
            rating.setUser(users.get(i));
            rating.setDeck(deck);
            rating.setValue(values.get(i));
            ratings.add(rating);
        }
        ratingRepository.saveAll(ratings);
        deck.setRatingsCount(values.size());
        deck.setRating(values.stream().mapToInt(Integer::intValue).average().orElse(0.0));
        deckRepository.save(deck);
    }

    private void addWeeklyStats(User user, LocalDate today, int[][] stats) {
        List<DailyUserStats> entries = new ArrayList<>();
        for (int i = 0; i < stats.length; i++) {
            int dayOffset = stats.length - 1 - i;
            int learned = stats[i][0];
            int reviewed = stats[i][1];
            int correct = stats[i][2];
            DailyUserStats entry = new DailyUserStats();
            entry.setUser(user);
            entry.setDate(today.minusDays(dayOffset));
            entry.setLearned(learned);
            entry.setReviewed(reviewed);
            entry.setCorrect(correct);
            entry.setStreakDays(learned + reviewed > 0 ? i + 1 : 0);
            entry.setPoints(points(user, learned, reviewed, correct, entry.getStreakDays()));
            entries.add(entry);
        }
        statsRepository.saveAll(entries);
    }

    private int points(User user, int learned, int reviewed, int correct, int streakDays) {
        int extraNew = Math.max(0, learned - user.getDailyNewLimit());
        int extraReview = Math.max(0, reviewed - user.getDailyReviewLimit());
        int goalScore = Math.min(learned, user.getDailyNewLimit()) * 10
                + Math.min(reviewed, user.getDailyReviewLimit()) * 4;
        int extraScore = Math.min(extraNew, 10) * 3 + Math.min(extraReview, 30);
        int attempts = learned + reviewed;
        int accuracy = attempts == 0 ? 0 : Math.round(correct * 100f / attempts);
        int accuracyBonus = attempts >= 10 ? Math.round((accuracy / 100f) * 30) : 0;
        int streakBonus = Math.min(streakDays, 30) * 2;
        return goalScore + extraScore + accuracyBonus + streakBonus;
    }

    private static List<CardSeed> coreReviewCards() {
        return List.of(
                c("reliable", "надежный", "[rɪˈlaɪəbl]", "A reliable source saves time during research.", PhraseType.WORD, Difficulty.MEDIUM),
                c("to figure out", "разобраться", "[ˈfɪɡər aʊt]", "I need to figure out the new schedule.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM),
                c("deadline", "срок сдачи", "[ˈdedlaɪn]", "The deadline is on Friday morning.", PhraseType.WORD, Difficulty.EASY),
                c("on the other hand", "с другой стороны", "[ɒn ði ˈʌðər hænd]", "On the other hand, the task is useful.", PhraseType.PHRASE, Difficulty.MEDIUM),
                c("significant improvement", "значительное улучшение", "[sɪɡˈnɪfɪkənt ɪmˈpruːvmənt]", "There was a significant improvement after practice.", PhraseType.COLLOCATION, Difficulty.MEDIUM),
                c("to keep track of", "отслеживать", "[kiːp træk əv]", "The app helps keep track of progress.", PhraseType.PHRASE, Difficulty.HARD),
                c("briefly", "кратко", "[ˈbriːfli]", "Could you briefly explain the rule?", PhraseType.WORD, Difficulty.EASY),
                c("to come across", "натолкнуться", "[kʌm əˈkrɒs]", "I came across this word in an article.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM),
                c("common mistake", "частая ошибка", "[ˈkɒmən mɪˈsteɪk]", "A common mistake is to translate it literally.", PhraseType.COLLOCATION, Difficulty.EASY),
                c("nevertheless", "тем не менее", "[ˌnevəðəˈles]", "Nevertheless, the result is clear.", PhraseType.WORD, Difficulty.HARD),
                c("to point out", "указать", "[pɔɪnt aʊt]", "The teacher pointed out the weak example.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM),
                c("in terms of", "с точки зрения", "[ɪn tɜːmz əv]", "In terms of grammar, the sentence is correct.", PhraseType.PHRASE, Difficulty.MEDIUM),
                c("benefit", "польза", "[ˈbenɪfɪt]", "The main benefit is faster recall.", PhraseType.WORD, Difficulty.EASY),
                c("to make progress", "продвигаться", "[meɪk ˈprəʊɡres]", "You make progress when you review regularly.", PhraseType.COLLOCATION, Difficulty.EASY),
                c("accurate", "точный", "[ˈækjərət]", "An accurate answer gets more points.", PhraseType.WORD, Difficulty.MEDIUM),
                c("to get used to", "привыкнуть", "[ɡet juːst tuː]", "You will get used to the interface quickly.", PhraseType.PHRASE, Difficulty.MEDIUM),
                c("roughly", "примерно", "[ˈrʌfli]", "It takes roughly ten minutes.", PhraseType.WORD, Difficulty.EASY),
                c("to catch up", "наверстать", "[kætʃ ʌp]", "I need to catch up on reviews.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM)
        );
    }

    private static List<CardSeed> newWordsCards() {
        return List.of(
                c("receipt", "чек", "[rɪˈsiːt]", "Keep the receipt after payment.", PhraseType.WORD, Difficulty.EASY),
                c("queue", "очередь", "[kjuː]", "There is a queue near the counter.", PhraseType.WORD, Difficulty.EASY),
                c("to refill", "пополнить", "[ˌriːˈfɪl]", "I need to refill my water bottle.", PhraseType.WORD, Difficulty.MEDIUM),
                c("available", "доступный", "[əˈveɪləbl]", "The room is available today.", PhraseType.WORD, Difficulty.EASY),
                c("to run out of", "закончиться", "[rʌn aʊt əv]", "We ran out of time.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM),
                c("at least", "по крайней мере", "[ət liːst]", "Review at least ten cards.", PhraseType.PHRASE, Difficulty.EASY),
                c("to depend on", "зависеть от", "[dɪˈpend ɒn]", "The schedule depends on your goal.", PhraseType.PHRASE, Difficulty.MEDIUM),
                c("nearby", "поблизости", "[ˌnɪəˈbaɪ]", "There is a cafe nearby.", PhraseType.WORD, Difficulty.EASY),
                c("to take notes", "делать заметки", "[teɪk nəʊts]", "Take notes during the lesson.", PhraseType.COLLOCATION, Difficulty.EASY),
                c("instead of", "вместо", "[ɪnˈsted əv]", "Use examples instead of a list.", PhraseType.PHRASE, Difficulty.MEDIUM),
                c("to look forward to", "ждать с нетерпением", "[lʊk ˈfɔːwəd tuː]", "I look forward to the next lesson.", PhraseType.PHRASAL_VERB, Difficulty.HARD),
                c("carefully", "внимательно", "[ˈkeəfəli]", "Read the question carefully.", PhraseType.WORD, Difficulty.EASY)
        );
    }

    private static List<CardSeed> travelCards() {
        return List.of(
                c("boarding pass", "посадочный талон", "[ˈbɔːdɪŋ pɑːs]", "Show your boarding pass at the gate.", PhraseType.COLLOCATION, Difficulty.EASY),
                c("carry-on luggage", "ручная кладь", "[ˈkæri ɒn ˈlʌɡɪdʒ]", "Carry-on luggage must fit above the seat.", PhraseType.COLLOCATION, Difficulty.MEDIUM),
                c("aisle seat", "место у прохода", "[aɪl siːt]", "Could I have an aisle seat?", PhraseType.COLLOCATION, Difficulty.EASY),
                c("customs", "таможня", "[ˈkʌstəmz]", "We went through customs quickly.", PhraseType.WORD, Difficulty.MEDIUM),
                c("to check in", "зарегистрироваться", "[tʃek ɪn]", "You can check in online.", PhraseType.PHRASAL_VERB, Difficulty.EASY),
                c("delayed", "задержанный", "[dɪˈleɪd]", "The flight is delayed by two hours.", PhraseType.WORD, Difficulty.EASY),
                c("reservation", "бронь", "[ˌrezəˈveɪʃn]", "I have a reservation under Demin.", PhraseType.WORD, Difficulty.MEDIUM),
                c("front desk", "стойка регистрации", "[frʌnt desk]", "Ask at the front desk.", PhraseType.COLLOCATION, Difficulty.EASY),
                c("to get around", "передвигаться по месту", "[ɡet əˈraʊnd]", "It is easy to get around by metro.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM),
                c("one-way ticket", "билет в одну сторону", "[wʌn weɪ ˈtɪkɪt]", "A one-way ticket is cheaper.", PhraseType.COLLOCATION, Difficulty.EASY)
        );
    }

    private static List<CardSeed> businessCards() {
        return List.of(
                c("agenda", "повестка", "[əˈdʒendə]", "The agenda has four items.", PhraseType.WORD, Difficulty.EASY),
                c("action item", "задача по итогам встречи", "[ˈækʃn ˈaɪtəm]", "Each action item has an owner.", PhraseType.COLLOCATION, Difficulty.MEDIUM),
                c("stakeholder", "заинтересованная сторона", "[ˈsteɪkhəʊldə]", "We invited the main stakeholder.", PhraseType.WORD, Difficulty.HARD),
                c("to follow up", "связаться после", "[ˈfɒləʊ ʌp]", "I will follow up by email.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM),
                c("to align on", "согласовать", "[əˈlaɪn ɒn]", "We need to align on priorities.", PhraseType.PHRASE, Difficulty.HARD),
                c("deadline", "срок", "[ˈdedlaɪn]", "The deadline is realistic.", PhraseType.WORD, Difficulty.EASY),
                c("take the lead", "взять инициативу", "[teɪk ðə liːd]", "Maria will take the lead.", PhraseType.PHRASE, Difficulty.MEDIUM),
                c("scope", "объем работ", "[skəʊp]", "The scope changed after the call.", PhraseType.WORD, Difficulty.MEDIUM),
                c("quick recap", "краткое резюме", "[kwɪk ˈriːkæp]", "Here is a quick recap.", PhraseType.COLLOCATION, Difficulty.EASY),
                c("to postpone", "перенести", "[pəʊstˈpəʊn]", "Can we postpone the meeting?", PhraseType.WORD, Difficulty.MEDIUM)
        );
    }

    private static List<CardSeed> academicCards() {
        return List.of(
                c("conduct research", "проводить исследование", "[kənˈdʌkt rɪˈsɜːtʃ]", "They conduct research every semester.", PhraseType.COLLOCATION, Difficulty.MEDIUM),
                c("reliable source", "надежный источник", "[rɪˈlaɪəbl sɔːs]", "Use a reliable source.", PhraseType.COLLOCATION, Difficulty.EASY),
                c("draw a conclusion", "сделать вывод", "[drɔː ə kənˈkluːʒn]", "We can draw a conclusion from the data.", PhraseType.COLLOCATION, Difficulty.MEDIUM),
                c("significant impact", "значительное влияние", "[sɪɡˈnɪfɪkənt ˈɪmpækt]", "The policy had a significant impact.", PhraseType.COLLOCATION, Difficulty.MEDIUM),
                c("in contrast", "в отличие", "[ɪn ˈkɒntrɑːst]", "In contrast, the second group improved.", PhraseType.PHRASE, Difficulty.MEDIUM),
                c("evidence", "доказательства", "[ˈevɪdəns]", "The evidence is strong.", PhraseType.WORD, Difficulty.EASY),
                c("to evaluate", "оценивать", "[ɪˈvæljueɪt]", "The task is to evaluate the results.", PhraseType.WORD, Difficulty.MEDIUM),
                c("key factor", "ключевой фактор", "[kiː ˈfæktə]", "Motivation is a key factor.", PhraseType.COLLOCATION, Difficulty.EASY),
                c("to support an argument", "поддержать аргумент", "[səˈpɔːt ən ˈɑːɡjumənt]", "Examples support an argument.", PhraseType.PHRASE, Difficulty.HARD),
                c("overall", "в целом", "[ˌəʊvərˈɔːl]", "Overall, the trend is positive.", PhraseType.WORD, Difficulty.EASY)
        );
    }

    private static List<CardSeed> techCards() {
        return List.of(
                c("deployment", "развертывание", "[dɪˈplɔɪmənt]", "Deployment starts after tests.", PhraseType.WORD, Difficulty.MEDIUM),
                c("edge case", "крайний случай", "[edʒ keɪs]", "This edge case breaks the form.", PhraseType.COLLOCATION, Difficulty.MEDIUM),
                c("backlog", "бэклог", "[ˈbæklɒɡ]", "The task is in the backlog.", PhraseType.WORD, Difficulty.EASY),
                c("rollout", "постепенный запуск", "[ˈrəʊlaʊt]", "The rollout takes one week.", PhraseType.WORD, Difficulty.MEDIUM),
                c("outage", "сбой сервиса", "[ˈaʊtɪdʒ]", "The outage lasted ten minutes.", PhraseType.WORD, Difficulty.HARD),
                c("scalable", "масштабируемый", "[ˈskeɪləbl]", "The service is scalable.", PhraseType.WORD, Difficulty.MEDIUM),
                c("to reproduce a bug", "воспроизвести ошибку", "[ˌriːprəˈdjuːs ə bʌɡ]", "Can you reproduce the bug?", PhraseType.PHRASE, Difficulty.MEDIUM),
                c("release notes", "заметки к релизу", "[rɪˈliːs nəʊts]", "Read the release notes.", PhraseType.COLLOCATION, Difficulty.EASY),
                c("fallback option", "запасной вариант", "[ˈfɔːlbæk ˈɒpʃn]", "We need a fallback option.", PhraseType.COLLOCATION, Difficulty.MEDIUM),
                c("to roll back", "откатить", "[rəʊl bæk]", "We had to roll back the release.", PhraseType.PHRASAL_VERB, Difficulty.HARD)
        );
    }

    private static List<CardSeed> everydayCards() {
        return List.of(
                c("How is it going?", "Как дела?", "[haʊ ɪz ɪt ˈɡəʊɪŋ]", "How is it going today?", PhraseType.PHRASE, Difficulty.EASY),
                c("to run errands", "делать бытовые дела", "[rʌn ˈerəndz]", "I need to run errands after work.", PhraseType.PHRASE, Difficulty.MEDIUM),
                c("neighborhood", "район", "[ˈneɪbəhʊd]", "This neighborhood is quiet.", PhraseType.WORD, Difficulty.EASY),
                c("to hang out", "проводить время", "[hæŋ aʊt]", "We hang out on weekends.", PhraseType.PHRASAL_VERB, Difficulty.EASY),
                c("Could you help me?", "Не могли бы вы помочь?", "[kʊd ju help mi]", "Could you help me with this?", PhraseType.PHRASE, Difficulty.EASY),
                c("appointment", "встреча", "[əˈpɔɪntmənt]", "I have an appointment at three.", PhraseType.WORD, Difficulty.MEDIUM),
                c("to pick up", "забрать", "[pɪk ʌp]", "I will pick up the package.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM),
                c("at the moment", "в данный момент", "[ət ðə ˈməʊmənt]", "I am busy at the moment.", PhraseType.PHRASE, Difficulty.EASY)
        );
    }

    private static List<CardSeed> foodCards() {
        return List.of(
                c("ingredients", "ингредиенты", "[ɪnˈɡriːdiənts]", "We need fresh ingredients.", PhraseType.WORD, Difficulty.EASY),
                c("to simmer", "томить на медленном огне", "[ˈsɪmə]", "Let the soup simmer.", PhraseType.WORD, Difficulty.MEDIUM),
                c("takeout", "еда навынос", "[ˈteɪkaʊt]", "Let's order takeout.", PhraseType.WORD, Difficulty.EASY),
                c("side dish", "гарнир", "[saɪd dɪʃ]", "Rice is a side dish.", PhraseType.COLLOCATION, Difficulty.EASY),
                c("to chop", "нарезать", "[tʃɒp]", "Chop the onions finely.", PhraseType.WORD, Difficulty.EASY),
                c("medium rare", "слабой прожарки", "[ˈmiːdiəm reə]", "I prefer steak medium rare.", PhraseType.PHRASE, Difficulty.MEDIUM),
                c("to book a table", "забронировать стол", "[bʊk ə ˈteɪbl]", "Can we book a table for two?", PhraseType.COLLOCATION, Difficulty.EASY),
                c("flavor", "вкус", "[ˈfleɪvə]", "The flavor is rich.", PhraseType.WORD, Difficulty.EASY)
        );
    }

    private static List<CardSeed> healthCards() {
        return List.of(
                c("symptoms", "симптомы", "[ˈsɪmptəmz]", "What symptoms do you have?", PhraseType.WORD, Difficulty.EASY),
                c("recovery", "восстановление", "[rɪˈkʌvəri]", "Recovery takes time.", PhraseType.WORD, Difficulty.MEDIUM),
                c("balanced diet", "сбалансированное питание", "[ˈbælənst ˈdaɪət]", "A balanced diet helps energy.", PhraseType.COLLOCATION, Difficulty.MEDIUM),
                c("workout", "тренировка", "[ˈwɜːkaʊt]", "The workout was short.", PhraseType.WORD, Difficulty.EASY),
                c("to warm up", "разогреться", "[wɔːm ʌp]", "Warm up before running.", PhraseType.PHRASAL_VERB, Difficulty.EASY),
                c("blood pressure", "давление", "[blʌd ˈpreʃə]", "Check your blood pressure.", PhraseType.COLLOCATION, Difficulty.MEDIUM),
                c("to make an appointment", "записаться на прием", "[meɪk ən əˈpɔɪntmənt]", "I need to make an appointment.", PhraseType.PHRASE, Difficulty.EASY),
                c("sore throat", "боль в горле", "[sɔː θrəʊt]", "I have a sore throat.", PhraseType.COLLOCATION, Difficulty.EASY)
        );
    }

    private static List<CardSeed> phrasalCards() {
        return List.of(
                c("give up", "сдаваться", "[ɡɪv ʌp]", "Do not give up after a mistake.", PhraseType.PHRASAL_VERB, Difficulty.EASY),
                c("put off", "откладывать", "[pʊt ɒf]", "Do not put off reviews.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM),
                c("look up", "искать информацию", "[lʊk ʌp]", "Look up the new word.", PhraseType.PHRASAL_VERB, Difficulty.EASY),
                c("work out", "получиться", "[wɜːk aʊt]", "The plan worked out well.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM),
                c("bring up", "поднимать тему", "[brɪŋ ʌp]", "She brought up an important point.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM),
                c("carry on", "продолжать", "[ˈkæri ɒn]", "Carry on with the exercise.", PhraseType.PHRASAL_VERB, Difficulty.EASY),
                c("turn down", "отказать", "[tɜːn daʊn]", "He turned down the offer.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM),
                c("run into", "случайно встретить", "[rʌn ˈɪntuː]", "I ran into a classmate.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM)
        );
    }

    private static List<CardSeed> idiomCards() {
        return List.of(
                c("break the ice", "разрядить обстановку", "[breɪk ði aɪs]", "A joke can break the ice.", PhraseType.IDIOM, Difficulty.MEDIUM),
                c("hit the nail on the head", "попасть в точку", "[hɪt ðə neɪl ɒn ðə hed]", "You hit the nail on the head.", PhraseType.IDIOM, Difficulty.HARD),
                c("under the weather", "неважно себя чувствовать", "[ˈʌndə ðə ˈweðə]", "I feel under the weather.", PhraseType.IDIOM, Difficulty.MEDIUM),
                c("once in a blue moon", "очень редко", "[wʌns ɪn ə bluː muːn]", "We eat there once in a blue moon.", PhraseType.IDIOM, Difficulty.HARD),
                c("on the same page", "одинаково понимать ситуацию", "[ɒn ðə seɪm peɪdʒ]", "Now we are on the same page.", PhraseType.IDIOM, Difficulty.MEDIUM),
                c("a piece of cake", "легче легкого", "[ə piːs əv keɪk]", "The test was a piece of cake.", PhraseType.IDIOM, Difficulty.EASY),
                c("cost an arm and a leg", "стоить очень дорого", "[kɒst ən ɑːm ənd ə leɡ]", "That laptop costs an arm and a leg.", PhraseType.IDIOM, Difficulty.HARD),
                c("call it a day", "закончить работу", "[kɔːl ɪt ə deɪ]", "Let's call it a day.", PhraseType.IDIOM, Difficulty.MEDIUM)
        );
    }

    private static List<CardSeed> financeCards() {
        return List.of(
                c("budget", "бюджет", "[ˈbʌdʒɪt]", "Set a monthly budget.", PhraseType.WORD, Difficulty.EASY),
                c("savings", "сбережения", "[ˈseɪvɪŋz]", "Savings help in emergencies.", PhraseType.WORD, Difficulty.EASY),
                c("invoice", "счет на оплату", "[ˈɪnvɔɪs]", "The invoice is due tomorrow.", PhraseType.WORD, Difficulty.MEDIUM),
                c("refund", "возврат денег", "[ˈriːfʌnd]", "I requested a refund.", PhraseType.WORD, Difficulty.EASY),
                c("subscription fee", "плата за подписку", "[səbˈskrɪpʃn fiː]", "The subscription fee increased.", PhraseType.COLLOCATION, Difficulty.MEDIUM),
                c("to pay off", "погасить долг", "[peɪ ɒf]", "She paid off the loan.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM),
                c("interest rate", "процентная ставка", "[ˈɪntrəst reɪt]", "The interest rate is high.", PhraseType.COLLOCATION, Difficulty.HARD),
                c("cash flow", "денежный поток", "[kæʃ fləʊ]", "Cash flow is stable.", PhraseType.COLLOCATION, Difficulty.MEDIUM)
        );
    }

    private static CardSeed c(
            String englishWord,
            String translation,
            String transcription,
            String exampleSentence,
            PhraseType phraseType,
            Difficulty difficulty
    ) {
        return new CardSeed(englishWord, translation, transcription, exampleSentence, phraseType, difficulty);
    }

    private record DeckCards(Deck deck, List<Flashcard> cards) {
    }

    private record CardSeed(
            String englishWord,
            String translation,
            String transcription,
            String exampleSentence,
            PhraseType phraseType,
            Difficulty difficulty
    ) {
    }
}
