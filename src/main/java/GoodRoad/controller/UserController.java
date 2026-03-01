package GoodRoad.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import GoodRoad.model.user;
import GoodRoad.repository.UserRepository;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository repository;

    @GetMapping
    public List<user> getAll() {
        return repository.findAll();
    }

    @PostMapping
    public user create(@RequestBody user user) {
        return repository.save(user);
    }
}
