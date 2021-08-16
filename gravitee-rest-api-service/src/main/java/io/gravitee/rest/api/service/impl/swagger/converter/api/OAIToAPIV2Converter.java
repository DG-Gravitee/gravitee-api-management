/**
 * Copyright (C) 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.rest.api.service.impl.swagger.converter.api;

import static io.gravitee.rest.api.service.validator.PolicyHelper.clearNullValues;
import static io.gravitee.rest.api.service.validator.PolicyHelper.getScope;
import static java.util.Collections.singleton;

import io.gravitee.common.http.HttpMethod;
import io.gravitee.definition.model.DefinitionVersion;
import io.gravitee.definition.model.flow.Flow;
import io.gravitee.definition.model.flow.Operator;
import io.gravitee.definition.model.flow.Step;
import io.gravitee.policy.api.swagger.Policy;
import io.gravitee.rest.api.model.ImportSwaggerDescriptorEntity;
import io.gravitee.rest.api.model.api.SwaggerApiEntity;
import io.gravitee.rest.api.service.GroupService;
import io.gravitee.rest.api.service.TagService;
import io.gravitee.rest.api.service.impl.swagger.policy.PolicyOperationVisitorManager;
import io.gravitee.rest.api.service.impl.swagger.visitor.v3.OAIOperationVisitor;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

/**
 * @author Guillaume CUSNIEUX (guillaume.cusnieux at graviteesource.com)
 * @author GraviteeSource Team
 */
public class OAIToAPIV2Converter extends OAIToAPIConverter {

    public OAIToAPIV2Converter(
        ImportSwaggerDescriptorEntity swaggerDescriptor,
        PolicyOperationVisitorManager policyOperationVisitorManager,
        GroupService groupService,
        TagService tagService
    ) {
        super(swaggerDescriptor, policyOperationVisitorManager, groupService, tagService);
    }

    @Override
    protected SwaggerApiEntity fill(SwaggerApiEntity apiEntity, OpenAPI oai) {
        // flows
        apiEntity.setGraviteeDefinitionVersion(DefinitionVersion.V2.getLabel());

        if (swaggerDescriptor.isWithPolicyPaths()) {
            List<Flow> allFlows = new ArrayList();
            oai
                .getPaths()
                .forEach(
                    (key, pathItem) -> {
                        String path = key.replaceAll("\\{(.[^/\\}]*)\\}", ":$1");

                        Map<PathItem.HttpMethod, Operation> operations = pathItem.readOperationsMap();
                        operations.forEach(
                            (httpMethod, operation) -> {
                                final Flow flow = createFlow(path, Collections.singleton(HttpMethod.valueOf(httpMethod.name())));

                                getVisitors()
                                    .forEach(
                                        (Consumer<OAIOperationVisitor>) oaiOperationVisitor -> {
                                            Optional<Policy> policy = (Optional<Policy>) oaiOperationVisitor.visit(oai, operation);
                                            if (policy.isPresent()) {
                                                final Step step = new Step();
                                                step.setName(policy.get().getName());
                                                step.setEnabled(true);
                                                step.setDescription(
                                                    operation.getSummary() == null
                                                        ? (
                                                            operation.getOperationId() == null
                                                                ? operation.getDescription()
                                                                : operation.getOperationId()
                                                        )
                                                        : operation.getSummary()
                                                );

                                                step.setPolicy(policy.get().getName());
                                                String configuration = clearNullValues(policy.get().getConfiguration());
                                                step.setConfiguration(configuration);

                                                String scope = getScope(configuration);
                                                if (scope != null && scope.toLowerCase().equals("response")) {
                                                    flow.getPost().add(step);
                                                } else {
                                                    flow.getPre().add(step);
                                                }
                                            }
                                        }
                                    );
                                allFlows.add(flow);
                            }
                        );
                    }
                );
            apiEntity.setFlows(allFlows);
            if (swaggerDescriptor.isWithPathMapping() && allFlows.size() > 0) {
                apiEntity.setPathMappings(allFlows.stream().map(flow -> flow.getPath()).collect(Collectors.toSet()));
            }
        }

        final String defaultDeclaredPath = "/";
        if (!swaggerDescriptor.isWithPathMapping()) {
            apiEntity.setPathMappings(singleton(defaultDeclaredPath));
        }

        return apiEntity;
    }

    private Flow createFlow(String path, Set<HttpMethod> methods) {
        final Flow flow = new Flow();
        flow.setName("");
        flow.setCondition("");
        flow.setEnabled(true);
        flow.setPath(path);
        flow.setOperator(Operator.EQUALS);
        flow.setMethods(methods);
        return flow;
    }
}
