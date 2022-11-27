package org.example;

import lombok.*;

import java.util.UUID;
@AllArgsConstructor
@Getter
@Setter
@ToString
@Builder
public class Time {
    private UUID id;
    private String week;
    private String time;
    private String courseId;

    public static TimeBuilder builder(){
        return new TimeBuilder().id(UUID.randomUUID());
    }
}
