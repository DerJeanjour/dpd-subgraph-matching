package de.haw.misc.utils;


import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class CollectionUtilsTest {

    @Test
    void testIntersection() {
        List<String> a = Arrays.asList( "A", "B", "C" );
        List<String> b = Arrays.asList( "B", "C", "D" );

        List<String> expected = Arrays.asList( "B", "C" );
        List<String> actual = CollectionUtils.intersection( a, b );

        assertEquals( expected, actual );
    }

}
