/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ct42.fnflow.manager;

import io.fabric8.kubernetes.api.model.*;
import io.fabric8.kubernetes.api.model.apps.Deployment;
import io.fabric8.kubernetes.api.model.apps.DeploymentBuilder;
import io.fabric8.kubernetes.api.model.apps.DeploymentCondition;
import io.fabric8.kubernetes.api.model.apps.DeploymentStatus;
import io.fabric8.kubernetes.client.KubernetesClient;
import lombok.RequiredArgsConstructor;
import org.ct42.fnflow.manager.config.ManagerProperties;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author Claas Thiele
 * @author Sajjad Safaeian
 */
@Service
@RequiredArgsConstructor
public class PipelineService {
    private static final String IMAGE="docker.io/ct42/fnflow-json-processors-kafka";
    private static final String NAME="fnflow-json-processors-kafka";
    private static final String NAMESPACE="default";
    private static final String PROCESSOR_PREFIX="proc-";

    private final KubernetesClient k8sClient;
    private final ManagerProperties cfgProps;

    public void createOrUpdatePipeline(String name, PipelineConfigDTO cfg) {
        List<String> args = new ArrayList<>();
        cfg.getPipeline().forEach(f -> {
            if(f instanceof PipelineConfigDTO.SingleFunction singleFunction) {
                prepareFunctionArgs(singleFunction.getFunction(), args);
            } else if(f instanceof PipelineConfigDTO.MultipleFunctions multipleFunctions) {
                multipleFunctions.getFunctions().forEach(functionCfg -> prepareFunctionArgs(functionCfg, args));
            }
        });

        String definition =
                cfg.getPipeline().stream()
                .map(function -> {
                    return switch (function) {
                        case PipelineConfigDTO.SingleFunction singleFunction ->
                                singleFunction.getFunction().getName();
                        case PipelineConfigDTO.MultipleFunctions multipleFunctions ->
                                multipleFunctions.getFunctions().stream()
                                        .map(PipelineConfigDTO.FunctionCfg::getName)
                                        .collect(Collectors.joining("+"));
                        default -> throw new IllegalStateException("Unexpected value: " + function);
                    };
                }).collect(Collectors.joining("|"));
        args.add("--org.ct42.fnflow.function.definition=" + definition);
        args.add("--spring.cloud.stream.kafka.default.producer.compression-type=lz4");
        args.add("--spring.cloud.stream.kafka.default.producer.configuration.batch.size=131072");
        args.add("--spring.cloud.stream.kafka.default.producer.configuration.linger.ms=50");
        args.add("--spring.cloud.stream.default.group=" + name);
        args.add("--spring.cloud.stream.bindings.fnFlowComposedFnBean-in-0.destination=" + cfg.getSourceTopic());
        args.add("--spring.cloud.stream.bindings.fnFlowComposedFnBean-out-0.destination=" + cfg.getEntityTopic());
        args.add("--spring.cloud.stream.bindings.fnFlowComposedFnBean-out-1.destination=" + cfg.getErrorTopic());
        args.add("--spring.cloud.stream.kafka.binder.autoAlterTopics=true");
        args.add("--spring.cloud.stream.kafka.bindings.fnFlowComposedFnBean-out-1.producer.topic.properties.retention.ms="
                + convertHoursToMilliseconds(cfg.getErrRetentionHours()));


        //TODO construct args for kafka batch and compression settings and group and destinations


        Map<String, String> selectorLabels = Map.of(
                "app.kubernetes.io/name", NAME,
                "app.kubernetes.io/instance", name
        );

        k8sClient.apps().deployments().inNamespace(NAMESPACE)
                .resource(new DeploymentBuilder().withNewMetadata()
                            .withName(PROCESSOR_PREFIX + name)
                            .withNamespace(NAMESPACE)
                            .withLabels(selectorLabels)
                        .endMetadata()
                        .withNewSpec()
                        .withReplicas(1)
                        .withNewSelector()
                            .withMatchLabels(selectorLabels)
                        .endSelector()
                        .withNewTemplate()
                            .withNewMetadata()
                                .withLabels(selectorLabels)
                            .endMetadata()
                            .withNewSpec()
                                .withContainers(new ContainerBuilder().withName(name)
                                    .withImage(IMAGE + ":" + cfg.getVersion())
                                    .withImagePullPolicy("IfNotPresent")
                                    .withPorts(new ContainerPortBuilder()
                                            .withName("http")
                                            .withContainerPort(8080).build())
                                    .withEnv(new EnvVarBuilder()
                                                .withName("OPENSEARCH_URIS")
                                                .withValue(cfgProps.getOsUris()).build(),
                                            new EnvVarBuilder()
                                                .withName("SPRING_CLOUD_STREAM_KAFKA_BINDER_BROKERS")
                                                .withValue(cfgProps.getKafkaBrokers()).build())
                                    .withArgs(args).build())
                            .endSpec()
                        .endTemplate()
                        .endSpec().build())
                .forceConflicts().serverSideApply();
    }

    /**
     *
     * @param name of the pipeline the status should be taken for
     * @return the status or <ode>null</ode> if a deployment for the given name does not exist
     */
    public DeploymentStatusDTO getPipelineStatus(String name) throws DeploymentDoesNotExistException {
        Deployment deployment = k8sClient.apps().deployments().inNamespace(NAMESPACE)
                .withName(PROCESSOR_PREFIX + name).get();
        if(deployment == null) throw new DeploymentDoesNotExistException(name);

        DeploymentStatusDTO status = new DeploymentStatusDTO();
        DeploymentStatus deploymentStatus = deployment.getStatus();
        Integer specReplicas = deployment.getSpec().getReplicas();

        if(deploymentStatus.getObservedGeneration() != null) {
            Integer updatedReplicas = deploymentStatus.getUpdatedReplicas();
            Integer availableReplicas = deploymentStatus.getAvailableReplicas();
            Integer replicas = deploymentStatus.getReplicas();

            Optional<DeploymentCondition> progressing = deploymentStatus.getConditions().stream().filter(c -> c.getType().equals("Progressing")).findFirst();
            if(progressing.isPresent()) {
                if(progressing.get().getReason().equals("ProgressDeadlineExceeded")) {
                    status.setStatus(DeploymentStatusDTO.Status.FAILED);
                    status.setMessage(progressing.get().getMessage());
                    return status;
                }
            }
            if(specReplicas != null && Optional.ofNullable(updatedReplicas).orElse(0) < specReplicas) {
                status.setStatus(DeploymentStatusDTO.Status.PROGRESSING);
                status.setMessage(String.format("Waiting for rollout to finish: %d out of %d new replicas have been updated...", updatedReplicas, specReplicas));
                return status;
            }
            if(Optional.ofNullable(replicas).orElse(0) > Optional.ofNullable(updatedReplicas).orElse(0)) {
                status.setStatus(DeploymentStatusDTO.Status.PROGRESSING);
                status.setMessage(String.format("Waiting for rollout to finish: %d old replicas are pending termination...", replicas - updatedReplicas));
                return status;
            }
            if(availableReplicas != null && availableReplicas < Optional.ofNullable(updatedReplicas).orElse(0)) {
                status.setStatus(DeploymentStatusDTO.Status.PROGRESSING);
                status.setMessage(String.format("Waiting for rollout to finish: %d of %d updated replicas are available...", availableReplicas, updatedReplicas));
                return status;
            }
            status.setStatus(DeploymentStatusDTO.Status.COMPLETED);
            status.setMessage("Successfully rolled out");
            return status;
        }
        return status;
    }

    public void deletePipeline(String name) {
        k8sClient.apps().deployments().inNamespace(NAMESPACE)
                .withName(PROCESSOR_PREFIX + name).delete();
    }

    /**
     * Translates the kubernetes deployment object into a fnflow pipeline configuration.
     * Several missconfiguration situations are not handled well:
     * - orphaned functions (a function configured but not part of the function definition; such functions well be left out
     * - deployments with more than one container
     * - property array notation without []
     *
     * @param name of the pipeline the configuration should be taken for
     * @return the config of the pipeline with given name or <code>null</code> if the deployment with given name does not exist
     */
    public PipelineConfigDTO getPipelineConfig(String name) throws DeploymentDoesNotExistException {
        Deployment deployment = k8sClient.apps().deployments().inNamespace(NAMESPACE)
                .withName(PROCESSOR_PREFIX + name).get();
        if(deployment == null) throw new DeploymentDoesNotExistException(name);

        Container container = deployment.getSpec().getTemplate().getSpec().getContainers().getFirst();

        PipelineConfigDTO config = new PipelineConfigDTO();

        String image = container.getImage();
        config.setVersion(image.substring(image.lastIndexOf(":") + 1));

        AtomicReference<String> fnDef = new AtomicReference<>();
        List<String> pipelineCfg = new ArrayList<>();

        container.getArgs().forEach(arg -> {
           if(arg.startsWith("--spring.cloud.stream.bindings.fnFlowComposedFnBean-in-0.destination=")) {
               config.setSourceTopic(arg.substring(arg.lastIndexOf("=") + 1));
           } else if(arg.startsWith("--spring.cloud.stream.bindings.fnFlowComposedFnBean-out-0.destination=")) {
                config.setEntityTopic(arg.substring(arg.lastIndexOf("=") + 1));
           } else if(arg.startsWith("--spring.cloud.stream.bindings.fnFlowComposedFnBean-out-1.destination=")) {
               config.setErrorTopic(arg.substring(arg.lastIndexOf("=") + 1));
           } else if(arg.startsWith("--spring.cloud.stream.kafka.bindings.fnFlowComposedFnBean-out-1.producer.topic.properties.retention.ms=")) {
               long milliseconds = Long.parseLong(arg.substring(arg.lastIndexOf("=") + 1));
               config.setErrRetentionHours((int)(milliseconds / 3600000));
           } else if(arg.startsWith("--org.ct42.fnflow.function.definition=")) {
               fnDef.set(arg.substring(arg.lastIndexOf("=") + 1));
           } else if(arg.startsWith("--cfgfns.")) {
               pipelineCfg.add(arg.substring(9));
           }
        });

        String fnDefStr = fnDef.get();
        if(fnDefStr != null) {
            String[] fnNames = fnDefStr.split("\\|");
            PipelineConfigDTO.FunctionCfg[] fnCfgs = new PipelineConfigDTO.FunctionCfg[fnNames.length];
            for(int i = 0; i < fnNames.length; i++) {
                fnCfgs[i] = new PipelineConfigDTO.FunctionCfg();
                fnCfgs[i].setName(fnNames[i]);
                int finalI = i;
                pipelineCfg.stream().filter(n -> n.matches("^[^.]+\\." + fnNames[finalI] + "\\..*$")).forEach(c -> {
                    String[] tokens = c.split("\\.", 3);
                    fnCfgs[finalI].setFunction(tokens[0]);

                    String param = tokens[2];
                    String paramName = param.substring(0, param.lastIndexOf("="));
                    String paramValue = param.substring(param.lastIndexOf("=") + 1);
                    if(!paramName.contains(".") && !paramName.endsWith("]")) { // simple String value
                        fnCfgs[finalI].getParameters().put(paramName, paramValue);
                    } else if(!paramName.contains(".") && paramName.endsWith("]")) { // array of String
                        String arrayName = paramName.substring(0, paramName.lastIndexOf("["));
                        int index = Integer.parseInt(paramName.substring(0, paramName.length() - 1).substring(paramName.lastIndexOf("[") + 1));
                        fnCfgs[finalI].getParameters().putIfAbsent(arrayName, new ArrayList<String>());
                        Object maybeList = fnCfgs[finalI].getParameters().get(arrayName);
                        if(maybeList instanceof List arrayList) {
                            if((arrayList.size() - 1) < index ) {
                                for(int a = 0; a < (index + 1 - arrayList.size()); a++) {
                                    arrayList.add(null);
                                }
                            }
                            arrayList.set(index, paramValue);
                        } else throw new IllegalStateException("Mixing list with single value for function " + fnCfgs[finalI].getName() + " and parameter " + paramName + " in function " + arrayName);
                    } else if(paramName.contains("].")) {
                        String arrayName = paramName.substring(0, paramName.lastIndexOf("["));
                        int index = Integer.parseInt(paramName.substring(0, paramName.lastIndexOf("].")).substring(paramName.lastIndexOf("[") + 1));
                        fnCfgs[finalI].getParameters().putIfAbsent(arrayName, new ArrayList<String>());
                        Object maybeList = fnCfgs[finalI].getParameters().get(arrayName);
                        if(maybeList instanceof List arrayList) {
                            if((arrayList.size() - 1) < index ) {
                                for(int a = 0; a < (index + 1 - arrayList.size()); a++) {
                                    arrayList.add(null);
                                }
                            }
                            if(arrayList.get(index) == null) {
                                arrayList.set(index, new HashMap<String, String>());
                            }
                            Object maybeMap = arrayList.get(index);
                            if(maybeMap instanceof Map map) {
                                String keyName = paramName.substring(paramName.lastIndexOf("].") + 2);
                                map.put(keyName, paramValue);
                            } else throw new IllegalStateException("Mixing map with single value for function " + fnCfgs[finalI].getName() + " and parameter " + paramName + " in function " + arrayName);
                        } else throw new IllegalStateException("Mixing list with single value for function " + fnCfgs[finalI].getName() + " and parameter " + paramName + " in function " + arrayName);
                    } else if(paramName.contains(".")) { // Map, for now we support one nested Map only
                        String mapName = paramName.substring(0, paramName.lastIndexOf("."));
                        String keyName = paramName.substring(paramName.lastIndexOf(".") + 1);
                        fnCfgs[finalI].getParameters().putIfAbsent(mapName, new HashMap<String, String>());
                        Object maybeMap = fnCfgs[finalI].getParameters().get(mapName);
                        if(maybeMap instanceof Map map) {
                            map.put(keyName, paramValue);
                        } else throw new IllegalStateException("Mixing map with single value for function " + fnCfgs[finalI].getName() + " and parameter " + paramName + " in function " + mapName);
                    }
                });
            }
            config.setPipeline(fnCfgs);
        }

        return config;
    }

    private Long convertHoursToMilliseconds(int hours) {
        return hours * 60L * 60L * 1000L;
    }

    private void prepareFunctionArgs(PipelineConfigDTO.FunctionCfg functionCfg, List<String> args) {
        String prefix = "--cfgfns." + functionCfg.getFunction() + "." + functionCfg.getName();
        functionCfg.getParameters().forEach((k, v) -> {
            if(v instanceof Map) { //for now, we support one nested map only
                ((Map<?, ?>) v).forEach((k2, v2) -> args.add(prefix + "." + k + "." + k2 + "=" + v2));
            } else if(v instanceof List) {
                for(int i=0; i<((List<?>) v).size(); i++) {
                    String elemPrefix = prefix + "." + k + "[" + i + "]";
                    Object elem = ((List<?>) v).get(i);
                    if(elem instanceof Map) { // we support Map here only (MapCreate mappings config)
                        ((Map<?, ?>) elem).forEach((k2, v2) -> args.add(elemPrefix + "." + k2 + "=" + v2));
                    } else {
                        args.add(elemPrefix + "=" + elem);
                    }
                }
            } else {
                args.add(prefix + "." + k + "=" + v);
            }
        });
    }
}
