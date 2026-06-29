package etiya.omniAutomation.common;

import io.grpc.*;

public class HeaderClientInterceptor implements ClientInterceptor {
    private final Metadata headers;

    public HeaderClientInterceptor(Metadata headers) {
        this.headers = headers;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {
        return new ForwardingClientCall.SimpleForwardingClientCall<>(next.newCall(method, callOptions)) {
            @Override
            public void start(Listener<RespT> responseListener, Metadata headersFromCall) {
                headersFromCall.merge(headers);
                super.start(responseListener, headersFromCall);
            }
        };
    }
}