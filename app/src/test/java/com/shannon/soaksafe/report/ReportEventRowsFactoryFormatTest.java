package com.shannon.soaksafe.report;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ReportEventRowsFactoryFormatTest {

    @Test
    public void formatChem_twoDecimalPlaces() {
        assertEquals("1.50", ReportEventRowsFactory.formatChem(1.5f));
        assertEquals("0.00", ReportEventRowsFactory.formatChem(0f));
    }

    @Test
    public void formatChem_wholeNumber_stillTwoDecimals() {
        assertEquals("3.00", ReportEventRowsFactory.formatChem(3f));
    }
}
