package com.dysonos.integration;

import com.dysonos.dto.DistributionPlanRequest;
import com.dysonos.dto.DistributionPlanResponse;
import com.dysonos.dto.WorkloadRequest;
import com.dysonos.dto.WorkloadResponse;
import com.dysonos.model.Node;
import com.dysonos.model.WorkloadStatus;
import com.dysonos.model.Zone;
import com.dysonos.repository.NodeRepository;
import com.dysonos.repository.WorkloadRepository;
import com.dysonos.repository.ZoneRepository;
import com.dysonos.service.WorkloadService;
import com.dysonos.service.ZoneDistributionService;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class PostgresWorkloadIntegrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("dysonos_test")
            .withUsername("dysonos_test")
            .withPassword("dysonos_test");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        properties.add("spring.datasource.username", POSTGRES::getUsername);
        properties.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private Flyway flyway;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ZoneRepository zoneRepository;

    @Autowired
    private NodeRepository nodeRepository;

    @Autowired
    private WorkloadRepository workloadRepository;

    @Autowired
    private WorkloadService workloadService;

    @Autowired
    private ZoneDistributionService zoneDistributionService;

    @Test
    @Transactional
    void migrationsPersistWorkloadsAndReleaseReservationsWhenStopped() {
        assertEquals("2", flyway.info().current().getVersion().getVersion());
        assertEquals("workloads", jdbcTemplate.queryForObject(
                "select to_regclass('public.workloads')::text", String.class));
        assertEquals("workload_allocations", jdbcTemplate.queryForObject(
                "select to_regclass('public.workload_allocations')::text", String.class));

        Zone zone = zoneRepository.saveAndFlush(new Zone("integration-zone"));
        Node node = nodeRepository.saveAndFlush(new Node("integration-node", 10, 20, zone));

        WorkloadResponse created = workloadService.create(zone.getId(),
                new WorkloadRequest("integration-workload", 30.0, 20.0));

        assertEquals(WorkloadStatus.ACTIVE, created.status());
        assertEquals(node.getId(), created.allocations().getFirst().nodeId());
        // JDBC does not trigger Hibernate's automatic flush, so persist pending JPA writes first.
        workloadRepository.flush();
        assertEquals(1, jdbcTemplate.queryForObject("select count(*) from workload_allocations", Integer.class));

        DistributionPlanResponse activePlan = zoneDistributionService.plan(zone.getId(),
                new DistributionPlanRequest(20.0, 15.0));
        assertTrue(activePlan.feasible());
        assertEquals(39.99, activePlan.availableCpu(), 0.000001);
        assertEquals(39.99, activePlan.availableMemory(), 0.000001);

        WorkloadResponse stopped = workloadService.stop(created.id());

        assertEquals(WorkloadStatus.STOPPED, stopped.status());
        assertNotNull(workloadRepository.findById(created.id()).orElse(null));

        DistributionPlanResponse releasedPlan = zoneDistributionService.plan(zone.getId(),
                new DistributionPlanRequest(20.0, 15.0));
        assertTrue(releasedPlan.feasible());
        assertEquals(69.99, releasedPlan.availableCpu(), 0.000001);
        assertEquals(59.99, releasedPlan.availableMemory(), 0.000001);
    }
}
