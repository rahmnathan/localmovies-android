package com.localmovies;

import com.localmovies.client.Client;

public interface AuthenticationProvider {
    Response updateAuthenticationToken(Client client);
}
