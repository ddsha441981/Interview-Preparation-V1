package com.cwc.interview.ai.service;


import com.cwc.interview.ai.payloads.request.TranscriptRequest;
import org.springframework.http.ResponseEntity;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public interface InterviewService {

    CompletableFuture<Map<String, String>> handleTranscription(TranscriptRequest transcriptRequest);

}
