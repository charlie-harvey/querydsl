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
package com.mysema.query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.annotation.Nullable;

import com.google.common.base.Objects;
import com.mysema.query.types.Expression;
import com.mysema.query.types.OrderSpecifier;
import com.mysema.query.types.ParamExpression;
import com.mysema.query.types.Path;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.ValidatingVisitor;

/**
 * DefaultQueryMetadata is the default implementation of the {@link QueryMetadata} interface
 *
 * @author tiwe
 */
public class DefaultQueryMetadata implements QueryMetadata, Cloneable {

    private static final long serialVersionUID = 317736313966701232L;

    private boolean distinct;

    private Set<Expression<?>> exprInJoins = new HashSet<Expression<?>>();

    private List<Expression<?>> groupBy = new ArrayList<Expression<?>>();

    private BooleanBuilder having = new BooleanBuilder();

    private List<JoinExpression> joins = new ArrayList<JoinExpression>();

    @Nullable
    private QueryModifiers modifiers = QueryModifiers.EMPTY;

    private List<OrderSpecifier<?>> orderBy = new ArrayList<OrderSpecifier<?>>();

    private List<Expression<?>> projection = new ArrayList<Expression<?>>();

    // NOTE : this is not necessarily serializable
    private Map<ParamExpression<?>,Object> params = new HashMap<ParamExpression<?>,Object>();

    private boolean unique;

    private BooleanBuilder where = new BooleanBuilder();

    private Set<QueryFlag> flags = new LinkedHashSet<QueryFlag>();
    
    private final ValidatingVisitor validatingVisitor = new ValidatingVisitor(exprInJoins);

    private boolean validate;
    
    public DefaultQueryMetadata(boolean validate) {
        this.validate = validate;
    }    
    
    public DefaultQueryMetadata() {
        this(true);
    }
    
    @Override
    public void addGroupBy(Expression<?>... o) {
        validate(o);
        groupBy.addAll(Arrays.<Expression<?>> asList(o));
    }

    @Override
    public void addHaving(Predicate... o) {
        for (Predicate e : o) {
            if (e != null && (!BooleanBuilder.class.isInstance(e) || ((BooleanBuilder)e).hasValue())) {
                validate(e);
                having.and(e);
            }
        }
    }

    @Override
    public void addJoin(JoinType joinType, Expression<?> expr) {
        addJoin(new JoinExpression(joinType, expr));
    }
    
    @Override
    public void addJoin(JoinExpression... j){
        for (JoinExpression join : j) {
            Expression<?> expr = join.getTarget();
            if (!exprInJoins.contains(expr)) {
                if (expr instanceof Path<?> && join.getType() == JoinType.DEFAULT) {
                    ensureRoot((Path<?>) expr);
                }
                exprInJoins.add(expr);
                validate(expr);
                joins.add(join);
            } else {
                throw new IllegalStateException(expr + " is already used");
            }    
        }        
    }

    @Override
    public void addJoinCondition(Predicate o) {
        if (!joins.isEmpty()) {
            validate(o);
            joins.get(joins.size() - 1).addCondition(o);
        }
    }

    @Override
    public void addOrderBy(OrderSpecifier<?>... o) {
        for (OrderSpecifier<?> os : o) {
            validate(os.getTarget());
        }
        orderBy.addAll(Arrays.asList(o));
    }

    @Override
    public void addProjection(Expression<?>... o) {
        validate(o);
        projection.addAll(Arrays.asList(o));
    }

    @Override
    public void addWhere(Predicate... o) {
        for (Predicate e : o) {
            if (e != null && (!BooleanBuilder.class.isInstance(e) || ((BooleanBuilder)e).hasValue())) {
                validate(e);
                where.and(e);
            }
        }
    }

    public void clearOrderBy(){
        orderBy = new ArrayList<OrderSpecifier<?>>();
    }

    public void clearProjection(){
        projection = new ArrayList<Expression<?>>();
    }

    public void clearWhere(){
        where = new BooleanBuilder();
    }

    @Override
    public QueryMetadata clone(){
        try {
            DefaultQueryMetadata clone = (DefaultQueryMetadata) super.clone();
            clone.exprInJoins = new HashSet<Expression<?>>(exprInJoins);
            clone.groupBy = new ArrayList<Expression<?>>(groupBy);
            clone.having = having.clone();
            clone.joins = new ArrayList<JoinExpression>(joins);
            clone.modifiers = new QueryModifiers(modifiers);
            clone.orderBy = new ArrayList<OrderSpecifier<?>>(orderBy);
            clone.projection = new ArrayList<Expression<?>>(projection);
            clone.params = new HashMap<ParamExpression<?>,Object>(params);
            clone.where = where.clone();
            clone.flags = new LinkedHashSet<QueryFlag>(flags);
            return clone;
        } catch (CloneNotSupportedException e) {
            throw new QueryException(e);
        }

    }

    private void ensureRoot(Path<?> path){
        if (path.getMetadata().getParent() != null) {
            throw new IllegalArgumentException("Only root paths are allowed for joins : " + path);
        }
    }

    @Override
    public List<? extends Expression<?>> getGroupBy() {
        return Collections.unmodifiableList(groupBy);
    }

    @Override
    public Predicate getHaving() {
        return having.hasValue() ? having.getValue() : null;
    }

    @Override
    public List<JoinExpression> getJoins() {
        return Collections.unmodifiableList(joins);
    }

    @Override
    @Nullable
    public QueryModifiers getModifiers() {
        return modifiers;
    }

    public Map<ParamExpression<?>,Object> getParams(){
        return Collections.unmodifiableMap(params);
    }

    @Override
    public List<OrderSpecifier<?>> getOrderBy() {
        return Collections.unmodifiableList(orderBy);
    }

    @Override
    public List<? extends Expression<?>> getProjection() {
        return Collections.unmodifiableList(projection);
    }

    @Override
    public Predicate getWhere() {
        return where.hasValue() ? where.getValue() : null;
    }

    @Override
    public boolean isDistinct() {
        return distinct;
    }

    @Override
    public boolean isUnique() {
        return unique;
    }

    @Override
    public void reset() {
        clearProjection();
        params = new HashMap<ParamExpression<?>,Object>();
        modifiers = QueryModifiers.EMPTY;
    }

    @Override
    public void setDistinct(boolean distinct) {
        this.distinct = distinct;
    }

    @Override
    public void setLimit(Long limit) {
        if (modifiers == null || modifiers.getOffset() == null) {
            modifiers = QueryModifiers.limit(limit);
        } else {
            modifiers = new QueryModifiers(limit, modifiers.getOffset());
        }
    }

    @Override
    public void setModifiers(@Nullable QueryModifiers restriction) {
        this.modifiers = restriction;
    }

    @Override
    public void setOffset(Long offset) {
        if (modifiers == null || modifiers.getLimit() == null) {
            modifiers = QueryModifiers.offset(offset);
        } else {
            modifiers = new QueryModifiers(modifiers.getLimit(), offset);
        }
    }

    @Override
    public void setUnique(boolean unique) {
        this.unique = unique;
    }

    @Override
    public <T> void setParam(ParamExpression<T> param, T value) {
        params.put(param, value);
    }

    @Override
    public void addFlag(QueryFlag flag) {
        flags.add(flag);        
    }

    @Override
    public Set<QueryFlag> getFlags() {
        return flags;
    }

    @Override
    public boolean hasFlag(QueryFlag flag) {
        return flags.contains(flag);
    }

    private void validate(Expression<?>... expr){
        if (validate) {
            for (Expression<?> e : expr) {
                e.accept(validatingVisitor, null);
            }
        }
    }
    
    public void setValidate(boolean v) {
        this.validate = v;
    }
    
    @Override
    public boolean equals(Object o) {
        if (o instanceof QueryMetadata) {
            QueryMetadata q = (QueryMetadata)o;
            return q.getFlags().equals(flags)
                && q.getGroupBy().equals(groupBy)
                && Objects.equal(q.getHaving(), having.getValue())
                && q.isDistinct() == distinct
                && q.isUnique() == unique
                && q.getJoins().equals(joins)
                && Objects.equal(q.getModifiers(), modifiers)
                && q.getOrderBy().equals(orderBy)
                && q.getParams().equals(params)
                && q.getProjection().equals(projection)
                && Objects.equal(q.getWhere(), where.getValue());
            
        } else {
            return false;
        }
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = prime + (distinct ? 1231 : 1237);
        result = prime * result + flags.hashCode();
        result = prime * result + groupBy.hashCode();
        result = prime * result + having.hashCode();
        result = prime * result + joins.hashCode();
        result = prime * result + ((modifiers == null) ? 0 : modifiers.hashCode());
        result = prime * result + orderBy.hashCode();
        result = prime * result + params.hashCode();
        result = prime * result + projection.hashCode();
        result = prime * result + (unique ? 1231 : 1237);
        result = prime * result + where.hashCode();
        return result;
    }
    
    
}
