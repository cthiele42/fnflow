import {ReactElement, useRef} from 'react';
import {Tree, TreeNodeTemplateOptions} from 'primereact/tree';
import { ContextMenu } from 'primereact/contextmenu';
import { Toast } from 'primereact/toast';
import { ReactAdapterElement, type RenderHooks } from 'Frontend/generated/flow/ReactAdapter';
import {TreeNode} from "primereact/treenode";
import 'primereact/resources/themes/mdc-light-indigo/theme.css';
import 'Frontend/integration/react/tree/pr-tree.css';
import 'primeicons/primeicons.css';

interface DeploymentAction {
    type: string;
    key: string;
}

class TreeElement extends ReactAdapterElement {
    protected override render(hooks: RenderHooks): ReactElement | null {
        const cmCommendEvent = hooks.useCustomEvent<DeploymentAction>("execute");
        const [nodes, setNodes] = hooks.useState<TreeNode[]>('nodes');
        const [selectedKey, setSelectedKey] = hooks.useState<string>('selectedKey');
        const toast = useRef(null);
        const cmContainer = useRef(null);
        const cmDepl = useRef(null);
        const deplContainerMenu = [
            {
                label: 'New',
                icon: 'pi pi-search',
                command: () => {
                    cmCommendEvent({type: 'new', key: selectedKey});
                }
            }
        ];
        const deplMenu = [
            {
                label: 'Load',
                icon: 'pi pi-search',
                command: () => {
                    cmCommendEvent({type: 'load', key: selectedKey});
                }
            },
            {
                label: 'Delete',
                icon: 'pi pi-search',
                command: () => {
                    cmCommendEvent({type: 'delete', key: selectedKey});
                }
            }
        ];

        const nodeTemplate = (node: TreeNode, options: TreeNodeTemplateOptions) => {
            let label = <>{node.label}</>;

            if (node.data) {
                label = <>{node.label} <span className="info">{node.data}</span></>;
            }

            return <span className={options.className}>{label}</span>;
        }

        return (
        <>
            <Toast ref={toast}/>
            <ContextMenu model={deplContainerMenu} ref={cmContainer}/>
            <ContextMenu model={deplMenu} ref={cmDepl}/>

            <div className="menu-title">Deployments</div>
            <Tree value={nodes} nodeTemplate={nodeTemplate}
                selectionMode="single"
                selectionKeys={selectedKey}
                  onContextMenuSelectionChange={(e) => setSelectedKey(e.value as string)}
                onContextMenu={(e) => {
                    // @ts-ignore
                    const key = e.node.key.toString();
                    if(key === 'procs' || key === 'projs') {
                        // @ts-ignore
                        cmContainer.current.show(e.originalEvent);
                    } else if(key.startsWith('proc-') || key.startsWith('projector-')) {
                        // @ts-ignore
                        cmDepl.current.show(e.originalEvent);
                    }
                }}
                onNodeDoubleClick={(e) => {
                    // @ts-ignore
                    const key = e.node.key.toString();
                    if(key.startsWith('proc-') || key.startsWith('projector-')) {
                        cmCommendEvent({type: 'load', key: key});
                    }
                }}
                className="pr-tree"/>
        </>)
    }
}

customElements.define('pr-tree', TreeElement);
