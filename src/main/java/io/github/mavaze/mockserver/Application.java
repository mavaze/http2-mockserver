package io.github.mavaze.mockserver;

import io.github.mavaze.mockserver.config.StubConfig;
import io.github.mavaze.mockserver.config.StubConfigProcessor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@Slf4j
public class Application {

    public static void main(String[] args) {

//        String path = Application.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath();
//        System.out.println(path);

        final Map<Command, List<String>> arguments = readArguments(new String[] {"-f", "src/test/resources/input.yaml"});
//        final Map<Command, List<String>> arguments = readArguments(args);

        final List<MockHttp2Server> listeners = new ArrayList<>();
        Collection<String> input = arguments.get(Command.INPUT);
        if (input == null || input.isEmpty()) {
            throw new IllegalArgumentException("Execution is ambiguous as no stub mappings provided.");
        }
        final List<StubConfig> configs = StubConfigProcessor.read(new HashSet<>(input));
        configs.removeIf(cfg -> {
            boolean hasNoPaths = cfg.getPaths() == null || cfg.getPaths().isEmpty();
            if(hasNoPaths) {
                log.info("Removing configuration {} as no paths configured", cfg);
            }
            return hasNoPaths;
        });
        log.info("Provided config: " + configs);
        configs.forEach(config -> {
            MockHttp2Server server = new MockHttp2Server(config);
            server.start();
            listeners.add(server);
        });

        Runtime.getRuntime().addShutdownHook(new Thread("shutdown-hook") {
            @Override
            public void run() {
                log.info("Program existed. Stopping all server listeners ...");
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
        if (flag == null || flag.trim().isEmpty() || !flag.startsWith("-")) {
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
