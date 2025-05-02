import {ReactAdapterElement, type RenderHooks} from "Frontend/generated/flow/ReactAdapter";
import { BlocklyEditor } from '@react-blockly/web';
import {ReactElement, useCallback, useRef} from "react";
import './blockly.css';
import * as Blockly from 'blockly/core';
import {FnFlowToolbox} from "./toolbox";
import {FnFlowBlockDefinitions} from "./blockdefs";
import {loadPipelineConfig} from "./load"
import {createGenerator} from "./generator";

Blockly.defineBlocksWithJsonArray(FnFlowBlockDefinitions);

Blockly.Blocks['multipleFunctions'].onchange = function(e:any) {
    if (this.workspace.isDragging()) return;
    if (e.type !== Blockly.Events.BLOCK_MOVE) return;
    if (this.getSurroundParent() !== null && this.getSurroundParent().type === 'multipleFunctions') this.previousConnection.disconnect();
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
        const [wsCode, setWsCode] = hooks.useState<string>('wsCode')
        const wsStateRef = useRef(wsState)

        const workspaceConfiguration: any = {
            toolbox: FnFlowToolbox,
            collapse: true,
            scrollbars: true,
            comments: true,
            toolboxPosition: 'end',
            plugins: {
                metricsManager: FixedEdgesMetricsManager,
            }
        };

        const onInject: ({workspace, xml, json}: { workspace: any; xml: any; json: any }) => void = useCallback(({ workspace, xml, json }) => {
            Blockly.serialization.workspaces.load(loadPipelineConfig(wsStateRef.current), workspace);
            workspace.addChangeListener(Blockly.Events.disableOrphans);

            const supportedEvents = new Set([
                Blockly.Events.BLOCK_CHANGE,
                Blockly.Events.BLOCK_CREATE,
                Blockly.Events.BLOCK_DELETE,
                Blockly.Events.BLOCK_MOVE,
            ]);

            const generator = createGenerator();
            // @ts-ignore
            function updateCode(event) {
                if (workspace.isDragging()) return; // Don't update while changes are happening.
                if (!supportedEvents.has(event.type)) return;
                const code = generator.workspaceToCode(workspace);
                setWsCode(code);
            }
            workspace.addChangeListener(updateCode);
        }, []);

        // @ts-ignore
        return <BlocklyEditor className={'editor'} workspaceConfiguration={workspaceConfiguration} onInject={onInject}/>
    }
}

customElements.define('fnflow-blockly', BlocklyElement);
