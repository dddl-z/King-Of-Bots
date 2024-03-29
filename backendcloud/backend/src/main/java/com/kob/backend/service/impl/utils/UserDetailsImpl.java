package com.kob.backend.service.impl.utils;

import com.kob.backend.pojo.User;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDetailsImpl implements UserDetails { // 重写 UserDetails 接口里的方法，返回对应的用户信息

    private User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return null;
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() { // 是否没过期
        return true;
    }

    @Override
    public boolean isAccountNonLocked() { // 是否没有被锁定
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() { // 授权是否不过期
        return true;
    }

    @Override
    public boolean isEnabled() { // 该用户是否被启用
        return true;
    }
}
