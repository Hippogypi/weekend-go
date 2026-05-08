package com.weekendgo.amap;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.weekendgo.amap.dto.AmapGeocode;
import com.weekendgo.amap.dto.AmapPoi;
import com.weekendgo.amap.dto.AmapReverseGeocode;
import com.weekendgo.amap.exception.AmapServiceException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

class AmapClientTest {

    private AmapClient client;
    private MockRestServiceServer server;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplateBuilder().build();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = new AmapClient(restTemplate, new AmapProperties("test-key", "https://restapi.amap.com"));
    }

    @Test
    void searchAroundUsesConfiguredKeyAndMapsPois() {
        server.expect(requestTo("https://restapi.amap.com/v3/place/around?key=test-key&location=116.481488,39.990464&keywords=cafe&radius=1000&offset=10&page=1&extensions=base"))
                .andRespond(withSuccess("""
                        {
                          "status": "1",
                          "info": "OK",
                          "pois": [
                            {
                              "id": "B000A8UIN8",
                              "name": "Office Coffee",
                              "type": "餐饮服务",
                              "address": "望京街",
                              "location": "116.481488,39.990464",
                              "adname": "朝阳区"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<AmapPoi> pois = client.searchAround("116.481488,39.990464", "cafe", 1000, 1, 10);

        assertThat(pois).hasSize(1);
        assertThat(pois.get(0).id()).isEqualTo("B000A8UIN8");
        assertThat(pois.get(0).name()).isEqualTo("Office Coffee");
        assertThat(pois.get(0).location()).isEqualTo("116.481488,39.990464");
        server.verify();
    }

    @Test
    void searchByKeywordUsesConfiguredKeyAndMapsPois() {
        server.expect(requestTo("https://restapi.amap.com/v3/place/text?key=test-key&keywords=library&city=%E5%8C%97%E4%BA%AC&offset=20&page=2&extensions=base"))
                .andRespond(withSuccess("""
                        {
                          "status": "1",
                          "info": "OK",
                          "pois": [
                            {
                              "id": "B0FFF",
                              "name": "City Library",
                              "type": "科教文化服务",
                              "address": "学院路",
                              "location": "116.300000,39.900000",
                              "adname": "海淀区"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<AmapPoi> pois = client.searchByKeyword("library", "北京", 2, 20);

        assertThat(pois).extracting(AmapPoi::name).containsExactly("City Library");
        server.verify();
    }

    @Test
    void geocodeUsesConfiguredKeyAndMapsCoordinates() {
        server.expect(requestTo("https://restapi.amap.com/v3/geocode/geo?key=test-key&address=%E5%8C%97%E4%BA%AC%E5%B8%82%E6%9C%9D%E9%98%B3%E5%8C%BA%E6%9C%9B%E4%BA%AC%E8%A1%97&city=%E5%8C%97%E4%BA%AC"))
                .andRespond(withSuccess("""
                        {
                          "status": "1",
                          "info": "OK",
                          "geocodes": [
                            {
                              "formatted_address": "北京市朝阳区望京街",
                              "province": "北京市",
                              "city": "北京市",
                              "district": "朝阳区",
                              "location": "116.481488,39.990464"
                            }
                          ]
                        }
                        """, MediaType.APPLICATION_JSON));

        List<AmapGeocode> geocodes = client.geocode("北京市朝阳区望京街", "北京");

        assertThat(geocodes).hasSize(1);
        assertThat(geocodes.get(0).formattedAddress()).isEqualTo("北京市朝阳区望京街");
        assertThat(geocodes.get(0).location()).isEqualTo("116.481488,39.990464");
        server.verify();
    }

    @Test
    void reverseGeocodeUsesConfiguredKeyAndMapsAddress() {
        server.expect(requestTo("https://restapi.amap.com/v3/geocode/regeo?key=test-key&location=116.481488,39.990464&radius=1000&extensions=base"))
                .andRespond(withSuccess("""
                        {
                          "status": "1",
                          "info": "OK",
                          "regeocode": {
                            "formatted_address": "北京市朝阳区望京街",
                            "addressComponent": {
                              "province": "北京市",
                              "city": "北京市",
                              "district": "朝阳区"
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        AmapReverseGeocode reverseGeocode = client.reverseGeocode("116.481488,39.990464", 1000);

        assertThat(reverseGeocode.formattedAddress()).isEqualTo("北京市朝阳区望京街");
        assertThat(reverseGeocode.district()).isEqualTo("朝阳区");
        server.verify();
    }

    @Test
    void throwsServiceExceptionWhenAmapRejectsRequest() {
        server.expect(requestTo("https://restapi.amap.com/v3/place/text?key=test-key&keywords=cafe&offset=20&page=1&extensions=base"))
                .andRespond(withSuccess("""
                        {
                          "status": "0",
                          "info": "INVALID_USER_IP",
                          "infocode": "10009"
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.searchByKeyword("cafe", null, 1, 20))
                .isInstanceOf(AmapServiceException.class)
                .hasMessageContaining("INVALID_USER_IP");
        server.verify();
    }
}
