package com.example.springsagaorchestrator.account.controller;

import com.example.springsagaorchestrator.account.domain.UserAccount;
import com.example.springsagaorchestrator.account.repository.UserAccountRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/accounts")
public class AccountController {

    private final UserAccountRepository userAccountRepository;

    public AccountController(UserAccountRepository userAccountRepository) {
        this.userAccountRepository = userAccountRepository;
    }

    /**
     * 사용자 계좌 및 현재 잔액 조회 API
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<UserAccount> getAccountByUserId(@PathVariable Long userId) {
        return userAccountRepository.findByUserId(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}
