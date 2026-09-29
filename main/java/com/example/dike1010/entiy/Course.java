package com.example.dike1010.entiy;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class Course {
    private int id;
    private String courseNo;
    private String courseName;
    private String departId;
    private String departName;

}
