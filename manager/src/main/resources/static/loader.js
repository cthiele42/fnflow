function loadFromDeploymentToWorkspace(name, workspace) {
    fetch('/pipelines/' + name).then(res => res.json()).then(data => {
        let state =
            {
                blocks: {
                    languageVersion: 0,
                    blocks: [
                        {
                            type: "processor",
                            id: Blockly.utils.idGenerator.genUid(),
                            x: 10,
                            y: 10,
                            deletable: false,
                            movable: false,
                            inputs: {
                                version: {
                                    shadow: {
                                        type: "text",
                                        id: Blockly.utils.idGenerator.genUid(),
                                        fields: {
                                            TEXT: data.version
                                        }
                                    }
                                },
                                inputTopic: {
                                    shadow: {
                                        type: "text",
                                        id: Blockly.utils.idGenerator.genUid(),
                                        fields: {
                                            TEXT: data.sourceTopic
                                        }
                                    }
                                },
                                outputTopic: {
                                    shadow: {
                                        type: "text",
                                        id: Blockly.utils.idGenerator.genUid(),
                                        fields: {
                                            TEXT: data.entityTopic
                                        }
                                    }
                                },
                                errorTopic: {
                                    shadow: {
                                        type: "text",
                                        id: Blockly.utils.idGenerator.genUid(),
                                        fields: {
                                            TEXT: data.errorTopic
                                        }
                                    }
                                },
                                errRetentionHours: {
                                    shadow: {
                                        type: "math_number",
                                        id: Blockly.utils.idGenerator.genUid(),
                                        fields: {
                                            NUM: data.errRetentionHours
                                        }
                                    }
                                },
                                pipeline: {

                                }
                            }
                        }
                    ]
                }
            };

        const typeMapping = {
            hasValueValidator: 'hasValue',
            trimNormalizer: 'trim',
            padNormalizer: 'pad',
            Match: 'Match',
            Reduce2One: 'reduceToOne',
            MergeCreate: 'mergeCreate'
        }
        let anchorObject = state.blocks.blocks[0].inputs;
        let anchorProp = 'pipeline';

        for( const p of data.pipeline ) {
            let fn = {
                block: {
                    type: typeMapping[p.function],
                    id: Blockly.utils.idGenerator.genUid(),
                    inputs: {
                        name: {
                            shadow: {
                                type: 'text',
                                id: Blockly.utils.idGenerator.genUid(),
                                fields: {
                                    TEXT: p.name
                                }
                            }
                        }
                    }
                }
            }

            switch(p.function) {
                case 'hasValueValidator':
                    fn.block.inputs.elementPath = {
                        shadow: {
                            type: 'text',
                            id: Blockly.utils.idGenerator.genUid(),
                            fields: {
                                TEXT: p.parameters.elementPath
                            }
                        }
                    }
                    break;
                case 'trimNormalizer':
                    fn.block['fields'] = {
                        dir: p.parameters.mode
                    }
                    fn.block.inputs.elementPath = {
                        shadow: {
                            type: 'text',
                            id: Blockly.utils.idGenerator.genUid(),
                            fields: {
                                TEXT: p.parameters.elementPath
                            }
                        }
                    }
                    break;
                case 'padNormalizer':
                    fn.block['fields'] = {
                        dir: p.parameters.pad
                    }
                    fn.block.inputs.elementPath = {
                        shadow: {
                            type: 'text',
                            id: Blockly.utils.idGenerator.genUid(),
                            fields: {
                                TEXT: p.parameters.elementPath
                            }
                        }
                    }
                    fn.block.inputs.length = {
                        shadow: {
                            type: 'math_number',
                            id: Blockly.utils.idGenerator.genUid(),
                            fields: {
                                NUM: p.parameters.length
                            }
                        }
                    }
                    fn.block.inputs.fillchar = {
                        shadow: {
                            type: 'text',
                            id: Blockly.utils.idGenerator.genUid(),
                            fields: {
                                TEXT: p.parameters.fillerCharacter
                            }
                        }
                    }
                    break;
                case 'Match':
                    fn.block.inputs.index = {
                        shadow: {
                            type: 'text',
                            id: Blockly.utils.idGenerator.genUid(),
                            fields: {
                                TEXT: p.parameters.index
                            }
                        }
                    }
                    fn.block.inputs.template = {
                        shadow: {
                            type: 'text',
                            id: Blockly.utils.idGenerator.genUid(),
                            fields: {
                                TEXT: p.parameters.template
                            }
                        }
                    }
                    let pAnchorObject = fn.block.inputs;
                    let pAnchorProp = 'parameters';

                    if(Object.hasOwn(p.parameters, 'paramsFromInput')) {
                        for(const pi in p.parameters.paramsFromInput) {
                            if (p.parameters.paramsFromInput.hasOwnProperty(pi)) {
                                let param = {
                                    block: {
                                        type: 'tParamFromInput',
                                        id: Blockly.utils.idGenerator.genUid(),
                                        inputs: {
                                            paramName: {
                                                shadow: {
                                                    type: 'text',
                                                    id: Blockly.utils.idGenerator.genUid(),
                                                    fields: {
                                                        TEXT: pi
                                                    }
                                                }
                                            },
                                            elementPath: {
                                                shadow: {
                                                    type: 'text',
                                                    id: Blockly.utils.idGenerator.genUid(),
                                                    fields: {
                                                        TEXT: p.parameters.paramsFromInput[pi]
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                pAnchorObject[pAnchorProp] = param;
                                pAnchorObject = param.block;
                                pAnchorProp = 'next';
                            }
                        }
                    }

                    if(Object.hasOwn(p.parameters, 'literalParams')) {
                        for(const pl in p.parameters.literalParams) {
                            if (p.parameters.literalParams.hasOwnProperty(pl)) {
                                let param = {
                                    block: {
                                        type: 'tParamLiteral',
                                        id: Blockly.utils.idGenerator.genUid(),
                                        inputs: {
                                            paramName: {
                                                shadow: {
                                                    type: 'text',
                                                    id: Blockly.utils.idGenerator.genUid(),
                                                    fields: {
                                                        TEXT: pl
                                                    }
                                                }
                                            },
                                            value: {
                                                shadow: {
                                                    type: 'text',
                                                    id: Blockly.utils.idGenerator.genUid(),
                                                    fields: {
                                                        TEXT: p.parameters.literalParams[pl]
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                                pAnchorObject[pAnchorProp] = param;
                                pAnchorObject = param.block;
                                pAnchorProp = 'next';
                            }
                        }
                    }
                    break;
                case 'Reduce2One':
                    // nothing to do here
                    break;
                case 'MergeCreate':
                    if(Object.hasOwn(p.parameters, 'mappings')) {
                        let mAnchorObject = fn.block.inputs;
                        let nAnchorProp = 'mappings';
                        for(const m of p.parameters.mappings) {
                            let mapping = {
                                block: {
                                    type: 'mergeMapping',
                                    id: Blockly.utils.idGenerator.genUid(),
                                    inputs: {
                                        from: {
                                            shadow: {
                                                type: 'text',
                                                id: Blockly.utils.idGenerator.genUid(),
                                                fields: {
                                                    TEXT: m.from
                                                }
                                            }
                                        },
                                        to: {
                                            shadow: {
                                                type: 'text',
                                                id: Blockly.utils.idGenerator.genUid(),
                                                fields: {
                                                    TEXT: m.to
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                            mAnchorObject[nAnchorProp] = mapping;
                            mAnchorObject = mapping.block;
                            nAnchorProp = 'next';
                        }
                    }
                    break;
            }

            anchorObject[anchorProp] = fn;
            anchorObject = fn.block;
            anchorProp = 'next';
        }

    console.log('>>> ' + JSON.stringify(state, null, 2));

    Blockly.serialization.workspaces.load(state, workspace);

    }).catch(error => {
        console.log(error)
    });
}
