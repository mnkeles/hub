package etiya.omniAutomation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors.ServiceDescriptor;
import com.google.protobuf.DynamicMessage;
import com.google.protobuf.Descriptors.*;
import com.google.protobuf.util.JsonFormat;
import etiya.omniAutomation.business.dto.ApiInformationDto;
import etiya.omniAutomation.common.GrpcReflectionUtil;
import etiya.omniAutomation.common.HeaderClientInterceptor;
import io.grpc.*;
import io.grpc.MethodDescriptor;
import io.grpc.protobuf.ProtoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class GRPCService {

    private final ObjectMapper objectMapper;

    public ResponseEntity<String> callGrpc(String url, ApiInformationDto apiInformationDto) throws InterruptedException {
        Metadata metadata = new Metadata();
        try {
            JsonNode jsonNode = objectMapper.readTree(apiInformationDto.getPlIn());
            JsonNode headerNode = jsonNode.get("header");
            Optional.ofNullable(headerNode)
                    .filter(JsonNode::isObject)
                    .map(JsonNode::fields)
                    .orElse(Collections.emptyIterator())
                    .forEachRemaining(item -> metadata.put(Metadata.Key.of(item.getKey(), Metadata.ASCII_STRING_MARSHALLER), item.getValue().asText()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }

        String[] urlParts = url.split(":(?=[0-9])");
        ManagedChannel channel = ManagedChannelBuilder
                .forAddress(urlParts[0], Integer.parseInt(urlParts[1]))
                //.forAddress("localhost",9090)
                .disableRetry()
                .intercept(new HeaderClientInterceptor(metadata))
                .usePlaintext()
                .build();

        try {
            // 1) Reflection ile service tanımı çek
            String serviceName = apiInformationDto.getServiceName();
            ServiceDescriptor serviceDescriptor = GrpcReflectionUtil
                    .getServiceDescriptor(channel, serviceName);

            String methodName = apiInformationDto.getMethodName();
            DescriptorProtos.MethodDescriptorProto methodDescriptorProto = serviceDescriptor
                    .toProto()
                    .getMethodList()
                    .stream()
                    .filter(m -> m.getName().equals(methodName))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Method bulunamadı: " + methodName));

            // 2) Request message tipini al
            Descriptor requestDescriptor = GrpcReflectionUtil
                    .findMessageDescriptor(channel, methodDescriptorProto.getInputType());

            // 3) JSON -> DynamicMessage dönüşümü
            DynamicMessage.Builder requestBuilder = DynamicMessage.newBuilder(requestDescriptor);
            JsonFormat.parser().merge(apiInformationDto.getPlIn(), requestBuilder);
            DynamicMessage requestMessage = requestBuilder.build();

            // 4) Response tipini al
            Descriptor responseDescriptor = GrpcReflectionUtil
                    .findMessageDescriptor(channel, methodDescriptorProto.getOutputType());

            // 5) Dinamik MethodDescriptor oluştur
            MethodDescriptor<DynamicMessage, DynamicMessage> method = MethodDescriptor
                    .<DynamicMessage, DynamicMessage>newBuilder()
                    .setType(MethodDescriptor.MethodType.UNARY)
                    .setFullMethodName(
                            MethodDescriptor.generateFullMethodName(serviceName, methodName)
                    )
                    .setRequestMarshaller(ProtoUtils.marshaller(requestMessage))
                    .setResponseMarshaller(ProtoUtils.marshaller(
                            DynamicMessage.newBuilder(responseDescriptor).build()
                    ))
                    .build();

            // 6) Çağrıyı yap
            DynamicMessage responseMessage = io.grpc.stub.ClientCalls.blockingUnaryCall(
                    channel, method, io.grpc.CallOptions.DEFAULT, requestMessage
            );

            // 7) Cevabı JSON olarak döndür
            String print = JsonFormat.printer().includingDefaultValueFields().print(responseMessage);
            return ResponseEntity.ok(print);

        } catch (StatusRuntimeException e) {
            HttpStatus httpStatus = mapGrpcStatusToHttp(e.getStatus().getCode());
            return ResponseEntity.status(httpStatus).body(e.getStatus().toString());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        } finally {
            channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
        }
    }

    private HttpStatus mapGrpcStatusToHttp(Status.Code grpcCode) {
        switch (grpcCode) {
            case OK: return HttpStatus.OK;
            case INVALID_ARGUMENT: return HttpStatus.BAD_REQUEST;
            case NOT_FOUND: return HttpStatus.NOT_FOUND;
            case PERMISSION_DENIED: return HttpStatus.FORBIDDEN;
            case UNAUTHENTICATED: return HttpStatus.UNAUTHORIZED;
            case UNAVAILABLE: return HttpStatus.SERVICE_UNAVAILABLE;
            default: return HttpStatus.INTERNAL_SERVER_ERROR;
        }
    }

}
