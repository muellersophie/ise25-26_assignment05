package de.seuhd.campuscoffee.acctest;

import de.seuhd.campuscoffee.api.dtos.PosDto;
import de.seuhd.campuscoffee.domain.model.CampusType;
import de.seuhd.campuscoffee.domain.model.PosType;
import de.seuhd.campuscoffee.domain.ports.PosService;
import io.cucumber.java.*;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;
import io.cucumber.spring.CucumberContextConfiguration;
import io.restassured.RestAssured;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.List;
import java.util.Map;

import static de.seuhd.campuscoffee.TestUtils.*;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Step definitions for the POS Cucumber tests.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@CucumberContextConfiguration
public class CucumberPosSteps {
    static final PostgreSQLContainer<?> postgresContainer;

    static {
        // share the same testcontainers instance across all Cucumber tests
        postgresContainer = getPostgresContainer();
        postgresContainer.start();
        // testcontainers are automatically stopped when the JVM exits
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        configurePostgresContainers(registry, postgresContainer);
    }

    @Autowired
    protected PosService posService;

    @LocalServerPort
    private Integer port;

    @Before
    public void beforeEach() {
        posService.clear();
        RestAssured.baseURI = "http://localhost:" + port;
    }

    @After
    public void afterEach() {
        posService.clear();
    }

    private List<PosDto> createdPosList;
    private PosDto updatedPos;

    /**
     * Register a Cucumber DataTable type for PosDto.
     * @param row the DataTable row to map to a PosDto object
     * @return the mapped PosDto object
     */
    @DataTableType
    @SuppressWarnings("unused")
    public PosDto toPosDto(Map<String,String> row) {
        return PosDto.builder()
                .name(row.get("name"))
                .description(row.get("description"))
                .type(PosType.valueOf(row.get("type")))
                .campus(CampusType.valueOf(row.get("campus")))
                .street(row.get("street"))
                .houseNumber(row.get("houseNumber"))
                .postalCode(Integer.parseInt(row.get("postalCode")))
                .city(row.get("city"))
                .build();
    }

    // Given -----------------------------------------------------------------------

    @Given("an empty POS list")
    public void anEmptyPosList() {
        List<PosDto> retrievedPosList = retrievePos();
        assertThat(retrievedPosList).isEmpty();
    }

    // TODO: Add Given step for new scenario
    @Given("a POS list that contains 3 POS entries, a POS that is to be updated + its name")
    public void aPosListThatContainsThreePOSAndAPosToUpdateAndItsName(List<PosDto> posList) {
        List<PosDto> retrievedPosList = retrievePos();
        assertThat(retrievedPosList).isEmpty();
        createdPosList = createPos(posList);
        assertThat(createdPosList).size().isEqualTo(posList.size());
        retrievedPosList = retrievePos();
        assertThat(retrievedPosList).size().isEqualTo(3);
        assertThat(retrievedPosList)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdAt", "updatedAt")
                .containsExactlyInAnyOrderElementsOf(createdPosList);
    }

    // When -----------------------------------------------------------------------

    @When("I insert POS with the following elements")
    public void insertPosWithTheFollowingValues(List<PosDto> posList) {
        createdPosList = createPos(posList);
        assertThat(createdPosList).size().isEqualTo(posList.size());
    }

    @When("I select the POS with the following name")
    public void selectThePOSWithTheFollowingName(String name) {
        assertThat(name != null && !name.isEmpty());
        name = name.trim();
        updatedPos = retrievePosByName(name);
        //Ich verstehe nicht, warum hier der Status-Code <500> ausgegeben wird. Hat der Name irgendwelche Leerzeichen?
        assertThat(updatedPos).isNotNull();
    }
    // TODO: Add When step for new scenario
    @When("I update one POS with the following elements")
    public void updatePosWithTheFollowingValuesAtGivenName(List<PosDto> posListContainingPosToUpdate) {
        PosDto posToUpdate = posListContainingPosToUpdate.getFirst();
        assertThat(posToUpdate).isNotNull();
        assert updatedPos.id() != null;
        for (PosDto posDto : createdPosList) {
            assert posDto.id() != null;
            if (posDto.id().equals(updatedPos.id())) {
                posDto = posToUpdate;
                assertThat(posDto).isNotNull();
            }
        }
    }

    // Then -----------------------------------------------------------------------

    @Then("the POS list should contain the same elements in the same order")
    public void thePosListShouldContainTheSameElementsInTheSameOrder() {
        List<PosDto> retrievedPosList = retrievePos();
        assertThat(retrievedPosList)
                .usingRecursiveFieldByFieldElementComparatorIgnoringFields("id", "createdAt", "updatedAt")
                .containsExactlyInAnyOrderElementsOf(createdPosList);
    }

    // TODO: Add Then step for new scenario
    @Then("the POS list should contain the same elements in the same order except for the updated POS")
    public void thePosListShouldContainTheSameElementsInTheSameOrderExceptForTheUpdatedPos() {
        List<PosDto> retrievedPosList = retrievePos();
        assertThat(updatedPos.id()).isNotNull();
        for (PosDto posDto : retrievedPosList) {
            assert posDto.id() != null;
            if (posDto.id().equals(updatedPos.id())) {
                assertThat(posDto).isNotNull();
                assertThat(retrievedPosList.get(Math.toIntExact(updatedPos.id())))
                        .isNotEqualTo(createdPosList.get(Math.toIntExact(updatedPos.id())));
            } else  {
                assertThat(retrievedPosList.get(Math.toIntExact(updatedPos.id())))
                        .isEqualTo(createdPosList.get(Math.toIntExact(updatedPos.id())));
            }
        }
    }
}
