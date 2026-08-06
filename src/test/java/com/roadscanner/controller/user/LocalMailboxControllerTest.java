package com.roadscanner.controller.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.web.servlet.view.RedirectView;

import com.roadscanner.service.user.LocalMailSendService;

public class LocalMailboxControllerTest {

    @Test
    public void mailboxListsAndClearsCapturedLocalMessages() throws Exception {
        LocalMailSendService service = new LocalMailSendService();
        service.sendRegistrationVerification("user@example.invalid", "123456");
        LocalMailboxController controller = new LocalMailboxController(service);
        ExtendedModelMap model = new ExtendedModelMap();
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertThat(controller.mailbox(model, response)).isEqualTo("local/mailbox");
        assertThat((Iterable<?>) model.get("messages")).hasSize(1);
        assertThat(response.getHeader("Cache-Control")).isEqualTo("no-store");

        RedirectView redirect = controller.clear();
        assertThat(redirect.getUrl()).isEqualTo("/local/mailbox");
        assertThat(service.getMessages()).isEmpty();
    }

    @Test
    public void localModelAdviceEnablesMailboxNavigationWithoutAddingRedirectParameters() {
        MockHttpServletRequest request = new MockHttpServletRequest();

        new LocalMailboxModelAdvice().exposeLocalMailboxNavigation(request);

        assertThat(request.getAttribute("localMailboxEnabled")).isEqualTo(Boolean.TRUE);
    }
}
