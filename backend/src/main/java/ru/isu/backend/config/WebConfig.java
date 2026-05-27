package ru.isu.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.format.FormatterRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import ru.isu.backend.model.Difficulty;
import ru.isu.backend.model.PhraseType;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(FormatterRegistry registry) {
        registry.addConverter(new DifficultyConverter());
        registry.addConverter(new PhraseTypeConverter());
    }

    private static class DifficultyConverter implements Converter<String, Difficulty> {
        @Override
        public Difficulty convert(String source) {
            return Difficulty.fromValue(source);
        }
    }

    private static class PhraseTypeConverter implements Converter<String, PhraseType> {
        @Override
        public PhraseType convert(String source) {
            return PhraseType.fromValue(source);
        }
    }
}
