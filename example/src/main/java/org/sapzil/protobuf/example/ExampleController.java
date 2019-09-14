package org.sapzil.protobuf.example;

import com.google.protobuf.Empty;
import com.google.showcase.v1beta1.CreateUserRequest;
import com.google.showcase.v1beta1.DeleteUserRequest;
import com.google.showcase.v1beta1.GetUserRequest;
import com.google.showcase.v1beta1.IdentityRpc;
import com.google.showcase.v1beta1.ListUsersRequest;
import com.google.showcase.v1beta1.ListUsersResponse;
import com.google.showcase.v1beta1.UpdateUserRequest;
import com.google.showcase.v1beta1.User;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class ExampleController implements IdentityRpc.Controller {
    private Map<String, User> users = new HashMap<>();

    @Override
    public User createUser(CreateUserRequest request) {
        User user = request.getUser();
        users.put(user.getName(), user);
        return user;
    }

    @Override
    public User getUser(GetUserRequest request) {
        return users.get(request.getName());
    }

    @Override
    public User updateUser(UpdateUserRequest request) {
        return null;
    }

    @Override
    public Empty deleteUser(DeleteUserRequest request) {
        users.remove(request.getName());
        return Empty.getDefaultInstance();
    }

    @Override
    public ListUsersResponse listUsers(ListUsersRequest request) {
        return ListUsersResponse.newBuilder()
                .addAllUsers(users.values())
                .build();
    }
}
