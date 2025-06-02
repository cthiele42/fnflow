import * as Blockly from 'blockly/core';

export function loadConfig(configString: string) {
    const projectorConfig = JSON.parse(configString);
    let state =
        {
            blocks: {
                languageVersion: 0,
                blocks: [
                    {
                        type: "projector",
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
                                        TEXT: projectorConfig.version
                                    }
                                }
                            },
                            topic: {
                                shadow: {
                                    type: "text",
                                    id: Blockly.utils.idGenerator.genUid(),
                                    fields: {
                                        TEXT: projectorConfig.topic
                                    }
                                }
                            },
                            index: {
                                shadow: {
                                    type: "text",
                                    id: Blockly.utils.idGenerator.genUid(),
                                    fields: {
                                        TEXT: projectorConfig.index
                                    }
                                }
                            }
                        }
                    }
                ]
            }
        };
    return state;
}
