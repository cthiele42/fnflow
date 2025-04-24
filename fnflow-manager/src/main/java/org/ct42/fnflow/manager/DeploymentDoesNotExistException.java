package org.ct42.fnflow.manager;

import lombok.Getter;

@Getter
public class DeploymentDoesNotExistException extends Exception {
  private final String pipelineName;

  public DeploymentDoesNotExistException(String pipelineName) {
    super("The deployment for pipeline with name " + pipelineName + " does not exist");
    this.pipelineName = pipelineName;
  }
}
