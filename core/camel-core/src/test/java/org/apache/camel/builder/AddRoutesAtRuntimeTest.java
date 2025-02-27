/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.builder;

import java.lang.reflect.Method;
import java.util.List;

import org.apache.camel.CamelContext;
import org.apache.camel.ContextTestSupport;
import org.apache.camel.impl.engine.AbstractCamelContext;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Test how to add routes at runtime using a RouteBuilder
 */
public class AddRoutesAtRuntimeTest extends ContextTestSupport {

    @Test
    public void testAddRoutesAtRuntime() throws Exception {
        getMockEndpoint("mock:start").expectedMessageCount(1);
        template.sendBody("direct:start", "Hello World");
        assertMockEndpointsSatisfied();
        assertEquals(1, context.getRoutes().size());

        getMockEndpoint("mock:foo").expectedMessageCount(1);
        context.addRoutes(new MyDynamcRouteBuilder(context, "direct:foo", "mock:foo"));
        template.sendBody("direct:foo", "Bye Camel");
        assertMockEndpointsSatisfied();
        assertEquals(2, context.getRoutes().size());

        // use reflection to test that we do not leak bootstraps when dynamic adding routes
        Method m = AbstractCamelContext.class.getDeclaredMethod("getBootstraps");
        m.setAccessible(true);
        Assertions.assertEquals(0, ((List) m.invoke(context)).size());

        getMockEndpoint("mock:bar").expectedMessageCount(1);
        context.addRoutes(new MyDynamcRouteBuilder(context, "direct:bar", "mock:bar"));
        template.sendBody("direct:bar", "Hi Camel");
        assertMockEndpointsSatisfied();
        assertEquals(3, context.getRoutes().size());

        // use reflection to test that we do not leak bootstraps when dynamic adding routes
        Assertions.assertEquals(0, ((List) m.invoke(context)).size());
    }

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            @Override
            public void configure() throws Exception {
                // here is an existing route
                from("direct:start").to("mock:start");
            }
        };
    }

    /**
     * This route builder is a skeleton to add new routes at runtime
     */
    private static final class MyDynamcRouteBuilder extends RouteBuilder {
        private final String from;
        private final String to;

        private MyDynamcRouteBuilder(CamelContext context, String from, String to) {
            super(context);
            this.from = from;
            this.to = to;
        }

        @Override
        public void configure() throws Exception {
            from(from).to(to);
        }
    }
}
