package com.cwc.interview.ai.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class PythonController {

    private final RestTemplate restTemplate;

    @PostMapping("/levenshtein")
    public ResponseEntity<String> getLevenshteinDistance(@RequestBody Map<String, String> request) {
        String str1 = request.get("str1");
        String str2 = request.get("str2");

        String url = "http://localhost:5000/levenshtein";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("str1", str1);
        map.add("str2", str2);
        HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(map, headers);

        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class);
        String distance = response.getBody();

        return ResponseEntity.ok("Levenshtein Distance: " + distance);
    }
}

