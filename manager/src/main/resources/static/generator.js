function createGenerator() {
    const gen = new Blockly.CodeGenerator('FnFlowProcCfg');

    gen.scrub_ = function(block, code, thisOnly) {
        const nextBlock =
            block.nextConnection && block.nextConnection.targetBlock();
        if (nextBlock && !thisOnly) {
            return code + ',\n' + gen.blockToCode(nextBlock);
        }
        return code;
    }

    gen.forBlock['processor'] = function(block, generator) {
        let code = '"version": ' + generator.valueToCode(block, 'version', 0) + ',\n';
        code += '"sourceTopic": ' + generator.valueToCode(block, 'inputTopic', 0) + ',\n';
        code += '"entityTopic": ' + generator.valueToCode(block, 'outputTopic', 0) + ',\n';
        code += '"errorTopic": ' + generator.valueToCode(block, 'errorTopic', 0) + ',\n';
        code += '"errRetentionHours": ' + generator.valueToCode(block, 'errRetentionHours', 0) + ',\n';

        const functions = generator.statementToCode(block, 'pipeline');
        code += '"pipeline": [\n' + functions + '\n]\n';
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

    gen.forBlock['pad'] = function(block, generator) {
        let code = '"name": ' + generator.valueToCode(block, 'name', 0) + ',\n';
        code += '"function": "padNormalizer",\n';
        code += '"parameters": {\n';
        let params = '"elementPath": ' + generator.valueToCode(block, 'elementPath', 0) + ',\n';
        params += '"pad": "' + block.getFieldValue('dir') + '",\n';
        params += '"length": ' + generator.valueToCode(block, 'length', 0) + ',\n';
        params += '"fillerCharacter": ' + generator.valueToCode(block, 'fillchar', 0) + '\n';
        code += generator.prefixLines(params, generator.INDENT);
        code += '}\n'
        return '{\n' + generator.prefixLines(code, generator.INDENT) + '}';
    }

    gen.forBlock['trim'] = function(block, generator) {
        let code = '"name": ' + generator.valueToCode(block, 'name', 0) + ',\n';
        code += '"function": "trimNormalizer",\n';
        code += '"parameters": {\n';
        let params = '"elementPath": ' + generator.valueToCode(block, 'elementPath', 0) + ',\n';
        params += '"mode": "' + block.getFieldValue('dir') + '"\n';
        code += generator.prefixLines(params, generator.INDENT);
        code += '}\n'
        return '{\n' + generator.prefixLines(code, generator.INDENT) + '}';
    }

    gen.forBlock['hasValue'] = function(block, generator) {
        let code = '"name": ' + generator.valueToCode(block, 'name', 0) + ',\n';
        code += '"function": "hasValueValidator",\n';
        code += '"parameters": {\n';
        const params = '"elementPath": ' + generator.valueToCode(block, 'elementPath', 0) + '\n';
        code += generator.prefixLines(params, generator.INDENT);
        code += '}\n'
        return '{\n' + generator.prefixLines(code, generator.INDENT) + '}';
    }

    gen.forBlock['Match'] = function(block, generator) {
        let code = '"name": ' + generator.valueToCode(block, 'name', 0) + ',\n';
        code += '"function": "Match",\n';
        code += '"parameters": {\n';
        let params = '"index": ' + generator.valueToCode(block, 'index', 0) + ',\n';
        params += '"template": ' + generator.valueToCode(block, 'template', 0);
        const tParamLines = generator.statementToCode(block, 'parameters').split('\n');
        const fromInput = tParamLines.filter(i => i.startsWith(generator.INDENT + "I")).map(s => generator.INDENT + s.substring(3));
        let fromInputLines = fromInput.join('\n');
        if(fromInputLines.endsWith(',')) fromInputLines = fromInputLines.slice(0, -1);
        const literal = tParamLines.filter(i => i.startsWith(generator.INDENT + "L")).map(s => generator.INDENT + s.substring(3));
        let literalLines = literal.join("\n");
        if(literalLines.endsWith(',')) literalLines = literalLines.slice(0, -1);

        if(fromInput.length > 0) params += ',\n"paramsFromInput": {\n' + generator.prefixLines(fromInputLines, generator.INDENT) + '\n}\n';
        if(literal.length > 0) params += ',\n"literalParams": {\n' + generator.prefixLines(literalLines, generator.INDENT) + '\n}\n';
        code += generator.prefixLines(params, generator.INDENT);
        code += '}\n'
        return '{\n' + generator.prefixLines(code, generator.INDENT) + '}';
    }

    gen.forBlock['tParamFromInput'] = function(block, generator) {
        return 'I' + generator.valueToCode(block, 'paramName', 0) + ': ' + generator.valueToCode(block, 'elementPath', 0)
    }

    gen.forBlock['tParamLiteral'] = function(block, generator) {
        return 'L' + generator.valueToCode(block, 'paramName', 0) + ': ' + generator.valueToCode(block, 'value', 0)
    }

    gen.forBlock['reduceToOne'] = function(block, generator) {
        let code = '"name": ' + generator.valueToCode(block, 'name', 0) + ',\n';
        code += '"function": "Reduce2One",\n';
        code += '"parameters": {\n';
        code += generator.INDENT + '"dummy": ""\n'
        code += '}\n'
        return '{\n' + generator.prefixLines(code, generator.INDENT) + '}';
    }

    gen.forBlock['mergeCreate'] = function(block, generator) {
        let code = '"name": ' + generator.valueToCode(block, 'name', 0) + ',\n';
        code += '"function": "MergeCreate",\n';
        code += '"parameters": {\n';
        const mappings = generator.statementToCode(block, 'mappings');
        let mappingBlock = '"mappings": [\n';
        mappingBlock += generator.prefixLines(generator.INDENT, mappings);
        mappingBlock += '\n]\n';
        code += generator.prefixLines(mappingBlock, generator.INDENT);
        code += '}\n'
        return '{\n' + generator.prefixLines(code, generator.INDENT) + '}';
    }

    gen.forBlock['mergeMapping'] = function(block, generator) {
        return '{"from": ' + generator.valueToCode(block, 'from', 0) + ', "to": ' + generator.valueToCode(block, 'to', 0) + '}'
    }

    return gen;
}
