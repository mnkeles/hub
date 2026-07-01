package etiya.omniAutomation.common;

import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos;
import com.google.protobuf.Descriptors;
import com.google.protobuf.DynamicMessage;
import io.grpc.ManagedChannel;
import io.grpc.reflection.v1.*;
import io.grpc.stub.StreamObserver;
import lombok.extern.log4j.Log4j2;

import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Log4j2
public class GrpcReflectionUtil {

    public static Descriptors.ServiceDescriptor getServiceDescriptor(ManagedChannel channel, String serviceName) throws Exception {
        List<Descriptors.FileDescriptor> fileDescriptors = getAllFileDescriptors(channel, serviceName);
        for (Descriptors.FileDescriptor fd : fileDescriptors) {
            Descriptors.ServiceDescriptor sd = fd.findServiceByName(getSimpleName(serviceName));
            if (sd != null) {
                return sd;
            }
        }
        throw new RuntimeException("Service not found: " + serviceName);
    }

    public static Descriptors.Descriptor findMessageDescriptor(ManagedChannel channel, String typeName) throws Exception {
        List<Descriptors.FileDescriptor> fileDescriptors = getAllFileDescriptors(channel, typeName);
        for (Descriptors.FileDescriptor fd : fileDescriptors) {
            Descriptors.Descriptor md = fd.findMessageTypeByName(getSimpleName(typeName));
            if (md != null) {
                return md;
            }
        }
        throw new RuntimeException("Message type not found: " + typeName);
    }

    private static String getSimpleName(String fullName) {
        if (fullName.contains(".")) {
            return fullName.substring(fullName.lastIndexOf(".") + 1);
        }
        return fullName;
    }

    private static List<Descriptors.FileDescriptor> getAllFileDescriptors(ManagedChannel channel, String symbol) throws Exception {
        ServerReflectionGrpc.ServerReflectionStub stub = ServerReflectionGrpc.newStub(channel);
        CountDownLatch latch = new CountDownLatch(1);
        List<Descriptors.FileDescriptor> resultList = new ArrayList<>();
        Set<String> seenFiles = new HashSet<>();

        StreamObserver<ServerReflectionResponse> responseObserver = new StreamObserver<>() {
            @Override
            public void onNext(ServerReflectionResponse value) {
                if (value.hasFileDescriptorResponse()) {
                    for (ByteString protoBytes : value.getFileDescriptorResponse().getFileDescriptorProtoList()) {
                        try {
                            DescriptorProtos.FileDescriptorProto fdp = DescriptorProtos.FileDescriptorProto.parseFrom(protoBytes);
                            resultList.add(buildFileDescriptor(fdp, resultList));
                        } catch (Exception e) {
                            throw new RuntimeException(e);
                        }
                    }
                } else if (value.hasListServicesResponse()) {
                    // Service list debug
                    System.out.println("Services: " + value.getListServicesResponse().getServiceList());
                }
                latch.countDown();
            }
            @Override
            public void onError(Throwable t) {
                t.printStackTrace();
                latch.countDown();
            }
            @Override
            public void onCompleted() {
                latch.countDown();
            }
        };

        StreamObserver<ServerReflectionRequest> requestObserver =
                stub.serverReflectionInfo(responseObserver);

        requestObserver.onNext(
                ServerReflectionRequest.newBuilder()
                        .setHost("localhost")
                        .setFileContainingSymbol("com.example.grpc.MultiService")
                        .build()
                // Düzenle 
        );
        requestObserver.onCompleted();

        latch.await(5, TimeUnit.SECONDS);
        return resultList;
    }

    private static Descriptors.FileDescriptor buildFileDescriptor(
            DescriptorProtos.FileDescriptorProto fdp,
            List<Descriptors.FileDescriptor> deps) throws Descriptors.DescriptorValidationException {

        List<Descriptors.FileDescriptor> dependencyDescriptors = new ArrayList<>();
        for (String depName : fdp.getDependencyList()) {
            for (Descriptors.FileDescriptor depFd : deps) {
                if (depFd.getName().equals(depName)) {
                    dependencyDescriptors.add(depFd);
                }
            }
        }
        return Descriptors.FileDescriptor.buildFrom(fdp,
                dependencyDescriptors.toArray(new Descriptors.FileDescriptor[0]));
    }

}
