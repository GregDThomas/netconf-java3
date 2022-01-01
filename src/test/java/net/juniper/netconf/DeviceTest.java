package net.juniper.netconf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceTest {

    @Test
    void willNotLeakPasswords() {
        final Device device = Device.builder()
            .address("my-device")
            .username("my-username")
            .password("secret password")
            .build();

        final String toString = device.toString();

        assertThat(toString).doesNotContain("secret");
    }

    @Test
    void willNotLeakCertificates() {
        final Device device = Device.builder()
            .address("my-device")
            .username("my-username")
            .privateKey("secret certificate")
            .build();

        final String toString = device.toString();

        assertThat(toString).doesNotContain("secret");
    }

    @Test
    void willNotTakePasswordAndCertificate() {

        assertThatThrownBy(() -> Device.builder()
            .address("my-device")
            .username("my-username")
            .password("secret password")
            .privateKey("secret certificate")
            .build()
        )
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("A privateKey cannot be supplied with a password");

    }
}