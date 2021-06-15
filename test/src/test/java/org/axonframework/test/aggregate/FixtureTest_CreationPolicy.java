/*
 * Copyright (c) 2010-2021. Axon Framework
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.axonframework.test.aggregate;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateCreationPolicy;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.CommandHandlerInterceptor;
import org.axonframework.modelling.command.CreationPolicy;
import org.axonframework.modelling.command.TargetAggregateIdentifier;
import org.junit.jupiter.api.*;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.axonframework.modelling.command.AggregateLifecycle.apply;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Fixture tests for validating {@link CreationPolicy} annotated command handlers.
 *
 * @author Marc Gathier
 * @author Steven van Beelen
 */
class FixtureTest_CreationPolicy {

    private static final ComplexAggregateId AGGREGATE_ID = new ComplexAggregateId(UUID.randomUUID(), 42);

    private FixtureConfiguration<TestAggregate> fixture;

    private static AtomicBoolean intercepted;

    @BeforeEach
    void setUp() {
        fixture = new AggregateTestFixture<>(TestAggregate.class);

        intercepted = new AtomicBoolean(false);
    }

    @Test
    void testCreateOrUpdatePolicyForNewInstance() {
        fixture.givenNoPriorActivity()
               .when(new CreateOrUpdateCommand(AGGREGATE_ID))
               .expectEvents(new CreatedOrUpdatedEvent(AGGREGATE_ID))
               .expectSuccessfulHandlerExecution();
        assertTrue(intercepted.get());
    }

    @Test
    void testCreateOrUpdatePolicyForExistingInstance() {
        fixture.given(new CreatedEvent(AGGREGATE_ID))
               .when(new CreateOrUpdateCommand(AGGREGATE_ID))
               .expectEvents(new CreatedOrUpdatedEvent(AGGREGATE_ID))
               .expectSuccessfulHandlerExecution();
        assertTrue(intercepted.get());
    }

    @Test
    void testAlwaysCreatePolicyWithoutResultReturnsAggregateId() {
        fixture.givenNoPriorActivity()
               .when(new AlwaysCreateWithoutResultCommand(AGGREGATE_ID))
               .expectEvents(new AlwaysCreatedEvent(AGGREGATE_ID))
               .expectResultMessagePayload(AGGREGATE_ID)
               .expectSuccessfulHandlerExecution();
        assertTrue(intercepted.get());
    }

    @Test
    void testAlwaysCreatePolicyWithResultReturnsCommandHandlingResult() {
        Object testResult = "some-result";
        fixture.givenNoPriorActivity()
               .when(new AlwaysCreateWithResultCommand(AGGREGATE_ID, testResult))
               .expectEvents(new AlwaysCreatedEvent(AGGREGATE_ID))
               .expectResultMessagePayload(testResult)
               .expectSuccessfulHandlerExecution();
        assertTrue(intercepted.get());
    }

    @Test
    void testAlwaysCreatePolicyWithResultReturnsNullCommandHandlingResult() {
        fixture.givenNoPriorActivity()
               .when(new AlwaysCreateWithResultCommand(AGGREGATE_ID, null))
               .expectEvents(new AlwaysCreatedEvent(AGGREGATE_ID))
               .expectResultMessagePayload(null)
               .expectSuccessfulHandlerExecution();
        assertTrue(intercepted.get());
    }

    @Test
    void testNeverCreatePolicy() {
        fixture.given(new CreatedEvent(AGGREGATE_ID))
               .when(new ExecuteOnExistingCommand(AGGREGATE_ID))
               .expectEvents(new ExecutedOnExistingEvent(AGGREGATE_ID))
               .expectSuccessfulHandlerExecution();
        assertTrue(intercepted.get());
    }

    @Test
    void testAlwaysCreatePolicyWithStateReturnsStateInCommandHandlingResult() {
        fixture.givenNoPriorActivity()
               .when(new AlwaysCreateWithEventSourcedResultCommand(AGGREGATE_ID))
               .expectEvents(new AlwaysCreatedEvent(AGGREGATE_ID))
               .expectResultMessagePayload(AGGREGATE_ID)
               .expectSuccessfulHandlerExecution();
        assertTrue(intercepted.get());
    }

    private static class CreateCommand {

        @TargetAggregateIdentifier
        private final ComplexAggregateId id;

        private CreateCommand(ComplexAggregateId id) {
            this.id = id;
        }

        public ComplexAggregateId getId() {
            return id;
        }
    }

    private static class CreateOrUpdateCommand {

        @TargetAggregateIdentifier
        private final ComplexAggregateId id;

        private CreateOrUpdateCommand(ComplexAggregateId id) {
            this.id = id;
        }

        public ComplexAggregateId getId() {
            return id;
        }
    }

    private static class AlwaysCreateWithoutResultCommand {

        @TargetAggregateIdentifier
        private final ComplexAggregateId id;

        private AlwaysCreateWithoutResultCommand(ComplexAggregateId id) {
            this.id = id;
        }

        public ComplexAggregateId getId() {
            return id;
        }
    }

    private static class AlwaysCreateWithResultCommand {

        @TargetAggregateIdentifier
        private final ComplexAggregateId id;
        private final Object result;

        private AlwaysCreateWithResultCommand(ComplexAggregateId id, Object result) {
            this.id = id;
            this.result = result;
        }

        public ComplexAggregateId getId() {
            return id;
        }

        public Object getResult() {
            return result;
        }
    }

    private static class AlwaysCreateWithEventSourcedResultCommand {

        @TargetAggregateIdentifier
        private final ComplexAggregateId id;

        private AlwaysCreateWithEventSourcedResultCommand(ComplexAggregateId id) {
            this.id = id;
        }

        public ComplexAggregateId getId() {
            return id;
        }
    }

    private static class ExecuteOnExistingCommand {

        @TargetAggregateIdentifier
        private final ComplexAggregateId id;

        private ExecuteOnExistingCommand(ComplexAggregateId id) {
            this.id = id;
        }

        public ComplexAggregateId getId() {
            return id;
        }
    }

    private static class CreatedEvent {

        private final ComplexAggregateId id;

        private CreatedEvent(ComplexAggregateId id) {
            this.id = id;
        }

        public ComplexAggregateId getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CreatedEvent that = (CreatedEvent) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    private static class CreatedOrUpdatedEvent {

        private final ComplexAggregateId id;

        private CreatedOrUpdatedEvent(ComplexAggregateId id) {
            this.id = id;
        }

        public ComplexAggregateId getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            CreatedOrUpdatedEvent that = (CreatedOrUpdatedEvent) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    private static class AlwaysCreatedEvent {

        private final ComplexAggregateId id;

        private AlwaysCreatedEvent(ComplexAggregateId id) {
            this.id = id;
        }

        public ComplexAggregateId getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AlwaysCreatedEvent that = (AlwaysCreatedEvent) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    private static class ExecutedOnExistingEvent {

        private final ComplexAggregateId id;

        private ExecutedOnExistingEvent(ComplexAggregateId id) {
            this.id = id;
        }

        public ComplexAggregateId getId() {
            return id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            ExecutedOnExistingEvent that = (ExecutedOnExistingEvent) o;
            return Objects.equals(id, that.id);
        }

        @Override
        public int hashCode() {
            return Objects.hash(id);
        }
    }

    @SuppressWarnings("unused")
    public static class TestAggregate {

        @AggregateIdentifier
        private ComplexAggregateId id;

        public TestAggregate() {
        }

        @CommandHandlerInterceptor
        public void intercept(Object command) {
            intercepted.set(true);
        }

        @CommandHandler
        public TestAggregate(CreateCommand command) {
            apply(new CreatedEvent(command.getId()));
        }

        @CommandHandler
        @CreationPolicy(AggregateCreationPolicy.CREATE_IF_MISSING)
        public void handle(CreateOrUpdateCommand command) {
            apply(new CreatedOrUpdatedEvent(command.getId()));
        }

        @CommandHandler
        @CreationPolicy(AggregateCreationPolicy.ALWAYS)
        public void handle(AlwaysCreateWithoutResultCommand command) {
            apply(new AlwaysCreatedEvent(command.getId()));
        }

        @CommandHandler
        @CreationPolicy(AggregateCreationPolicy.ALWAYS)
        public Object handle(AlwaysCreateWithResultCommand command) {
            apply(new AlwaysCreatedEvent(command.getId()));
            return command.getResult();
        }

        @CommandHandler
        @CreationPolicy(AggregateCreationPolicy.ALWAYS)
        public ComplexAggregateId handle(AlwaysCreateWithEventSourcedResultCommand command) {
            apply(new AlwaysCreatedEvent(command.getId()));
            // On apply, the event sourcing handlers should be invoked first.
            // Hence, we should be able to return the identifier of the aggregate directly.
            return id;
        }

        @CommandHandler
        @CreationPolicy(AggregateCreationPolicy.NEVER)
        public void handle(ExecuteOnExistingCommand command) {
            apply(new ExecutedOnExistingEvent(command.getId()));
        }

        @EventSourcingHandler
        public void on(CreatedEvent event) {
            this.id = event.getId();
        }

        @EventSourcingHandler
        public void on(CreatedOrUpdatedEvent event) {
            this.id = event.getId();
        }

        @EventSourcingHandler
        public void on(AlwaysCreatedEvent event) {
            this.id = event.getId();
        }
    }

    /**
     * Test id introduces due too https://github.com/AxonFramework/AxonFramework/pull/1356
     */
    private static class ComplexAggregateId {

        private final UUID actualId;
        @SuppressWarnings({"FieldCanBeLocal", "unused"})
        private final Integer someOtherField;

        private ComplexAggregateId(UUID actualId, Integer someOtherField) {
            this.actualId = actualId;
            this.someOtherField = someOtherField;
        }

        @Override
        public String toString() {
            return actualId.toString();
        }
    }
}
