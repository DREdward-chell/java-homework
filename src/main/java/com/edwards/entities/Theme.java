package com.edwards.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data @NoArgsConstructor @AllArgsConstructor
public class Theme {
    @Data @NoArgsConstructor @AllArgsConstructor
    public  static class Word {
        String word;
        String hint;
    }

    String theme;
    List<Word> easy;
    List<Word> medium;
    List<Word> hard;
}
