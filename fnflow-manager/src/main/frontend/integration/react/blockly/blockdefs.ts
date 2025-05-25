export const FnFlowBlockDefinitions = [
    {
        "type": "hasValue",
        "tooltip": "hasValue validator\n validating the element on the given path has a value",
        "helpUrl": "https://fnflow.ct42.org/fnflow/processor-fns.html#hasvaluevalidator",
        "message0": "hasValue\n name %1 elementPath %2",
        "args0": [
            {
                "type": "input_value",
                "name": "name",
                "align": "RIGHT",
                "check": "String"
            },
            {
                "type": "input_value",
                "name": "elementPath",
                "align": "RIGHT",
                "check": "String"
            }
        ],
        "previousStatement": "function",
        "nextStatement": "function",
        "colour": 330,
        "inputsInline": false
    },
    {
        "type": "tParamTopic",
        "tooltip": "Template parameter for topic",
        "helpUrl": "",
        "message0": "Topic: name %1 cleanUp %2 cleanUpTime %3 ",
        "args0": [
            {
                "type": "input_value",
                "name": "name",
                "align": "RIGHT",
                "check": "String"
            },
            {
                "type": "field_dropdown",
                "name": "cleanUpMode",
                "options": [
                    [
                        "COMPACT",
                        "COMPACT"
                    ],
                    [
                        "DELETE",
                        "DELETE"
                    ]
                ]
            },
            {
                "type": "input_value",
                "name": "cleanUpTimeHours",
                "align": "RIGHT",
                "check": "Number"
            }
        ],
        "colour": 80,
        "output": "Topic",
        "inputsInline": true
    },
    {
        "type": "processor",
        "tooltip": "A FnFlow processor",
        "helpUrl": "",
        "message0": "processor\n version %1 inputTopic %2 outputTopic %3 errorTopic %4 errRetentionHours %5\n pipeline %6",
        "args0": [
            {
                "type": "input_value",
                "name": "version",
                "align": "RIGHT",
                "check": "String"
            },
            {
                "type": "input_value",
                "name": "inputTopic",
                "align": "RIGHT",
                "check": "String"
            },
            {
                "type": "input_value",
                "name": "outputTopic",
                "align": "RIGHT",
                "check": "Topic"
            },
            {
                "type": "input_value",
                "name": "errorTopic",
                "align": "RIGHT",
                "check": "String"
            },
            {
                "type": "input_value",
                "name": "errRetentionHours",
                "align": "RIGHT",
                "check": "Number"
            },
            {
                "type": "input_statement",
                "name": "pipeline",
                "align": "RIGHT",
                "check": "function"
            }
        ],
        "colour": 210,
        "inputsInline": false
    },
    {
        "type": "tParamFromInput",
        "tooltip": "Template parameter from input",
        "helpUrl": "",
        "message0": "from input  name %1 elementPath %2",
        "args0": [
            {
                "type": "input_value",
                "name": "paramName",
                "align": "RIGHT",
                "check": "String"
            },
            {
                "type": "input_value",
                "name": "elementPath",
                "align": "RIGHT",
                "check": "String"
            }
        ],
        "previousStatement": "templateParam",
        "nextStatement": "templateParam",
        "colour": 60,
        "inputsInline": true
    },
    {
        "type": "tParamLiteral",
        "tooltip": "Template parameter",
        "helpUrl": "",
        "message0": "literal  name %1 value %2",
        "args0": [
            {
                "type": "input_value",
                "name": "paramName",
                "align": "RIGHT",
                "check": "String"
            },
            {
                "type": "input_value",
                "name": "value",
                "align": "RIGHT",
                "check": "String"
            }
        ],
        "previousStatement": "templateParam",
        "nextStatement": "templateParam",
        "colour": 60,
        "inputsInline": true
    },
    {
        "type": "mergeMapping",
        "tooltip": "a mapping from source element to target element for a merger",
        "helpUrl": "",
        "message0": "from %1 to %2",
        "args0": [
            {
                "type": "input_value",
                "name": "from",
                "align": "RIGHT",
                "check": "String"
            },
            {
                "type": "input_value",
                "name": "to",
                "align": "RIGHT",
                "check": "String"
            }
        ],
        "previousStatement": "mergeMapping",
        "nextStatement": "mergeMapping",
        "colour": 15,
        "inputsInline": true
    },
    {
        "type": "Match",
        "tooltip": "Match on entities using a searchtemplate",
        "helpUrl": "https://fnflow.ct42.org/fnflow/processor-fns.html#match",
        "message0": "match\n  name %1 index %2 template %3\n parameters %4",
        "args0": [
            {
                "type": "input_value",
                "name": "name",
                "align": "RIGHT",
                "check": "String"
            },
            {
                "type": "input_value",
                "name": "index",
                "align": "RIGHT",
                "check": "String"
            },
            {
                "type": "input_value",
                "name": "template",
                "align": "RIGHT",
                "check": "String"
            },
            {
                "type": "input_statement",
                "name": "parameters",
                "align": "RIGHT",
                "check": "templateParam"
            }
        ],
        "previousStatement": "function",
        "nextStatement": "function",
        "colour": 60,
        "inputsInline": false
    },
    {
        "type": "pad",
        "tooltip": "fill a string left or right up to a certain length",
        "helpUrl": "https://fnflow.ct42.org/fnflow/processor-fns.html#padnormalizer",
        "message0": "pad %1\n name %2 elementPath %3 up to length %4 fill character %5",
        "args0": [
            {
                "type": "field_dropdown",
                "name": "dir",
                "options": [
                    [
                        "LEFT",
                        "LEFT"
                    ],
                    [
                        "RIGHT",
                        "RIGHT"
                    ]
                ]
            },
            {
                "type": "input_value",
                "name": "name",
                "align": "RIGHT",
                "check": "String"
            },
            {
                "type": "input_value",
                "name": "elementPath",
                "align": "RIGHT",
                "check": "String"
            },
            {
                "type": "input_value",
                "name": "length",
                "align": "RIGHT",
                "check": "Number"
            },
            {
                "type": "input_value",
                "name": "fillchar",
                "align": "RIGHT",
                "check": "String"
            }
        ],
        "previousStatement": "function",
        "nextStatement": "function",
        "colour": 285,
        "inputsInline": false
    },
    {
        "type": "trim",
        "tooltip": "trim a string left, right or both sides",
        "helpUrl": "https://fnflow.ct42.org/fnflow/processor-fns.html#trimnormalizer",
        "message0": "trim %1\n name %2 elementPath %3",
        "args0": [
            {
                "type": "field_dropdown",
                "name": "dir",
                "options": [
                    [
                        "LEFT",
                        "LEFT"
                    ],
                    [
                        "RIGHT",
                        "RIGHT"
                    ],
                    [
                        "BOTH",
                        "BOTH"
                    ]
                ]
            },
            {
                "type": "input_value",
                "name": "name",
                "align": "RIGHT",
                "check": "String"
            },
            {
                "type": "input_value",
                "name": "elementPath",
                "align": "RIGHT",
                "check": "String"
            }
        ],
        "previousStatement": "function",
        "nextStatement": "function",
        "colour": 285,
        "inputsInline": false
    },
    {
        "type": "reduceToOne",
        "tooltip": "reduce match results to one result",
        "helpUrl": "",
        "message0": "reduce to one result\n name %1",
        "args0": [
            {
                "type": "input_value",
                "name": "name",
                "align": "RIGHT",
                "check": "String"
            }
        ],
        "previousStatement": "function",
        "nextStatement": "function",
        "colour": 330,
        "inputsInline": false
    },
    {
        "type": "mergeCreate",
        "tooltip": "merge if target element does not exist",
        "helpUrl": "",
        "message0": "merge mode create\n name %1\n mappings %2",
        "args0": [
            {
                "type": "input_value",
                "name": "name",
                "align": "RIGHT",
                "check": "String"
            },
            {
                "type": "input_statement",
                "name": "mappings",
                "align": "RIGHT",
                "check": "mergeMapping"
            }
        ],
        "previousStatement": "function",
        "nextStatement": "function",
        "colour": 15,
        "inputsInline": false
    },
    {
        "type": "emitter",
        "tooltip": "emitter\n Emit an input message (or selected part of the input message with probable message key to configurable topic) ",
        "helpUrl": "",
        "message0": "emitter\n name %1 eventContent %2 eventKey %3 topic %4",
        "args0": [
            {
                "type": "input_value",
                "name": "name",
                "align": "RIGHT",
                "check": "String"
            },
            {
                "type": "input_value",
                "name": "eventContent",
                "align": "RIGHT",
                "check": "String"
            },
            {
                "type": "input_value",
                "name": "eventKey",
                "align": "RIGHT",
                "check": "String"
            },
            {
                "type": "input_value",
                "name": "topic",
                "align": "RIGHT",
                "check": "Topic"
            }
        ],
        "previousStatement": "function",
        "nextStatement": "function",
        "colour": 430,
        "inputsInline": false
    },
    {
        "type": "multipleFunctions",
        "tooltip": "multipleFunctions combine multiple functions together.",
        "helpUrl": "",
        "message0": "multipleFunctions\n functions %1",
        "args0": [
            {
                "type": "input_statement",
                "name": "functions",
                "align": "RIGHT",
                "check": "function"
            }
        ],
        "previousStatement": "function",
        "nextStatement": "function",
        "colour": 565,
        "inputsInline": false
    }
];
