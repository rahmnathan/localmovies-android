package com.localmovies;

import com.localmovies.client.Client;

public interface AuthenticationProvider {
    void updateAccessToken(Client client);
}
