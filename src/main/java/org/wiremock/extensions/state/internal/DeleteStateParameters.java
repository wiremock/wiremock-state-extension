package org.wiremock.extensions.state.internal;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class DeleteStateParameters {
    private String context;
    private ListParameters list;

    public ListParameters getList() {
        return list;
    }

    public void setList(ListParameters list) {
        this.list = list;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ListParameters {
        private Boolean deleteFirst;
        private Boolean deleteLast;
        private String deleteIndex;
        private Where deleteWhere;

        public Boolean getDeleteFirst() {
            return deleteFirst;
        }

        public void setDeleteFirst(Boolean deleteFirst) {
            this.deleteFirst = deleteFirst;
        }

        public Boolean getDeleteLast() {
            return deleteLast;
        }

        public void setDeleteLast(Boolean deleteLast) {
            this.deleteLast = deleteLast;
        }

        public String getDeleteIndex() {
            return deleteIndex;
        }

        public void setDeleteIndex(String deleteIndex) {
            this.deleteIndex = deleteIndex;
        }

        public Where getDeleteWhere() {
            return deleteWhere;
        }

        public void setDeleteWhere(Where deleteWhere) {
            this.deleteWhere = deleteWhere;
        }

        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Where {
            private String property;
            private String value;

            public String getProperty() {
                return property;
            }

            public void setProperty(String property) {
                this.property = property;
            }

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }
        }
    }
}
