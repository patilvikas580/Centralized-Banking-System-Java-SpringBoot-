package com.proj.Banking_System.Services;

import com.proj.Banking_System.DTO.*;
import com.proj.Banking_System.Entity.Role;
import com.proj.Banking_System.Entity.User;
import com.proj.Banking_System.Repository.UserRepository;
import com.proj.Banking_System.Utils.AccountUtils;
import com.proj.Banking_System.config.JwtTokenProvider;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    @Autowired
    UserRepository userRepository;
    @Autowired
    EmailService emailService;
    @Autowired
    TransactionService transactionService;
    @Autowired
    PasswordEncoder passwordEncoder;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @Override
    @Transactional
    public BankResponse createAccount(UserRequest userRequest) {
        if (userRepository.existsByEmail(userRequest.getEmail())) {
            return BankResponse.builder()
                    .responseCode("001")
                    .responseMessage("Account already exists")
                    .accountInfo(null).build();

        }
        User newUser =  User.builder().firstName(userRequest.getFirstName())
                .lastName(userRequest.getLastName()).otherName(userRequest.getOtherName())
                .gender(userRequest.getGender()).address(userRequest.getAddress())
                .stateofOrigin(userRequest.getStateofOrigin())
                .accountNumber(AccountUtils.generateAccountNumber())
                .Acount_balance(BigDecimal.ZERO).email(userRequest.getEmail())
                .password(passwordEncoder.encode(userRequest.getPassword()))
                .phoneNumber(userRequest.getPhoneNumber())
                .alternativePhoneNumber(userRequest.getAlternativePhoneNumber()).status("ACTIVE")
                .role(Role.valueOf("ROLE_ADMIN"))
                .build();

        User savedUser = userRepository.save(newUser);

        EmailDetails emailDetails=EmailDetails.builder().
                recipient(savedUser.getEmail()).subject("New Bank Account Created").messageBody("Congratulations! your account has been successfully created with VM Banking solutions.\n Your Account Details:\n" +
                        " Account Name :"+savedUser.getFirstName()+" "+savedUser.getOtherName()+" "+savedUser.getLastName()+"\n Account Number:"+savedUser.getAccountNumber()).build();
        try {
            emailService.sendEmailAlert(emailDetails);
        } catch (Exception e) {
            log.error("Failed to send alert", e);
        }

        return BankResponse.builder().responseCode("002")
                .responseMessage("Account created successfully").accountInfo(AccountInfo.builder()
                        .accountBalance(savedUser.getAcount_balance()).accountNumber(savedUser.getAccountNumber())
                        .accountName(savedUser.getFirstName()+" "+savedUser.getOtherName()+" "+savedUser.getLastName())
                        .build()).build();
    }
    public BankResponse login(LoginDto loginDto ) {

        Authentication authentication=null;
        authentication=authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginDto.getEmail(), loginDto.getPassword())
        );
        EmailDetails loginDetails=EmailDetails.builder().subject("You're logged in").
                recipient(loginDto.getEmail()).messageBody("you're logged into your account.If you did not initiate this request,please contact patilvikas580@gmail.com").build();

        emailService.sendEmailAlert(loginDetails);
        return BankResponse.builder().responseCode("Login successfull").responseMessage(jwtTokenProvider.generateToken(authentication)).
                build();
    }

    @Override
    public BankResponse balanceEnquiry(EnquiryRequest request) {
        if (userRepository.existsByAccountNumber(request.getAccountNumber())) {
            User foundUser=userRepository.findUserByAccountNumber(request.getAccountNumber());
            return BankResponse.builder().responseCode("001").responseMessage("Account fetched successfully").accountInfo(AccountInfo.builder()
                    .accountBalance(foundUser.getAcount_balance()).accountNumber(request.getAccountNumber()).accountName(foundUser.getFirstName()+" "+foundUser.getOtherName()+" "+foundUser.getLastName()).build()).build();
        }
        return BankResponse.builder().responseCode("003").responseMessage("Account not exist")
                .accountInfo(null).build();
    }

//    @Override
//    public String nameEnquiry(EnquiryRequest request) {
//        if (userRepository.existsByAccountNumber(request.getAccountNumber())) {
//            User foundUser=userRepository.findUserByAccountNumber(request.getAccountNumber());
//            return foundUser.getFirstName()+" "+foundUser.getOtherName()+" "+foundUser.getLastName();
//        }
//        return BankResponse.builder().responseMessage(AccountUtils.ACCOUNT_NOT_EXIST_MESSAGE)
//                .build().toString();
//    }

    @Override
    @Transactional
    public BankResponse creditAccount(CrediDebitRequest request) {
        User userToCredit=userRepository.findUserByAccountNumber(request.getAccountNumber());
        if (userToCredit!=null){
            userToCredit.setAcount_balance(userToCredit.getAcount_balance().add(request.getAmount()));
            userRepository.save(userToCredit);
            TransactionDto transactionDto=TransactionDto.builder().accountNumber(userToCredit.getAccountNumber())
                    .transactionType("CREDIT")
                    .amount(request.getAmount())
                    .build();
            EmailDetails creditAlertAccount=EmailDetails.builder().recipient(userToCredit.getEmail()).subject("Account Credited").messageBody("Rs."+request.getAmount()+" has been Credited in your Account :"+userToCredit.getAccountNumber()+
                    "\nYour current balance is Rs."+userToCredit.getAcount_balance()).build();
            transactionService.saveTransaction(transactionDto);
            try {
                emailService.sendEmailAlert(creditAlertAccount);
            } catch (Exception e) {
                log.error("Failed to send credit alert", e);
            }


            return BankResponse.builder().responseCode("05").responseMessage("Account credited ").accountInfo(AccountInfo.builder()
                    .accountName(userToCredit.getFirstName()+" "+userToCredit.getOtherName()+" "+userToCredit.getLastName()).accountNumber(userToCredit.getAccountNumber()).accountBalance(userToCredit.getAcount_balance()).build()).build();
        }
        return BankResponse.builder().responseCode("003").responseMessage("Account does not exist")
                .accountInfo(null).build();
    }

    @Override
    @Transactional
    public BankResponse debitAccount(CrediDebitRequest request) {
        User userToDebit=userRepository.findUserByAccountNumber(request.getAccountNumber());
        if (userToDebit!=null){
            if(userToDebit.getAcount_balance().subtract(request.getAmount()).compareTo(BigDecimal.ZERO)>=0){
                userToDebit.setAcount_balance(userToDebit.getAcount_balance().subtract(request.getAmount()));
                userRepository.save(userToDebit);
                TransactionDto transactionDto=TransactionDto.builder().accountNumber(userToDebit.getAccountNumber())
                        .transactionType("DEBIT")
                        .amount(request.getAmount())
                        .build();
                transactionService.saveTransaction(transactionDto);
                EmailDetails debitAlertAccount=EmailDetails.builder().recipient(userToDebit.getEmail()).subject("Account Debited").messageBody("Rs."+request.getAmount()+" has been debited from your Account :"+userToDebit.getAccountNumber()+
                        "\nYour current balance is Rs."+userToDebit.getAcount_balance()).build();
                try {
                    emailService.sendEmailAlert(debitAlertAccount);
                } catch (Exception e) {
                    log.error("Failed to send Debit Account alert", e);
                }

                return BankResponse.builder().responseCode("007").responseMessage("Account Debited").accountInfo(AccountInfo.builder()
                        .accountName(userToDebit.getFirstName()+" "+userToDebit.getOtherName()+" "+userToDebit.getLastName()).accountNumber(userToDebit.getAccountNumber()).accountBalance(userToDebit.getAcount_balance()).build()).build();
            }else {
                return BankResponse.builder().responseCode("007").responseMessage("Insufficient Account Balance")
                        .accountInfo(AccountInfo.builder()
                                .accountName(userToDebit.getFirstName()+" "+userToDebit.getOtherName()+" "+userToDebit.getLastName()).accountNumber(userToDebit.getAccountNumber()).accountBalance(userToDebit.getAcount_balance()).build()).build();
            }
        }
        return BankResponse.builder().responseCode("003").responseMessage("Account does not exist")
                .accountInfo(null).build();
    }

    @Override
    @Transactional
    public BankResponse transfer(TransferRequest request) {
        User sourceUser=userRepository.findUserByAccountNumber(request.getSourceAccountNumber());
        if (sourceUser==null) {
            return BankResponse.builder().responseCode("003").responseMessage("Source Account Not Exist")
                    .accountInfo(null).build();
        }
        User destinationUser=userRepository.findUserByAccountNumber(request.getDestinationAccountNumber());
        if (destinationUser==null) {
            return BankResponse.builder().responseCode("003").responseMessage("Destination Account Not Exist")
                    .accountInfo(null).build();
        }
        if (destinationUser.getAccountNumber().equals(sourceUser.getAccountNumber())) {
            return BankResponse.builder().responseCode("555").responseMessage("Enter different Account numbers as both Account numbers which you entered are identical")
                    .accountInfo(null).build();
        }
        if (!(request.getAmount().compareTo(sourceUser.getAcount_balance())>0)){
          sourceUser.setAcount_balance(sourceUser.getAcount_balance().subtract(request.getAmount()));
          userRepository.save(sourceUser);
          destinationUser.setAcount_balance(destinationUser.getAcount_balance().add(request.getAmount()));
          userRepository.save(destinationUser);
          EmailDetails debitAlert=EmailDetails.builder().recipient(sourceUser.getEmail()).subject("Account Debited").messageBody("Rs."+request.getAmount()+" has been debited from your Account :"+sourceUser.getAccountNumber()+
                  "\nYour current balance is :"+sourceUser.getAcount_balance()+"\nTransfered to: "+destinationUser.getFirstName()+" "+destinationUser.getOtherName()+" "+destinationUser.getLastName()+"\n"+"Account Number :"+destinationUser.getAccountNumber()).build();
          EmailDetails creditAlert=EmailDetails.builder().subject("Account Credited").recipient(destinationUser.getEmail()).messageBody("Your Account has been credited for Rs."+request.getAmount()+"\n" +"Account Number :"+destinationUser.getAccountNumber()+"\n"+"Current Balance :"+destinationUser.getAcount_balance()).build();
          emailService.sendEmailAlert(debitAlert);
            TransactionDto transactionDto=TransactionDto.builder().accountNumber(sourceUser.getAccountNumber())
                    .transactionType("DEBIT")
                    .amount(request.getAmount())
                    .build();
                    transactionService.saveTransaction(transactionDto);
          emailService.sendEmailAlert(creditAlert);
            TransactionDto transactionDto1=TransactionDto.builder().accountNumber(destinationUser.getAccountNumber())
                    .transactionType("CREDIT")
                    .amount(request.getAmount())
                    .build();
            transactionService.saveTransaction(transactionDto1);

          return BankResponse.builder().responseMessage("Transfer Successful|| Your current Balance is "+sourceUser.getAcount_balance()).build();
        }
        return BankResponse.builder().responseCode("007").responseMessage("Insufficient Account Balance "+"Required Defficiet Amount :"+(request.getAmount().subtract(sourceUser.getAcount_balance())))
                .accountInfo(AccountInfo.builder()
                        .accountName(sourceUser.getFirstName()+" "+sourceUser.getOtherName()+" "+sourceUser.getLastName()).accountNumber(sourceUser.getAccountNumber()).accountBalance(sourceUser.getAcount_balance()).build()).build();

    }
}
