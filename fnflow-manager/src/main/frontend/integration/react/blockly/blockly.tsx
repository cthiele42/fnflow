import {ReactAdapterElement, type RenderHooks} from "Frontend/generated/flow/ReactAdapter";
import { BlocklyEditor } from '@react-blockly/web';
import {ReactElement, useCallback, useRef} from "react";
import './blockly.css';
import * as Blockly from 'blockly/core';
import * as En from 'blockly/msg/en';
import {FnFlowBlockDefinitions} from "./blockdefs";
import type {BlocklyCbStateType} from "@react-blockly/core/lib/typescript/src/types/BlocklyStateType";

import * as processorImports from './processor';
import * as projectorImports from './projector';

const imports = {
    processor: processorImports,
    projector: projectorImports,
} as const;
type KeyType = keyof typeof imports;

function showHelp(this: any) {
    window.open(this.helpUrl, 'helpview');
};

// @ts-ignore
Blockly.setLocale(En)
Blockly.defineBlocksWithJsonArray(FnFlowBlockDefinitions);

Blockly.Blocks['multipleFunctions'].onchange = function(e:any) {
    if (this.workspace.isDragging()) return;
    if (e.type !== Blockly.Events.BLOCK_MOVE) return;
    if (this.getSurroundParent() !== null && this.getSurroundParent().type === 'multipleFunctions') this.previousConnection.disconnect();
}

for(const block in Blockly.Blocks) {
    Blockly.Blocks[block].showHelp = showHelp;
}

class FixedEdgesMetricsManager extends Blockly.MetricsManager {
    constructor(workspace: Blockly.WorkspaceSvg) {
        super(workspace);
    }

    hasFixedEdges() {
        return true;
    }

    getComputedFixedEdges_(cachedViewMetrics = undefined) {
        const hScrollEnabled = this.workspace_.isMovableHorizontally();
        const vScrollEnabled = this.workspace_.isMovableVertically();

        const viewMetrics = cachedViewMetrics || this.getViewMetrics(false);

        const edges = {
            top: 0,
            bottom: undefined,
            left: 0,
            right: undefined,
        };
        if (!vScrollEnabled) {
            // @ts-ignore
            edges.bottom = edges.top + viewMetrics.height;
        }
        if (!hScrollEnabled) {
            // @ts-ignore
            edges.right = edges.left + viewMetrics.width;
        }
        return edges;
    }
}

class BlocklyElement extends ReactAdapterElement {
    protected override render(hooks: RenderHooks): ReactElement | null {
        const [wsState, setWsState] = hooks.useState<string>('wsState');
        const wsStateRef = useRef(wsState)

        const [wsType, setWsType] = hooks.useState<string>('wsType');
        const wsTypeRef = useRef(wsType)

        const ref = useRef(null);

        const workspaceConfiguration: any = {
            toolbox: imports[String(wsTypeRef.current) as KeyType].FnFlowToolbox,
            collapse: true,
            scrollbars: true,
            comments: true,
            toolboxPosition: 'end',
            plugins: {
                metricsManager: FixedEdgesMetricsManager,
            }
        };

        // @ts-ignore
        const onInject = useCallback((state: BlocklyCbStateType) => {
            // @ts-ignore
            Blockly.serialization.workspaces.load(imports[String(wsTypeRef.current) as KeyType].loadConfig(wsStateRef.current), state.workspace);
            // @ts-ignore
            state.workspace.addChangeListener(Blockly.Events.disableOrphans);

            const supportedEvents = new Set([
                Blockly.Events.BLOCK_CHANGE,
                Blockly.Events.BLOCK_CREATE,
                Blockly.Events.BLOCK_DELETE,
                Blockly.Events.BLOCK_MOVE,
            ]);

            let generator = imports[String(wsTypeRef.current) as KeyType].createGenerator();
            // @ts-ignore
            function updateCode(event) {
                // @ts-ignore
                if (state.workspace.isDragging()) return; // Don't update while changes are happening.
                if (!supportedEvents.has(event.type)) return;
                // @ts-ignore
                const code = generator.workspaceToCode(state.workspace);
                // @ts-ignore
                ref.current.setAttribute('data-code', code);
            }
            // @ts-ignore
            state.workspace.addChangeListener(updateCode);
        }, []);

        return (
            <>
                <div ref={ref} style={{display: 'none'}}/>
                <BlocklyEditor
                    className={'editor'}
                    workspaceConfiguration={workspaceConfiguration}
                    onInject={onInject}/>
            </>)
    }
}

customElements.define('fnflow-blockly', BlocklyElement);
