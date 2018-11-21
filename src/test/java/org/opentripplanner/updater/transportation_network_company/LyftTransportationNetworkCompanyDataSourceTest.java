package org.opentripplanner.updater.transportation_network_company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.opentripplanner.routing.transportation_network_company.ArrivalTime;
import org.opentripplanner.routing.transportation_network_company.RideEstimate;
import org.opentripplanner.updater.transportation_network_company.lyft.LyftAuthenticationRequestBody;
import org.opentripplanner.updater.transportation_network_company.lyft.LyftTransportationNetworkCompanyDataSource;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.junit.Assert.assertEquals;

public class LyftTransportationNetworkCompanyDataSourceTest {

    private static LyftTransportationNetworkCompanyDataSource source = new LyftTransportationNetworkCompanyDataSource(
        "http://localhost:8089/",
        "testClientId",
        "testClientSecret"
    );

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(
        options()
            .port(8089)
            .usingFilesUnderDirectory("src/test/resources/updater/")
    );

    @Before
    public void setUp() throws Exception {
        // setup mock server to respond to ride estimate request
        ObjectMapper mapper = new ObjectMapper();
        stubFor(
            post(urlPathEqualTo("/oauth/token"))
                .withBasicAuth("testClientId", "testClientSecret")
                .withRequestBody(equalToJson(mapper.writeValueAsString(new LyftAuthenticationRequestBody(
                    "client_credentials",
                    "public"
                ))))
                .willReturn(
                    aResponse()
                        .withBodyFile("lyft_authentication.json")
                )
        );
    }

    @Test
    public void testGetArrivalTimes () throws IOException, ExecutionException {
        // setup mock server to respond to ride estimate request
        stubFor(
            get(urlPathEqualTo("/v1/eta"))
                .withQueryParam("lat", equalTo("1.2"))
                .withQueryParam("lng", equalTo("3.4"))
                .willReturn(
                    aResponse()
                        .withBodyFile("lyft_eta_estimates.json")
                )
        );

        List<ArrivalTime> arrivalTimes = source.getArrivalTimes(1.2, 3.4);

        assertEquals(arrivalTimes.size(),  3);
        ArrivalTime arrival = arrivalTimes.get(0);
        assertEquals(arrival.displayName, "Lyft Line");
        assertEquals(arrival.productId, "lyft_line");
        assertEquals(arrival.estimatedSeconds, 120);
    }

    @Test
    public void testGetEstimatedRideTime () throws IOException, ExecutionException {
        // setup mock server to respond to estimated ride time request
        stubFor(
            get(urlPathEqualTo("/v1/cost"))
                .withQueryParam("start_lat", equalTo("1.2"))
                .withQueryParam("start_lng", equalTo("3.4"))
                .withQueryParam("end_lat", equalTo("1.201"))
                .withQueryParam("end_lng", equalTo("3.401"))
                .willReturn(
                    aResponse()
                        .withBodyFile("lyft_trip_estimates.json")
                )
        );

        RideEstimate rideTime = source.getRideEstimate(
            "lyft",
            1.2,
            3.4,
            1.201,
            3.401
        );

        assertEquals(rideTime.duration, 913);
    }
}
