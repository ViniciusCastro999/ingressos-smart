package com.viniciuscastro.ingressos.data.local

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "events")
data class EventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String,
    val venue: String,
    val city: String,
    @ColumnInfo(name = "starts_at") val startsAtEpochMillis: Long,
    @ColumnInfo(name = "price_cents") val priceCents: Long,
    @ColumnInfo(name = "available_tickets") val availableTickets: Int,
)

/** The primary key is the checkout idempotency key: a second insert with the same key is rejected. */
@Entity(tableName = "purchases", indices = [Index("status")])
data class PurchaseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "event_id") val eventId: String,
    @ColumnInfo(name = "event_title") val eventTitle: String,
    @ColumnInfo(name = "event_venue") val eventVenue: String,
    @ColumnInfo(name = "event_starts_at") val eventStartsAtEpochMillis: Long,
    val quantity: Int,
    @ColumnInfo(name = "unit_price_cents") val unitPriceCents: Long,
    @ColumnInfo(name = "payment_method") val paymentMethod: String?,
    val status: String,
    @ColumnInfo(name = "created_at") val createdAtEpochMillis: Long,
    @ColumnInfo(name = "updated_at") val updatedAtEpochMillis: Long,
    @ColumnInfo(name = "reason_type") val reasonType: String?,
    @ColumnInfo(name = "reason_code") val reasonCode: Int?,
    @ColumnInfo(name = "reason_message") val reasonMessage: String?,
    @Embedded(prefix = "receipt_") val receipt: ReceiptEntity?,
)

/** All columns nullable: Room maps a row whose receipt columns are all null to `receipt = null`. */
data class ReceiptEntity(
    @ColumnInfo(name = "terminal_order_id") val terminalOrderId: String?,
    @ColumnInfo(name = "paid_amount_cents") val paidAmountCents: Long?,
    @ColumnInfo(name = "authorization_code") val authorizationCode: String?,
    val nsu: String?,
    @ColumnInfo(name = "card_brand") val cardBrand: String?,
    @ColumnInfo(name = "card_mask") val cardMask: String?,
    @ColumnInfo(name = "product_name") val productName: String?,
    val installments: Int?,
    @ColumnInfo(name = "terminal_id") val terminalId: String?,
)

@Entity(
    tableName = "tickets",
    foreignKeys = [
        ForeignKey(
            entity = PurchaseEntity::class,
            parentColumns = ["id"],
            childColumns = ["purchase_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("purchase_id")],
)
data class TicketEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "purchase_id") val purchaseId: String,
    @ColumnInfo(name = "event_id") val eventId: String,
    val number: Int,
)

data class PurchaseWithTickets(
    @Embedded val purchase: PurchaseEntity,
    @Relation(parentColumn = "id", entityColumn = "purchase_id")
    val tickets: List<TicketEntity>,
)
