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
    And a pipeline processing app with name 'sample-pipeline' and with this configs:
    """
    {
     "version": "0.0.11",
     "sourceTopic": "input-topic",
     "entityTopic": "output-topic-wrong",
     "errorTopic": "error-topic",
     "errRetentionHours": 1,
     "pipeline": [
         {
             "name": "idExist",
             "function": "hasValueValidator",
             "parameters": {
                 "elementPath": "/id"
             }
         },
         {
             "name": "idMatch",
             "function": "Match",
             "parameters": {
                 "index": "testindex",
                 "template": "testtemplate",
                 "paramsFromInput": {
                    "ids": "/id"
                 },
                 "literalParams": {
                    "field": "id"
                 }
             }
         }
     ]
    }
    """
    Then a topic with name 'output-topic-wrong' and messageCount 0 exists
    And a pipeline processing app with name 'sample-pipeline' and with this configs:
    """
    {
      "version": "0.0.11",
      "sourceTopic": "input-topic",
      "entityTopic": "output-topic",
      "errorTopic": "error-topic",
      "errRetentionHours": 1,
      "pipeline": [
        {
          "name": "idExist",
          "function": "hasValueValidator",
          "parameters": {
            "elementPath": "/id"
          }
        },
        {
          "name": "idMatch",
          "function": "Match",
          "parameters": {
            "index": "testindex",
            "template": "testtemplate",
            "paramsFromInput": {
              "ids": "/id"
            },
            "literalParams": {
              "field": "id"
            }
          }
        },
        {
          "name": "reduce",
          "function": "Reduce2One",
          "parameters": {
            "dummy": ""
          }
        },
        {
          "name": "merge",
          "function": "MergeCreate",
          "parameters": {
            "mappings": [
              {
                "from": "/name",
                "to": "/name"
              },
              {
                "from": "/name",
                "to": "/product/fullName"
              }
            ]
          }
        },
        [
          {
            "name": "inputEmitter",
            "function": "ChangeEventEmit",
            "parameters": {
              "eventContent": "/input",
              "topic": "source-topic"
            }
          },
          {
            "name": "outputEmitter",
            "function": "ChangeEventEmit",
            "parameters": {
              "eventContent": "/matches/0/source",
              "eventKey": "/matches/0/id"
            }
          }
        ]
      ]
    }
    """
    And documents from 'entities/two-docs.json' were indexed to 'testindex'
    When messages from 'input/six-valid-four-invalid.json' were sent to the topic 'input-topic'
    Then a number of 6 messages are landing in the topic 'output-topic'
    Then a number of 6 messages are landing in the topic 'source-topic'
    And a number of 4 messages are landing in the topic 'error-topic'
    And in topic 'output-topic' all messages are having a key