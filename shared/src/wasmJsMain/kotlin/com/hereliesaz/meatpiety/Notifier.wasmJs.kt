package com.hereliesaz.meatpiety

// Browsers can't run the daily check with the page closed, so the web build
// never notifies.
actual object Notifier {
    actual fun foreground(snapshot: LedgerSnapshot, notices: List<Notice>) = Unit
}
