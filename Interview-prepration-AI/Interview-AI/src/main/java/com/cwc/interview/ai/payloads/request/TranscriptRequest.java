package com.cwc.interview.ai.payloads.request;

import lombok.*;


@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@ToString
@Builder
public class TranscriptRequest {

    private String transcript;
}
