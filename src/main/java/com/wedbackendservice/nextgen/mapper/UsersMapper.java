package com.wedbackendservice.nextgen.mapper;

import com.wedbackendservice.nextgen.dto.response.UsersResponse;
import com.wedbackendservice.nextgen.dto.response.auth.UserRegisterResponse;
import com.wedbackendservice.nextgen.entity.User;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface UsersMapper {
    UsersResponse usersResponse(User users);
    UserRegisterResponse userRegisterResponse(User users);
}
