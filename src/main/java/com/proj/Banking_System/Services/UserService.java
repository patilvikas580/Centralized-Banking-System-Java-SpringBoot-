package com.proj.Banking_System.Services;

import com.proj.Banking_System.DTO.*;

public interface UserService {
    BankResponse createAccount(UserRequest  userRequest);
    BankResponse balanceEnquiry(EnquiryRequest request);

    default String nameEnquiry(EnquiryRequest request){
        return null;
    };
    BankResponse creditAccount(CrediDebitRequest request);
    BankResponse debitAccount(CrediDebitRequest request);
    BankResponse transfer(TransferRequest request);
    BankResponse login(LoginDto loginDto);
}
