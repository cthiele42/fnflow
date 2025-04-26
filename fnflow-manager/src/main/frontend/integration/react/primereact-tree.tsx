import type { ReactElement } from 'react';
import {Tree} from 'primereact/tree';
import { ReactAdapterElement, type RenderHooks } from 'Frontend/generated/flow/ReactAdapter';
import {TreeNode} from "primereact/treenode";
import 'primereact/resources/themes/mdc-light-indigo/theme.css';
import 'Frontend/integration/react/pr-tree.css';
import 'primeicons/primeicons.css';

class TreeElement extends ReactAdapterElement {
    protected override render(hooks: RenderHooks): ReactElement | null {
        const [nodes, setNodes] = hooks.useState<TreeNode[]>('nodes');
        const [selectedKey, setSelectedKey] = hooks.useState<string>('selectedKey');

        return <Tree value={nodes} selectionMode="single" selectionKeys={selectedKey} onSelectionChange={(e) => setSelectedKey(e.value as string)} className="pr-tree"/>
    }
}

customElements.define('pr-tree', TreeElement);
