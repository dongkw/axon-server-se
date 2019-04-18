package io.axoniq.axonserver.enterprise.config;

import io.axoniq.axonserver.licensing.Feature;
import io.axoniq.axonserver.config.FeatureChecker;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * @author Marc Gathier
 */
public class ClusteringAllowed implements Condition {

    @Override
    public boolean matches(ConditionContext conditionContext, AnnotatedTypeMetadata annotatedTypeMetadata) {
        FeatureChecker limits = conditionContext.getBeanFactory().getBean(FeatureChecker.class);
        return Feature.CLUSTERING.enabled(limits);
    }


}
