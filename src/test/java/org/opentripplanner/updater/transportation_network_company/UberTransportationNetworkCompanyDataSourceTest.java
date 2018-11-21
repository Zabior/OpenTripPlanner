package org.opentripplanner.updater.transportation_network_company;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;
import org.opentripplanner.routing.transportation_network_company.ArrivalTime;
import org.opentripplanner.routing.transportation_network_company.RideEstimate;
import org.opentripplanner.updater.transportation_network_company.uber.UberTransportationNetworkCompanyDataSource;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.assertEquals;

public class UberTransportationNetworkCompanyDataSourceTest {

    private static UberTransportationNetworkCompanyDataSource source = new UberTransportationNetworkCompanyDataSource(
        "test",
        "http://localhost:8089/"
    );

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(
        options()
            .port(8089)
            .usingFilesUnderDirectory("src/test/resources/updater/")
    );

    @Test
    public void testGetArrivalTimes () throws IOException, ExecutionException {
        // setup mock server to respond to ride estimate request
        stubFor(
            get(urlPathEqualTo("/estimates/time"))
                .withQueryParam("start_latitude", equalTo("1.2"))
                .withQueryParam("start_longitude", equalTo("3.4"))
                .willReturn(
                    aResponse()
                        .withBodyFile("uber_arrival_estimates.json")
                )
        );

        List<ArrivalTime> arrivalTimes = source.getArrivalTimes(1.2, 3.4);

        assertEquals(arrivalTimes.size(),  8);
        ArrivalTime arrival = arrivalTimes.get(0);
        assertEquals(arrival.displayName, "POOL");
        assertEquals(arrival.productId, "26546650-e557-4a7b-86e7-6a3942445247");
        assertEquals(arrival.estimatedSeconds, 60);
    }

    @Test
    public void testGetEstimatedRideTime () throws IOException, ExecutionException {
        // setup mock server to respond to estimated ride time request
        stubFor(
            get(urlPathEqualTo("/estimates/price"))
                .withQueryParam("start_latitude", equalTo("1.2"))
                .withQueryParam("start_longitude", equalTo("3.4"))
                .withQueryParam("end_latitude", equalTo("1.201"))
                .withQueryParam("end_longitude", equalTo("3.401"))
                .willReturn(
                    aResponse()
                        .withBodyFile("uber_trip_estimates.json")
                )
        );

        RideEstimate rideTime = source.getRideEstimate(
            "26546650-e557-4a7b-86e7-6a3942445247",
            1.2,
            3.4,
            1.201,
            3.401
        );

        assertEquals(rideTime.duration, 1080);
    }
}
