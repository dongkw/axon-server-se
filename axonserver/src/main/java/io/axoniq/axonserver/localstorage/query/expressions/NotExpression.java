package io.axoniq.axonserver.localstorage.query.expressions;

import io.axoniq.axonserver.localstorage.query.Expression;
import io.axoniq.axonserver.localstorage.query.ExpressionContext;
import io.axoniq.axonserver.localstorage.query.ExpressionResult;
import io.axoniq.axonserver.localstorage.query.PipeExpression;
import io.axoniq.axonserver.localstorage.query.Pipeline;
import io.axoniq.axonserver.localstorage.query.QueryResult;
import io.axoniq.axonserver.localstorage.query.result.BooleanExpressionResult;

/**
 * Author: marc
 */
public class NotExpression implements Expression, PipeExpression {

    private final String alias;
    private final Expression[] parameters;

    public NotExpression(String alias, Expression[] parameters) {
        this.alias = alias;
        this.parameters = parameters;
    }

    @Override
    public ExpressionResult apply(ExpressionContext context, ExpressionResult input) {
        Expression parameter = parameters[0];
        return parameter.apply(context, input).isTrue() ? BooleanExpressionResult.FALSE : BooleanExpressionResult.TRUE;
    }

    @Override
    public String alias() {
        return alias;
    }

    @Override
    public boolean process(ExpressionContext context, QueryResult value, Pipeline next) {
        if (apply(context, value.getValue()).isTrue()) {
            return next.process(value);
        }
        return true;
    }
}