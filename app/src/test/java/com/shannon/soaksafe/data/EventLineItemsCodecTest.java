package com.shannon.soaksafe.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class EventLineItemsCodecTest {

    @Test
    public void decode_nullOrBlank_returnsEmpty() {
        assertTrue(EventLineItemsCodec.decode(null).isEmpty());
        assertTrue(EventLineItemsCodec.decode("").isEmpty());
        assertTrue(EventLineItemsCodec.decode("   ").isEmpty());
    }

    @Test
    public void decode_invalidJson_returnsEmpty() {
        assertTrue(EventLineItemsCodec.decode("not-json").isEmpty());
    }

    @Test
    public void decode_skipsBlankLabels() {
        String json = "[{\"label\":\"\",\"amount\":null},{\"label\":\"  \",\"amount\":null}]";
        assertTrue(EventLineItemsCodec.decode(json).isEmpty());
    }

    @Test
    public void decode_taskAndChemical_parsesAmounts() {
        String json = "[{\"label\":\"Vacuum\",\"amount\":null},{\"label\":\"Chlorine\",\"amount\":1.5}]";
        List<EventLineItem> items = EventLineItemsCodec.decode(json);
        assertEquals(2, items.size());
        assertEquals("Vacuum", items.get(0).label);
        assertNull(items.get(0).amount);
        assertEquals("Chlorine", items.get(1).label);
        assertEquals(1.5f, items.get(1).amount, 0.001f);
    }

    @Test
    public void encode_decode_roundTrip() {
        List<EventLineItem> original = Arrays.asList(
                new EventLineItem("Brush", null),
                new EventLineItem("pH up", 2f)
        );
        String json = EventLineItemsCodec.encode(original);
        List<EventLineItem> back = EventLineItemsCodec.decode(json);
        assertEquals(2, back.size());
        assertEquals("Brush", back.get(0).label);
        assertNull(back.get(0).amount);
        assertEquals("pH up", back.get(1).label);
        assertEquals(2f, back.get(1).amount, 0.001f);
    }

    @Test
    public void encode_emptyList_returnsEmptyArrayJson() {
        assertEquals("[]", EventLineItemsCodec.encode(Collections.emptyList()));
    }
}
