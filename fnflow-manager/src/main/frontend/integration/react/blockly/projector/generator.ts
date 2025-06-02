import * as Blockly from 'blockly/core';

export function createGenerator() {
    const gen = new Blockly.CodeGenerator('FnFlowProcCfg');

    gen.scrub_ = function(block, code, thisOnly) {
        const nextBlock =
            block.nextConnection && block.nextConnection.targetBlock();
        if (nextBlock && !thisOnly) {
            return code + ',\n' + gen.blockToCode(nextBlock);
        }
        return code;
    }

    gen.forBlock['projector'] = function(block, generator) {
        let code = '"version": ' + generator.valueToCode(block, 'version', 0) + ',\n';
        code += '"topic": ' + generator.valueToCode(block, 'topic', 0) + ',\n';
        code += '"index": ' + generator.valueToCode(block, 'index', 0) + '\n';

        return '{\n' + generator.prefixLines(code, generator.INDENT) + '}';
    };

    gen.forBlock['text'] = function(block) {
        const textValue = block.getFieldValue('TEXT');
        const code = `"${textValue}"`;
        return [code, 0];
    }

    gen.forBlock['math_number'] = function(block) {
        const code = String(block.getFieldValue('NUM'));
        return [code, 0];
    }

    return gen;
}
