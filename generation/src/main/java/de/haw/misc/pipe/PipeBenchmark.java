package de.haw.misc.pipe;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PipeBenchmark {

    @CsvBindByPosition( position = 0 )
    @CsvBindByName( column = "name" )
    private final String name;

    @CsvBindByPosition( position = 1 )
    @CsvBindByName( column = "process_name" )
    private final String processName;

    @CsvBindByPosition( position = 2 )
    @CsvBindByName( column = "process_count" )
    private final int processCount;

    @CsvBindByPosition( position = 3 )
    @CsvBindByName( column = "process_time_sec" )
    private final String processTimeSec;

    @CsvBindByPosition( position = 4 )
    @CsvBindByName( column = "total_time_sec" )
    private final String totalTimeSec;

}
