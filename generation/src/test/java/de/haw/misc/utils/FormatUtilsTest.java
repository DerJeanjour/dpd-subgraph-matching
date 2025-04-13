package de.haw.misc.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.IllegalFormatPrecisionException;
import java.util.UnknownFormatConversionException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FormatUtilsTest {

    @ParameterizedTest
    @CsvSource( {
            "1.23456, 2, 1.23", "1.23456, 4, 1.2346", "0, 3, 0.000", "-1.234, 2, -1.23", "1.9999, 3, 2.000"
    } )
    void testFormatValid( double value, int precision, String expected ) {
        assertEquals( expected, FormatUtils.format( value, precision ) );
    }

    @Test
    void testFormatNegativePrecisionThrows() {
        assertThrows( UnknownFormatConversionException.class, () -> FormatUtils.format( 1.23, -1 ) );
    }
}
