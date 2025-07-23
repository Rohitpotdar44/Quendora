package com.RohitPotdar.myJournalApp.service;

import com.RohitPotdar.myJournalApp.entity_5.User_12;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import lombok.Builder;
import java.util.stream.Stream;

public class userArgumentsProvider_2 implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) throws Exception {
        return Stream.of(
                Arguments.of(User_12.builder().userName("Ramesh").password("Ramesh").build()),
                Arguments.of(User_12.builder().userName("Suresh").password("Suresh").build())
        );
    }
}
