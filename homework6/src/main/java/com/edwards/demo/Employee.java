package com.edwards.demo;

import com.edwards.csvparser.CsvName;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    private int id;
    @CsvName("full_name")
    private String name;
    private Address address;
}
