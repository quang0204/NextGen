package com.wedbackendservice.nextgen.module.user.mapper;

import com.wedbackendservice.nextgen.module.user.dto.client.response.AddressResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.CreateAddressResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.SetDefaultAddressResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.UserAddressItemResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.UserMeResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.UserProfileResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.UserRegisterResponse;
import com.wedbackendservice.nextgen.module.user.dto.client.response.UsersResponse;
import com.wedbackendservice.nextgen.module.user.dto.portal.response.CreateInternalUserResponse;
import com.wedbackendservice.nextgen.module.user.entity.User;
import com.wedbackendservice.nextgen.module.user.entity.UserAddress;
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

  @Mapping(source = "user.userId", target = "userId")
  CreateAddressResponse toCreateAddressResponse(UserAddress address);

  @Mapping(source = "addressId", target = "defaultAddressId")
  SetDefaultAddressResponse toSetDefaultAddressResponse(UserAddress address);

  CreateInternalUserResponse toCreateInternalUserResponse(User users);
}
