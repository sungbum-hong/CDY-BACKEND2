package com.cdy.cdy.domain.shop.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

/**
 * 토스페이먼츠 결제 승인 클라이언트.
 * 시크릿 키는 서버에만 두고 프론트로 절대 내보내지 않는다.
 */
@Slf4j
@Component
public class TossPaymentClient {

    private static final String CONFIRM_URL = "https://api.tosspayments.com/v1/payments/confirm";

    private final RestClient restClient = RestClient.create();
    private final String secretKey;

    public TossPaymentClient(@Value("${toss.secretKey:}") String secretKey) {
        this.secretKey = secretKey;
    }

    /**
     * 결제 승인. 실패하면 토스가 내려준 메시지를 담아 IllegalStateException 을 던진다(→ 400).
     *
     * @return 승인 응답 본문 (status, approvedAt, method 등)
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> confirm(String paymentKey, String orderNumber, int amount) {

        if (secretKey == null || secretKey.isBlank()) {
            throw new IllegalStateException("토스 시크릿 키가 설정되지 않았습니다. (환경변수 TOSS_SECRET_KEY)");
        }

        String basic = Base64.getEncoder()
                .encodeToString((secretKey + ":").getBytes(StandardCharsets.UTF_8));

        try {
            return restClient.post()
                    .uri(CONFIRM_URL)
                    .header(HttpHeaders.AUTHORIZATION, "Basic " + basic)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(Map.of(
                            "paymentKey", paymentKey,
                            "orderId", orderNumber,
                            "amount", amount))
                    .retrieve()
                    .body(Map.class);

        } catch (org.springframework.web.client.RestClientResponseException e) {
            String body = e.getResponseBodyAsString();
            log.warn("[Toss] 결제 승인 실패 - orderNumber: {}, status: {}, body: {}",
                    orderNumber, e.getStatusCode(), body);
            throw new IllegalStateException("결제 승인에 실패했습니다. " + extractMessage(body));
        }
    }

    /** 토스 오류 본문에서 message 필드만 뽑아낸다 (파싱 실패 시 원문 일부) */
    private String extractMessage(String body) {
        if (body == null || body.isBlank()) return "";
        int i = body.indexOf("\"message\"");
        if (i < 0) return body.length() > 200 ? body.substring(0, 200) : body;
        int start = body.indexOf('"', i + 9 + 1);
        int end = body.indexOf('"', start + 1);
        if (start < 0 || end < 0) return body;
        return body.substring(start + 1, end);
    }
}
