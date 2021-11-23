package net.juniper.netconf;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import net.juniper.netconf.exception.NetconfException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DeviceTest {

    @Mock
    private NetconfSessionFactory netconfSessionFactory;

    @Test
    void willNotLeakSecrets() {
        final Device device = Device.builder()
            .address("my-device")
            .username("my-username")
            .password("secret password")
            .certificate("secret certificate")
            .build();

        final String toString = device.toString();

        assertThat(toString).doesNotContain("secret");
    }

    @Test
    void credentialsMustBeSupplied() {

        assertThatThrownBy(() ->
            Device.builder()
                .address("my-device")
                .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Credentials in the form of a client certificate"
                + " and/or username and password must be supplied");
    }

    @Test
    void passwordMustBeSuppliedWithUsername() {
        assertThatThrownBy(() -> Device.builder()
            .address("my-device")
            .username("my-username")
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("A username must be supplied with a password");

        assertThatThrownBy(() -> Device.builder()
            .address("my-device")
            .password("my-password")
            .build())
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("A username must be supplied with a password");
    }

    @Test
    void willOnlyUseCertificateToConnect() throws Exception {

        final Device device = Device.builder()
            .address("my-device")
            .certificate("secret certificate")
            .netconfSessionFactory(netconfSessionFactory)
            .build();

        doThrow(new NetconfException())
            .when(netconfSessionFactory)
            .createSessionUsingCertificate(device);

        assertThatThrownBy(device::openSession)
            .isInstanceOf(NetconfException.class);

        verify(netconfSessionFactory).createSessionUsingCertificate(device);
        verifyNoMoreInteractions(netconfSessionFactory);
    }

    @Test
    void willOnlyUsePasswordToConnect() throws Exception {

        final Device device = Device.builder()
            .address("my-device")
            .username("my-username")
            .password("my-password")
            .netconfSessionFactory(netconfSessionFactory)
            .build();

        doThrow(new NetconfException())
            .when(netconfSessionFactory)
            .createSessionUsingPassword(device);

        assertThatThrownBy(device::openSession)
            .isInstanceOf(NetconfException.class);

        verify(netconfSessionFactory).createSessionUsingPassword(device);
        verifyNoMoreInteractions(netconfSessionFactory);
    }

    @Test
    void willUseCertificateFollowedByPassword() throws Exception {

        final Device device = Device.builder()
            .address("my-device")
            .certificate("my-certificate")
            .username("my-username")
            .password("my-password")
            .netconfSessionFactory(netconfSessionFactory)
            .build();
        final NetconfSession mockSession = mock(NetconfSession.class);

        doThrow(new NetconfException())
            .when(netconfSessionFactory)
            .createSessionUsingCertificate(device);
        doReturn(mockSession)
            .when(netconfSessionFactory)
                .createSessionUsingPassword(device);

        final NetconfSession session = device.openSession();

        assertThat(session).isSameAs(mockSession);
        verify(netconfSessionFactory).createSessionUsingCertificate(device);
        verify(netconfSessionFactory).createSessionUsingPassword(device);
        verifyNoMoreInteractions(netconfSessionFactory);
    }
}