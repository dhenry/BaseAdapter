package com.dhenry.sampleapp.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hendavid on 7/29/16.
 */
public class SimpleListItem extends SimpleItem {

    public final String text;
    public List<SimpleItem> itemsList = new ArrayList<>();

    public SimpleListItem(int value) {
        super(value);
        this.text = "I am a list item. I have " + value + " children";
        for (int i = 0; i < value; i++) {
            itemsList.add(new SimpleItem(i));
        }
    }
}
