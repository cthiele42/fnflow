function createToolbox() {
    return {
        kind: 'flyoutToolbox',
        contents: [
            {
                kind: 'block',
                type: 'text'
            },
            {
                kind: 'block',
                type: 'math_number'
            },
            {
                kind: 'block',
                type: 'tParamTopic',
                inputs: {
                    name: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "topic-name"
                            }
                        }
                    },
                    cleanUpTimeHours: {
                        shadow: {
                            type: "math_number",
                            fields: {
                                NUM: 336
                            }
                        }
                    }
                }
            },
            {
                kind: 'block',
                type: 'tParamFromInput',
                inputs: {
                    paramName: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "param"
                            }
                        }
                    },
                    elementPath: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "/prop"
                            }
                        }
                    }
                }
            },
            {
                kind: 'block',
                type: 'tParamLiteral',
                inputs: {
                    paramName: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "param"
                            }
                        }
                    },
                    value: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "value"
                            }
                        }
                    }
                }
            },
            {
                kind: 'block',
                type: 'mergeMapping',
                inputs: {
                    from: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "/attributes/ID"
                            }
                        }
                    },
                    to: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "/id"
                            }
                        }
                    }
                }
            },
            {
                kind: 'block',
                type: 'hasValue',
                inputs: {
                    name: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "idExist"
                            }
                        }
                    },
                    elementPath: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "/id"
                            }
                        }
                    }
                }
            },
            {
                kind: 'block',
                type: 'pad',
                inputs: {
                    name: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "padEan"
                            }
                        }
                    },
                    elementPath: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "/ean"
                            }
                        }
                    },
                    length: {
                        shadow: {
                            type: "math_number",
                            fields: {
                                NUM: 8
                            }
                        }
                    },
                    fillchar: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "0"
                            }
                        }
                    }
                }
            },
            {
                kind: 'block',
                type: 'trim',
                inputs: {
                    name: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "trimEan"
                            }
                        }
                    },
                    elementPath: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "/ean"
                            }
                        }
                    }
                }
            },
            {
                kind: 'block',
                type: 'Match',
                inputs: {
                    name: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "exact"
                            }
                        }
                    },
                    index: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "entities"
                            }
                        }
                    },
                    template: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "exactMatch"
                            }
                        }
                    }
                }
            },
            {
                kind: 'block',
                type: 'reduceToOne',
                inputs: {
                    name: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "reduce2one"
                            }
                        }
                    }
                }
            },
            {
                kind: 'block',
                type: 'mergeCreate',
                inputs: {
                    name: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "mergeCreate"
                            }
                        }
                    }
                }
            },
            {
                kind: 'block',
                type: 'emitter',
                inputs: {
                    name: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "inputEmitter"
                            }
                        }
                    },
                    eventContent: {
                        shadow: {
                            type: "text",
                            fields: {
                                TEXT: "/input"
                            }
                        }
                    }
                }
            },
            {
                kind: 'block',
                type: 'multipleFunctions'
            }
        ]
    };
}
