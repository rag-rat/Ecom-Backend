package com.ecom.address.controller;

import com.ecom.address.dto.AddressResponse;
import com.ecom.address.dto.CreateAddressRequest;
import com.ecom.address.dto.UpdateAddressRequest;
import com.ecom.address.service.AddressService;
import com.ecom.common.dto.ApiResponse;
import com.ecom.security.CustomUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
@Tag(name = "Addresses", description = "Customer shipping address management")
public class AddressController {

    private final AddressService addressService;


    // getAllAddresses()

    @GetMapping
    @Operation(summary = "List all addresses for the current user (default first)")
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getAllAddresses(
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        List<AddressResponse> response = addressService.getAllAddresses(userDetails.getUsername());
        return ResponseEntity.ok(ApiResponse.success("Addresses fetched", response));
    }



    // getAddressById()

    @GetMapping("/{addressId}")
    @Operation(summary = "Get a specific address")
    public ResponseEntity<ApiResponse<AddressResponse>> getAddressById(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String addressId) {

        AddressResponse response =
                addressService.getAddressById(userDetails.getUsername(), addressId);
        return ResponseEntity.ok(ApiResponse.success("Address fetched", response));
    }



    // createAddress()

    @PostMapping
    @Operation(summary = "Add a new address")
    public ResponseEntity<ApiResponse<AddressResponse>> createAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateAddressRequest request) {

        AddressResponse response =
                addressService.createAddress(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Address added successfully", response));
    }



    // updateAddress()

    @PutMapping("/{addressId}")
    @Operation(summary = "Update an address (partial update)")
    public ResponseEntity<ApiResponse<AddressResponse>> updateAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String addressId,
            @Valid @RequestBody UpdateAddressRequest request) {

        AddressResponse response =
                addressService.updateAddress(userDetails.getUsername(), addressId, request);
        return ResponseEntity.ok(ApiResponse.success("Address updated successfully", response));
    }



    // deleteAddress()

    @DeleteMapping("/{addressId}")
    @Operation(summary = "Delete an address")
    public ResponseEntity<ApiResponse<Void>> deleteAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String addressId) {

        addressService.deleteAddress(userDetails.getUsername(), addressId);
        return ResponseEntity.ok(ApiResponse.success("Address deleted successfully"));
    }



    // setDefaultAddress()

    @PutMapping("/{addressId}/set-default")
    @Operation(summary = "Set an address as the default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefaultAddress(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String addressId) {

        AddressResponse response =
                addressService.setDefaultAddress(userDetails.getUsername(), addressId);
        return ResponseEntity.ok(ApiResponse.success("Default address updated", response));
    }

}
