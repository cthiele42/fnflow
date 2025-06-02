package org.ct42.fnflow.manager.deployment;

import lombok.Getter;

@Getter
public class DeploymentDoesNotExistException extends Exception {
  private final String pipelineName;

  public DeploymentDoesNotExistException(String pipelineName, String deploymentType) {
    super("The deployment for " + deploymentType + " with name " + pipelineName + " does not exist");
    this.pipelineName = pipelineName;
  }
}
