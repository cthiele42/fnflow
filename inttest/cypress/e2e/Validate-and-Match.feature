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
    And documents from 'entities/two-docs.json' were indexed to 'testindex'
    When messages from 'input/six-valid-four-invalid.json' were sent to the topic 'fnFlowComposedFnBean-in-0'
    Then a number of 6 messages are landing in the topic 'fnFlowComposedFnBean-out-0'
    And a number of 4 messages are landing in the topic 'fnFlowComposedFnBean-out-1'
    And in topic 'fnFlowComposedFnBean-out-0' for input ID1, ID2 and ID4 there will be matches