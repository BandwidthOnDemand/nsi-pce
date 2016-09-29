/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.es.nsi.pce.pf;

import java.util.Optional;
import java.util.function.Supplier;
import javax.ws.rs.WebApplicationException;
import net.es.nsi.pce.path.api.Exceptions;

/**
 *
 * @author hacksaw
 */
public class ExceptionSupplier implements Supplier<WebApplicationException> {
    private Optional<WebApplicationException> exception = Optional.empty();

    public ExceptionSupplier(WebApplicationException exception) {
        this.exception = Optional.ofNullable(exception);
    }

    public ExceptionSupplier() {
    }

    @Override
    public WebApplicationException get()
    {
        return new WebApplicationException(exception.orElse(Exceptions.internalServerError("Unknown error has occured")));

    }
}
