package ru.isu.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.isu.backend.model.DailyUserStats;
import ru.isu.backend.model.Deck;
import ru.isu.backend.model.DeckRating;
import ru.isu.backend.model.Difficulty;
import ru.isu.backend.model.Flashcard;
import ru.isu.backend.model.FlashcardProgress;
import ru.isu.backend.model.LearningStatus;
import ru.isu.backend.model.PhraseType;
import ru.isu.backend.model.User;
import ru.isu.backend.repository.DailyUserStatsRepository;
import ru.isu.backend.repository.DeckRatingRepository;
import ru.isu.backend.repository.DeckRepository;
import ru.isu.backend.repository.FlashcardProgressRepository;
import ru.isu.backend.repository.FlashcardRepository;
import ru.isu.backend.repository.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final DeckRepository deckRepository;
    private final FlashcardRepository flashcardRepository;
    private final FlashcardProgressRepository progressRepository;
    private final DailyUserStatsRepository statsRepository;
    private final DeckRatingRepository ratingRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(String... args) {
        migrateLegacyStatuses();
        migrateSchedulerFields();
        if (userRepository.count() > 0) {
            return;
        }

        User student = user("Aliya Sokolova", "student@flashlex.local", "password", 12, 35);
        User businessTeacher = user("Elena Morozova", "teacher@flashlex.local", "password", 18, 45);
        User examCoach = user("Language Lab", "lab@flashlex.local", "password", 20, 50);
        User travelTutor = user("Max Tutor", "max@flashlex.local", "password", 15, 40);
        User marina = user("Marina Volkova", "marina@flashlex.local", "password", 15, 35);
        userRepository.saveAll(List.of(student, businessTeacher, examCoach, travelTutor, marina));

        Deck meetings = seedDeck(
                businessTeacher,
                "Business meetings B1",
                "Фразы для созвонов, уточнений, решений и follow-up после встречи.",
                true,
                "B1",
                List.of("работа", "созвоны", "business"),
                4.8,
                126,
                new Object[][]{
                        {"kick off", "начать проект или встречу", "Let's kick off with the agenda.", PhraseType.PHRASAL_VERB, Difficulty.EASY},
                        {"follow up", "вернуться к вопросу позже", "I will follow up with the client tomorrow.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM},
                        {"on the same page", "одинаково понимать ситуацию", "Let's make sure we are on the same page.", PhraseType.IDIOM, Difficulty.MEDIUM},
                        {"action item", "задача после встречи", "Each action item has an owner.", PhraseType.COLLOCATION, Difficulty.EASY},
                        {"blocker", "препятствие", "The missing data is our main blocker.", PhraseType.WORD, Difficulty.MEDIUM},
                        {"quick sync", "короткая встреча для сверки", "We need a quick sync after lunch.", PhraseType.PHRASE, Difficulty.EASY},
                        {"align on priorities", "согласовать приоритеты", "Let's align on priorities before Friday.", PhraseType.COLLOCATION, Difficulty.HARD},
                        {"circle back", "вернуться к теме", "We can circle back to pricing later.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM},
                        {"take ownership", "взять ответственность", "She will take ownership of the launch plan.", PhraseType.COLLOCATION, Difficulty.HARD},
                        {"status update", "обновление по статусу", "Send a short status update by noon.", PhraseType.COLLOCATION, Difficulty.EASY}
                }
        );

        Deck email = seedDeck(
                businessTeacher,
                "Work emails B1-B2",
                "Деловая переписка: просьбы, согласование сроков, вежливые ответы.",
                true,
                "B2",
                List.of("работа", "письма", "business"),
                4.7,
                98,
                new Object[][]{
                        {"please find attached", "во вложении вы найдете", "Please find attached the updated invoice.", PhraseType.PHRASE, Difficulty.EASY},
                        {"as discussed", "как обсуждалось", "As discussed, we will move the deadline.", PhraseType.PHRASE, Difficulty.EASY},
                        {"at your earliest convenience", "как только вам будет удобно", "Please reply at your earliest convenience.", PhraseType.IDIOM, Difficulty.HARD},
                        {"to whom it may concern", "заинтересованным лицам", "To whom it may concern, I am writing to confirm...", PhraseType.PHRASE, Difficulty.MEDIUM},
                        {"kind reminder", "вежливое напоминание", "This is a kind reminder about the payment.", PhraseType.COLLOCATION, Difficulty.MEDIUM},
                        {"moving forward", "в дальнейшем", "Moving forward, we will share weekly reports.", PhraseType.PHRASE, Difficulty.MEDIUM},
                        {"clarify the scope", "уточнить объем работ", "Could you clarify the scope of the task?", PhraseType.COLLOCATION, Difficulty.HARD},
                        {"meet the deadline", "уложиться в срок", "We need to meet the deadline.", PhraseType.COLLOCATION, Difficulty.MEDIUM},
                        {"get back to you", "ответить позже", "I will get back to you by Monday.", PhraseType.PHRASAL_VERB, Difficulty.EASY},
                        {"pending approval", "ожидает подтверждения", "The budget is pending approval.", PhraseType.COLLOCATION, Difficulty.HARD}
                }
        );

        Deck travel = seedDeck(
                travelTutor,
                "Travel essentials A2",
                "Аэропорт, отель, дорога, просьбы и простые уточнения в путешествии.",
                true,
                "A2",
                List.of("путешествия", "аэропорт", "hotel"),
                4.6,
                143,
                new Object[][]{
                        {"boarding pass", "посадочный талон", "Please show your boarding pass at the gate.", PhraseType.PHRASE, Difficulty.EASY},
                        {"check in", "зарегистрироваться", "I would like to check in.", PhraseType.PHRASAL_VERB, Difficulty.EASY},
                        {"carry-on luggage", "ручная кладь", "Is this carry-on luggage allowed?", PhraseType.COLLOCATION, Difficulty.MEDIUM},
                        {"window seat", "место у окна", "Could I have a window seat?", PhraseType.PHRASE, Difficulty.EASY},
                        {"delayed flight", "задержанный рейс", "Our delayed flight leaves at nine.", PhraseType.COLLOCATION, Difficulty.MEDIUM},
                        {"lost property", "бюро находок", "Where is the lost property office?", PhraseType.COLLOCATION, Difficulty.MEDIUM},
                        {"single room", "одноместный номер", "I booked a single room.", PhraseType.PHRASE, Difficulty.EASY},
                        {"room service", "обслуживание номеров", "Does the hotel have room service?", PhraseType.COLLOCATION, Difficulty.EASY},
                        {"get directions", "узнать маршрут", "Can I get directions to the station?", PhraseType.PHRASE, Difficulty.MEDIUM},
                        {"return ticket", "обратный билет", "I need a return ticket to Oxford.", PhraseType.PHRASE, Difficulty.EASY}
                }
        );

        Deck exam = seedDeck(
                examCoach,
                "Exam connectors B2",
                "Связки для эссе, аргументации, устных ответов и сравнения идей.",
                true,
                "B2",
                List.of("экзамен", "эссе", "аргументация"),
                4.9,
                211,
                new Object[][]{
                        {"on the one hand", "с одной стороны", "On the one hand, online learning is flexible.", PhraseType.PHRASE, Difficulty.EASY},
                        {"to some extent", "в некоторой степени", "I agree with this to some extent.", PhraseType.PHRASE, Difficulty.MEDIUM},
                        {"it is worth noting", "стоит отметить", "It is worth noting that costs may rise.", PhraseType.PHRASE, Difficulty.MEDIUM},
                        {"by contrast", "в отличие от этого", "By contrast, the second method is cheaper.", PhraseType.PHRASE, Difficulty.MEDIUM},
                        {"a key drawback", "ключевой недостаток", "A key drawback is the lack of feedback.", PhraseType.COLLOCATION, Difficulty.HARD},
                        {"compelling evidence", "убедительные доказательства", "There is compelling evidence for this view.", PhraseType.COLLOCATION, Difficulty.HARD},
                        {"raise awareness", "повысить осведомленность", "Campaigns can raise awareness.", PhraseType.COLLOCATION, Difficulty.MEDIUM},
                        {"take into account", "принимать во внимание", "We should take cultural differences into account.", PhraseType.IDIOM, Difficulty.MEDIUM},
                        {"long-term impact", "долгосрочное влияние", "The long-term impact is unclear.", PhraseType.COLLOCATION, Difficulty.MEDIUM},
                        {"draw a conclusion", "сделать вывод", "We can draw a conclusion from the data.", PhraseType.COLLOCATION, Difficulty.EASY}
                }
        );

        Deck phrasal = seedDeck(
                student,
                "Phrasal verbs from series",
                "Личный набор фразовых глаголов из сериалов и статей.",
                false,
                "B1",
                List.of("фразовые глаголы", "повседневная речь", "series"),
                0.0,
                0,
                new Object[][]{
                        {"figure out", "разобраться", "I need to figure out how this works.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM},
                        {"put off", "отложить", "Don't put off your review session.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM},
                        {"come across", "случайно встретить", "I came across this expression yesterday.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM},
                        {"run into", "столкнуться или встретить", "I ran into an old friend.", PhraseType.PHRASAL_VERB, Difficulty.EASY},
                        {"bring up", "поднять тему", "She brought up an important point.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM},
                        {"look up to", "уважать", "Many students look up to her.", PhraseType.PHRASAL_VERB, Difficulty.HARD},
                        {"carry on", "продолжать", "Carry on with the exercise.", PhraseType.PHRASAL_VERB, Difficulty.EASY},
                        {"turn down", "отклонить", "He turned down the offer.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM},
                        {"work out", "получиться", "I hope everything works out.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM},
                        {"catch up", "нагнать", "Let's catch up after class.", PhraseType.PHRASAL_VERB, Difficulty.EASY}
                }
        );

        Deck daily = seedDeck(
                student,
                "Everyday speaking A2-B1",
                "Фразы для бытовых ситуаций, просьб, уточнений и коротких диалогов.",
                true,
                "B1",
                List.of("общение", "повседневность", "speaking"),
                4.4,
                35,
                new Object[][]{
                        {"Could you repeat that?", "Не могли бы вы повторить?", "Could you repeat that, please?", PhraseType.PHRASE, Difficulty.EASY},
                        {"I didn't catch that", "Я не расслышал", "Sorry, I didn't catch that.", PhraseType.PHRASE, Difficulty.MEDIUM},
                        {"make sure", "убедиться", "I want to make sure I understand.", PhraseType.COLLOCATION, Difficulty.EASY},
                        {"sounds good", "звучит хорошо", "Friday sounds good to me.", PhraseType.PHRASE, Difficulty.EASY},
                        {"no worries", "ничего страшного", "No worries, we can try again.", PhraseType.IDIOM, Difficulty.EASY},
                        {"hang on", "подождите", "Hang on, I need a second.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM},
                        {"I see your point", "я понимаю вашу мысль", "I see your point, but I disagree.", PhraseType.PHRASE, Difficulty.MEDIUM},
                        {"let me know", "дайте знать", "Let me know when you are ready.", PhraseType.PHRASE, Difficulty.EASY},
                        {"get used to", "привыкнуть", "It takes time to get used to it.", PhraseType.PHRASE, Difficulty.HARD},
                        {"take your time", "не торопитесь", "Take your time with the answer.", PhraseType.IDIOM, Difficulty.EASY}
                }
        );

        Deck tech = seedDeck(
                examCoach,
                "Tech product vocabulary B2",
                "Лексика для IT-продуктов, интерфейсов, ошибок и пользовательских сценариев.",
                true,
                "B2",
                List.of("it", "product", "ux"),
                4.7,
                87,
                new Object[][]{
                        {"user flow", "пользовательский сценарий", "This user flow has too many steps.", PhraseType.COLLOCATION, Difficulty.MEDIUM},
                        {"edge case", "пограничный случай", "We forgot to test this edge case.", PhraseType.COLLOCATION, Difficulty.HARD},
                        {"roll out", "выпускать постепенно", "We will roll out the feature next week.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM},
                        {"bug report", "отчет об ошибке", "The bug report includes screenshots.", PhraseType.COLLOCATION, Difficulty.EASY},
                        {"load time", "время загрузки", "The load time is too high.", PhraseType.COLLOCATION, Difficulty.EASY},
                        {"break down", "разбить на части", "Let's break down the task.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM},
                        {"fallback state", "резервное состояние", "The fallback state appears offline.", PhraseType.COLLOCATION, Difficulty.HARD},
                        {"release notes", "заметки к релизу", "Update the release notes before launch.", PhraseType.COLLOCATION, Difficulty.EASY},
                        {"click-through rate", "показатель переходов", "The click-through rate improved.", PhraseType.COLLOCATION, Difficulty.HARD},
                        {"user feedback", "обратная связь пользователей", "User feedback changed our priorities.", PhraseType.COLLOCATION, Difficulty.MEDIUM}
                }
        );

        Deck medicine = seedDeck(
                marina,
                "Health and appointments B1",
                "Запись к врачу, симптомы, рекомендации и аптечная лексика.",
                true,
                "B1",
                List.of("health", "doctor", "appointments"),
                4.5,
                64,
                new Object[][]{
                        {"make an appointment", "записаться на прием", "I need to make an appointment.", PhraseType.COLLOCATION, Difficulty.EASY},
                        {"sore throat", "больное горло", "I have a sore throat.", PhraseType.COLLOCATION, Difficulty.EASY},
                        {"side effect", "побочный эффект", "Drowsiness is a common side effect.", PhraseType.COLLOCATION, Difficulty.MEDIUM},
                        {"prescription", "рецепт", "The doctor gave me a prescription.", PhraseType.WORD, Difficulty.MEDIUM},
                        {"over the counter", "без рецепта", "This medicine is available over the counter.", PhraseType.IDIOM, Difficulty.HARD},
                        {"blood pressure", "давление", "The nurse checked my blood pressure.", PhraseType.COLLOCATION, Difficulty.MEDIUM},
                        {"feel dizzy", "чувствовать головокружение", "I feel dizzy after standing up.", PhraseType.COLLOCATION, Difficulty.MEDIUM},
                        {"take a dose", "принять дозу", "Take a dose after meals.", PhraseType.COLLOCATION, Difficulty.EASY},
                        {"recover from", "восстановиться после", "It took a week to recover from the flu.", PhraseType.PHRASAL_VERB, Difficulty.MEDIUM},
                        {"medical record", "медицинская карта", "Your medical record is updated.", PhraseType.COLLOCATION, Difficulty.HARD}
                }
        );

        addStats(student, 18, 7, 15, 248, 9);
        addStats(businessTeacher, 12, 4, 10, 168, 14);
        addStats(examCoach, 24, 9, 22, 342, 21);
        addStats(travelTutor, 15, 6, 12, 207, 6);
        addStats(marina, 20, 8, 17, 279, 11);

        rate(student, meetings, 5);
        rate(marina, meetings, 5);
        rate(student, email, 5);
        rate(travelTutor, exam, 5);
        rate(student, travel, 4);
        rate(businessTeacher, daily, 4);
        rate(student, tech, 5);
        rate(examCoach, medicine, 4);

        addProgress(student, phrasal, 0, LearningStatus.LEARNING, 10, 2.2, 2, 2);
        addProgress(student, phrasal, 1, LearningStatus.LEARNING, 30, 2.1, 1, 3);
        addProgress(student, phrasal, 2, LearningStatus.LEARNING, 0, 2.4, 4, 1);
        addProgress(student, daily, 0, LearningStatus.REVIEW, 7 * 24 * 60, 2.8, 9, 1);
        addProgress(student, daily, 1, LearningStatus.REVIEW, 24 * 60, 2.3, 3, 1);
    }

    private User user(String name, String email, String password, int dailyNewLimit, int dailyReviewLimit) {
        User user = new User();
        user.setName(name);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(password));
        user.setDailyNewLimit(dailyNewLimit);
        user.setDailyReviewLimit(dailyReviewLimit);
        return user;
    }

    private Deck seedDeck(
            User author,
            String name,
            String description,
            boolean published,
            String level,
            List<String> tags,
            double rating,
            int clones,
            Object[][] cards
    ) {
        Deck deck = new Deck();
        deck.setAuthor(author);
        deck.setName(name);
        deck.setDescription(description);
        deck.setPublished(published);
        deck.setLevel(level);
        deck.getTags().addAll(tags);
        deck.setRating(rating);
        deck.setRatingsCount(rating == 0 ? 0 : 2);
        deck.setClonesCount(clones);
        Deck saved = deckRepository.save(deck);

        for (Object[] item : cards) {
            Flashcard card = new Flashcard();
            card.setDeck(saved);
            card.setEnglishWord((String) item[0]);
            card.setTranslation((String) item[1]);
            card.setExampleSentence((String) item[2]);
            card.setPhraseType((PhraseType) item[3]);
            card.setDifficulty((Difficulty) item[4]);
            flashcardRepository.save(card);
        }

        return saved;
    }

    private void addStats(User user, int reviewed, int learned, int correct, int points, int streakDays) {
        DailyUserStats stats = new DailyUserStats();
        stats.setUser(user);
        stats.setDate(LocalDate.now());
        stats.setReviewed(reviewed);
        stats.setLearned(learned);
        stats.setCorrect(correct);
        stats.setPoints(points);
        stats.setStreakDays(streakDays);
        statsRepository.save(stats);
    }

    private void rate(User user, Deck deck, int value) {
        DeckRating rating = new DeckRating();
        rating.setUser(user);
        rating.setDeck(deck);
        rating.setValue(value);
        ratingRepository.save(rating);
    }

    private void addProgress(
            User user,
            Deck deck,
            int cardIndex,
            LearningStatus status,
            int intervalMinutes,
            double easeFactor,
            int correct,
            int wrong
    ) {
        List<Flashcard> cards = flashcardRepository.findByDeckIdOrderByIdAsc(deck.getId());
        FlashcardProgress progress = new FlashcardProgress();
        progress.setUser(user);
        progress.setFlashcard(cards.get(cardIndex));
        progress.setStatus(statusForInterval(status, intervalMinutes));
        progress.setIntervalMinutes(intervalMinutes);
        progress.setIntervalSeconds(intervalMinutes * 60L);
        progress.setIntervalDays(intervalMinutes / (24 * 60));
        progress.setEaseFactor(easeFactor);
        progress.setNextReviewAt(LocalDate.now().atStartOfDay());
        progress.setNextReviewDate(LocalDate.now());
        progress.setCorrectAnswers(correct);
        progress.setWrongAnswers(wrong);
        progress.setRemainingSteps(progress.getStatus() == LearningStatus.LEARNING ? 1 : 0);
        progress.setLastReviewedAt(LocalDate.now().minusDays(Math.max(1, progress.getIntervalDays())).atStartOfDay());
        progressRepository.save(progress);
    }

    private LearningStatus statusForInterval(LearningStatus status, int intervalMinutes) {
        if (status == LearningStatus.NEW) {
            return LearningStatus.NEW;
        }
        return intervalMinutes >= 24 * 60 ? LearningStatus.REVIEW : LearningStatus.LEARNING;
    }

    private void migrateSchedulerFields() {
        List<FlashcardProgress> rows = progressRepository.findAll();
        if (rows.isEmpty()) {
            return;
        }

        rows.forEach(progress -> {
            int intervalDays = progress.getIntervalDays() == null ? 0 : progress.getIntervalDays();
            int intervalMinutes = progress.getIntervalMinutes() == null ? intervalDays * 24 * 60 : progress.getIntervalMinutes();

            if (progress.getStatus() != LearningStatus.NEW) {
                progress.setStatus(statusForInterval(progress.getStatus(), intervalMinutes));
            }
            if (progress.getIntervalMinutes() == null) {
                progress.setIntervalMinutes(intervalMinutes);
            }
            if (intervalMinutes < 24 * 60 && progress.getIntervalDays() != null && progress.getIntervalDays() > 0) {
                progress.setIntervalDays(0);
                intervalDays = 0;
            }
            if (progress.getIntervalSeconds() == null || progress.getIntervalSeconds() == 0) {
                progress.setIntervalSeconds(intervalMinutes * 60L);
            }
            if (progress.getRemainingSteps() == null) {
                progress.setRemainingSteps(progress.getStatus() == LearningStatus.LEARNING ? 1 : 0);
            }
            if (
                    progress.getStatus() == LearningStatus.LEARNING
                            && intervalDays == 0
                            && intervalMinutes >= 10
            ) {
                progress.setRemainingSteps(1);
            }
            if (progress.getLapseCount() == null) {
                progress.setLapseCount(0);
            }
            if (progress.getLeeched() == null) {
                progress.setLeeched(false);
            }
            if (progress.getNextReviewAt() == null && progress.getNextReviewDate() != null) {
                progress.setNextReviewAt(progress.getNextReviewDate().atStartOfDay());
            }
            if (progress.getNextReviewDate() == null && progress.getNextReviewAt() != null) {
                progress.setNextReviewDate(progress.getNextReviewAt().toLocalDate());
            }
            if (progress.getLastReviewedAt() == null) {
                LocalDateTime nextReviewAt = progress.getNextReviewAt() == null
                        ? LocalDate.now().atStartOfDay()
                        : progress.getNextReviewAt();
                progress.setLastReviewedAt(nextReviewAt.minusMinutes(Math.max(intervalMinutes, 1)));
            }
        });
        progressRepository.saveAll(rows);
    }

    private void migrateLegacyStatuses() {
        try {
            jdbcTemplate.update("update flashcard_progress set status = 'REVIEW' where status = 'GRADUATED'");
            jdbcTemplate.update("update flashcard_progress set status = 'LEARNING' where status = 'RELEARNING'");
        } catch (DataAccessException ignored) {
            // The table may not exist yet on a fresh database before Hibernate creates it.
        }
    }
}
