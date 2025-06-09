/*
 * Copyright 2025-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.ct42.fnflow.manager.ui;

import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.dependency.NpmPackage;
import com.vaadin.flow.component.react.ReactAdapterComponent;

/**
 * @author Sajjad Safaeian
 */
@NpmPackage(value="primereact", version="10.9.5")
@NpmPackage(value="primeicons", version="7.0.0")
@JsModule("Frontend/integration/react/icon/primereact-icon.tsx")
@Tag("pr-icon")
public class PrimeIcon extends ReactAdapterComponent {

    public PrimeIcon(String iconName) {
        setIconName(iconName);
    }

    private void setIconName(String iconName) {
        setState("iconName", iconName);
    }
}
