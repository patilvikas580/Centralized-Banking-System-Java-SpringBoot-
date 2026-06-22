package com.proj.Banking_System.controller;

import com.proj.Banking_System.DTO.*;
import com.proj.Banking_System.Services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user")
public class UserController {


    @Autowired
    UserService userService;

    @PostMapping
    public BankResponse createAccount(@RequestBody UserRequest userRequest){
        return  userService.createAccount(userRequest);
    }

    @PostMapping("/login")
    public BankResponse login(@RequestBody LoginDto loginDto){
        return userService.login(loginDto);
    }

    @GetMapping("/balanceEnquiry")
    public BankResponse balanceEnquiry(@RequestBody EnquiryRequest request){
        return userService.balanceEnquiry(request);
    }

//    @GetMapping("/nameEnquiry")
//    public String nameEnquiry(@RequestBody EnquiryRequest request){
//        return userService.nameEnquiry(request);
//    }

    @PostMapping("/creditACcountBalance")
    public BankResponse creditBalance(@RequestBody CrediDebitRequest request){
        return userService.creditAccount(request);
    }

    @PostMapping("/debitACcountBalance")
    public BankResponse debitBalance(@RequestBody CrediDebitRequest request){
        return userService.debitAccount(request);
    }

    @PostMapping("/transferMoney")
    public BankResponse transferMoney(@RequestBody TransferRequest request){
       return userService.transfer(request);
    }
}
