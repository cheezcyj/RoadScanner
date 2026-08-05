package com.roadscanner.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.Test;

public class LocalMailSendServiceTest {

    @Test
    public void localAdapterCapturesMessagesWithoutExposingFullRecipient() throws Exception {
        LocalMailSendService service = new LocalMailSendService(
                Clock.fixed(Instant.parse("2026-01-02T03:04:05Z"), ZoneOffset.UTC));

        service.sendRegistrationVerification("user@example.invalid", "123456");
        service.sendPasswordResetVerification("user@example.invalid", "654321");
        assertThat(service.findId("user@example.invalid", "localuser"))
                .isEqualTo("user@example.invalid");

        assertThat(service.getMessages()).hasSize(3);
        assertThat(service.getMessages().get(0).getType()).isEqualTo("아이디 찾기");
        assertThat(service.getMessages().get(0).getMaskedRecipient())
                .isEqualTo("u***@example.invalid");
        assertThat(service.getMessages().get(0).getContents()).isEqualTo("아이디: localuser");
        assertThat(service.getMessages().get(0).getCreatedAt())
                .isEqualTo(Instant.parse("2026-01-02T03:04:05Z"));
        assertThat(service.getMessages().toString()).doesNotContain("user@example.invalid");
    }

    @Test
    public void mailboxIsBoundedAndCanBeCleared() throws Exception {
        LocalMailSendService service = new LocalMailSendService();
        for (int index = 0; index < 25; index++) {
            service.sendRegistrationVerification("user@example.invalid", "123456");
        }

        assertThat(service.getMessages()).hasSize(20);
        service.clear();
        assertThat(service.getMessages()).isEmpty();
    }

    @Test
    public void invalidLocalMessageDataIsRejected() {
        LocalMailSendService service = new LocalMailSendService();

        assertThatThrownBy(() -> service.sendRegistrationVerification(
                "user@example.invalid", "123"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findId("invalid-address", "localuser"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.findId("user@example.invalid", " "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
