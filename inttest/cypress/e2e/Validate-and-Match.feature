Feature: Validate and match input data
  Scenario: Validate and match input data
    Given a searchtemplate with name 'testtemplate' with:
    """
    {
      "script": {
          "lang": "mustache",
          "source": "{\"query\":{\"terms\":{\"{{field}}\":{{#toJson}}ids{{/toJson}}}}}"
      }
    }
    """
    And an index 'testindex' with mapping:
    """
    {
      "mappings": {
        "properties": {
          "id": {
            "type": "keyword"
          }
        }
      }
    }
    """
    And two documents in the index
    When messages from 'input/six-valid-four-invalid.json' were sent to the topic 'fnFlowComposedFnBean-in-0'
    Then six messages are landing in the output topic
    And three messages are landing in the error topic