package com.orienet.tresorie.security;

import com.orienet.tresorie.model.Utilisateur;
import com.orienet.tresorie.repository.UtilisateurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UtilisateurRepository utilisateurRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilisateur utilisateur = utilisateurRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Utilisateur introuvable : " + username));

        return new User(
                utilisateur.getUsername(),
                utilisateur.getPassword(),
                utilisateur.isActif(),
                true, true, true,
                List.of(new SimpleGrantedAuthority(utilisateur.getRole()))
        );
    }
}
