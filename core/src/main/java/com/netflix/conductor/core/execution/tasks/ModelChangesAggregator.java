/*
 * Copyright 2023 Netflix, Inc.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package com.netflix.conductor.core.execution.tasks;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.netflix.conductor.core.config.ConductorProperties;
import com.netflix.conductor.core.dal.ExecutionDAOFacade;
import com.netflix.conductor.core.exception.NonTransientException;
import com.netflix.conductor.core.execution.WorkflowExecutor;
import com.netflix.conductor.model.TaskModel;
import com.netflix.conductor.model.WorkflowModel;

import com.fasterxml.jackson.databind.ObjectMapper;

import static com.netflix.conductor.common.metadata.tasks.TaskType.TASK_TYPE_MODEL_CHANGES_AGGREGATOR;

@Component(TASK_TYPE_MODEL_CHANGES_AGGREGATOR)
public class ModelChangesAggregator extends WorkflowSystemTask {

    private static final Logger LOGGER = LoggerFactory.getLogger(ModelChangesAggregator.class);
    private static final String AGGREGATOR_KEY = "aggregatorKey";

    private final ConductorProperties properties;
    private final ObjectMapper objectMapper;

    private final ExecutionDAOFacade executionDAOFacade;

    public ModelChangesAggregator(
            ConductorProperties properties,
            ObjectMapper objectMapper,
            ExecutionDAOFacade executionDAOFacade) {
        super(TASK_TYPE_MODEL_CHANGES_AGGREGATOR);
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.executionDAOFacade = executionDAOFacade;
    }

    private boolean validateVariablesSize(
            WorkflowModel workflow, TaskModel task, Map<String, Object> variables) {
        String workflowId = workflow.getWorkflowId();
        long maxThreshold = properties.getMaxWorkflowVariablesPayloadSizeThreshold().toKilobytes();

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream()) {
            this.objectMapper.writeValue(byteArrayOutputStream, variables);
            byte[] payloadBytes = byteArrayOutputStream.toByteArray();
            long payloadSize = payloadBytes.length;

            if (payloadSize > maxThreshold * 1024) {
                String errorMsg =
                        String.format(
                                "The variables payload size: %d of workflow: %s is greater than the permissible limit: %d bytes",
                                payloadSize, workflowId, maxThreshold);
                LOGGER.error(errorMsg);
                task.setReasonForIncompletion(errorMsg);
                return false;
            }
            return true;
        } catch (IOException e) {
            LOGGER.error(
                    "Unable to validate variables payload size of workflow: {}", workflowId, e);
            throw new NonTransientException(
                    "Unable to validate variables payload size of workflow: " + workflowId, e);
        }
    }

    @Override
    public boolean execute(WorkflowModel workflow, TaskModel task, WorkflowExecutor provider) {
        Map<String, Object> variables = workflow.getVariables();
        Map<String, Object> input = task.getInputData();
        String taskId = task.getTaskId();
        ArrayList<String> newKeys;
        Map<String, Object> previousValues;
        String aggregatorKey = (String) input.remove(AGGREGATOR_KEY);
        if (aggregatorKey == null) {
            task.setStatus(TaskModel.Status.FAILED_WITH_TERMINAL_ERROR);
            return true;
        }

        Map<String, Object> aggregatorObject =
                variables.get(aggregatorKey) == null
                        ? new HashMap<>()
                        : (Map<String, Object>) variables.get(aggregatorKey);
        ArrayList<String> newKeysToAggregatorObject;

        if (input != null && input.size() > 0) {
            newKeys = new ArrayList<>();
            newKeysToAggregatorObject = new ArrayList<>();
            previousValues = new HashMap<>();

            input.keySet()
                    .forEach(
                            key -> {
                                if (variables.containsKey(key)) {
                                    previousValues.put(key, aggregatorObject.get(key));
                                    if (!aggregatorObject.containsKey(key)) {
                                        newKeysToAggregatorObject.add(key);
                                    }
                                } else {
                                    newKeys.add(key);
                                }
                                variables.put(key, input.get(key));
                                aggregatorObject.put(key, input.get(key));
                                LOGGER.debug(
                                        "Task: {} setting value for variable: {}", taskId, key);
                            });
            if (!validateVariablesSize(workflow, task, aggregatorObject)) {
                // restore previous variables
                previousValues
                        .keySet()
                        .forEach(
                                key -> {
                                    variables.put(key, previousValues.get(key));
                                    if (!newKeysToAggregatorObject.contains(key))
                                        aggregatorObject.put(key, previousValues.get(key));
                                });
                newKeys.forEach(
                        newKey -> {
                            variables.remove(newKey);
                            aggregatorObject.remove(newKey);
                        });
                task.setStatus(TaskModel.Status.FAILED_WITH_TERMINAL_ERROR);
                return true;
            }
        }

        variables.put(aggregatorKey, aggregatorObject);
        task.setStatus(TaskModel.Status.COMPLETED);
        executionDAOFacade.updateWorkflow(workflow);
        return true;
    }
}
