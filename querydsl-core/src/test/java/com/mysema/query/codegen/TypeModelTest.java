package com.mysema.query.codegen;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TypeModelTest {
    
    @Test
    public void arrayType(){
        assertEquals("Object[]",TypeModels.OBJECTS.getLocalRawName(TypeModels.OBJECT));
    }

}