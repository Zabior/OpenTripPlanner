package org.opentripplanner.api.resource;

import org.opentripplanner.api.model.ApiPlace;
import org.opentripplanner.api.model.TransportationNetworkCompanyResponse;
import org.opentripplanner.routing.transportation_network_company.TransportationNetworkCompanyService;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import static org.opentripplanner.api.resource.ServerInfo.Q;

@Path("/routers/{routerId}/transportation_network_company")
public class TransportationNetworkCompanyResource {
    public static final String[] ACCEPTED_RIDE_TYPES = {
        "lyft", // standard lyft pickup
        "a6eef2e1-c99a-436f-bde9-fefb9181c0b0" // uberX
    };
    private static final Logger LOG = LoggerFactory.getLogger(TransportationNetworkCompanyResource.class);

    @Context
    OTPServer otpServer;

    @GET
    @Path("/eta_estimate")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q})
    public TransportationNetworkCompanyResponse getEtaEstimates(
        @QueryParam("from") String from,
        @QueryParam("companies") String companies,
        @PathParam("routerId") String routerId
    ) {
        LOG.info("Received eta_estimate request");

        TransportationNetworkCompanyResponse response = new TransportationNetworkCompanyResponse();

        try {
            requireParameter(companies, "companies");
            ApiPlace fromPlace = getPlace(from, "from");
            TransportationNetworkCompanyService service = getService(routerId);
            response.setEtaEstimates(service.getArrivalTimes(fromPlace.lat, fromPlace.lon, companies));
        } catch (Exception e) {
            response.setError(e);
        }

        return response;
    }

    @GET
    @Path("/ride_estimate")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q})
    public TransportationNetworkCompanyResponse getRideEstimate(
        @QueryParam("from") String from,
        @QueryParam("to") String to,
        @QueryParam("company") String company,
        @QueryParam("rideType") String rideType,
        @PathParam("routerId") String routerId
    ) {
        LOG.info("Received ride_estimate request");

        TransportationNetworkCompanyResponse response = new TransportationNetworkCompanyResponse();

        try {
            requireParameter(company, "company");
            requireParameter(rideType, "rideType");
            ApiPlace fromPlace = getPlace(from, "from");
            ApiPlace toPlace = getPlace(to, "to");
            TransportationNetworkCompanyService service = getService(routerId);
            response.setRideEstimate(service.getRideEstimate(company, rideType, fromPlace, toPlace));
        } catch (Exception e) {
            response.setError(e);
        }

        return response;
    }

    private ApiPlace getPlace(String positionString, String paramName) throws Exception {
        requireParameter(positionString, paramName);

        String[] fields = positionString.split(",");
        double latitude, longitude;
        try {
            latitude = Double.parseDouble(fields[0]);
            longitude = Double.parseDouble(fields[1]);
        } catch (Exception e) {
            throw new Exception("Invalid format of " + paramName + " parameter.  Should be `lat,lon`.");
        }

        // TODO TNC - these methods does not exist
        //          - We should replace lat,lon with a coordinate type that does this validation,
        //          - feel free to add the validation here as a intermediate step
        //checkRangeInclusive(latitude, -90, 90);
        //checkRangeInclusive(longitude, -180, 180);

        ApiPlace place = new ApiPlace();
        place.lat = latitude;
        place.lon = longitude;

        return place;
    }

    private TransportationNetworkCompanyService getService(String routerId) {
        Router router = otpServer.getRouter(routerId);
        if (router == null) {
            throw new UnsupportedOperationException("Unable to find router with id: " + routerId);
        }

        TransportationNetworkCompanyService service = router.graph.getService(TransportationNetworkCompanyService.class);
        if (service == null) {
            throw new UnsupportedOperationException("Unconfigured Transportaiton Network Company service for router with id: " + routerId);
        }

        return service;
    }

    private void requireParameter(String param, String paramName) {
        if (param == null) {
            throw new UnsupportedOperationException(paramName + " paramater is required");
        }
    }
}
