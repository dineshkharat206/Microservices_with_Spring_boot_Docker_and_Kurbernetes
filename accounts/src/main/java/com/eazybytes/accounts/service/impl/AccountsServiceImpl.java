package com.eazybytes.accounts.service.impl;

import com.eazybytes.accounts.constants.AccountsConstants;
import com.eazybytes.accounts.dto.AccountsDto;
import com.eazybytes.accounts.dto.CustomerDto;
import com.eazybytes.accounts.entity.Accounts;
import com.eazybytes.accounts.entity.Customer;
import com.eazybytes.accounts.exception.CustomerAlreadyExistsException;
import com.eazybytes.accounts.exception.ResourceNotFoundException;
import com.eazybytes.accounts.mapper.AccountsMapper;
import com.eazybytes.accounts.mapper.CustomerMapper;
import com.eazybytes.accounts.repository.AccountsRepository;
import com.eazybytes.accounts.repository.CustomerRepositoy;
import com.eazybytes.accounts.service.IAccountsService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@AllArgsConstructor
public class AccountsServiceImpl implements IAccountsService {

    private AccountsRepository accountsRepository;

    private CustomerRepositoy customerRepositoy;

    @Override
    public void createAccount(CustomerDto customerDto) {

        Customer customer = CustomerMapper.mapToCustomer(customerDto, new Customer());
        Optional<Customer> optionalCustomer = customerRepositoy.findByMobileNumber(customerDto.getMobileNumber());

        if (optionalCustomer.isPresent()) {
            throw new CustomerAlreadyExistsException("Customer Already Registered with this Mobile Number"
                    +customerDto.getMobileNumber());
        }

        customer.setCreatedAt(LocalDateTime.now());
        customer.setCreatedBy("Annonymous");

        Customer savedCustomer = customerRepositoy.save(customer);

        accountsRepository.save(createNewAccount(savedCustomer));

    }

    private Accounts createNewAccount(Customer customer) {
        Accounts newAccount = new Accounts();
        newAccount.setCustomerId(customer.getCustomerId());
        long randomAccNumber = 1000000000L + new Random().nextInt(900000000);

        newAccount.setAccountNumber(randomAccNumber);
        newAccount.setAccountType(AccountsConstants.SAVINGS);
        newAccount.setBranchAddress(AccountsConstants.ADDRESS);

        newAccount.setCreatedAt(LocalDateTime.now());
        newAccount.setCreatedBy("Annonymous");
        return newAccount;
    }

    @Override
    public CustomerDto fetchAccountDetails(String mobileNumber) {
        Customer customer = customerRepositoy.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)
        );

        Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(
                () -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString())
        );

        CustomerDto customerDto = CustomerMapper.mapToCustomerDto(customer, new CustomerDto());
        customerDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));

        return customerDto;
    }

    @Override
    public boolean updateAccount(CustomerDto customerDto) {

        boolean isUpdated = false;

        AccountsDto accountsDto = customerDto.getAccountsDto();
        if (accountsDto != null) {
            Accounts accounts = accountsRepository.findById(accountsDto.getAccountNumber()).orElseThrow(
                    () -> new ResourceNotFoundException("Account", "AccountNumber", accountsDto.getAccountNumber().toString())
            );

            AccountsMapper.mapToAccounts(accountsDto, accounts);
            accountsRepository.save(accounts);

            Long customerId = accounts.getCustomerId();
            Customer customer = customerRepositoy.findById(customerId).orElseThrow(
                    () -> new ResourceNotFoundException("Customer", "CustomerId", customerId.toString())
            );

            CustomerMapper.mapToCustomer(customerDto, customer);
            customerRepositoy.save(customer);

            isUpdated = true;
        }

        return isUpdated;
    }

    @Override
    public boolean deleteAccount(String mobileNumber)
    {
        Customer customer = customerRepositoy.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "Mobile Number", mobileNumber)
        );

        accountsRepository.deleteByCustomerId(customer.getCustomerId());
        customerRepositoy.deleteById(customer.getCustomerId());
        return true;
    }
}
