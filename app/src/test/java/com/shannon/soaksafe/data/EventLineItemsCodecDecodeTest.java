package com.shannon.soaksafe.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

/**
 * JVM tests for {@link EventLineItemsCodec#decode(String)} used by report cards.
 */
public class EventLineItemsCodecDecodeTest {

    @Test
    public void decode_taskRowWithNullAmount() {
        List<EventLineItem> items = EventLineItemsCodec.decode(
                "[{\"label\":\"Vacuum\",\"amount\":null}]"
        );
        assertEquals(1, items.size());
        assertEquals("Vacuum", items.get(0).label);
        assertEquals(null, items.get(0).amount);
    }

    @Test
    public void decode_chemicalRowWithAmount() {
        List<EventLineItem> items = EventLineItemsCodec.decode(
                "[{\"label\":\"Chlorine\",\"amount\":2.5}]"
        );
        assertEquals(1, items.size());
        assertEquals("Chlorine", items.get(0).label);
        assertTrue(items.get(0).amount != null && items.get(0).amount == 2.5f);
    }

    @Test
    public void decode_emptyArray_returnsEmpty() {
        assertTrue(EventLineItemsCodec.decode("[]").isEmpty());
    }
}
