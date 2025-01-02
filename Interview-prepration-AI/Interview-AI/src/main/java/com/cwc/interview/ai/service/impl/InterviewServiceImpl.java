package com.cwc.interview.ai.service.impl;

import com.cwc.interview.ai.payloads.request.TranscriptRequest;
import com.cwc.interview.ai.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
@RequiredArgsConstructor
public class InterviewServiceImpl implements InterviewService {

    private final RestTemplate restTemplate;

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${google.api.key}")
    private String googleApiKey;

    @Value("${copilot.api.key}")
    private String copilotApiKey;

    @Value("${openai.url}")
    private String openaiUrl;

    @Value("${google.url}")
    private String googleUrl;

    @Value("${copilot.url}")
    private String copilotUrl;

    @Value("${python.service.url}")
    private String pythonUrl;

    @Override
    @Cacheable(value = "transcriptsCache", key = "#transcriptRequest.transcript")
    public CompletableFuture<Map<String, String>> handleTranscription(TranscriptRequest transcriptRequest) {
        String transcripts = transcriptRequest.getTranscript();
        log.info("Received transcript: {}", transcripts);

        // Python microservice
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("str1", transcripts);
        map.add("str2", "");

        HttpEntity<MultiValueMap<String, String>> pythonRequestEntity = new HttpEntity<>(map, headers);

        //preparing to call after checking python responsess and send it by uing
        CompletableFuture<String> chatGptResponse = CompletableFuture.supplyAsync(() ->
                sendRequestWithRetry(openaiUrl, createRequestEntity(
                                "{\"model\": \"gpt-4o-mini\", \"messages\": [{\"role\": \"user\", \"content\": \"" + transcripts + "\"}]}", openaiApiKey, false)
                        , String.class)
        );

        CompletableFuture<String> copilotResponse = CompletableFuture.supplyAsync(() ->
                sendRequestWithRetry(copilotUrl, createRequestEntity(
                                "{\"input\": \"" + transcripts + "\"}", copilotApiKey, false)
                        , String.class)
        );

        CompletableFuture<String> geminiResponse = CompletableFuture.supplyAsync(() -> {
            String geminiUrlWithApiKey = googleUrl + "?key=" + googleApiKey;
            String payload = "{\"contents\":[{\"parts\":[{\"text\":\"" + transcripts + "\"}]}]}";
            return sendRequestWithRetry(geminiUrlWithApiKey, createRequestEntity(payload, "", true), String.class);
        });

        CompletableFuture<String> pythonResponse = CompletableFuture.supplyAsync(() ->
                sendRequestWithRetry(pythonUrl, pythonRequestEntity, String.class)
        );

        return CompletableFuture.allOf(chatGptResponse, copilotResponse, geminiResponse, pythonResponse)
                .thenApply(voidResult -> {
                    Map<String, String> responses = new HashMap<>();
                    responses.put("chatgpt", chatGptResponse.join());
                    responses.put("copilot", copilotResponse.join());
                    responses.put("gemini", geminiResponse.join());
                    responses.put("python", pythonResponse.join());

                    return responses;
                }).exceptionally(ex -> {
                    log.error("Error occurred while processing transcription", ex);
                    Map<String, String> errorResponse = new HashMap<>();
                    errorResponse.put("error", "An error occurred while processing the transcription");
                    return errorResponse;
                });
    }
    private HttpEntity<String> createRequestEntity(String body, String apiKey, boolean isGoogleApi) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        if (!isGoogleApi) {
            headers.set("Authorization", "Bearer " + apiKey);
        }

        return new HttpEntity<>(body, headers);
    }

    private <T> String sendRequestWithRetry(String url, HttpEntity<?> entity, Class<T> responseType) {
        int maxRetries = 3;
        int attempt = 0;

        while (attempt < maxRetries) {
            try {
                ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
                return (String) response.getBody();
            } catch (HttpClientErrorException e) {
                if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    attempt++;
                    try {
                        long backoffTime = (long) Math.pow(2, attempt) * 1000;
                        Thread.sleep(backoffTime);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("Interrupted during backoff", ie);
                    }
                } else {
                    return "Error: " + e.getMessage();
                }
            } catch (Exception e) {
                return "Error: " + e.getMessage();
            }
        }
        return "Max retries exceeded";
    }
}