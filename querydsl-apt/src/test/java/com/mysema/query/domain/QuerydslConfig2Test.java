package com.mysema.query.domain;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import com.mysema.query.annotations.QueryEntity;
import com.mysema.query.annotations.QuerydslConfig;

public class QuerydslConfig2Test {

    @QuerydslConfig(entityAccessors=true)
    @QueryEntity
    public static class Entity extends Superclass{
        
        Entity prop1;
        
    }
    
    @QuerydslConfig(createDefaultVariable=false)
    @QueryEntity
    public static class Entity2 extends Superclass2{
        
        Entity prop1;
        
    }
    
    @QueryEntity
    public static class Superclass{
        
        Entity prop2;
    }
        
    @QuerydslConfig(entityAccessors=true)
    @QueryEntity
    public static class Superclass2{
        
        Entity prop2;
    }
    
    @Test
    public void test(){
        assertNotNull(QQuerydslConfig2Test_Entity.entity);
    }
    
    @Test(expected=NoSuchFieldException.class)
    public void createDefaultVariable() throws SecurityException, NoSuchFieldException{
        QQuerydslConfig2Test_Entity2.class.getField("entity2");
    }
}
    