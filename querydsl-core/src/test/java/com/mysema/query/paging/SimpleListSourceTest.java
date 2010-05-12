package com.mysema.query.paging;

import static org.junit.Assert.*;

import java.util.Arrays;

import org.junit.Test;

public class SimpleListSourceTest {

    @Test
    public void testGetResults() {
        ListSource<String> strings = new SimpleListSource<String>("a","b","c","d","e","f");
        assertEquals(Arrays.asList("a","b"), strings.getResults(0, 2));
        assertEquals(Arrays.asList("c","d"), strings.getResults(2, 4));
    }

}