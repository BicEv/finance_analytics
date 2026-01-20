package ru.bicev.finance_analytics.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ru.bicev.finance_analytics.dto.UserDto;
import ru.bicev.finance_analytics.entity.User;
import ru.bicev.finance_analytics.service.UserService;

@RestController
@RequestMapping("/api/users")
public class UserRestController {

    private final UserService userService;

    public UserRestController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        User user = userService.getCurrentUser();
        UserDto userDto = new UserDto(user.getId(), user.getEmail(), user.getName());
        return ResponseEntity.status(HttpStatus.OK).body(userDto);
    }

}
