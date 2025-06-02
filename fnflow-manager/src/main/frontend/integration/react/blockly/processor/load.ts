import * as Blockly from 'blockly/core';

export function loadConfig(configString: string) {
    const pipelineConfig = JSON.parse(configString);
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
                                        TEXT: pipelineConfig.version
                                    }
                                }
                            },
                            inputTopic: {
                                shadow: {
                                    type: "text",
                                    id: Blockly.utils.idGenerator.genUid(),
                                    fields: {
                                        TEXT: pipelineConfig.sourceTopic
                                    }
                                }
                            },
                            outputTopic: {
                                shadow: {
                                        type: "tParamTopic",
                                        id: Blockly.utils.idGenerator.genUid(),
                                        inputs: {
                                            name: {
                                                shadow: {
                                                    type: "text",
                                                    fields: {
                                                        TEXT: pipelineConfig.entityTopic
                                                    }
                                                }
                                            },
                                            cleanUpTimeHours: {
                                                shadow: {
                                                    type: "math_number",
                                                    fields: {
                                                        NUM: pipelineConfig.cleanUpTimeHours
                                                    }
                                                }
                                            }
                                        },
                                    fields: {
                                        cleanUpMode: pipelineConfig.cleanUpMode
                                    }
                                }
                            },
                            errorTopic: {
                                shadow: {
                                    type: "text",
                                    id: Blockly.utils.idGenerator.genUid(),
                                    fields: {
                                        TEXT: pipelineConfig.errorTopic
                                    }
                                }
                            },
                            errRetentionHours: {
                                shadow: {
                                    type: "math_number",
                                    id: Blockly.utils.idGenerator.genUid(),
                                    fields: {
                                        NUM: pipelineConfig.errRetentionHours
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

    let anchorObject = state.blocks.blocks[0].inputs;
    let anchorProp = 'pipeline';

    for( const p of pipelineConfig.pipeline ) {
        if (Array.isArray(p)) {
            if(p.length > 0 ) {
                let fn = {
                    block: {
                        type: 'multipleFunctions',
                        id: Blockly.utils.idGenerator.genUid(),
                        inputs: {}
                    }
                }

                let mAnchorObject = fn.block.inputs;
                let nAnchorProp = 'functions';
                for(const f of p) {
                    let inFn = createBlockContent(f)
                    // @ts-ignore
                    mAnchorObject[nAnchorProp] = inFn;
                    mAnchorObject = inFn.block;
                    nAnchorProp = 'next';
                }

                // @ts-ignore
                anchorObject[anchorProp] = fn;
                // @ts-ignore
                anchorObject = fn.block;
                anchorProp = 'next';
            }
        } else {
            let fn = createBlockContent(p);

            // @ts-ignore
            anchorObject[anchorProp] = fn;
            // @ts-ignore
            anchorObject = fn.block;
            anchorProp = 'next';
        }
    }
    return state;
}

function createBlockContent(p: any) {
    const typeMapping = {
        hasValueValidator: 'hasValue',
        trimNormalizer: 'trim',
        padNormalizer: 'pad',
        Match: 'Match',
        Reduce2One: 'reduceToOne',
        MergeCreate: 'mergeCreate',
        ChangeEventEmit: 'emitter'
    }

    let fn = {
        block: {
            // @ts-ignore
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
            // @ts-ignore
            fn.block.inputs['elementPath'] = {
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
            // @ts-ignore
            fn.block['fields'] = {
                dir: p.parameters.mode
            }
            // @ts-ignore
            fn.block.inputs['elementPath'] = {
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
            // @ts-ignore
            fn.block['fields'] = {
                dir: p.parameters.pad
            }
            // @ts-ignore
            fn.block.inputs['elementPath'] = {
                shadow: {
                    type: 'text',
                    id: Blockly.utils.idGenerator.genUid(),
                    fields: {
                        TEXT: p.parameters.elementPath
                    }
                }
            }
            // @ts-ignore
            fn.block.inputs['length'] = {
                shadow: {
                    type: 'math_number',
                    id: Blockly.utils.idGenerator.genUid(),
                    fields: {
                        NUM: p.parameters.length
                    }
                }
            }
            // @ts-ignore
            fn.block.inputs['fillchar'] = {
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
            // @ts-ignore
            fn.block.inputs['index'] = {
                shadow: {
                    type: 'text',
                    id: Blockly.utils.idGenerator.genUid(),
                    fields: {
                        TEXT: p.parameters.index
                    }
                }
            }
            // @ts-ignore
            fn.block.inputs['template'] = {
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
                        // @ts-ignore
                        pAnchorObject[pAnchorProp] = param;
                        // @ts-ignore
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
                        // @ts-ignore
                        pAnchorObject[pAnchorProp] = param;
                        // @ts-ignore
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
                    // @ts-ignore
                    mAnchorObject[nAnchorProp] = mapping;
                    // @ts-ignore
                    mAnchorObject = mapping.block;
                    nAnchorProp = 'next';
                }
            }
            break;
        case 'ChangeEventEmit':
            // @ts-ignore
            fn.block.inputs['eventContent'] = {
                shadow: {
                    type: 'text',
                    id: Blockly.utils.idGenerator.genUid(),
                    fields: {
                        TEXT: p.parameters.eventContent
                    }
                }
            }
            if(Object.hasOwn(p.parameters, 'eventKey')) {
                // @ts-ignore
                fn.block.inputs['eventKey'] = {
                    shadow: {
                        type: 'text',
                        id: Blockly.utils.idGenerator.genUid(),
                        fields: {
                            TEXT: p.parameters.eventKey
                        }
                    }
                }
            }
            if(Object.hasOwn(p.parameters, 'topic')) {
                // @ts-ignore
                fn.block.inputs['topic'] = {
                    shadow: {
                        type: 'tParamTopic',
                        id: Blockly.utils.idGenerator.genUid(),
                        inputs: {
                            name: {
                                shadow: {
                                    type: "text",
                        fields: {
                            TEXT: p.parameters.topic
                        }
                    }
                            },
                            cleanUpTimeHours: {
                                shadow: {
                                    type: "math_number",
                                    fields: {
                                        NUM: p.parameters.cleanUpTimeHours
                                    }
                                }
                            }
                        },
                        fields: {
                            cleanUpMode: p.parameters.cleanUpMode
                        }
                    }
                }
            }
            break;
    }
    return fn
}
