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

import io.fabric8.kubernetes.api.model.PodList;
import io.fabric8.kubernetes.client.Config;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.k3s.K3sContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.List;
import java.time.Duration;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.BDDAssertions.then;
import static org.awaitility.Awaitility.await;

/**
 * @author Claas Thiele
 * @author Sajjad Safaeian
 */
@Testcontainers
@SpringBootTest(properties = {
		"processorcfg.os-uris=http://opensearch-cluster-master.default.svc:9200",
		"processorcfg.kafka-brokers=kafka.default.svc:9092"
})
class ManagerApplicationTests {
	@Container
	static final K3sContainer k3s = new K3sContainer(DockerImageName.parse("rancher/k3s:v1.21.3-k3s1"));


	@TestBean
	KubernetesClient kubernetesClient;

	static KubernetesClient kubernetesClient() {
		String config = k3s.getKubeConfigYaml();
		KubernetesClientBuilder builder = new KubernetesClientBuilder();

		// we do this in a conditional manner to satisfy processTestAOT
		if(config != null) {
			Config kubeconfig = Config.fromKubeconfig(config);
			builder.withConfig(kubeconfig);
		}
		return builder.build();
	}

	@Autowired
	private PipelineService pipelineService;

	@Test
	void testCreatePod() {
		PipelineConfigDTO dto = new PipelineConfigDTO();
		dto.setVersion("0.0.7");
		dto.setSourceTopic("sourceTopic");
		dto.setEntityTopic("entityTopic");
		dto.setErrorTopic("errorTopic");

		PipelineConfigDTO.FunctionCfg valiCfg = new PipelineConfigDTO.FunctionCfg();
		valiCfg.setFunction("hasValueValidator");
		valiCfg.setName("idExist");
		valiCfg.setParameters(Map.of("elementPath", "/id"));

		PipelineConfigDTO.FunctionCfg matchCfg = new PipelineConfigDTO.FunctionCfg();
		matchCfg.setFunction("Match");
		matchCfg.setName("idMatch");
		matchCfg.setParameters(Map.of(
				"index", "testindex",
				"template", "testtemplate",
				"paramsFromInput", Map.of("ids", "/id"),
				"literalParams", Map.of("field", "id")));

		PipelineConfigDTO.FunctionCfg mergeCreate = new PipelineConfigDTO.FunctionCfg();
		mergeCreate.setFunction("MergeCreate");
		mergeCreate.setName("merge");
		mergeCreate.setParameters(Map.of(
				"mappings", List.of(
					Map.of("from","/name", "to", "/name"),
					Map.of("from", "/name", "to", "/product/fullName")
				)
		));

		dto.setPipeline(new PipelineConfigDTO.FunctionCfg[]{valiCfg, matchCfg, mergeCreate});

		pipelineService.createOrUpdatePipeline("pipeline-name", dto);

		PodList podList = kubernetesClient.pods()
				.inNamespace("default")
				.withLabel("app.kubernetes.io/instance", "pipeline-name")
				.list();
		then(podList.getItems()).hasSize(1);
		then(podList.getItems().getFirst().getSpec().getContainers().getFirst().getArgs()).contains("--spring.cloud.stream.bindings.fnFlowComposedFnBean-in-0.destination=sourceTopic");

		await().atMost(Duration.ofSeconds(180)).untilAsserted(() -> assertThat(pipelineService.getPipelineStatus("pipeline-name").getStatus()).isEqualTo(DeploymentStatusDTO.Status.COMPLETED));

		dto.setSourceTopic("sourceTopicChanged");
		pipelineService.createOrUpdatePipeline("pipeline-name", dto);
		await().atMost(Duration.ofSeconds(180)).untilAsserted(() -> assertThat(pipelineService.getPipelineStatus("pipeline-name").getStatus()).isEqualTo(DeploymentStatusDTO.Status.COMPLETED));

		await().untilAsserted(() -> assertThat(kubernetesClient.pods()
				.inNamespace("default")
				.withLabel("app.kubernetes.io/instance", "pipeline-name")
				.list().getItems().stream().filter(p -> p.getStatus().getPhase().equals("Running")).collect(Collectors.toList())).hasSize(1));

		then(kubernetesClient.pods()
				.inNamespace("default")
				.withLabel("app.kubernetes.io/instance", "pipeline-name")
				.list().getItems().getFirst().getSpec().getContainers().getFirst().getArgs()).contains("--spring.cloud.stream.bindings.fnFlowComposedFnBean-in-0.destination=sourceTopicChanged");

	}

	@Test
	void testDeletePod() {
		PipelineConfigDTO dto = new PipelineConfigDTO();
		dto.setVersion("0.0.1");
		dto.setSourceTopic("sourceTopic");
		dto.setEntityTopic("entityTopic");
		dto.setErrorTopic("errorTopic");

		PipelineConfigDTO.FunctionCfg valiCfg = new PipelineConfigDTO.FunctionCfg();
		valiCfg.setFunction("hasValueValidator");
		valiCfg.setName("idExist");
		valiCfg.setParameters(Map.of("elementPath", "/id"));

		PipelineConfigDTO.FunctionCfg matchCfg = new PipelineConfigDTO.FunctionCfg();
		matchCfg.setFunction("Match");
		matchCfg.setName("idMatch");
		matchCfg.setParameters(Map.of(
				"index", "testindex",
				"template", "testtemplate",
				"paramsFromInput", Map.of("ids", "/id"),
				"literalParams", Map.of("field", "id")));

		dto.setPipeline(new PipelineConfigDTO.FunctionCfg[]{valiCfg, matchCfg});

		pipelineService.createOrUpdatePipeline("pipeline-name", dto);

		PodList podList = kubernetesClient.pods()
				.inNamespace("default")
				.withLabel("app.kubernetes.io/instance", "pipeline-name")
				.list();
		then(podList.getItems()).hasSize(1);
		then(podList.getItems().getFirst().getSpec().getContainers().getFirst().getArgs()).contains("--spring.cloud.stream.bindings.fnFlowComposedFnBean-in-0.destination=sourceTopic");

        then(podList.getItems().getFirst().getSpec().getContainers().getFirst().getArgs()).contains(
                "--cfgfns.MergeCreate.merge.mappings.0.from=/name",
                "--cfgfns.MergeCreate.merge.mappings.0.to=/name",
                "--cfgfns.MergeCreate.merge.mappings.1.from=/name",
                "--cfgfns.MergeCreate.merge.mappings.1.to=/product/fullName");

        await().atMost(Duration.ofSeconds(180)).untilAsserted(() -> assertThat(pipelineService.getPipelineStatus("pipeline-name").getStatus()).isEqualTo(DeploymentStatusDTO.Status.COMPLETED));

		pipelineService.deletePipeline("pipeline-name");

		await().untilAsserted(() -> assertThat(kubernetesClient.pods()
				.inNamespace("default")
				.withLabel("app.kubernetes.io/instance", "pipeline-name")
				.list().getItems()).isEmpty());
	}
}
