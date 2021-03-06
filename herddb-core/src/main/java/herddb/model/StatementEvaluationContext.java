/*
 Licensed to Diennea S.r.l. under one
 or more contributor license agreements. See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership. Diennea S.r.l. licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.

 */
package herddb.model;

import herddb.core.DBManager;
import herddb.sql.TranslatedQuery;
import herddb.utils.DataAccessor;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;

/**
 * Context for each statement evaluation. Statements are immutable and cachable objects, and cannot retain state
 *
 * @author enrico.olivelli
 */
public class StatementEvaluationContext {

    private static final Logger LOGGER = Logger.getLogger(StatementEvaluationContext.class.getName());

    private DBManager manager;
    private TransactionContext transactionContext;
    private final Map<String, List<DataAccessor>> subqueryCache = new HashMap<>();
    private String defaultTablespace = TableSpace.DEFAULT;

    public static StatementEvaluationContext DEFAULT_EVALUATION_CONTEXT() {
        return new StatementEvaluationContext();
    }

    public String getDefaultTablespace() {
        return defaultTablespace;
    }

    public void setDefaultTablespace(String defaultTablespace) {
        this.defaultTablespace = defaultTablespace;
    }

    public TransactionContext getTransactionContext() {
        return transactionContext;
    }

    public void setTransactionContext(TransactionContext transactionContext) {
        this.transactionContext = transactionContext;
    }

    public DBManager getManager() {
        return manager;
    }

    public void setManager(DBManager manager) {
        this.manager = manager;
    }

    public List<Object> getJdbcParameters() {
        return Collections.emptyList();
    }

    public List<DataAccessor> executeSubquery(PlainSelect select) throws StatementExecutionException {
        StringBuilder buffer = new StringBuilder();
        SelectDeParser deparser = new SelectDeParser();
        deparser.setBuffer(buffer);
        deparser.setExpressionVisitor(new ExpressionDeParser(deparser, buffer));
        deparser.visit(select);
        String subquery = deparser.getBuffer().toString();
        List<DataAccessor> cached = subqueryCache.get(subquery);
        if (cached != null) {
            return cached;
        }
//        LOGGER.log(Level.SEVERE, "executing subquery " + subquery);
        TranslatedQuery translated = manager.getPlanner().translate(defaultTablespace,
            subquery, Collections.emptyList(), true, true, false, -1);
        try (ScanResult result = (ScanResult) manager.executePlan(translated.plan, translated.context, transactionContext);) {
            List<DataAccessor> fullResult = result.dataScanner.consume();
//            LOGGER.log(Level.SEVERE, "executing subquery " + subquery+" -> "+fullResult);
            subqueryCache.put(subquery, fullResult);
            return fullResult;
        } catch (DataScannerException error) {
            throw new StatementExecutionException(error);
        }

    }

}
