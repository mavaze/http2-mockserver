package io.github.mavaze.mockserver.config;

import lombok.Data;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@Data
public class StubConfig {

    private ListenConfig ports;
    private List<Stubbing> paths;

    @Data
    public static class ListenConfig {
        private int http;
        private int https;
    }

    @Data
    @ToString(of = "name")
    public static class Stubbing {
        private String name;
        private RequestSpecification request;
        private List<ResponseSpecification> responses;

        public final String getName() {
            if (name != null && name.trim().length() > 0) {
                return name;
            }
            this.name = String.format("Methods: %s; URI: %s", request.getMethods(),  request.uri);
            return this.name;
        }

        @Data
        public static class RequestSpecification {
            private String methods;
            private String uri;
        }

        @Data
        @ToString(of = "name")
        public static class ResponseSpecification {
            private String name;
            private int status;
            private Map<String, String> headers;
            private ResponseBody body;
            private String selector = "default";

            public final String getName() {
                if (name != null && name.trim().length() > 0) {
                    return name;
                }
                this.name = String.format("Status: %d; Selector: %s", status, selector);
                return this.name;
            }

            @Data
            public static class ResponseBody {
                private String file;
                private String inline;
                private Format format;
            }

            public enum Format {
                INLINE, FILE, REQUEST, EMPTY
            }
        }
    }
}
