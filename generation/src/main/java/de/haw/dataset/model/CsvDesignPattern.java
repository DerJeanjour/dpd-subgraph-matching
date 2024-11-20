package de.haw.dataset.model;

import com.opencsv.bean.CsvBindByName;
import lombok.Data;

@Data
public class CsvDesignPattern {

    @CsvBindByName( column = "Project" )
    private String projectName;

    @CsvBindByName( column = "Class" )
    private String className;

    @CsvBindByName( column = "Pattern" )
    private String patternName;

}
