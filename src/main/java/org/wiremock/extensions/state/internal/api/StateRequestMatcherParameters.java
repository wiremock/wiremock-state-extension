/*
 * Copyright (C) 2023 Dirk Bolte
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.wiremock.extensions.state.internal.api;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class StateRequestMatcherParameters {
    String hasContext;

    String hasNotContext;
    String updateCountEqualTo;
    String updateCountLessThan;
    String updateCountMoreThan;
    String listSizeEqualTo;
    String listSizeLessThan;
    String listSizeMoreThan;

    public String getHasNotContext() {
        return hasNotContext;
    }

    public void setHasNotContext(String hasNotContext) {
        this.hasNotContext = hasNotContext;
    }

    public String getHasContext() {
        return hasContext;
    }

    public void setHasContext(String hasContext) {
        this.hasContext = hasContext;
    }

    public String getUpdateCountEqualTo() {
        return updateCountEqualTo;
    }

    public void setUpdateCountEqualTo(String updateCountEqualTo) {
        this.updateCountEqualTo = updateCountEqualTo;
    }

    public String getUpdateCountLessThan() {
        return updateCountLessThan;
    }

    public void setUpdateCountLessThan(String updateCountLessThan) {
        this.updateCountLessThan = updateCountLessThan;
    }

    public String getUpdateCountMoreThan() {
        return updateCountMoreThan;
    }

    public void setUpdateCountMoreThan(String updateCountMoreThan) {
        this.updateCountMoreThan = updateCountMoreThan;
    }

    public String getListSizeEqualTo() {
        return listSizeEqualTo;
    }

    public void setListSizeEqualTo(String listSizeEqualTo) {
        this.listSizeEqualTo = listSizeEqualTo;
    }

    public String getListSizeLessThan() {
        return listSizeLessThan;
    }

    public void setListSizeLessThan(String listSizeLessThan) {
        this.listSizeLessThan = listSizeLessThan;
    }

    public String getListSizeMoreThan() {
        return listSizeMoreThan;
    }

    public void setListSizeMoreThan(String listSizeMoreThan) {
        this.listSizeMoreThan = listSizeMoreThan;
    }
}
