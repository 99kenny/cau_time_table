package org.example;

import lombok.Getter;

import java.util.UUID;

@Getter
public class Building {
    private UUID id;
    private String building;
    private String room;

    Building(String building, String room){
        this.id = UUID.randomUUID();
        this.building = building;
        this.room = room;
    }
}
