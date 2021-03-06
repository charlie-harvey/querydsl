/*
 * Copyright 2011, Mysema Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mysema.query.types;

import java.util.Arrays;
import java.util.List;

import com.mysema.commons.lang.Assert;

/**
 * Default implementation of the TemplateExpression interface
 * 
 * @author tiwe
 *
 * @param <T> expression type
 */
public class TemplateExpressionImpl<T> extends ExpressionBase<T> implements TemplateExpression<T> {

    private static final long serialVersionUID = 6951623726800809083L;

    private final List<Expression<?>> args;

    private final Template template;

    /**
     * Create a new TemplateExpression with the given template in String form and template arguments
     * 
     * @param <C>
     * @param cl
     * @param template
     * @param args
     * @return
     */
    public static <C> Expression<C> create(Class<C> cl, String template, Expression<?>... args){
        return create(cl, TemplateFactory.DEFAULT.create(template), args);
    }

    /**
     * Create a new TemplateExpression with the given template and template arguments
     * 
     * @param <C>
     * @param cl
     * @param template
     * @param args
     * @return
     */
    public static <C> Expression<C> create(Class<C> cl, Template template, Expression<?>... args){
        return new TemplateExpressionImpl<C>(cl, template, args);
    }

    public TemplateExpressionImpl(Class<? extends T> type, Template template, Expression<?>... args){
        this(type, template, Arrays.<Expression<?>>asList(args));
    }    
    
    public TemplateExpressionImpl(Class<? extends T> type, Template template, List<Expression<?>> args){
        super(type);
        this.args = Assert.notNull(args,"args");
        this.template = Assert.notNull(template,"template");
    }

    @Override
    public Expression<?> getArg(int index) {
        return getArgs().get(index);
    }

    @Override
    public List<Expression<?>> getArgs() {
        return args;
    }

    @Override
    public Template getTemplate() {
        return template;
    }

    @Override
    public boolean equals(Object o) {
       if (o == this) {
           return true;
       } else if (o instanceof TemplateExpression) {
           TemplateExpression<?> c = (TemplateExpression<?>)o;
           return c.getTemplate().equals(template)
               && c.getType().equals(getType());
       } else {
           return false;
       }
    }

    @Override
    public int hashCode(){
        return getType().hashCode();
    }
    
    @Override
    public <R, C> R accept(Visitor<R, C> v, C context) {
        return v.visit(this, context);
    }

}
