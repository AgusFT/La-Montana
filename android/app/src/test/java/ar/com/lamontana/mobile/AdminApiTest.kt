package ar.com.lamontana.mobile

import ar.com.lamontana.mobile.data.AdminApi
import ar.com.lamontana.mobile.model.*
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AdminApiTest {
    private val id = "79cf1686-eb5f-46f1-8b84-84d027caf3ba"
    private val operation = "6bd73366-e95e-4299-afd9-08c9b1831805"
    private fun api(response: String = "{}") = AdminApi({ response }, { _, _, _ -> response })
    private fun printer() = AdminPrinter(id, id, "La Plata", "Canon", "OPERATIVA", false, true, null, 100, 0, null, null)
    private fun location(status: String = "ABIERTA") = AdminAvailability(id, "La Plata", "Calle 7", status, 0, null, null)
    private fun profile(role: String = "ADMIN_ADMIN", rider: Boolean = true) = Profile("Ale", "Prueba", "ale@prueba.local", role, false, false, rider)

    @Test fun adminCanAlternateBetweenGlobalWorkspacesAndOwnRoute() {
        AdminSection.entries.forEach { assertTrue(profile().canOpen(it)) }
    }
    @Test fun employeeCannotOpenGlobalAdministration() {
        AdminSection.entries.forEach { assertFalse(profile("EMPLEADO").canOpen(it)) }
    }
    @Test fun adminWithoutRiderAccessKeepsCounterAndManagement() {
        assertFalse(profile(rider = false).canOpen(AdminSection.RIDER))
        assertTrue(profile(rider = false).canOpen(AdminSection.PRINTERS))
    }
    @Test fun initialPasswordCannotOpenAdminModes() {
        assertFalse(profile().copy(debeCambiarContrasena = true).canOpen(AdminSection.PAUSES))
    }
    @Test fun disablingUsesOperationalEndpointAndExpectedVersion() {
        val c = api().printerCommand(printer(), false, operation)
        assertEquals("POST", c.method)
        assertEquals("/api/operacion/impresoras/$id", c.path)
        assertEquals("DESHABILITAR", JSONObject(c.body).getString("accion"))
        assertEquals(0, JSONObject(c.body).getLong("version"))
        assertEquals(operation, JSONObject(c.body).getString("operacion"))
    }
    @Test(expected = IllegalArgumentException::class) fun retiredPrinterCannotChange() {
        api().printerCommand(printer().copy(retired = true), false, operation)
    }
    @Test(expected = IllegalArgumentException::class) fun structurallyDisabledPrinterCannotEnable() {
        api().printerCommand(printer().copy(enabled = false, configuredState = "DESHABILITADA"), true, operation)
    }
    @Test fun branchPauseDoesNotDeactivateOrEditBranchDefinition() {
        val c = api().availabilityCommand(AdminResource.BRANCH, location(), true, operation)
        assertEquals("${AdminApi.BRANCHES}/$id", c.path)
        assertEquals("PUT", c.method)
        assertEquals("PAUSADA", JSONObject(c.body).getString("estado"))
        assertFalse(JSONObject(c.body).has("nombre"))
    }
    @Test fun pointPauseReusesWebAvailability() {
        val c = api().availabilityCommand(AdminResource.POINT, location("HABILITADO"), true, operation)
        assertEquals("${AdminApi.POINTS}/$id", c.path)
        assertEquals("DESHABILITADO", JSONObject(c.body).getString("estado"))
    }
    @Test fun undefinedPointMustBeExplicitlyEnabled() {
        val c = api().availabilityCommand(AdminResource.POINT, location("SIN_DEFINIR"), false, operation)
        assertEquals("HABILITADO", JSONObject(c.body).getString("estado"))
    }
    @Test fun retryUsesIdenticalCommandAndAcceptsCurrentStateAfterAnotherOperator() {
        val calls = mutableListOf<Triple<String, String, String>>()
        val api = AdminApi({ "" }, { p, m, b -> calls += Triple(p, m, b); """{"sucursal":"$id","estado":"ABIERTA","version":2}""" })
        val c = api.availabilityCommand(AdminResource.BRANCH, location(), true, operation)
        api.execute(c); api.execute(c)
        assertEquals(calls[0], calls[1])
    }
    @Test(expected = IllegalArgumentException::class) fun responseForDifferentResourceCannotConfirmChange() {
        val a = api("""{"sucursal":"otro","estado":"PAUSADA","version":1}""")
        a.execute(a.availabilityCommand(AdminResource.BRANCH, location(), true, operation))
    }
    @Test(expected = IllegalArgumentException::class) fun unchangedVersionCannotConfirmChange() {
        val a = api("""{"sucursal":"$id","estado":"PAUSADA","version":0}""")
        a.execute(a.availabilityCommand(AdminResource.BRANCH, location(), true, operation))
    }
    @Test fun unassignedPrinterAndUnknownPaperAreDisplayedWithoutInventingCount() {
        val p = api("""{"puedeGestionar":true,"impresoras":[{"codigoPublico":"$id","sucursal":null,"ubicacion":null,"nombre":"Canon","estadoConfigurado":"OPERATIVA","retirada":false,"capacidadHojas":100,"operacion":{"habilitada":true,"hojas":null,"version":0,"actor":null,"actualizadaEn":null}}]}""").printers().printers.single()
        assertNull(p.sheets); assertNull(p.branch); assertEquals("Sin sucursal asignada", p.location)
    }
    @Test fun branchAvailabilityReadsAuditAndState() {
        val b = api("""[{"codigoPublico":"$id","nombre":"La Plata","direccion":"Calle 7","estado":"PAUSADA","version":1,"actor":"Ale","actualizadaEn":"2026-11-07T18:00:00Z"}]""").branches().single()
        assertEquals("PAUSADA", b.status); assertEquals("Ale", b.actor)
    }
    @Test fun inheritedPointPauseIsVisibleWithoutRewritingItsManualDecision() {
        val p = api("""{"puntos":[{"codigoPublico":"$id","nombre":"City Bell","direccion":"Centenario","disponibilidad":{"estado":"HABILITADO","version":4,"actor":null,"actualizadaEn":null},"disponibleParaPedidos":false,"motivo":"Momentáneamente fuera de servicio","sucursales":[{"codigoPublico":"$id","nombre":"La Plata","disponible":false}]}]}""").points().single()
        assertEquals("HABILITADO", p.status); assertEquals(4, p.version); assertTrue(p.outOfService)
        assertEquals("La Plata", p.origins.single().name); assertFalse(p.origins.single().available)
    }
    @Test fun sharedPointRemainsAvailableThroughAnotherBranch() {
        val p = location("HABILITADO").copy(available = true, origins = listOf(AdminOrigin(id, "Floresta", true)))
        assertFalse(p.outOfService)
    }
    @Test fun manualPointPauseStillBlocksEvenWithAvailableBranch() {
        assertTrue(location("DESHABILITADO").copy(available = false, origins = listOf(AdminOrigin(id, "Floresta", true))).outOfService)
    }
}

