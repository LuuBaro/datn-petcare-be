package org.example.petcarebe.service;

import org.example.petcarebe.dto.request.AddressRequest;
import org.example.petcarebe.model.Address;
import org.example.petcarebe.model.User;
import org.example.petcarebe.repository.AddressRepository;
import org.example.petcarebe.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AddressService {

    @Autowired
    private AddressRepository addressRepository;

    @Autowired
    private UserRepository userRepository;

    public List<Address> getAddressesByUserId(Long userId) {
        return addressRepository.findByUserUserId(userId);
    }

    public List<Address> getAllAddresses() {
        return addressRepository.findAll();
    }

    public Address createAddress(Long userId, AddressRequest addressRequest) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Address address = new Address();
        address.setUser(user);
        address.setProvince(addressRequest.getProvince());
        address.setDistrict(addressRequest.getDistrict());
        address.setWard(addressRequest.getWard());
        address.setStreet(addressRequest.getStreet());

        return addressRepository.save(address);
    }

    public void deleteAddress(Long addressId) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Địa chỉ không tồn tại!"));
        addressRepository.delete(address);
    }

    public Address updateAddress(Long addressId, Long userId, AddressRequest addressRequest) {
        Address address = addressRepository.findById(addressId)
                .orElseThrow(() -> new RuntimeException("Address not found"));
        if (!address.getUser().getUserId().equals(userId)) {
            throw new RuntimeException("Unauthorized to update this address");
        }

        address.setProvince(addressRequest.getProvince());
        address.setDistrict(addressRequest.getDistrict());
        address.setWard(addressRequest.getWard());
        address.setStreet(addressRequest.getStreet());


        return addressRepository.save(address);
    }
}
