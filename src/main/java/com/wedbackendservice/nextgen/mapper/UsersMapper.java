package com.wedbackendservice.nextgen.mapper;

import com.wedbackendservice.nextgen.dto.response.UsersResponse;
import com.wedbackendservice.nextgen.dto.response.auth.UserRegisterResponse;
import com.wedbackendservice.nextgen.dto.response.user.*;
import com.wedbackendservice.nextgen.entity.User;
import com.wedbackendservice.nextgen.entity.UserAddress;
import jakarta.mail.Address;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UsersMapper {
    UsersResponse usersResponse(User users);
    UserRegisterResponse userRegisterResponse(User users);
    AddressResponse toAddressResponse(UserAddress userAddress);
    @Mapping(target = "defaultAddress", ignore = true)
    UserMeResponse toUserMeResponse(User user);
    UserProfileResponse toUserProfileResponse(User user);
    UserAddressItemResponse toUserAddressItemResponse(UserAddress address);
    CreateAddressResponse toCreateAddressResponse(UserAddress address);
    SetDefaultAddressResponse toSetDefaultAddressResponse(UserAddress address);
}
