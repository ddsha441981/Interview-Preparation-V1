package com.cwc.interview.ai.controller;

import com.cwc.interview.ai.payloads.request.TranscriptRequest;
import com.cwc.interview.ai.service.InterviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

    @RestController
    @RequestMapping("/api/v1/interview/ai")
    @Slf4j
    @RequiredArgsConstructor
    @CrossOrigin("*")
    public class InterviewController {

        private final InterviewService interviewService;

        @PostMapping("/transcribe")
        public  CompletableFuture<Map<String, String>> handleTranscription(@RequestBody TranscriptRequest transcriptRequest) {
            log.info("Received transcript: {}", transcriptRequest.getTranscript());
            CompletableFuture<Map<String, String>> responses = interviewService.handleTranscription(transcriptRequest);
            return new ResponseEntity<>(responses, HttpStatus.OK).getBody();
        }


    }





















//        @PostMapping("/transcribe")
//        public CompletableFuture<ResponseEntity<Map<String, String>>> handleTranscription(@RequestBody Transcript transcript) {
//            String transcripts = transcript.getTranscript();
//            log.info("Received transcript: {}", transcript.getTranscript());
//
//            CompletableFuture<String> chatGptResponse = CompletableFuture.supplyAsync(() -> {
//                return sendRequestWithRetry(openaiUrl, createRequestEntity(
//                        "{\"model\": \"gpt-4o-mini\", \"messages\": [{\"role\": \"user\", \"content\": \"" + transcripts + "\"}]}", openaiApiKey, false));
//            });
//
//
////            CompletableFuture<String> chatGptResponse = CompletableFuture.supplyAsync(() -> {
////                return sendRequestWithRetry(openaiUrl, createRequestEntity("{\"model\": \"gpt-3.5-turbo\", \"messages\": [{\"role\": \"user\", \"content\": \"" + transcripts + "\"}]}", openaiApiKey, false));
////            });
//
//            CompletableFuture<String> copilotResponse = CompletableFuture.supplyAsync(() -> {
//                return sendRequestWithRetry(copilotUrl, createRequestEntity("{\"input\": \"" + transcripts + "\"}", copilotApiKey, false));
//            });
//
//            CompletableFuture<String> geminiResponse = CompletableFuture.supplyAsync(() -> {
//                String geminiUrlWithApiKey = googleUrl + "?key=" + googleApiKey;
//                String payload = "{\"contents\":[{\"parts\":[{\"text\":\"" + transcripts + "\"}]}]}";
//                return sendRequestWithRetry(geminiUrlWithApiKey, createRequestEntity(payload, "", true));
//            });
//
//            return CompletableFuture.allOf(chatGptResponse, copilotResponse, geminiResponse).thenApply(voidResult -> {
//                Map<String, String> responses = new HashMap<>();
//                responses.put("chatgpt", chatGptResponse.join());
//                responses.put("copilot", copilotResponse.join());
//                responses.put("gemini", geminiResponse.join());
//
//                return new ResponseEntity<>(responses, HttpStatus.OK);
//            });
//        }
//
//        private HttpEntity<String> createRequestEntity(String body, String apiKey, boolean isGoogleApi) {
//            HttpHeaders headers = new HttpHeaders();
//            headers.setContentType(MediaType.APPLICATION_JSON);
//
//            if (!isGoogleApi) {  // If it's not Google API, set Authorization header
//                headers.set("Authorization", "Bearer " + apiKey);
//            }
//
//            return new HttpEntity<>(body, headers);
//        }
//
//        private String sendRequestWithRetry(String url, HttpEntity<String> entity) {
//            int maxRetries = 3;
//            int attempt = 0;
//
//            while (attempt < maxRetries) {
//                try {
//                    ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
//                    return response.getBody();
//                } catch (HttpClientErrorException e) {
//                    if (e.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
//                        attempt++;
//                        try {
//                            long backoffTime = (long) Math.pow(2, attempt) * 1000;
//                            Thread.sleep(backoffTime);
//                        } catch (InterruptedException ie) {
//                            Thread.currentThread().interrupt();
//                            throw new RuntimeException("Interrupted during backoff", ie);
//                        }
//                    } else {
//                        return "Error: " + e.getMessage();
//                    }
//                } catch (Exception e) {
//                    return "Error: " + e.getMessage();
//                }
//            }
//            return "Max retries exceeded";
//        }
