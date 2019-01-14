package io.axoniq.axonserver.localstorage.query.expressions.functions;

import io.axoniq.axonserver.localstorage.query.*;

import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author Marc Gathier
 */
public class LimitExpression implements PipeExpression {

    private final AtomicLong remaining;
    private final long limit;

    public LimitExpression( Expression expression) {
        Number number = (Number) expression.apply(null, null).getValue();
        this.limit = number.longValue();
        this.remaining = new AtomicLong(limit);

    }

    @Override
    public List<String> getColumnNames(List<String> inputColumns) {
        return inputColumns;
    }

    @Override
    public boolean process(ExpressionContext context, QueryResult result, Pipeline next) {
        if (result.getId() != null)  {
            Set<ExpressionResult> results = context.scoped(this).computeIfAbsent("results", () -> new ConcurrentHashMap<ExpressionResult, Object>().keySet(new Object()));
            if( result.isDeleted()) {
                results.remove(result.getId());
            } else {
                results.add(result.getId());
            }

            return results.size() <= limit &&  next.process(result);
        }
        return remaining.getAndDecrement() > 0 && next.process(result);
    }

}
