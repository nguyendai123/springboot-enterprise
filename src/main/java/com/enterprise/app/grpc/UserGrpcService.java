package com.enterprise.app.grpc;

import com.enterprise.app.exception.ResourceNotFoundException;
import com.enterprise.app.model.entity.User;
import com.enterprise.app.repository.UserRepository;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@GrpcService
@RequiredArgsConstructor
public class UserGrpcService extends UserServiceGrpc.UserServiceImplBase {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Override
    public void getUser(GetUserRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            UUID id = UUID.fromString(request.getId());

            User user = userRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("User", id.toString()));

            responseObserver.onNext(toProto(user));
            responseObserver.onCompleted();

        } catch (IllegalArgumentException e) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Invalid UUID")
                    .asRuntimeException());
        } catch (ResourceNotFoundException e) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(e.getMessage())
                    .asRuntimeException());
        }
    }

    @Override
    public void createUser(CreateUserRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            if (userRepository.existsByEmail(request.getEmail())) {
                responseObserver.onError(Status.ALREADY_EXISTS
                        .withDescription("Email exists")
                        .asRuntimeException());
                return;
            }

            User user = User.builder()
                    .username(request.getUsername())
                    .email(request.getEmail())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .phoneNumber(request.getPhoneNumber())
                    .build();

            user = userRepository.save(user);

            responseObserver.onNext(toProto(user));
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void listUsers(ListUsersRequest request, StreamObserver<ListUsersResponse> responseObserver) {
        try {
            PageRequest pageReq = PageRequest.of(
                    request.getPage().getPage(),
                    request.getPage().getSize(),
                    Sort.by("createdAt").descending()
            );

            Page<User> page = userRepository.findAll(pageReq);

            ListUsersResponse.Builder builder = ListUsersResponse.newBuilder()
                    .setPage(PageResponse.newBuilder()
                            .setTotalPages(page.getTotalPages())
                            .setTotalElements(page.getTotalElements())
                            .setCurrentPage(page.getNumber())
                            .setPageSize(page.getSize())
                            .build());

            page.getContent().forEach(u -> builder.addUsers(toProto(u)));

            responseObserver.onNext(builder.build());
            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.withDescription(e.getMessage()).asRuntimeException());
        }
    }

    @Override
    public void watchUserEvents(GetUserRequest request, StreamObserver<UserResponse> responseObserver) {
        try {
            // demo stream 1 record
            responseObserver.onNext(UserResponse.newBuilder()
                    .setId(request.getId())
                    .setUsername("demo-stream")
                    .build());

            responseObserver.onCompleted();

        } catch (Exception e) {
            responseObserver.onError(Status.INTERNAL.asRuntimeException());
        }
    }

    private UserResponse toProto(User user) {
        UserResponse.Builder b = UserResponse.newBuilder()
                .setId(user.getId().toString())
                .setUsername(user.getUsername())
                .setEmail(user.getEmail())
                .setStatus(user.getStatus().name());

        if (user.getFirstName() != null) b.setFirstName(user.getFirstName());
        if (user.getLastName() != null) b.setLastName(user.getLastName());
        if (user.getCreatedAt() != null) b.setCreatedAt(user.getCreatedAt().format(FORMATTER));

        user.getRoles().forEach(r -> b.addRoles(r.name()));

        return b.build();
    }
}