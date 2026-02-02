import {ReactAdapterElement, type RenderHooks} from "Frontend/generated/flow/ReactAdapter";
import { BlocklyEditor } from '@react-blockly/web';
import {ReactElement, useEffect, useCallback, useRef} from "react";
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

// @ts-ignore
Blockly.setLocale(En)
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
        const wsStateRef = useRef(wsState)

        const [wsType, setWsType] = hooks.useState<string>('wsType');
        const wsTypeRef = useRef(wsType)

        const ref = useRef(null);

        const containerRef = useRef<HTMLDivElement>(null);
        const workspaceRef = useRef<Blockly.WorkspaceSvg | null>(null);

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
       useEffect(() => {
            if (!containerRef.current) return;

            const observer = new ResizeObserver(() => {
                // @ts-ignore
                const workspace = workspaceRef.current;

                if (workspace && typeof workspace.resize === 'function') {
                    // @ts-ignore
                    workspace.resize();

                    setTimeout(() => {
                        patchFlyoutVisuals();
                    }, 50);
                }
            });

            observer.observe(containerRef.current);

            return () => observer.disconnect();
        }, []);

        function patchFlyoutVisuals() {
            // @ts-ignore
            const workspace = workspaceRef.current;

            // @ts-ignore
            const flyout = workspace.getFlyout();

            // @ts-ignore
            const width = flyout.getWidth();

            // @ts-ignore
            const svg = workspace.getParentSvg();
            if (!svg || !svg.parentElement) return;

            const path = svg.parentElement.querySelector('.blocklyFlyout path');
            const svgFlyout = svg.parentElement.querySelector('.blocklyFlyout');

            if (path && svgFlyout) {
                // @ts-ignore
                const height = path.getBBox().height;

                path.setAttribute('d', `M 0 0 h ${width} v ${height} h -${width} Z`);
                svgFlyout.setAttribute('width', String(width));
            }
        };

        // @ts-ignore
        const onInject = useCallback((state: BlocklyCbStateType) => {
                addHelpListener(state);

                // @ts-ignore
                workspaceRef.current = state.workspace;

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

                addLabel(state);
        }, []);

        function addHelpListener(state: BlocklyCbStateType) {
            // @ts-ignore
            const workspace = state.workspace;

            const vaadinElement = containerRef.current?.closest('fnflow-blockly') as any;

            // @ts-ignore
            workspace.addChangeListener((event) => {
                if (event.type === Blockly.Events.CREATE) {
                    // @ts-ignore
                    for (const id of event.ids || []) {
                        // @ts-ignore
                        const block = workspace.getBlockById(id);
                        if (block) {
                            block.showHelp = function (this: any) {
                                const helpUrl = this.helpUrl;
                                const type = this.type;
                                if (vaadinElement?.$server?.showHelp) {
                                    vaadinElement.$server.showHelp(type, helpUrl);
                                }
                            };
                        }
                    }
                }
            });
        }

        function addLabel(state: BlocklyCbStateType) {
            // @ts-ignore
            const workspace = state.workspace;

            // @ts-ignore
            const flyout = workspace.getFlyout();

            if(!flyout) return;

            const LABEL_HEIGHT = 44;
            const MIN_WIDTH = 200;

            // @ts-ignore
            const originalGetFlyoutWidth = flyout.getWidth.bind(flyout);

            // Patch getWidth()
            flyout.getWidth = function () {
              const width = originalGetFlyoutWidth();
              return Math.max(width, MIN_WIDTH);
            };

            // @ts-ignore
            workspace.resize();

            // @ts-ignore
            const svg = workspace.getParentSvg();
            if (!svg || !svg.parentElement) return;

            const workspaceGroup = svg.parentElement.querySelector('.blocklyFlyout .blocklyBlockCanvas');
            if (!workspaceGroup) return;

                // @ts-ignore
                const width = flyout.getWidth();

                if (!workspaceGroup.querySelector('.toolbox-label')) {
                    const bg = document.createElementNS('http://www.w3.org/2000/svg', 'rect');
                    bg.setAttribute('x', '0');
                    bg.setAttribute('y', '0');
                    bg.setAttribute('width', `${width}`);
                    bg.setAttribute('height', `${LABEL_HEIGHT}`);
                    bg.setAttribute('fill', '#bbdef1');
                    bg.classList.add('toolbox-label-bg');
                    bg.setAttribute('rx', '6');
                    bg.setAttribute('ry', '6');

                    const label = document.createElementNS('http://www.w3.org/2000/svg', 'text');
                    label.setAttribute('x', '20');
                    label.setAttribute('y', '26');
                    label.textContent = 'Blocks Toolbox';
                    label.classList.add('toolbox-label');

                    // Move existing blocks down
                    const children = Array.from(workspaceGroup.children);
                    for (const child of children) {
                        if (!child.classList.contains('toolbox-label') && !child.classList.contains('toolbox-label-bg')) {
                            const currentTransform = child.getAttribute('transform') || '';
                            const match = currentTransform.match(/translate\(\s*([-\d.]+)[,\s]+([-\d.]+)\s*\)/);
                            let x = 0, y = LABEL_HEIGHT;
                            if (match) {
                                x = parseFloat(match[1]);
                                y += parseFloat(match[2]);
                            }
                            child.setAttribute('transform', `translate(${x}, ${y})`);
                        }
                    }

                    workspaceGroup.insertBefore(bg, workspaceGroup.firstChild);
                    workspaceGroup.insertBefore(label, bg.nextSibling);
              }
        }

        return (
            <div ref={containerRef} style={{ width: '100%', height: '100%' }}>
                <div ref={ref} style={{display: 'none'}}/>
                <BlocklyEditor
                    className={'editor'}
                    workspaceConfiguration={workspaceConfiguration}
                    onInject={onInject}/>
            </div>)
    }
}

customElements.define('fnflow-blockly', BlocklyElement);
