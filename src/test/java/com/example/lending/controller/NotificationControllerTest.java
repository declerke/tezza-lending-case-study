package com.example.lending.controller;

import com.example.lending.domain.notification.*;
import com.example.lending.dto.notification.*;
import com.example.lending.exception.*;
import com.example.lending.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(NotificationController.class)
@Import(GlobalExceptionHandler.class)
class NotificationControllerTest {
    @Autowired MockMvc mvc;
    @MockBean NotificationService notifications;

    @Test
    void updatesTemplateAndReturnsItsCode() throws Exception {
        when(notifications.updateTemplate(eq(4L), any())).thenReturn(new NotificationTemplateResponse(
                4L, "T", NotificationEventType.LOAN_CREATED, NotificationChannel.EMAIL, "subject", "body", true));

        mvc.perform(put("/api/notifications/templates/4").contentType(MediaType.APPLICATION_JSON).content("""
                {"code":"T","eventType":"LOAN_CREATED","channel":"EMAIL","bodyTemplate":"body"}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("T"));
    }

    @Test
    void invalidTemplateUpdateReturnsStable400Error() throws Exception {
        mvc.perform(put("/api/notifications/templates/4").contentType(MediaType.APPLICATION_JSON).content("""
                {"code":"T","eventType":"LOAN_CREATED","channel":"EMAIL","bodyTemplate":""}
                """))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.message").value("Validation failed"));
    }

    @Test
    void missingTemplateReturns404() throws Exception {
        when(notifications.setTemplateActive(999L, true))
                .thenThrow(new ResourceNotFoundException("Notification template not found"));

        mvc.perform(post("/api/notifications/templates/999/activate"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void updatesAndDisablesRule() throws Exception {
        when(notifications.updateRule(eq(8L), any())).thenReturn(new NotificationRuleResponse(
                8L, NotificationEventType.LOAN_DISBURSED, 2L, "RETAIL", NotificationChannel.SMS, true));
        when(notifications.setRuleEnabled(8L, false)).thenReturn(new NotificationRuleResponse(
                8L, NotificationEventType.LOAN_DISBURSED, 2L, "RETAIL", NotificationChannel.SMS, false));

        mvc.perform(put("/api/notifications/rules/8").contentType(MediaType.APPLICATION_JSON).content("""
                {"eventType":"LOAN_DISBURSED","productId":2,"customerSegment":"RETAIL","channel":"SMS"}
                """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
        mvc.perform(post("/api/notifications/rules/8/disable"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
    }

    @Test
    void duplicateEnabledRuleReturns409() throws Exception {
        when(notifications.createRule(any())).thenThrow(new ConflictException(
                "An enabled notification rule already exists for the same matching scope."));

        mvc.perform(post("/api/notifications/rules").contentType(MediaType.APPLICATION_JSON).content("""
                {"eventType":"LOAN_CREATED","channel":"EMAIL"}
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("An enabled notification rule already exists for the same matching scope."));
    }

    @Test
    void conflictingRuleEnableReturns409() throws Exception {
        when(notifications.setRuleEnabled(8L, true)).thenThrow(new ConflictException(
                "An enabled notification rule already exists for the same matching scope."));

        mvc.perform(post("/api/notifications/rules/8/enable"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void duplicateActiveTemplateReturns409() throws Exception {
        when(notifications.createTemplate(any())).thenThrow(new ConflictException(
                "An active template already exists for this event and channel."));

        mvc.perform(post("/api/notifications/templates").contentType(MediaType.APPLICATION_JSON).content("""
                {"code":"SECOND","eventType":"LOAN_CREATED","channel":"EMAIL","bodyTemplate":"body"}
                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value("An active template already exists for this event and channel."));
    }

    @Test
    void conflictingTemplateActivationReturns409() throws Exception {
        when(notifications.setTemplateActive(9L, true)).thenThrow(new ConflictException(
                "An active template already exists for this event and channel."));

        mvc.perform(post("/api/notifications/templates/9/activate"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }
}
