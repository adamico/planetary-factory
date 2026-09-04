package com.planetaryfactory.core.assembler;

import java.util.List;
import java.util.Map;

/** Fixture helpers the resolver tests share, so an inventory reads as one line rather than four. */
final class TestBags {

    private TestBags() {}

    /** An {@link ItemBag} from alternating item and count: {@code have("plate", 2, "copper", 1)}. */
    static ItemBag have(Object... pairs) {
        ItemBag bag = new ItemBag();
        for (int index = 0; index < pairs.length; index += 2) {
            bag.add((String) pairs[index], (Integer) pairs[index + 1]);
        }
        return bag;
    }

    /** A player holding exactly what a bag holds. */
    static TestPlayerItems stocked(ItemBag inventory) {
        TestPlayerItems items = new TestPlayerItems();
        inventory.asMap().forEach(items::with);
        return items;
    }

    /** Amounts summed by item, which is how every assertion here wants to read them. */
    static Map<String, Integer> asMap(List<ItemAmount> amounts) {
        return ItemBag.ofAmounts(amounts).asMap();
    }
}
