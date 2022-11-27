package org.example;

import lombok.*;

@AllArgsConstructor
@Builder
@Getter
@ToString
@Setter
public class Course {
    private String id;
    private String name;
    private String prof;
    private String note;
    private String building;
    private String room;

    public static CourseBuilder builder(String id){
        return new CourseBuilder().id(id);
    }
}
