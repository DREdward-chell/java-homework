package com.edwards.demo;

import com.edwards.csvparser.CsvCollection;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {
    private String name;
    @CsvCollection(delimiter = ";")
    private List<String> tags;
    private List<String> authors;
}
