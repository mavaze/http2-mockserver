package io.github.mavaze.mockserver;

import io.github.mavaze.mockserver.config.StubConfig;
import io.github.mavaze.mockserver.config.StubConfigProcessor;
import io.github.mavaze.mockserver.utils.ApplicationUtils;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class Application {

    public static void main(String[] args) {

        final Map<Command, List<String>> arguments = readArguments(args);

        final List<MockHttp2Server> listeners = new ArrayList<>();
        Collection<String> input = arguments.get(Command.INPUT);
        if (ApplicationUtils.isEmpty(input)) {
            throw new IllegalArgumentException("Execution is ambiguous as no stub mappings provided.");
        }
        final List<StubConfig> configs = StubConfigProcessor.read(new HashSet<>(input));
        configs.removeIf(cfg -> {
            boolean hasNoPaths = ApplicationUtils.isEmpty(cfg.getPaths());
            if (hasNoPaths) {
                log.info("Removing configuration {} as no paths configured", cfg);
            }
            return hasNoPaths;
        });
        log.info("Provided config: " + configs);
        configs.forEach(config -> {
            final MockHttp2Server server = new MockHttp2Server(config);
            server.start();
            listeners.add(server);
        });

        Runtime.getRuntime().addShutdownHook(new Thread("shutdown-hook") {
            @Override
            public void run() {
                log.info("Program exited. Stopping all server listeners ...");
                listeners.forEach(MockHttp2Server::stop);
            }
        });
    }

    private static Map<Command, List<String>> readArguments(final String[] args) {

        Command action = Command.DONE;
        final Map<Command, List<String>> argumentList = new HashMap<>();
        for (String arg : args) {
            switch (action) {
                case DONE:
                    action = readFlag(arg);
                    break;
                case INPUT:
                    List<String> input = argumentList.computeIfAbsent(Command.INPUT, k -> new ArrayList<>());
                    input.add(arg);
                    action = Command.DONE;
                    break;
            }
        }
        if (Command.DONE != action) {
            throw new IllegalArgumentException("A value is expected after a flag specified");
        }
        return argumentList;
    }

    private static Command readFlag(final String flag) {
        if (ApplicationUtils.isEmpty(flag) || !flag.trim().startsWith("-")) {
            throw new IllegalArgumentException("Invalid character(s). A valid flag is expected but found " + flag);
        }
        if ("-f".equals(flag)) {
            return Command.INPUT;
        }
        throw new IllegalArgumentException(flag + " not a supported flag");
    }

    private enum Command {
        DONE, INPUT
    }
}
