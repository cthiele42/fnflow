package org.ct42.fnflow.manager.pipeline;

import io.fabric8.kubernetes.api.model.Container;
import lombok.RequiredArgsConstructor;
import org.ct42.fnflow.manager.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

/**
 * @author Claas Thiele
 * @author Sajjad Safaeian
 */
@Service
@RequiredArgsConstructor
public class PipelineService implements DeploymentService<PipelineConfigDTO> {
    private static final String IMAGE="docker.io/ct42/fnflow-json-processors-kafka";
    private static final String APP_NAME="fnflow-json-processors-kafka";
    private static final String PROCESSOR_PREFIX="proc-";

    private final KubernetesHelperService kubernetesHelperService;

    @Override
    public void createOrUpdate(String name, PipelineConfigDTO config) {
        List<String> args = new ArrayList<>();
        config.getPipeline().forEach(f -> {
            if(f instanceof PipelineConfigDTO.SingleFunction singleFunction) {
                prepareFunctionArgs(singleFunction.getFunction(), args);
            } else if(f instanceof PipelineConfigDTO.MultipleFunctions multipleFunctions) {
                multipleFunctions.getFunctions().forEach(functionCfg -> prepareFunctionArgs(functionCfg, args));
            }
        });

        String definition =
                config.getPipeline().stream()
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
        args.add("--spring.cloud.stream.bindings.fnFlowComposedFnBean-in-0.destination=" + config.getSourceTopic());
        args.add("--spring.cloud.stream.bindings.fnFlowComposedFnBean-out-0.destination=" + config.getEntityTopic());
        args.add("--spring.cloud.stream.bindings.fnFlowComposedFnBean-out-1.destination=" + config.getErrorTopic());
        args.add("--spring.cloud.stream.kafka.binder.autoAlterTopics=true");
        args.add("--spring.cloud.stream.kafka.bindings.fnFlowComposedFnBean-out-0.producer.topic.properties.cleanup.policy="
                + config.getCleanUpMode().toString().toLowerCase());
        args.add(
            "--spring.cloud.stream.kafka.bindings.fnFlowComposedFnBean-out-0.producer.topic.properties." +
            getCleanUpConfig(config.getCleanUpMode()) + "=" + convertHoursToMilliseconds(config.getCleanUpTimeHours())
        );
        args.add("--spring.cloud.stream.kafka.bindings.fnFlowComposedFnBean-out-1.producer.topic.properties.retention.ms="
                + convertHoursToMilliseconds(config.getErrRetentionHours()));

        kubernetesHelperService.createOrUpdateDeployment(APP_NAME, name, PROCESSOR_PREFIX, IMAGE, config.getVersion(), args);
    }


    /**
     *
     * @param name of the pipeline the status should be taken for
     * @return the status or <ode>null</ode> if a deployment for the given name does not exist
     */
    @Override
    public DeploymentStatusDTO getStatus(String name) throws DeploymentDoesNotExistException {
        return kubernetesHelperService.getDeploymentStatus(name, PROCESSOR_PREFIX);
    }

    @Override
    public void delete(String name) {
        kubernetesHelperService.deleteDeployment(name, PROCESSOR_PREFIX);
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
    @Override
    public PipelineConfigDTO getConfig(String name) throws DeploymentDoesNotExistException {
        Container container = kubernetesHelperService.getDeploymentContainer(name, PROCESSOR_PREFIX);

        PipelineConfigDTO config = new PipelineConfigDTO();

        String image = container.getImage();
        config.setVersion(image.substring(image.lastIndexOf(":") + 1));

        AtomicReference<String> fnDef = new AtomicReference<>();
        List<String> pipelineCfg = new ArrayList<>();

        container.getArgs().forEach(arg -> {
            if(arg.startsWith("--spring.cloud.stream.bindings.fnFlowComposedFnBean-in-0.destination=")) {
                config.setSourceTopic(getArgValue(arg));
            } else if(arg.startsWith("--spring.cloud.stream.bindings.fnFlowComposedFnBean-out-0.destination=")) {
                config.setEntityTopic(getArgValue(arg));
            } else if(arg.startsWith("--spring.cloud.stream.bindings.fnFlowComposedFnBean-out-1.destination=")) {
                config.setErrorTopic(getArgValue(arg));
            } else if(arg.startsWith("--spring.cloud.stream.kafka.bindings.fnFlowComposedFnBean-out-1.producer.topic.properties.retention.ms=")) {
                long milliseconds = Long.parseLong(getArgValue(arg));
                config.setErrRetentionHours(convertMillisecondsToHours(milliseconds));
            } else if(arg.startsWith("--spring.cloud.stream.kafka.bindings.fnFlowComposedFnBean-out-0.producer.topic.properties.cleanup.policy=")) {
                config.setCleanUpMode(PipelineConfigDTO.CleanUpMode.valueOf(getArgValue(arg).toUpperCase()));
            } else if(arg.startsWith("--spring.cloud.stream.kafka.bindings.fnFlowComposedFnBean-out-0.producer.topic.properties.retention.ms=") ||
                    arg.startsWith("--spring.cloud.stream.kafka.bindings.fnFlowComposedFnBean-out-0.producer.topic.properties.max.compaction.lag.ms=")) {
                long milliseconds = Long.parseLong(getArgValue(arg));
                config.setCleanUpTimeHours(convertMillisecondsToHours(milliseconds));
            } else if(arg.startsWith("--org.ct42.fnflow.function.definition=")) {
                fnDef.set(getArgValue(arg));
            } else if(arg.startsWith("--cfgfns.")) {
                pipelineCfg.add(arg.substring(9));
            }
        });

        String fnDefStr = fnDef.get();
        if(fnDefStr != null) {
            String[] fnPipes = fnDefStr.split("\\|");
            List<PipelineConfigDTO.Function> functions = new ArrayList<>();
            for (String fnPipe : fnPipes) {
                String[] fnNames = fnPipe.split("\\+");
                List<PipelineConfigDTO.FunctionCfg> functionCfgs = new ArrayList<>();
                for (int j = 0; j < fnNames.length; j++) {
                    PipelineConfigDTO.FunctionCfg functionCfg = new PipelineConfigDTO.FunctionCfg();
                    functionCfg.setName(fnNames[j]);
                    int finalJ = j;
                    pipelineCfg.stream().filter(n -> n.matches("^[^.]+\\." + fnNames[finalJ] + "\\..*$")).forEach(c -> {
                        String[] tokens = c.split("\\.", 3);
                        functionCfg.setFunction(tokens[0]);

                        String param = tokens[2];
                        String paramName = param.substring(0, param.lastIndexOf("="));
                        String paramValue = param.substring(param.lastIndexOf("=") + 1);
                        if (!paramName.contains(".") && !paramName.endsWith("]")) { // simple String value
                            functionCfg.getParameters().put(paramName, paramValue);
                        } else if (!paramName.contains(".") && paramName.endsWith("]")) { // array of String
                            String arrayName = paramName.substring(0, paramName.lastIndexOf("["));
                            int index = Integer.parseInt(paramName.substring(0, paramName.length() - 1).substring(paramName.lastIndexOf("[") + 1));
                            functionCfg.getParameters().putIfAbsent(arrayName, new ArrayList<String>());
                            Object maybeList = functionCfg.getParameters().get(arrayName);
                            if (maybeList instanceof List arrayList) {
                                if ((arrayList.size() - 1) < index) {
                                    for (int a = 0; a < (index + 1 - arrayList.size()); a++) {
                                        arrayList.add(null);
                                    }
                                }
                                arrayList.set(index, paramValue);
                            } else
                                throw new IllegalStateException("Mixing list with single value for function " + functionCfg.getName() + " and parameter " + paramName + " in function " + arrayName);
                        } else if (paramName.contains("].")) {
                            String arrayName = paramName.substring(0, paramName.lastIndexOf("["));
                            int index = Integer.parseInt(paramName.substring(0, paramName.lastIndexOf("].")).substring(paramName.lastIndexOf("[") + 1));
                            functionCfg.getParameters().putIfAbsent(arrayName, new ArrayList<String>());
                            Object maybeList = functionCfg.getParameters().get(arrayName);
                            if (maybeList instanceof List arrayList) {
                                if ((arrayList.size() - 1) < index) {
                                    for (int a = 0; a < (index + 1 - arrayList.size()); a++) {
                                        arrayList.add(null);
                                    }
                                }
                                if (arrayList.get(index) == null) {
                                    arrayList.set(index, new HashMap<String, String>());
                                }
                                Object maybeMap = arrayList.get(index);
                                if (maybeMap instanceof Map map) {
                                    String keyName = paramName.substring(paramName.lastIndexOf("].") + 2);
                                    map.put(keyName, paramValue);
                                } else
                                    throw new IllegalStateException("Mixing map with single value for function " + functionCfg.getName() + " and parameter " + paramName + " in function " + arrayName);
                            } else
                                throw new IllegalStateException("Mixing list with single value for function " + functionCfg.getName() + " and parameter " + paramName + " in function " + arrayName);
                        } else if (paramName.contains(".")) { // Map, for now we support one nested Map only
                            String mapName = paramName.substring(0, paramName.lastIndexOf("."));
                            String keyName = paramName.substring(paramName.lastIndexOf(".") + 1);
                            functionCfg.getParameters().putIfAbsent(mapName, new HashMap<String, String>());
                            Object maybeMap = functionCfg.getParameters().get(mapName);
                            if (maybeMap instanceof Map map) {
                                map.put(keyName, paramValue);
                            } else
                                throw new IllegalStateException("Mixing map with single value for function " + functionCfg.getName() + " and parameter " + paramName + " in function " + mapName);
                        }
                    });

                    functionCfgs.add(functionCfg);
                }

                if (functionCfgs.size() == 1) {
                    functions.add(new PipelineConfigDTO.SingleFunction(functionCfgs.getFirst()));
                } else {
                    functions.add(new PipelineConfigDTO.MultipleFunctions(functionCfgs));
                }
            }
            config.setPipeline(functions);
        }

        return config;
    }

    @Override
    public List<DeploymentDTO> getList() {
        return kubernetesHelperService.getDeploymentsByLabel(APP_NAME, PROCESSOR_PREFIX);
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

    private String getCleanUpConfig(PipelineConfigDTO.CleanUpMode cleanUpMode) {
        return switch (cleanUpMode) {
            case DELETE -> "retention.ms";
            case COMPACT -> "max.compaction.lag.ms";
        };
    }

    private Long convertHoursToMilliseconds(int hours) {
        return hours * 60L * 60L * 1000L;
    }

    private int convertMillisecondsToHours(long milliseconds) {
        return (int)(milliseconds / 3600000);
    }

    private String getArgValue(String arg) {
        return arg.substring(arg.lastIndexOf("=") + 1);
    }

}
