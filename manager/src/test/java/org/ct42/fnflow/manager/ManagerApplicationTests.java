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

import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;

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
		dto.setVersion("0.0.1");
		dto.setConsumerGroups("consumerGroup");
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

		pipelineService.createPipeline("pipeline-name", dto);

		PodList podList = kubernetesClient.pods()
				.inNamespace("default")
				.withLabel("app.kubernetes.io/instance", "pipeline-name")
				.list();
		then(podList.getItems()).hasSize(1);
	}
}
