package ar.com.lamontana.mobile

import ar.com.lamontana.mobile.model.RiderOrder
import ar.com.lamontana.mobile.model.RiderRoute
import org.junit.Assert.*
import org.junit.Test

class RiderBoardTest {
    private fun order(id: String, mode: String = "RETIRO_PUNTO_ENTREGA", status: String = "EN_VIAJE",
        point: String = "palermo", presented: Boolean = false, due: String? = null, finished: String? = null) =
        RiderOrder(id, id.filter(Char::isDigit).toIntOrNull() ?: 1, 3, mode, "Punto Palermo · Av. Santa Fe 3200",
            point, "Lucía Fernández", status, presented, due, finished)
    private fun board(vararg orders: RiderOrder) = riderBoard(listOf(RiderRoute("ruta", orders.toList())))

    @Test fun currentAndDeliveredNeverMixEvenAtTheSamePoint() {
        val board = board(order("p1"), order("p2", status = "DISPONIBLE_PUNTO", finished = "2026-11-07T15:00:00Z"))
        assertEquals(listOf(1), board.stops(RiderSection.CURRENT).single().orders)
        assertEquals(listOf(2), board.stops(RiderSection.DELIVERED).single().orders)
        assertNotEquals(board.pending.single().key, board.delivered.single().key)
        assertEquals(1, board.pendingOrders); assertEquals(1, board.deliveredOrders)
    }

    @Test fun presentedOrdersWaitForPointReceptionInsteadOfLookingDelivered() {
        val board = board(order("p1", presented = true))
        assertEquals("Pendiente de recepción", board.pending.single().status)
        assertFalse(board.pending.single().items.single().canPresent)
        assertTrue(board.delivered.isEmpty())
    }

    @Test fun pendingOrdersFromPreviousDaysAreNotHidden() {
        val board = board(order("p1", due = "2026-10-31T18:00:00Z"))
        assertEquals(1, board.pendingOrders)
        assertEquals(1, board.stops(RiderSection.POINTS).size)
    }

    @Test fun homeDeliveriesComeFirstAndUseTheClosestDeadline() {
        val board = board(order("p1"), order("p2", "ENVIO_DOMICILIO", due = "2026-11-07T20:00:00Z"),
            order("p3", "ENVIO_DOMICILIO", due = "2026-11-07T18:00:00Z"))
        assertEquals(listOf(3, 2, 1), board.pending.flatMap { it.orders })
        assertEquals(listOf(1, 2, 3), board.pending.map { it.order })
        assertEquals(2, board.homeOrders)
    }

    @Test fun absentCustomerRemainsPendingAndCanBeFoundInHomeView() {
        val board = board(order("p1", "ENVIO_DOMICILIO", status = "CLIENTE_AUSENTE"))
        assertEquals("Sin receptor", board.stops(RiderSection.HOME).single().status)
        assertTrue(board.delivered.isEmpty())
    }

    @Test fun pointViewIncludesTransfersToAnotherBranchAndExcludesHome() {
        val board = board(order("p1"), order("p2", "RETIRO_SUCURSAL"), order("p3", "ENVIO_DOMICILIO"))
        assertEquals(listOf(1, 2), board.stops(RiderSection.POINTS).flatMap { it.orders })
        assertEquals(listOf(3), board.stops(RiderSection.HOME).flatMap { it.orders })
    }

    @Test fun cancellationsAndUnknownStatesAreNotCountedAsDelivered() {
        val board = board(order("p1", status = "CANCELADO", finished = "2026-11-07T15:00:00Z"),
            order("p2", status = "DESCONOCIDO", finished = "2026-11-07T15:00:00Z"))
        assertTrue(board.pending.isEmpty()); assertTrue(board.delivered.isEmpty())
    }

    @Test fun deliveryRequiresAConfirmedCompletionTimeFromTheServer() {
        assertTrue(board(order("p1", status = "ENTREGADO")).delivered.isEmpty())
    }

    @Test fun completedStopsAreOrderedByMostRecentConfirmation() {
        val board = board(order("p1", status = "ENTREGADO", point = "a", finished = "2026-11-07T15:00:00Z"),
            order("p2", status = "ENTREGADO", point = "b", finished = "2026-11-07T16:00:00Z"))
        assertEquals(listOf(2, 1), board.delivered.flatMap { it.orders })
    }

    @Test fun searchFindsCustomerAddressAndExactOrderAcrossEveryView() {
        val board = board(order("p133"), order("p135", "ENVIO_DOMICILIO"))
        assertEquals(2, board.stops(RiderSection.CURRENT, "lucía").size)
        assertEquals(2, board.stops(RiderSection.CURRENT, "Santa Fe").size)
        assertEquals(listOf(133), board.stops(RiderSection.CURRENT, " PED-133 ").single().orders)
        assertEquals(listOf(135), board.stops(RiderSection.CURRENT, "135").single().orders)
        assertTrue(board.stops(RiderSection.CURRENT, "otra persona").isEmpty())
    }

    @Test fun separateRoutesToOnePointKeepSeparatePresentationCommands() {
        val board = riderBoard(listOf(RiderRoute("ruta1", listOf(order("p1"))), RiderRoute("ruta2", listOf(order("p2")))))
        assertEquals(2, board.pending.size)
        assertEquals(2, board.pending.map { it.key }.toSet().size)
        assertEquals(listOf("ruta1", "ruta2"), board.pending.map { it.routeId })
    }

    @Test fun largeManifestKeepsEveryOrderAndGroupsThePoint() {
        val board = board(*(1..250).map { order("p$it") }.toTypedArray())
        assertEquals(250, board.pendingOrders); assertEquals(1, board.pending.size)
        assertEquals(250, board.pending.single().items.size)
    }
}
