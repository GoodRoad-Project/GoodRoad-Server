package goodroad.obstacle;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users/obstacles")
@RequiredArgsConstructor
public class UserObstaclePolicyController {

    private final UserObstaclePolicyService policyService;

    @GetMapping("")
    public List<UserObstaclePolicyService.PolicyItem> getUserObstaclePolicies(Authentication authentication) {
        return policyService.getUserObstaclePolicies(authentication.getName());
    }

    @PutMapping("")
    public List<UserObstaclePolicyService.PolicyItem> replaceUserObstaclePolicies(
            Authentication authentication,
            @RequestBody UserObstaclePolicyService.ReplacePolicyReq req
    ) {
        return policyService.replaceUserObstaclePolicies(authentication.getName(), req);
    }
}