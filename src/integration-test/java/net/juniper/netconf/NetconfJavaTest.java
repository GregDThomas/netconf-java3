package net.juniper.netconf;

import static java.util.Optional.ofNullable;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.io.File;
import java.time.Duration;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;

@Log4j2
class NetconfJavaTest {

    @SneakyThrows
    @ParameterizedTest
    @ArgumentsSource(AvailableDevicesProvider.class)
    void connectToDevices(final File deviceDetailsFile) {
        if (!deviceDetailsFile.isFile() || !deviceDetailsFile.canRead()) {
            log.info("Skipping file " + deviceDetailsFile);
            return;
        }
        log.info("Connecting to device detailed in file '{}'", deviceDetailsFile::getName);
        final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        final Map<String, String> deviceDetails = objectMapper.readValue(
            deviceDetailsFile, new HashMapTypeReference());

        final Device device = Device.builder()
            .address(deviceDetails.get("address"))
            .port(ofNullable(deviceDetails.get("port"))
                .map(Integer::valueOf)
                .orElse(null))
            .username(deviceDetails.get("username"))
            .password(deviceDetails.get("password"))
            .privateKey(deviceDetails.get("privateKey"))
            .connectTimeout(ofNullable(deviceDetails.get("connectTimeout"))
                .map(Duration::parse)
                .orElse(null))
            .loginTimeout(ofNullable(deviceDetails.get("loginTimeout"))
                .map(Duration::parse)
                .orElse(null))
            .readTimeout(ofNullable(deviceDetails.get("readTimeout"))
                .map(Duration::parse)
                .orElse(null))
            .build();

        try (final NetconfSession session = device.openSession()) {
            log.info("Connected!");
            assertThat(session.isConnected()).isTrue();
        }

    }


    public static class AvailableDevicesProvider implements ArgumentsProvider {

        @Override
        public Stream<? extends Arguments> provideArguments(final ExtensionContext context) {

            final File secretsDir = new File("src/integration-test/resources/secrets");
            final File[] files = secretsDir
                .listFiles((dir, name) -> name.endsWith(".yml"));
            return Arrays.stream(ofNullable(files).orElse(new File[] {secretsDir}))
                .map(Arguments::of);
        }
    }

    private static class HashMapTypeReference extends TypeReference<HashMap<String, String>> {

    }
}
