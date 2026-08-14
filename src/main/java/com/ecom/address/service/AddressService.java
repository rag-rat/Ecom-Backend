package com.ecom.address.service;

import com.ecom.address.dto.AddressResponse;
import com.ecom.address.dto.CreateAddressRequest;
import com.ecom.address.dto.UpdateAddressRequest;
import com.ecom.address.entity.Address;
import com.ecom.address.mapper.AddressMapper;
import com.ecom.address.repository.AddressRepository;
import com.ecom.exception.BusinessException;
import com.ecom.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;



@Service
@RequiredArgsConstructor
@Slf4j
public class AddressService {

    private final AddressRepository addressRepository;
    private final AddressMapper addressMapper;


    // createAddress()

    public AddressResponse createAddress(String userId, CreateAddressRequest request) {

        boolean isFirstAddress = addressRepository.countByUserId(userId) == 0;
        boolean shouldBeDefault = request.isSetAsDefault() || isFirstAddress;

//        System.out.println(shouldBeDefault);

        if (shouldBeDefault) {
            demoteCurrentDefault(userId);
        }

        Address address = Address.builder()
                .userId(userId)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .country(request.getCountry() != null ? request.getCountry() : "India")
                .defaultIs(shouldBeDefault)
                .build();


        Address saved = addressRepository.save(address);

        log.info("Address created for user {}: {} (default={})",
                userId, saved.getId(), shouldBeDefault);

        return addressMapper.toResponse(saved);
    }



    // updateAddress()

    public AddressResponse updateAddress(
            String userId, String addressId, UpdateAddressRequest request) {

        Address address = getOwnedAddressOrThrow(userId, addressId);

        if (request.getFullName() != null) address.setFullName(request.getFullName());
        if (request.getPhoneNumber() != null) address.setPhoneNumber(request.getPhoneNumber());
        if (request.getAddressLine1() != null) address.setAddressLine1(request.getAddressLine1());
        if (request.getAddressLine2() != null) address.setAddressLine2(request.getAddressLine2());
        if (request.getCity() != null) address.setCity(request.getCity());
        if (request.getState() != null) address.setState(request.getState());
        if (request.getPincode() != null) address.setPincode(request.getPincode());
        if (request.getCountry() != null) address.setCountry(request.getCountry());

        Address updated = addressRepository.save(address);
        log.info("Address {} updated for user {}", addressId, userId);
        return addressMapper.toResponse(updated);

    }



    // deleteAddress()

    public void deleteAddress(String userId, String addressId) {

        Address address = getOwnedAddressOrThrow(userId, addressId);
        boolean wasDefault = address.isDefaultIs();

        addressRepository.delete(address);
        log.info("Address {} deleted for user {}", addressId, userId);

        if (wasDefault) {

            List<Address> remaining = addressRepository.findByUserId(userId);

            if (!remaining.isEmpty()) {
                Address mostRecent = remaining.stream()
                        .max(Comparator.comparing(Address::getCreatedAt))
                        .orElseThrow();
                mostRecent.setDefaultIs(true);
                addressRepository.save(mostRecent);

                log.info("Address {} auto-promoted to default for user {}",
                        mostRecent.getId(), userId);
            }

        }
    }



    // setDefaultAddress()

    public AddressResponse setDefaultAddress(String userId, String addressId) {

        Address address = getOwnedAddressOrThrow(userId, addressId);

        if (address.isDefaultIs()) {
            // Already default — nothing to do, return as-is (idempotent,
            // same philosophy as WishlistService's duplicate-add handling)
            return addressMapper.toResponse(address);
        }

        demoteCurrentDefault(userId);

        address.setDefaultIs(true);
        Address updated = addressRepository.save(address);

        log.info("Address {} set as default for user {}", addressId, userId);

        return addressMapper.toResponse(updated);
    }



    // getAllAddresses()

    public List<AddressResponse> getAllAddresses(String userId) {

        List<Address> addresses = addressRepository.findByUserId(userId);

        addresses.sort(Comparator.comparing(Address::isDefaultIs).reversed());

        return addressMapper.toResponseList(addresses);

    }



    // getAddressById()

    public AddressResponse getAddressById(String userId, String addressId) {

        Address address = getOwnedAddressOrThrow(userId, addressId);
        return addressMapper.toResponse(address);

    }




    // PRIVATE HELPERS


    // getOwnedAddressOrThrow()

    private Address getOwnedAddressOrThrow(String userId, String addressId) {

        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Address not found: " + addressId));

        if (!address.getUserId().equals(userId)) {
            throw new BusinessException("You can only manage your own addresses");
        }

        return address;
    }



    // demoteCurrentDefault()

    private void demoteCurrentDefault(String userId) {

        addressRepository.findByUserIdAndDefaultIsIsTrue(userId)
                .ifPresent(current -> {
                    current.setDefaultIs(false);
                    addressRepository.save(current);
                });

    }

}

