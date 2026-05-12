package com.shannon.soaksafe.report;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Exercises {@link MaintenanceReportSearchFilter#haystackContainsAllTokens(String, String)} used
 * for report search and line filtering (no Android {@link android.content.Context} required).
 */
public class MaintenanceReportSearchFilterTokenTest {

    @Test
    public void haystack_nullOrBlankQuery_matches() {
        assertTrue(MaintenanceReportSearchFilter.haystackContainsAllTokens("anything", null));
        assertTrue(MaintenanceReportSearchFilter.haystackContainsAllTokens("anything", ""));
        assertTrue(MaintenanceReportSearchFilter.haystackContainsAllTokens("anything", "   "));
    }

    @Test
    public void haystack_singleToken_substringCaseInsensitive() {
        assertTrue(MaintenanceReportSearchFilter.haystackContainsAllTokens(
                "Chlorine 2.50 added", "chlorine"));
        assertTrue(MaintenanceReportSearchFilter.haystackContainsAllTokens(
                "pH Up 1.00", "ph"));
        assertFalse(MaintenanceReportSearchFilter.haystackContainsAllTokens(
                "Chlorine 2.50", "acid"));
    }

    @Test
    public void haystack_multipleTokens_allRequired() {
        assertTrue(MaintenanceReportSearchFilter.haystackContainsAllTokens(
                "ph up chemical treatment", "ph up"));
        assertFalse(MaintenanceReportSearchFilter.haystackContainsAllTokens(
                "ph up only", "ph up chlorine"));
    }

    @Test
    public void haystack_extraWhitespaceBetweenTokens_stillSplits() {
        assertTrue(MaintenanceReportSearchFilter.haystackContainsAllTokens(
                "vacuum pool floor", "vacuum  floor"));
    }
}
