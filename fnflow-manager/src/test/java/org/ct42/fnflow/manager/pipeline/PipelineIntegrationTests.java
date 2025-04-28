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

package org.ct42.fnflow.manager.pipeline;

import org.ct42.fnflow.manager.AbstractIntegrationTests;
import org.ct42.fnflow.manager.DeploymentDTO;
import org.ct42.fnflow.manager.DeploymentDoesNotExistException;
import org.ct42.fnflow.manager.DeploymentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.BDDAssertions.then;
import static org.assertj.core.api.BDDAssertions.thenThrownBy;

/**
 * @author Claas Thiele
 * @author Sajjad Safaeian
 */
@Testcontainers
@SpringBootTest(properties = {
		"deploymentcfg.os-uris=http://opensearch-cluster-master.default.svc:9200",
		"deploymentcfg.kafka-brokers=kafka.default.svc:9092"
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class PipelineIntegrationTests extends AbstractIntegrationTests {

	@Autowired
	private PipelineService pipelineService;

	@Test
	void testCreatePod() {
		//GIVEN
		PipelineConfigDTO dto = new PipelineConfigDTO();
		dto.setVersion("0.0.9");
		dto.setSourceTopic("sourceTopic");
		dto.setEntityTopic("entityTopic");
		dto.setErrorTopic("errorTopic");
		dto.setCleanUpMode(PipelineConfigDTO.CleanUpMode.DELETE);
		dto.setCleanUpTimeHours(1);

		PipelineConfigDTO.FunctionCfg validCfg = new PipelineConfigDTO.FunctionCfg();
		validCfg.setFunction("hasValueValidator");
		validCfg.setName("idExist");
		validCfg.setParameters(Map.of("elementPath", "/id"));

		PipelineConfigDTO.FunctionCfg emitCfg = new PipelineConfigDTO.FunctionCfg();
		emitCfg.setFunction("ChangeEventEmit");
		emitCfg.setName("validateEmitter");
		emitCfg.setParameters(Map.of(
				"eventContent", "",
				"topic", "validate-topic"
		));
		PipelineConfigDTO.MultipleFunctions functions =
				new PipelineConfigDTO.MultipleFunctions(List.of(validCfg, emitCfg));

		PipelineConfigDTO.FunctionCfg matchCfg = new PipelineConfigDTO.FunctionCfg();
		matchCfg.setFunction("Match");
		matchCfg.setName("idMatch");
		matchCfg.setParameters(Map.of(
				"index", "testindex",
				"template", "testtemplate",
				"paramsFromInput", Map.of("ids", "/id"),
				"literalParams", Map.of("field", "id")));
		PipelineConfigDTO.SingleFunction match = new PipelineConfigDTO.SingleFunction(matchCfg);

		PipelineConfigDTO.FunctionCfg mergeCreate = new PipelineConfigDTO.FunctionCfg();
		mergeCreate.setFunction("MergeCreate");
		mergeCreate.setName("merge");
		mergeCreate.setParameters(Map.of(
				"mappings", List.of(
					Map.of("from","/name", "to", "/name"),
					Map.of("from", "/name", "to", "/product/fullName")
				)
		));
		PipelineConfigDTO.SingleFunction merge = new PipelineConfigDTO.SingleFunction(mergeCreate);

		dto.setPipeline(List.of(functions, match, merge));

		//WHEN
		pipelineService.createOrUpdate("pipeline-name", dto);

		//THEN
		thenCountOfPodRunningAndWithInstanceLabel("pipeline-name", 1);
		thenPodWithInstanceNameArgumentsContains("pipeline-name", "--spring.cloud.stream.bindings.fnFlowComposedFnBean-in-0.destination=sourceTopic");
		thenPodWithInstanceNameArgumentsContains("pipeline-name",
				"--cfgfns.MergeCreate.merge.mappings[0].from=/name",
				"--cfgfns.MergeCreate.merge.mappings[0].to=/name",
				"--cfgfns.MergeCreate.merge.mappings[1].from=/name",
				"--cfgfns.MergeCreate.merge.mappings[1].to=/product/fullName",
				"--spring.cloud.stream.kafka.bindings.fnFlowComposedFnBean-out-0.producer.topic.properties.cleanup.policy=delete",
				"--spring.cloud.stream.kafka.bindings.fnFlowComposedFnBean-out-0.producer.topic.properties.retention.ms=3600000");
		thenDeploymentIsCompleted("pipeline-name");

		//WHEN
		dto.setSourceTopic("sourceTopicChanged");
		pipelineService.createOrUpdate("pipeline-name", dto);

		//THEN
		thenDeploymentIsCompleted("pipeline-name");
		thenCountOfPodRunningAndWithInstanceLabel("pipeline-name", 1);
		thenPodWithInstanceNameArgumentsContains("pipeline-name", "--spring.cloud.stream.bindings.fnFlowComposedFnBean-in-0.destination=sourceTopicChanged");
	}

	@Test
	void testDeletePod() {
		//GIVEN
		PipelineConfigDTO dto = new PipelineConfigDTO();
		dto.setVersion("0.0.9");
		dto.setSourceTopic("sourceTopic");
		dto.setEntityTopic("entityTopic");
		dto.setErrorTopic("errorTopic");

		PipelineConfigDTO.FunctionCfg validCfg = new PipelineConfigDTO.FunctionCfg();
		validCfg.setFunction("hasValueValidator");
		validCfg.setName("idExist");
		validCfg.setParameters(Map.of("elementPath", "/id"));
		PipelineConfigDTO.SingleFunction valid = new PipelineConfigDTO.SingleFunction(validCfg);

		PipelineConfigDTO.FunctionCfg matchCfg = new PipelineConfigDTO.FunctionCfg();
		matchCfg.setFunction("Match");
		matchCfg.setName("idMatch");
		matchCfg.setParameters(Map.of(
				"index", "testindex",
				"template", "testtemplate",
				"paramsFromInput", Map.of("ids", "/id"),
				"literalParams", Map.of("field", "id")));
		PipelineConfigDTO.SingleFunction match = new PipelineConfigDTO.SingleFunction(matchCfg);

		dto.setPipeline(List.of(valid, match));

		pipelineService.createOrUpdate("pipeline-tobedeleted", dto);

		thenCountOfPodRunningAndWithInstanceLabel("pipeline-tobedeleted", 1);
		thenPodWithInstanceNameArgumentsContains("pipeline-tobedeleted", "--spring.cloud.stream.bindings.fnFlowComposedFnBean-in-0.destination=sourceTopic");
		thenDeploymentIsCompleted("pipeline-tobedeleted");

		//WHEN
		pipelineService.delete("pipeline-tobedeleted");

		//THEN
		thenCountOfPodRunningAndWithInstanceLabel("pipeline-tobedeleted", 0);
	}

	@Test
	void testGetDeployment() throws DeploymentDoesNotExistException {
		//GIVEN
		PipelineConfigDTO dto = new PipelineConfigDTO();
		dto.setVersion("0.0.9");
		dto.setSourceTopic("sourceTopic");
		dto.setEntityTopic("entityTopic");
		dto.setErrorTopic("errorTopic");
		dto.setCleanUpTimeHours(1);

		PipelineConfigDTO.FunctionCfg validCfg = new PipelineConfigDTO.FunctionCfg();
		validCfg.setFunction("hasValueValidator");
		validCfg.setName("idExist");
		validCfg.setParameters(Map.of("elementPath", "/id"));

		PipelineConfigDTO.FunctionCfg emitCfg = new PipelineConfigDTO.FunctionCfg();
		emitCfg.setFunction("ChangeEventEmit");
		emitCfg.setName("validateEmitter");
		emitCfg.setParameters(Map.of(
				"eventContent", "/",
				"topic", "validate-topic"
		));
		PipelineConfigDTO.MultipleFunctions functions =
				new PipelineConfigDTO.MultipleFunctions(List.of(validCfg, emitCfg));


		PipelineConfigDTO.FunctionCfg matchCfg = new PipelineConfigDTO.FunctionCfg();
		matchCfg.setFunction("Match");
		matchCfg.setName("idMatch");
		matchCfg.setParameters(Map.of(
				"index", "testindex",
				"template", "testtemplate",
				"paramsFromInput", Map.of("ids", "/id"),
				"literalParams", Map.of("field", "id")));
		PipelineConfigDTO.SingleFunction match = new PipelineConfigDTO.SingleFunction(matchCfg);

		PipelineConfigDTO.FunctionCfg mergeCfg = new PipelineConfigDTO.FunctionCfg();
		mergeCfg.setFunction("MergeCreate");
		mergeCfg.setName("createmerge");
		mergeCfg.setParameters(Map.of(
			"mappings", List.of(
					Map.of(
							"from", "/id",
						"to", "/identifier/id"
					),
					Map.of(
							"from", "/id",
							"to", "/ID"
					),
					Map.of(
							"from", "/doesNotExist",
							"to", "/foo"
					)
				)
		));
		PipelineConfigDTO.SingleFunction merge = new PipelineConfigDTO.SingleFunction(mergeCfg);

		dto.setPipeline(List.of(functions, match, merge));

		pipelineService.createOrUpdate("pipeline-toberead", dto);

		thenCountOfPodRunningAndWithInstanceLabel("pipeline-toberead", 1);
		thenPodWithInstanceNameArgumentsContains("pipeline-toberead", "--spring.cloud.stream.bindings.fnFlowComposedFnBean-in-0.destination=sourceTopic");
		thenDeploymentIsCompleted("pipeline-toberead");

		//WHEN
		PipelineConfigDTO config = pipelineService.getConfig("pipeline-toberead");
		then(config).isEqualTo(dto);
	}

	@Test
	void testGetDeploymentThatDoesNotExist() {
		thenThrownBy(() -> pipelineService.getConfig("this-does-not-exist")).isInstanceOf(DeploymentDoesNotExistException.class);
	}

	@Test
	void getListTest() {
		//GIVEN
		PipelineConfigDTO dto = new PipelineConfigDTO();
		dto.setVersion("0.0.9");
		dto.setSourceTopic("sourceTopic");
		dto.setEntityTopic("entityTopic");
		dto.setErrorTopic("errorTopic");

		PipelineConfigDTO.FunctionCfg validCfg = new PipelineConfigDTO.FunctionCfg();
		validCfg.setFunction("hasValueValidator");
		validCfg.setName("idExist");
		validCfg.setParameters(Map.of("elementPath", "/id"));
		PipelineConfigDTO.SingleFunction valid = new PipelineConfigDTO.SingleFunction(validCfg);

		dto.setPipeline(List.of(valid));

		pipelineService.createOrUpdate("pipeline-name-1", dto);
		pipelineService.createOrUpdate("pipeline-name-2", dto);
		thenCountOfPodRunningAndWithInstanceLabel("pipeline-name-1", 1);
		thenCountOfPodRunningAndWithInstanceLabel("pipeline-name-2", 1);

		//When
		List<DeploymentDTO> deployments = pipelineService.getList();

		//Then
		then(deployments)
				.hasSize(2)
				.extracting("name")
				.contains("pipeline-name-1", "pipeline-name-2");
	}

	@Override
	protected DeploymentService<?> getDeploymentService() {
		return pipelineService;
	}

}
