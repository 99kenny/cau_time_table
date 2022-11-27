package org.example;

import lombok.*;

@AllArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class CrawlData {
    private String code;
    private String name;
    private String prof;
    private String timeInfo;
    private String note;
}
