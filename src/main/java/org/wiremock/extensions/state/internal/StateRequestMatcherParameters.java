package org.wiremock.extensions.state.internal;

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
