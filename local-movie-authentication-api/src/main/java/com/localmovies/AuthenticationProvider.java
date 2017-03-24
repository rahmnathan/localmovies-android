package com.localmovies;

import com.phoneinfo.Phone;

public interface AuthenticationProvider {
    Response updateAuthenticationToken(Phone phone);
}
