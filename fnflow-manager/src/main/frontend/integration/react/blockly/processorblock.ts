export const ProcessorBlock = {
    "blocks": {
        "languageVersion": 0,
        "blocks": [
            {
                "type": "processor",
                "id": "Q%Pg:_nxYCCCsX^yG}%t",
                "x": 10,
                "y": 10,
                "deletable": false,
                "movable": false,
                "inputs": {
                    "version": {
                        "shadow": {
                            "type": "text",
                            "id": "KRP:3OR5V-^E9DW1z}Sx",
                            "fields": {
                                "TEXT": "0.0.11"
                            }
                        }
                    },
                    "inputTopic": {
                        "shadow": {
                            "type": "text",
                            "id": "jZ([0ev~wf:5?jgG;vHx",
                            "fields": {
                                "TEXT": "input"
                            }
                        }
                    },
                    "outputTopic": {
                        "shadow": {
                            "type": "tParamTopic",
                            "id": "Z)Qntf5Z=DImj46`ZIC:",
                            "inputs": {
                                "name": {
                                    "shadow": {
                                        "type": "text",
                                        "fields": {
                                            "TEXT": "output"
                                        }
                                    }
                                },
                                "cleanUpTimeHours": {
                                    "shadow": {
                                        "type": "math_number",
                                        "fields": {
                                            "NUM": 336
                                        }
                                    }
                                }
                            }
                        }
                    },
                    "errorTopic": {
                        "shadow": {
                            "type": "text",
                            "id": "PB!.dWJo+E8zXK.p,S/?",
                            "fields": {
                                "TEXT": "error"
                            }
                        }
                    },
                    "errRetentionHours": {
                        "shadow": {
                            "type": "math_number",
                            "id": "IHFbBU![X6u@?2`n8+xz",
                            "fields": {
                                "NUM": 336
                            }
                        }
                    }
                }
            }
        ]
    }
};
