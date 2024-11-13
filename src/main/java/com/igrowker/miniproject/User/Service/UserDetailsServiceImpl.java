package com.igrowker.miniproject.User.Service;

import com.igrowker.miniproject.Config.Jwt.JwtUtils;
import com.igrowker.miniproject.User.Dto.AuthCreateUserRequestDto;
import com.igrowker.miniproject.User.Dto.AuthLoginRequestDto;
import com.igrowker.miniproject.User.Dto.AuthResponseDto;
import com.igrowker.miniproject.User.Dto.AuthResponseRegisterDto;
import com.igrowker.miniproject.User.Model.Role;
import com.igrowker.miniproject.User.Model.UserEntity;
import com.igrowker.miniproject.User.Repository.RoleRepository;
import com.igrowker.miniproject.User.Repository.UserRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
@Validated
public class UserDetailsServiceImpl implements UserDetailsService {

    private final PasswordEncoder passwordEncoder;
    private final UserRepository userRepository;
    private final JwtUtils jwtUtils;
    //private final RoleRepository roleRepository;

    /*@Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByuserName(username).orElseThrow(() -> new UsernameNotFoundException(
                "El usuario" + username + "no existe"));

        List<SimpleGrantedAuthority> authorities = new ArrayList<>();

        userEntity.getRoles().forEach(role -> {
            authorities.add(new SimpleGrantedAuthority("ROLE_".concat(role.getEnumRole().name())));
        });

        userEntity.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .forEach(permission -> authorities.add(new SimpleGrantedAuthority(permission.getName())));

        return new User(userEntity.getUserName(),
                userEntity.getPassword(),
                userEntity.isEnabled(),
                userEntity.isAccountNoExpired(),
                userEntity.isCredentialsNoExpired(),
                userEntity.isAccountNoLocked(),
                authorities);
    }*/

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserEntity userEntity = userRepository.findByuserName(username)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "El usuario " + username + " no existe"));

        // Construye las autoridades basándose en el único rol del usuario
        List<SimpleGrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(userEntity.getRole()));

        return new User(
                userEntity.getUserName(),
                userEntity.getPassword(),
                userEntity.isEnabled(),
                userEntity.isAccountNoExpired(),
                userEntity.isCredentialsNoExpired(),
                userEntity.isAccountNoLocked(),
                authorities
        );
    }


    public AuthResponseDto loginUser(@Valid AuthLoginRequestDto authDto) {
        String username = authDto.username();
        String password = authDto.password();

        Long id = userRepository.findByuserName(username)
                .map(UserEntity::getId)
                .orElseThrow(() -> new UsernameNotFoundException("El Id del usuario " + username + " no existe"));

        log.debug("Attempting to authenticate user: {}", username);

        try{
            Authentication authentication = this.authenticate(username, password);
            SecurityContextHolder.getContext().setAuthentication(authentication);
            log.debug("User authenticated successfully. Generating JWT token.");
            String token = jwtUtils.generateJwtToken(authentication);
            log.debug("JWT token generated successfully.");
            return new AuthResponseDto(id, username, "User logged successfully", token, true);
        }
        catch (Exception e) {
            log.error("Authentication failed for user: {}", username, e);
            throw e;
        }
    }

    public Authentication authenticate(String username, String password) {
        UserDetails userDetails = loadUserByUsername(username);
        if(userDetails == null) {
            throw new BadCredentialsException("Invalid username or password");
        }

        if(!passwordEncoder.matches(password, userDetails.getPassword())) {
            throw new BadCredentialsException("Invalid password");
        }

        return new UsernamePasswordAuthenticationToken(userDetails, userDetails.getPassword(), userDetails.getAuthorities());

    }

    /*public AuthResponseRegisterDto createUser(AuthCreateUserRequestDto authCreateUserDto) {

        String username = authCreateUserDto.userName();
        String password = authCreateUserDto.password();
        String email = authCreateUserDto.email();

        log.debug("Attempting to create user: username: {}, email: {}, password: {}", username, email, password);
        List<String> roles = authCreateUserDto.roleDto().roles();
        log.debug("User roles: {}", roles);

        Set<Role> roleEntities = new HashSet<>(roleRepository.findRoleEntitiesByEnumRoleIn(roles));

        log.debug("Role entities: {}", roleEntities);

        if(roleEntities.isEmpty()){
            throw new IllegalArgumentException("Los roles especificados no existen");
        }

        UserEntity userEntity = UserEntity.builder()
                .userName(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .roles(roleEntities)
                .isEnabled(true)
                .accountNoLocked(true)
                .accountNoExpired(true)
                .credentialsNoExpired(true)
                .build();

        log.debug("User entity: {}", userEntity);

        UserEntity userCreated = userRepository.save(userEntity);
        log.debug("User created: {}", userCreated);
        ArrayList<SimpleGrantedAuthority> authoritiesList = new ArrayList<>();

        log.debug("User roles: {}", userCreated.getRoles());

        userCreated.getRoles().forEach(role -> {
            authoritiesList.add(new SimpleGrantedAuthority("ROLE_".concat(role.getEnumRole().name())));
        });
        log.debug("User authorities: {}", authoritiesList);

        userCreated.getRoles().stream()
                .flatMap(role -> role.getPermissions().stream())
                .forEach(permission -> authoritiesList.add(new SimpleGrantedAuthority(permission.getName())));
        log.debug("User permissions: {}", authoritiesList);

        Authentication authentication = new UsernamePasswordAuthenticationToken(userCreated.getUserName(), userCreated.getPassword(), authoritiesList);
        log.debug("User authentication: {}", authentication);
        String accessToken = jwtUtils.generateJwtToken(authentication);

        log.debug("JWT token generated successfully.");

        return new AuthResponseRegisterDto(username, "User created successfully", accessToken, true);
        //return new AuthResponseRegisterDto(username, "User created successfully", accessToken, true);
    }*/

    public AuthResponseRegisterDto createUser(AuthCreateUserRequestDto authCreateUserDto) {

        String username = authCreateUserDto.userName();
        String password = authCreateUserDto.password();
        String email = authCreateUserDto.email();

        log.debug("Attempting to create user: username: {}, email: {}", username, email);

        String role = "ROLE_USER";

        UserEntity userEntity = UserEntity.builder()
                .userName(username)
                .password(passwordEncoder.encode(password))
                .email(email)
                .role(role) // Asigna directamente el rol como un string
                .isEnabled(true)
                .accountNoLocked(true)
                .accountNoExpired(true)
                .credentialsNoExpired(true)
                .build();

        log.debug("User entity: {}", userEntity);

        UserEntity userCreated = userRepository.save(userEntity);
        log.debug("User created: {}", userCreated);

        // Construye las autoridades
        List<SimpleGrantedAuthority> authoritiesList = new ArrayList<>();
        authoritiesList.add(new SimpleGrantedAuthority(role));
        log.debug("User authorities: {}", authoritiesList);

        Authentication authentication = new UsernamePasswordAuthenticationToken(userCreated.getUserName(), userCreated.getPassword(), authoritiesList);
        log.debug("User authentication: {}", authentication);

        String accessToken = jwtUtils.generateJwtToken(authentication);
        log.debug("JWT token generated successfully.");

        return new AuthResponseRegisterDto(username, "User created successfully", accessToken, true);
    }

}
