package com.localmovies;

import com.localmovies.client.Client;

public interface AuthenticationProvider {
    Response updateAccessToken(Client client);
}
