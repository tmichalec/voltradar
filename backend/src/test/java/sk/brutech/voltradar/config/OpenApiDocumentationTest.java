package sk.brutech.voltradar.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import sk.brutech.voltradar.ingestion.zse.ZseDataIngestionService;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class OpenApiDocumentationTest {

    @Autowired
    private WebApplicationContext webApplicationContext;

    @MockitoBean
    private ZseDataIngestionService zseDataIngestionService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
    }

    @Test
    void generatesValidOpenApiDocs() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.info.title").value("VoltRadar API"))
                .andExpect(jsonPath("$.info.version").value("0.1.0-SNAPSHOT"))
                .andExpect(jsonPath("$.paths['/api/v1/ingestion/zse/bratislava']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ingestion/zse/viewport']").exists())
                .andExpect(jsonPath("$.paths['/api/v1/ingestion/zse/stations/{stationId}']").exists());
    }
}
