package com.viniciuscastro.ingressos.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.viniciuscastro.ingressos.ui.checkout.CheckoutRoute
import com.viniciuscastro.ingressos.ui.events.EventsRoute
import com.viniciuscastro.ingressos.ui.purchases.PurchasesRoute
import com.viniciuscastro.ingressos.ui.receipt.ReceiptRoute

@Composable
fun IngressosNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = EventsDestination) {
        composable<EventsDestination> {
            EventsRoute(
                onEventClick = { eventId -> navController.navigate(CheckoutDestination(eventId)) },
                onTicketsClick = {
                    navController.navigate(PurchasesDestination) {
                        popUpTo<EventsDestination>()
                        launchSingleTop = true
                    }
                },
                onPendingPurchaseClick = navController::showPurchase,
            )
        }
        composable<CheckoutDestination> {
            CheckoutRoute(
                onBack = navController::navigateUp,
                // The checkout is popped: going back never re-opens a checkout whose key was already used.
                onShowReceipt = navController::showPurchase,
            )
        }
        composable<ReceiptDestination> {
            ReceiptRoute(
                onBack = navController::navigateUp,
                onRetry = { eventId, quantity ->
                    // A new checkout entry means a new idempotency key: a retry is a new, deliberate attempt.
                    navController.navigate(CheckoutDestination(eventId, quantity)) {
                        popUpTo<EventsDestination>()
                    }
                },
                onDone = { navController.popBackStack<EventsDestination>(inclusive = false) },
            )
        }
        composable<PurchasesDestination> {
            PurchasesRoute(
                onHomeClick = { navController.popBackStack<EventsDestination>(inclusive = false) },
                onPurchaseClick = { purchaseId -> navController.navigate(ReceiptDestination(purchaseId)) },
            )
        }
    }
}

/** Shows a purchase on top of the event list, whatever screen is currently open. */
fun NavHostController.showPurchase(purchaseId: String) {
    val current = currentBackStackEntry
    val alreadyShown = current?.destination?.hasRoute<ReceiptDestination>() == true &&
        current.toRoute<ReceiptDestination>().purchaseId == purchaseId
    if (alreadyShown) return
    navigate(ReceiptDestination(purchaseId)) {
        popUpTo<EventsDestination>()
    }
}
