import {ReactElement, useRef} from 'react';
import { ReactAdapterElement, type RenderHooks } from 'Frontend/generated/flow/ReactAdapter';
import 'primereact/resources/themes/mdc-light-indigo/theme.css';
import 'primeicons/primeicons.css';

class IconElement extends ReactAdapterElement {
    protected override render(hooks: RenderHooks): ReactElement | null {
        const [iconName, setIconName] = hooks.useState<string>('iconName');
        const iconNameRef = useRef(iconName);

        return (
        <>
            <span className={iconNameRef.current} style={{padding: '0.3rem'}}></span>
        </>)
    }
}

customElements.define('pr-icon', IconElement);
