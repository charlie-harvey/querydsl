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
package com.mysema.query.sql.dml;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mysema.commons.lang.Assert;
import com.mysema.commons.lang.Pair;
import com.mysema.query.DefaultQueryMetadata;
import com.mysema.query.JoinType;
import com.mysema.query.QueryException;
import com.mysema.query.QueryFlag;
import com.mysema.query.QueryFlag.Position;
import com.mysema.query.QueryMetadata;
import com.mysema.query.dml.UpdateClause;
import com.mysema.query.sql.Configuration;
import com.mysema.query.sql.RelationalPath;
import com.mysema.query.sql.SQLSerializer;
import com.mysema.query.sql.SQLTemplates;
import com.mysema.query.sql.types.Null;
import com.mysema.query.types.ConstantImpl;
import com.mysema.query.types.Expression;
import com.mysema.query.types.Path;
import com.mysema.query.types.Predicate;
import com.mysema.query.types.expr.Param;

/**
 * SQLUpdateClause defines a UPDATE clause
 *
 * @author tiwe
 *
 */
public class SQLUpdateClause extends AbstractSQLClause implements UpdateClause<SQLUpdateClause> {

    private static final Logger logger = LoggerFactory.getLogger(SQLInsertClause.class);

    private final Connection connection;

    private final RelationalPath<?> entity;

    private final List<QueryMetadata> batchMetadata = new ArrayList<QueryMetadata>();
    
    private final List<List<Pair<Path<?>,Expression<?>>>> batchUpdates = new ArrayList<List<Pair<Path<?>,Expression<?>>>>();
    
    private List<Pair<Path<?>,Expression<?>>> updates = new ArrayList<Pair<Path<?>,Expression<?>>>();

    private QueryMetadata metadata = new DefaultQueryMetadata();
    
    private transient String queryString;
    
    public SQLUpdateClause(Connection connection, SQLTemplates templates, RelationalPath<?> entity) {
        this(connection, new Configuration(templates), entity);
    }
    
    public SQLUpdateClause(Connection connection, Configuration configuration, RelationalPath<?> entity) {
        super(configuration);
        this.connection = Assert.notNull(connection,"connection");
        this.entity = Assert.notNull(entity,"entity");
        metadata.addJoin(JoinType.DEFAULT, entity);
    }
    
    /**
     * Add the given String literal at the given position as a query flag
     * 
     * @param position
     * @param flag
     * @return
     */
    public SQLUpdateClause addFlag(Position position, String flag) {
        metadata.addFlag(new QueryFlag(position, flag));
        return this;
    }
    
    /**
     * Add the given Expression at the given position as a query flag
     *
     * @param position
     * @param flag
     * @return
     */
    public SQLUpdateClause addFlag(Position position, Expression<?> flag) {
        metadata.addFlag(new QueryFlag(position, flag));
        return this;
    }
    
    /**
     * Add the current state of bindings as a batch item
     * 
     * @return
     */
    public SQLUpdateClause addBatch() {
        batchUpdates.add(updates);
        batchMetadata.add(metadata);
        updates = new ArrayList<Pair<Path<?>,Expression<?>>>();
        metadata = new DefaultQueryMetadata();
        metadata.addJoin(JoinType.DEFAULT, entity);
        return this;
    }

    private PreparedStatement createStatement() throws SQLException{
        PreparedStatement stmt;
        if (batchUpdates.isEmpty()) {
            SQLSerializer serializer = new SQLSerializer(configuration.getTemplates(), true);
            serializer.serializeForUpdate(metadata, entity, updates);
            queryString = serializer.toString();
            logger.debug(queryString);
            stmt = connection.prepareStatement(queryString);
            setParameters(stmt, serializer.getConstants(), serializer.getConstantPaths(), 
                    Collections.<Param<?>,Object>emptyMap());
        } else {
            SQLSerializer serializer = new SQLSerializer(configuration.getTemplates(), true);
            serializer.serializeForUpdate(batchMetadata.get(0), entity, batchUpdates.get(0));
            queryString = serializer.toString();
            logger.debug(queryString);
            
            // add first batch
            stmt = connection.prepareStatement(queryString);
            setParameters(stmt, serializer.getConstants(), serializer.getConstantPaths(), 
                    Collections.<Param<?>,Object>emptyMap());
            stmt.addBatch();
            
            // add other batches
            for (int i = 1; i < batchUpdates.size(); i++) {
                serializer = new SQLSerializer(configuration.getTemplates(), true, true);
                serializer.serializeForUpdate(batchMetadata.get(i), entity, batchUpdates.get(i));
                setParameters(stmt, serializer.getConstants(), serializer.getConstantPaths(), 
                        Collections.<Param<?>,Object>emptyMap());
                stmt.addBatch();
            }
        }
        return stmt;
    }
    
    @Override
    public long execute() {
        PreparedStatement stmt = null;
        try {
            stmt = createStatement();
            if (batchUpdates.isEmpty()) {
                return stmt.executeUpdate();    
            } else {
                return executeBatch(stmt);
            }  
        } catch (SQLException e) {
            throw new QueryException("Caught " + e.getClass().getSimpleName() + " for " + queryString, e);
        } finally {
            if (stmt != null) {
                close(stmt);
            }
        }
    }
    
    @Override
    public <T> SQLUpdateClause set(Path<T> path, T value) {
        if (value instanceof Expression<?>) {
            updates.add(Pair.<Path<?>,Expression<?>>of(path, (Expression<?>)value));
        }else if (value != null) {
            updates.add(Pair.<Path<?>,Expression<?>>of(path, new ConstantImpl<Object>(value)));
        } else {
            updates.add(Pair.<Path<?>,Expression<?>>of(path, Null.CONSTANT));
        }
        return this;
    }
    
    @Override
    public <T> SQLUpdateClause set(Path<T> path, Expression<? extends T> expression) {
        updates.add(Pair.<Path<?>,Expression<?>>of(path, expression));
        return this;
    }
    
    @Override
    public <T> SQLUpdateClause setNull(Path<T> path) {
        updates.add(Pair.<Path<?>,Expression<?>>of(path, Null.CONSTANT));
        return this;
    }

    @java.lang.SuppressWarnings("unchecked")
    @Override
    public SQLUpdateClause set(List<? extends Path<?>> paths, List<?> values) {
        for (int i = 0; i < paths.size(); i++) {
            if (values.get(i) instanceof Expression) {
                updates.add(Pair.<Path<?>,Expression<?>>of(paths.get(i), (Expression<?>)values.get(i)));
            }else if (values.get(i) != null) {
                updates.add(Pair.<Path<?>,Expression<?>>of(paths.get(i), new ConstantImpl<Object>(values.get(i))));
            } else {
                updates.add(Pair.<Path<?>,Expression<?>>of(paths.get(i), Null.CONSTANT));
            }
        }
        return this;
    }

    @Override
    public SQLUpdateClause where(Predicate... o) {
        metadata.addWhere(o);
        return this;
    }

    @Override
    public String toString() {
        SQLSerializer serializer = new SQLSerializer(configuration.getTemplates(), true);
        serializer.serializeForUpdate(metadata, entity, updates);
        return serializer.toString();
    }

    /**
     * Populate the UPDATE clause with the properties of the given bean.
     * The properties need to match the fields of the clause's entity instance.
     * Primary key columns are skipped in the population.
     * 
     * @param bean
     * @return
     */
    @SuppressWarnings("unchecked")
    public SQLUpdateClause populate(Object bean) {
        return populate(bean, DefaultMapper.DEFAULT);
    }
    
    /**
     * Populate the UPDATE clause with the properties of the given bean using the given Mapper.
     * 
     * @param obj
     * @param mapper
     * @return
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <T> SQLUpdateClause populate(T obj, Mapper<T> mapper) {
        Collection<? extends Path<?>> primaryKeyColumns = entity.getPrimaryKey() != null 
                ? entity.getPrimaryKey().getLocalColumns() 
                : Collections.<Path<?>>emptyList();
        Map<Path<?>, Object> values = mapper.createMap(entity, obj);
        for (Map.Entry<Path<?>, Object> entry : values.entrySet()) {
            if (!primaryKeyColumns.contains(entry.getKey())) {
                set((Path)entry.getKey(), entry.getValue());    
            }            
        }        
        return this;
    }
}
