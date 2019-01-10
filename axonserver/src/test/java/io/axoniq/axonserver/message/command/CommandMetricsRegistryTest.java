package io.axoniq.axonserver.message.command;

import io.axoniq.axonserver.message.ClientIdentification;
import io.axoniq.axonserver.metric.DefaultMetricCollector;
import io.axoniq.axonserver.topology.Topology;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.runners.*;

import static org.junit.Assert.*;

/**
 * Author: marc
 */
@RunWith(MockitoJUnitRunner.class)
public class CommandMetricsRegistryTest {

    private CommandMetricsRegistry testSubject;

    @Before
    public void setUp() {
        testSubject = new CommandMetricsRegistry(new SimpleMeterRegistry(), new DefaultMetricCollector());
    }

    @Test
    public void add() {
        testSubject.add("Command", new ClientIdentification(Topology.DEFAULT_CONTEXT,"Client1"), 1);

        assertEquals(1L, testSubject.commandMetric("Command", "default/Client1", null).getCount());
    }

}