package io.github.mavaze.mockserver.config;

import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@NoArgsConstructor(access = PRIVATE)
public final class Constants {

    public static final String HOSTNAME = "hostname";

    public static final String STUBS_CONFIG_FOLDER = "stubs";
    public static final String RESPONSES_FOLDER = "responses";
}
