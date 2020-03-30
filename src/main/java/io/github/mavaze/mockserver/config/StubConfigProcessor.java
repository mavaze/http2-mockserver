package io.github.mavaze.mockserver.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLParser;
import io.github.mavaze.mockserver.utils.ApplicationUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES;
import static java.util.stream.Collectors.toList;

@Slf4j
public class StubConfigProcessor {

    private static final ObjectMapper mapper = new ObjectMapper(new YAMLFactory()).configure(FAIL_ON_UNKNOWN_PROPERTIES, false);

    public static List<StubConfig> read(final Set<String> input) {
        return input.stream().map(StubConfigProcessor::read).flatMap(List::stream).collect(toList());
    }

	public static List<StubConfig> read(final String configFile) {
		try (InputStream configStream = ApplicationUtils.getResourceAsStream(configFile)) {
			YAMLParser parser = new YAMLFactory().createParser(configStream);
			if (configStream != null) {
				return readByJackson2_10(parser);
			}
		} catch (Exception e) {
			log.error("Failed to read config file {}", configFile, e);
		}
		return Collections.emptyList();
	}

//    private static List<StubConfig> readByJackson2_9(final YAMLParser parser) throws IOException {
//        return mapper.<StubConfig>readValues(parser, new TypeReference<StubConfig>() {}).readAll();
//    }

    private static List<StubConfig> readByJackson2_10(final YAMLParser parser) throws IOException {
        final Iterator<StubConfig> stubConfigIterator = mapper.readValues(parser, new TypeReference<StubConfig>() {});
        final List<StubConfig> stubConfigs = new ArrayList<>();
        while (stubConfigIterator.hasNext()) {
            stubConfigs.add(stubConfigIterator.next());
        }
        return stubConfigs;
    }
}
