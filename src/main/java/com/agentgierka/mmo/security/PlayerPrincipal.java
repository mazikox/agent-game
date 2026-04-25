package com.agentgierka.mmo.security;

import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;
import java.util.UUID;

@Getter
public class PlayerPrincipal extends User {
    private final UUID playerId;

    public PlayerPrincipal(String username, String password, Collection<? extends GrantedAuthority> authorities, UUID playerId) {
        super(username, password, authorities);
        this.playerId = playerId;
    }

    public boolean isSamePlayer(UUID otherPlayerId) {
        return this.playerId.equals(otherPlayerId);
    }
}
