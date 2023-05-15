package ru.mpei.cimmaintainer.owl;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
@AllArgsConstructor
public class StationObject {
    private String name;
    private List<String> dataProperties;
    private List<String> objectProperties;

}
