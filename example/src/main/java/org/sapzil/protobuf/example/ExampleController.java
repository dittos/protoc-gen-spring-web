package org.sapzil.protobuf.example;

import com.google.showcase.v1beta1.CreateUserRequest;
import com.google.showcase.v1beta1.GetUserRequest;
import com.google.showcase.v1beta1.IdentityRpc;
import com.google.showcase.v1beta1.User;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;

@RestController
public class ExampleController implements IdentityRpc.Controller {
    private Map<String, User> users = new HashMap<>();

    @Override
    public User createUser(CreateUserRequest request, HttpServletRequest context) {
        User user = request.getUser();
        users.put(user.getName(), user);
        return user;
    }

    @Override
    public User getUser(GetUserRequest request, HttpServletRequest context) {
        return users.get(request.getName());
    }
}
