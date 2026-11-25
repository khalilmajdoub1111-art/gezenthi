package tn.gezenthi.inquiry;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import tn.gezenthi.common.RateLimiter;

/** Quote request endpoints: public creation + admin workflow. */
final class InquiryControllers {

    private InquiryControllers() {
    }

    @RestController
    @RequestMapping("/api/public/inquiries")
    static class PublicInquiryController {
        private final InquiryService service;
        private final RateLimiter limiter = new RateLimiter(5, Duration.ofMinutes(10), "Trop de demandes. Appelez-nous directement.");

        PublicInquiryController(InquiryService service) {
            this.service = service;
        }

        @PostMapping
        @ResponseStatus(HttpStatus.CREATED)
        InquiryDtos.Created create(@Valid @RequestBody InquiryDtos.CreateInquiry body, HttpServletRequest request) {
            limiter.check(request.getRemoteAddr());
            return service.create(body);
        }
    }

    @RestController
    @RequestMapping("/api/admin/inquiries")
    static class AdminInquiryController {
        private final InquiryService service;

        AdminInquiryController(InquiryService service) {
            this.service = service;
        }

        @GetMapping
        List<InquiryDtos.InquirySummary> list(@RequestParam(required = false) InquiryDtos.Status status) {
            return service.list(status);
        }

        @GetMapping("/{id}")
        InquiryDtos.InquiryDetail detail(@PathVariable UUID id) {
            return service.detail(id);
        }

        @PostMapping("/{id}/status")
        InquiryDtos.InquiryDetail status(@PathVariable UUID id, @Valid @RequestBody InquiryDtos.StatusChange body, Authentication auth) {
            return service.changeStatus(id, body.status(), body.note(), auth.getName());
        }

        @PostMapping("/{id}/notes")
        InquiryDtos.InquiryDetail note(@PathVariable UUID id, @Valid @RequestBody InquiryDtos.Note body, Authentication auth) {
            return service.addNote(id, body.note(), auth.getName());
        }
    }
}
