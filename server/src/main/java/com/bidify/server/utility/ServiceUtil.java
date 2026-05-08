package com.bidify.server.utility;

import com.bidify.common.enums.RequestStatus;
import com.bidify.common.exception.ValidationException;
import com.bidify.common.model.Response;
import com.bidify.server.exception.DatabaseException;

import java.time.format.DateTimeParseException;
import java.util.function.Supplier;

public class ServiceUtil {
    private ServiceUtil() {}

    public static Response handleRequest(Supplier<Response> action) {
        try {
            return action.get();
        }
        catch (DateTimeParseException e) {
            return new Response(RequestStatus.FAILED, "Invalid date time format");
        }
        catch (ValidationException | DatabaseException e) {
            return new Response(RequestStatus.FAILED, e.getMessage());
        }
        catch (Exception e) {
            return new Response(RequestStatus.ERROR, "Internal server error: " + e.getMessage());
        }
    }
}
