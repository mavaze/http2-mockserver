package io.github.mavaze.mockserver.config;

import lombok.Data;

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
    public static class Stubbing {
        private RequestSpecification request;
        private List<ResponseSpecification> responses;

        @Data
        public static class RequestSpecification {
            private String methods;
            private String uri;
        }

        @Data
        public static  class ResponseSpecification {
            private int status;
            private Map<String, String> headers;
            private ResponseBody body;
            private String selector;

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
